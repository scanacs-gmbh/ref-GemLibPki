/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.pki.certificate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import de.gematik.pki.common.OcspResponderMock;
import de.gematik.pki.error.ErrorCode;
import de.gematik.pki.exception.GemPkiException;
import de.gematik.pki.ocsp.OcspRequestGenerator;
import de.gematik.pki.ocsp.OcspRespCache;
import de.gematik.pki.tsl.TslInformationProvider;
import de.gematik.pki.tsl.TslReader;
import de.gematik.pki.tsl.TspService;
import de.gematik.pki.utils.CertificateProvider;
import de.gematik.pki.utils.ResourceReader;
import de.gematik.pki.utils.VariableSource;
import eu.europa.esig.trustedlist.jaxb.tsl.AttributedNonEmptyURIType;
import eu.europa.esig.trustedlist.jaxb.tsl.ServiceSupplyPointsType;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import lombok.SneakyThrows;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

class TucPki018VerifierTest {

    private static TucPki018Verifier tucPki018Verifier;
    private static X509Certificate VALID_X509_EE_CERT;
    private static final String FILE_NAME_TSL_DEFAULT = "tsls/valid/TSL_default.xml";
    private static final CertificateProfile certificateProfile = CertificateProfile.C_HCI_AUT_ECC;
    private static final List<CertificateProfile> certificateProfiles = List.of(certificateProfile);
    private static final OcspRespCache ocspRespCache = new OcspRespCache(30);
    private final static String LOCAL_SSP_DIR = "/services/ocsp";
    private final static String OCSP_HOST = "http://localhost:";
    private static OcspResponderMock ocspResponderMock;
    private final static String PRODUCT_TYPE_IDP = "IDP";

    @BeforeAll
    public static void start() throws Exception {
        ocspResponderMock = new OcspResponderMock(LOCAL_SSP_DIR, OCSP_HOST);
        VALID_X509_EE_CERT = CertificateProvider
            .getX509Certificate("src/test/resources/certificates/GEM.SMCB-CA10/valid/DrMedGunther.pem");
        configureOcspResponderMockForOcspRequest(VALID_X509_EE_CERT);
        tucPki018Verifier = buildTucPki18Verifier(certificateProfiles);
    }

    @SneakyThrows
    private static void configureOcspResponderMockForOcspRequest(final X509Certificate x509EeCert) {
        final X509Certificate VALID_X509_ISSUER_CERT = CertificateProvider
            .getX509Certificate("src/test/resources/certificates/GEM.RCA1_TEST-ONLY.pem");
        final OCSPReq ocspReq = OcspRequestGenerator.generateSingleOcspRequest(x509EeCert,
            VALID_X509_ISSUER_CERT);
        ocspResponderMock.configureForOcspRequest(ocspReq);
    }

    private static TucPki018Verifier buildTucPki18Verifier(final List<CertificateProfile> certificateProfiles)
        throws GemPkiException, IOException {

        final List<TspService> tspServiceList = new TslInformationProvider(
            TslReader.getTsl(ResourceReader.getFilePathFromResources(FILE_NAME_TSL_DEFAULT)).orElseThrow())
            .getTspServices();

        overwriteSspUrls(tspServiceList);

        return TucPki018Verifier.builder()
            .productType(PRODUCT_TYPE_IDP)
            .tspServiceList(tspServiceList)
            .certificateProfiles(certificateProfiles)
            .ocspRespCache(ocspRespCache)
            .build();
    }

    @SneakyThrows
    private static void overwriteSspUrls(final List<TspService> tspServiceList) {
        final ServiceSupplyPointsType serviceSupplyPointsType = new ServiceSupplyPointsType();
        final AttributedNonEmptyURIType newSspUrl = new AttributedNonEmptyURIType();
        newSspUrl.setValue(ocspResponderMock.getSspUrl());
        serviceSupplyPointsType.getServiceSupplyPoint().add(newSspUrl);
        tspServiceList.forEach(tspService -> tspService.getTspServiceType().getServiceInformation()
            .setServiceSupplyPoints(serviceSupplyPointsType));
    }

    @Test
    void verifyPerformTucPki18ChecksValid() {
        assertDoesNotThrow(() -> tucPki018Verifier.performTucPki18Checks(VALID_X509_EE_CERT));
    }

    @Test
    void verifyEgkAutEccCertValid() {
        assertDoesNotThrow(() -> buildTucPki18Verifier(List.of(CertificateProfile.C_CH_AUT_ECC))
            .performTucPki18Checks(CertificateProvider
                .getX509Certificate("src/test/resources/certificates/GEM.EGK-CA10/JunaFuchs.pem")));
    }

    @Test
    void verifyHbaAutEccCertValid() {
        assertDoesNotThrow(() -> buildTucPki18Verifier(List.of(CertificateProfile.C_HP_AUT_ECC))
            .performTucPki18Checks(CertificateProvider
                .getX509Certificate("src/test/resources/certificates/GEM.HBA-CA13/GüntherOtís.pem")));
    }

