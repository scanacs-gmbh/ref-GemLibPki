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

package de.gematik.pki.utils;

import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

public class XmlCompare {

    public static boolean documentsAreEqual(final Object actual, final Object expected) {
        final Diff docDiff = DiffBuilder
            .compare(actual)
            .withTest(expected)
            .withNodeFilter(
                // ignore element "Signature" (there are new namespaces)
                node -> !node.getNodeName().contains(":Signature")
            )
            .ignoreWhitespace()
            .build();
        if (docDiff.hasDifferences()) {
            System.out.println("Diffs: " + docDiff.getDifferences());
        }
        return !docDiff.hasDifferences();
    }

}
