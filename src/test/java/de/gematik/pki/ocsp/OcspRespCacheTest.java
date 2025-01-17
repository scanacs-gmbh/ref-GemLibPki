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

package de.gematik.pki.ocsp;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import de.gematik.pki.utils.CertificateProvider;
import java.security.cert.X509Certificate;
import lombok.SneakyThrows;
import org.bouncycastle.cert.ocsp.OCSPReq;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OcspRespCacheTest {

    static X509Certificate VALID_X509_EE_CERT;
    static X509Certificate VALID_X509_ISSUER_CERT;
    static OCSPReq ocspReq;

    @SneakyThrows
    @BeforeAll
    static void setup() {
        VALID_X509_EE_CERT = CertificateProvider
            .getX509Certificate("src/test/resources/certificates/GEM.SMCB-CA10/valid/DrMedGunther.pem");
        // certificate is not issued by VALID_X509_ISSUER_CERT - this will fail when certHash check is implemented
        VALID_X509_ISSUER_CERT = CertificateProvider
            .getX509Certificate("src/test/resources/certificates/GEM.RCA1_TEST-ONLY.pem");
        ocspReq = OcspRequestGenerator.generateSingleOcspRequest(VALID_X509_EE_CERT,
            VALID_X509_ISSUER_CERT);
    }

    @Test
    void setAndGetOcspGracePeriodSeconds() {
        final int OCSP_GRACE_PERIOD = 10;
        final OcspRespCache ocspRespCache = new OcspRespCache(OCSP_GRACE_PERIOD);
        assertThat(ocspRespCache.getOcspGracePeriodSeconds()).isEqualTo(OCSP_GRACE_PERIOD);
    }

    @SneakyThrows
    @Test
    void saveCheckSize() {
        final OcspRespCache ocspRespCache = new OcspRespCache(30);
        assertThat(ocspRespCache.getSize()).isZero();
        final OCSPResp ocspResp = OcspResponseGenerator.builder()
            .signer(OcspConstants.getOcspSignerRsa())
            .build()
            .gen(ocspReq);
        ocspRespCache.saveResponse(VALID_X509_EE_CERT.getSerialNumber(), ocspResp);
        assertThat(ocspRespCache.getSize()).isEqualTo(1);
    }

    @SneakyThrows
    @Test
    void saveAndGetResponse() {
        final OcspRespCache ocspRespCache = new OcspRespCache(30);

        assertThat(ocspRespCache.getResponse(VALID_X509_EE_CERT.getSerialNumber())).isEmpty();
        final OCSPResp ocspResp = OcspResponseGenerator.builder()
            .signer(OcspConstants.getOcspSignerRsa())
            .build()
            .gen(ocspReq);
        ocspRespCache.saveResponse(VALID_X509_EE_CERT.getSerialNumber(), ocspResp);
        assertThat(ocspRespCache.getResponse(VALID_X509_EE_CERT.getSerialNumber())).isPresent();
    }

}