    @Test
    void verifySmcbAutRsaCertValid() {
        assertDoesNotThrow(() -> buildTucPki18Verifier(List.of(CertificateProfile.C_HCI_AUT_RSA))
            .performTucPki18Checks(CertificateProvider
                .getX509Certificate("src/test/resources/certificates/GEM.SMCB-CA24-RSA/AschoffscheApotheke.pem")));
    }

    @Test
    void verifySigDCertValid() {
        assertDoesNotThrow(() -> buildTucPki18Verifier(List.of(CertificateProfile.C_FD_SIG))
            .performTucPki18Checks(CertificateProvider.getX509Certificate(
                "src/test/resources/certificates/GEM.KOMP-CA10/c.fd.sig_keyUsage_digiSig.pem")));
    }

    @Test
    void verifySmcbOsigRsaCertValid() {
        assertDoesNotThrow(() -> buildTucPki18Verifier(List.of(CertificateProfile.C_HCI_OSIG))
            .performTucPki18Checks(CertificateProvider.getX509Certificate(
                "src/test/resources/certificates/GEM.SMCB-CA24-RSA/c-hci-osig_apo.valid.crt")));
    }

    @Test
    void verifyProfessionOidsValid() throws IOException, GemPkiException, CertificateEncodingException, URISyntaxException {
        final X509Certificate cert = CertificateProvider.getX509Certificate("src/test/resources/certificates/GEM.SMCB-CA24-RSA/c-hci-osig_apo.valid.crt");
        assertThat(buildTucPki18Verifier(List.of(CertificateProfile.C_HCI_OSIG))
            .performTucPki18Checks(cert).getProfessionOids()).contains(Role.OID_OEFFENTLICHE_APOTHEKE.getProfessionOid());
    }

    @Test
    void verifyNotEveryKeyUsagePresent() {
        assertThatThrownBy(() -> tucPki018Verifier.performTucPki18Checks(CertificateProvider
            .getX509Certificate("src/test/resources/certificates/GEM.SMCB-CA24-RSA/AschoffscheApotheke.pem")))
            .isInstanceOf(GemPkiException.class)
            .hasMessageContaining(ErrorCode.SE_1016.name());
    }

    @Test
    void multipleCertificateProfiles_shouldSelectCorrectOne() {
        assertDoesNotThrow(() -> buildTucPki18Verifier(List.of(
            CertificateProfile.C_TSL_SIG_ECC, CertificateProfile.C_HCI_AUT_RSA, CertificateProfile.C_HCI_AUT_ECC
        )).performTucPki18Checks(VALID_X509_EE_CERT));
    }

    @Test
    void multipleCertificateProfiles_shouldThrowKeyUsageError() throws IOException {
        final X509Certificate eeWrongKeyUsage = CertificateProvider
            .getX509Certificate(
                "src/test/resources/certificates/GEM.SMCB-CA10/invalid/DrMedGunther_invalid-keyusage.pem");
        assertThatThrownBy(
            () -> buildTucPki18Verifier(List.of(CertificateProfile.C_HCI_AUT_ECC, CertificateProfile.C_HP_AUT_ECC
            )).performTucPki18Checks(eeWrongKeyUsage))
            .isInstanceOf(GemPkiException.class)
            .hasMessageContaining(ErrorCode.SE_1016.name());
    }

    @Test
    void multipleCertificateProfiles_shouldThrowCertTypeError() throws IOException {
        final X509Certificate eeWrongKeyUsage = CertificateProvider
            .getX509Certificate(
                "src/test/resources/certificates/GEM.SMCB-CA10/invalid/DrMedGunther_invalid-certificate-type.pem");
        assertThatThrownBy(
            () -> buildTucPki18Verifier(List.of(CertificateProfile.C_HCI_AUT_ECC, CertificateProfile.C_HP_AUT_ECC
            )).performTucPki18Checks(eeWrongKeyUsage))
            .isInstanceOf(GemPkiException.class)
            .hasMessageContaining(ErrorCode.SE_1018.name());
    }

    @Test
    void verifyCertNull() {
        assertThatThrownBy(() -> tucPki018Verifier.performTucPki18Checks(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("x509EeCert");
    }

    @Test
    void verifyCertProfilesNull() {
        assertThatThrownBy(() -> buildTucPki18Verifier(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("certificateProfiles");
    }

    @Test
    void verifyCertProfilesEmpty() {
        assertThatThrownBy(() -> buildTucPki18Verifier(List.of()).performTucPki18Checks(VALID_X509_EE_CERT))
            .isInstanceOf(GemPkiException.class)
            .hasMessageContaining(ErrorCode.UNKNOWN.name());
    }

    @ParameterizedTest
    @ArgumentsSource(CertificateProvider.class)
    @VariableSource(value = "valid")
    void verifyPerformTucPki18ChecksValid(final X509Certificate cert) {
        assertDoesNotThrow(() -> tucPki018Verifier.performTucPki18Checks(cert));
    }

    @ParameterizedTest
    @ArgumentsSource(CertificateProvider.class)
    @VariableSource(value = "invalid")
    void verifyPerformTucPki18ChecksInvalid(final X509Certificate cert) {
        assertThatThrownBy(() -> tucPki018Verifier.performTucPki18Checks(cert))
            .as("Test invalid certificates")
            .isInstanceOf(GemPkiException.class);
    }
}

