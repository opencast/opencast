/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.publication.youtube.auth;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import org.opencastproject.publication.youtube.UnitTestUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.util.store.DataStore;

import org.json.simple.parser.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

public class GoogleAPICredentialRequestorTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidArgs() throws IOException, ParseException {
    GoogleAPICredentialRequestor.main(new String[0]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFileDoesNotExist() throws IOException, ParseException {
    GoogleAPICredentialRequestor.main(new String[] {
        "/this/path/does/exist",
        "credentialDataStore",
        "dataStoreDirectory"
    });
  }

  @Test
  public void testClientSecrets() throws IOException, ParseException {
    final String clientId = "clientId";
    final String credentialDataStore = "credentialDataStore";
    final String dataStoreDirectory = "dataStoreDirectory";
    final File clientSecrets = UnitTestUtils.getMockClientSecretsFile(clientId,
        testFolder.newFile("client-secrets-youtube-v3.json"));
    try {
      final OAuth2CredentialFactory credentialFactory = createMock(OAuth2CredentialFactory.class);
      expect(credentialFactory.getDataStore(credentialDataStore, dataStoreDirectory))
        .andReturn(createMock(DataStore.class)).once();
      expect(credentialFactory.getGoogleCredential(anyObject(DataStore.class), anyObject(ClientCredentials.class)))
        .andReturn(new GoogleCredential()).once();
      replay(credentialFactory);
      GoogleAPICredentialRequestor.setCredentialFactory(credentialFactory);
      GoogleAPICredentialRequestor
        .main(new String[] {clientSecrets.getAbsolutePath(), credentialDataStore, dataStoreDirectory});
    } finally {
      if (clientSecrets != null) {
        clientSecrets.delete();
      }
    }
  }
}
