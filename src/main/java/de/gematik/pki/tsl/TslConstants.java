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

package de.gematik.pki.tsl;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Utility class for constants related to tsl processing
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TslConstants {

    public static final String STI_PKC = "http://uri.etsi.org/TrstSvc/Svctype/CA/PKC";
    public static final String STI_SRV_CERT_CHANGE = "http://uri.etsi.org/TrstSvc/Svctype/TSLServiceCertChange";

    public static final List<String> STI_CA_LIST = List.of(STI_PKC, STI_SRV_CERT_CHANGE);

    public static final String TSL_DOWNLOAD_URL_OID_PRIMARY = "1.2.276.0.76.4.120";
    public static final String TSL_DOWNLOAD_URL_OID_BACKUP = "1.2.276.0.76.4.121";
    public static final String TSL_ID_PREFIX = "ID";
    public static final String TSL_VERSION = "3";

}
