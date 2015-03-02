/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.publication.youtube.auth;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.opencastproject.publication.youtube.UnitTestUtils;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ClientCredentialsTest {

  @Test
  public void testToStringNullTolerance() {
    assertNotNull(new ClientCredentials().toString());
  }

  @Test
  public void testParsingJSON() throws IOException, ParseException {
    final String clientId = "652137994117.apps.googleusercontent.com";
    final File clientSecretsFile = UnitTestUtils.getMockClientSecretsFile(clientId);
    try {
      final ClientCredentials cc = new ClientCredentials();
      cc.setClientSecrets(clientSecretsFile);
      assertEquals(clientId, cc.getClientId());
    } finally {
      clientSecretsFile.delete();
    }
  }

  @Test
  public void testGetScopes() {
    final ClientCredentials cc = new ClientCredentials();
    for (final String scope : cc.getScopes()) {
      if (!scope.startsWith("https://www.googleapis.com/auth/youtube")) {
        fail("Invalid YouTube v3 auth configuration where scope = " + scope);
      }
    }
  }
}
