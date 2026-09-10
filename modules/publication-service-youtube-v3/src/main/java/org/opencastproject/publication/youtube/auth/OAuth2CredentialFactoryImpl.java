/*
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
  public DataStore<StoredCredential> getDataStore(final String id, final String dataStoreDirectory)
          throws IOException {
    return new FileDataStoreFactory(new File(dataStoreDirectory)).getDataStore(id);
  }

  @Override
  public GoogleCredential getGoogleCredential(
      final DataStore<StoredCredential> datastore,
      final ClientCredentials authContext
  ) throws IOException {
    final GoogleCredential gCred;
    final LocalServerReceiver localReceiver = new LocalServerReceiver();

    try {
      // Reads the client id and client secret from a file name passed in authContext
      final GoogleClientSecrets gClientSecrets = GoogleClientSecrets.load(new JacksonFactory(),
              new FileReader(authContext.getClientSecrets()));

      // This flow supports installed applications
      final GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
              new NetHttpTransport(), new JacksonFactory(), gClientSecrets, authContext.getScopes())
          .setCredentialDataStore(datastore)
          .setApprovalPrompt("auto")
          .setAccessType("offline")
          .build();

      // Reuse the credential in the data store if there is one and Google still honors its refresh
      // token. Actively exercising the refresh token here also keeps Google from considering it
      // inactive and revoking it, even if this credential ends up never being used to call the
      // YouTube API. If there's no stored credential, or Google has already revoked it (e.g. after
      // prolonged inactivity), fall back to the interactive consent flow to obtain a new one.
      Credential cred = flow.loadCredential(authContext.getClientId());
      if (cred != null) {
        try {
          cred.refreshToken();
          logger.debug(MessageFormat.format(
              "Found credential for client {0} in data store {1}", authContext.getClientId(), datastore.getId()));
        } catch (final IOException e) {
          logger.warn("Stored YouTube credential for client {} could not be refreshed, requesting a new one: {}",
              authContext.getClientId(), e.getMessage());
          datastore.delete(authContext.getClientId());
          cred = null;
        }
      }
      if (cred == null) {
        cred = new AuthorizationCodeInstalledApp(flow, localReceiver).authorize(authContext.getClientId());
        logger.debug(MessageFormat.format(
            "Created new credential for client {0} in data store {1}", authContext.getClientId(), datastore.getId()));
      }

      gCred = new GoogleCredential.Builder()
          .setClientSecrets(gClientSecrets).setJsonFactory(new JacksonFactory())
          .setTransport(new NetHttpTransport()).build();
      gCred.setAccessToken(cred.getAccessToken());
      gCred.setRefreshToken(cred.getRefreshToken());
      logger.debug(MessageFormat.format(
          "Found credential {0} using {1}", gCred.getRefreshToken(), authContext.toString()));
    } finally {
      localReceiver.stop();
    }
    return gCred;
  }
}
