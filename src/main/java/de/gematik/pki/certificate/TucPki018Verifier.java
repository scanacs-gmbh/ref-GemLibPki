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

import de.gematik.pki.error.ErrorCode;
import de.gematik.pki.exception.GemPkiException;
import de.gematik.pki.exception.GemPkiParsingException;
import de.gematik.pki.ocsp.OcspRespCache;
import de.gematik.pki.ocsp.OcspTransceiver;
import de.gematik.pki.tsl.TspInformationProvider;
import de.gematik.pki.tsl.TspService;
import de.gematik.pki.tsl.TspServiceSubset;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.EnumMap;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Entry point to access a verification of certificate(s) regarding standard process called TucPki18. This class works with parameterized variables (defined by
 * builder pattern) and with given variables provided by runtime (method parameters).
 */

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class TucPki018Verifier {

    @NonNull
    protected final String productType;
    @NonNull
    protected final List<TspService> tspServiceList;
    @NonNull
    protected final List<CertificateProfile> certificateProfiles;
    @Builder.Default
    protected final boolean withOcspCheck = true;
    protected final OcspRespCache ocspRespCache;

    /**
     * Verify given end-entity certificate against a list of parameterized certificate profiles {@link CertificateProfile}. If there is no {@link
     * GemPkiException} the verification process ends successfully.
     *
     * @param x509EeCert end-entity certificate to check
     * @return the determined {@link Admission}
     * @throws GemPkiException if the certificate is invalid
     */
    public Admission performTucPki18Checks(@NonNull final X509Certificate x509EeCert) throws GemPkiException {
        log.debug("TucPki018Checks including OCSP");
        final TspServiceSubset tspServiceSubset = new TspInformationProvider(tspServiceList, productType).getTspServiceSubset(x509EeCert);
        if (withOcspCheck) {
            doOcsp(x509EeCert, tspServiceSubset);
        } else {
            log.info("Ocsp verification turned off!");
        }
        tucPki018CommonChecks(x509EeCert, tspServiceSubset);
        return tucPki018ProfileChecks(x509EeCert, tspServiceSubset);
    }

    protected Admission tucPki018ProfileChecks(@NonNull final X509Certificate x509EeCert, @NonNull final TspServiceSubset tspServiceSubset)
        throws GemPkiException {
        if (certificateProfiles.isEmpty()) {
            throw new GemPkiException(productType, ErrorCode.UNKNOWN);
        }

        final EnumMap<CertificateProfile, GemPkiException> errors = new EnumMap<>(CertificateProfile.class);
        for (final CertificateProfile certificateProfile : certificateProfiles) {
            try {
                tucPki018ChecksForProfile(x509EeCert, certificateProfile, tspServiceSubset);
                log.debug("Übergebenes Zertifikat wurde erfolgreich gegen das Zertifikatsprofil {} getestet.",
                    certificateProfile);
                log.debug("Rolle(n): {}", new Admission(x509EeCert).getProfessionItems());
                return new Admission(x509EeCert);
            } catch (final RuntimeException | CertificateEncodingException | IOException e) {
                errors.put(certificateProfile, new GemPkiException(productType, ErrorCode.UNKNOWN, e));
            } catch (final GemPkiException e) {
                errors.put(certificateProfile, e);
            }
        }
        throw new GemPkiParsingException(productType, errors);
    }

    protected void doOcsp(@NonNull final X509Certificate x509EeCert, @NonNull final TspServiceSubset tspServiceSubset) throws GemPkiException {
        final boolean ocspVerification = OcspTransceiver.builder()
            .x509EeCert(x509EeCert)
            .x509IssuerCert(tspServiceSubset.getX509IssuerCert())
            .ssp(tspServiceSubset.getServiceSupplyPoint())
            .build()
            .verifyOcspStatusGood(ocspRespCache);
        log.info("OCSP verification: " + (ocspVerification ? "pass" : "fail"));
        if (!ocspVerification) {
            // TODO create new OCSP exceptions: OCSP_CHECK_REVOCATION_FAILED, OCSP_CHECK_REVOCATION_ERROR, OCSP_NOT_AVAILABLE...
            throw new GemPkiException(productType, ErrorCode.UNKNOWN);
        }
    }

    /**
     * Verify given end-entity certificate against a parameterized single certificate profile {@link CertificateProfile}. If there is no {@link GemPkiException}
     * the verification process ends successfully.
     *
     * @param x509EeCert         end-entity certificate to check
     * @param certificateProfile the profile to check the certificate against
     * @param tspServiceSubset   the issuing certificates as trust store
     * @throws GemPkiException if the certificate is invalid
     */
    protected void tucPki018ChecksForProfile(@NonNull final X509Certificate x509EeCert, @NonNull final CertificateProfile certificateProfile,
        @NonNull final TspServiceSubset tspServiceSubset) throws GemPkiException {
        final CertificateProfileVerification cv = CertificateProfileVerification.builder()
            .x509EeCert(x509EeCert)
            .certificateProfile(certificateProfile)
            .tspServiceSubset(tspServiceSubset)
            .productType(productType)
            .build();

        cv.verifyKeyUsage();
        cv.verifyExtendedKeyUsage();
        cv.verifyCertificateType();
    }

    protected void tucPki018CommonChecks(@NonNull final X509Certificate x509EeCert, @NonNull final TspServiceSubset tspServiceSubset) throws GemPkiException {
        final CertificateCommonVerification cv = CertificateCommonVerification.builder()
            .x509EeCert(x509EeCert)
            .tspServiceSubset(tspServiceSubset)
            .productType(productType)
            .build();

        cv.verifyValidity();
        cv.verifySignature(tspServiceSubset.getX509IssuerCert());
        cv.verifyIssuerServiceStatus();
    }
}
