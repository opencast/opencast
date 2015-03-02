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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * @see com.google.api.client.googleapis.auth.oauth2.GoogleCredential
 */
public final class OAuth2CredentialFactoryImpl implements OAuth2CredentialFactory {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  public OAuth2CredentialFactoryImpl() {
  }

  @Override
  public GoogleCredential getGoogleCredential(final ClientCredentials credentials) throws IOException {
    // Use the default which is file-based; name and location are configurable
    final DataStore<StoredCredential> datastore = getDataStore(credentials.getCredentialDatastore(),
        credentials.getDataStoreDirectory());
    return getGoogleCredential(datastore, credentials);
  }

  @Override
  public DataStore<StoredCredential> getDataStore(final String id, final String dataStoreDirectory) throws IOException {
    return new FileDataStoreFactory(new File(dataStoreDirectory)).getDataStore(id);
  }

  @Override
  public GoogleCredential getGoogleCredential(final DataStore<StoredCredential> datastore, final ClientCredentials authContext)
          throws IOException {
    final GoogleCredential gCred;
    final LocalServerReceiver localReceiver = new LocalServerReceiver();
    final String accessToken;
    final String refreshToken;

    try {
      // Reads the client id and client secret from a file name passed in authContext
      final GoogleClientSecrets gClientSecrets = GoogleClientSecrets.load(new JacksonFactory(),
              new FileReader(authContext.getClientSecrets()));

      // Obtain tokens from credential in data store, or obtains a new one from
      // Google if one doesn't exist
      final StoredCredential sCred = datastore.get(authContext.getClientId());
      if (sCred != null) {
        accessToken = sCred.getAccessToken();
        refreshToken = sCred.getRefreshToken();
        logger.debug(MessageFormat.format("Found credential for client {0} in data store {1}", authContext.getClientId(), datastore.getId()));
      } else {
        // This flow supports installed applications
        final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(new NetHttpTransport(), new JacksonFactory(),
                gClientSecrets, authContext.getScopes()).setCredentialDataStore(datastore).setApprovalPrompt("auto")
                .setAccessType("offline").build();
        final Credential cred = new AuthorizationCodeInstalledApp(flow, localReceiver).authorize(authContext.getClientId());
        accessToken = cred.getAccessToken();
        refreshToken = cred.getRefreshToken();
        logger.debug(MessageFormat.format("Created new credential for client {0} in data store {1}", authContext.getClientId(), datastore.getId()));
      }
      gCred = new GoogleCredential.Builder()
          .setClientSecrets(gClientSecrets).setJsonFactory(new JacksonFactory())
          .setTransport(new NetHttpTransport()).build();
      gCred.setAccessToken(accessToken);
      gCred.setRefreshToken(refreshToken);
      logger.debug(MessageFormat.format("Found credential {0} using {1}", gCred.getRefreshToken(), authContext.toString()));
    } finally {
      localReceiver.stop();
    }
    return gCred;
  }
}
