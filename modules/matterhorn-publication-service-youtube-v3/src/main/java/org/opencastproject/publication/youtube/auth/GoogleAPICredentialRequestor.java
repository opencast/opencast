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

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;

/**
 * <code>GoogleAPICredentialRequestor</code> obtains credentials from Google
 * and persists them in a data store on the local file system for later use
 * when invoking the Google Data API V3 to upload a file to YouTube.
 */
public final class GoogleAPICredentialRequestor {

  private static OAuth2CredentialFactory credentialFactory = null;

  /**
   * Private constructor.
   */
  private GoogleAPICredentialRequestor() {
  }

  /**
   * The authorization process is dependent on authorization context parameters
   * which is data that is required to execute the request and persist the
   * result:
   * <ul>
   * <li>client secrets - a file containing the client id and password</li>
   * <li>credentialDatastore - name of the datastore which stores tokens</li>
   * <li>dataStoreDirectory - location of the datastore in the file system</li>
   * </ul>
   *
   * @param args
   *          override parameters
   */
  public static void main(final String[] args) throws IOException, ParseException {
    int size = (args == null) ? 0 : args.length;
    if (size != 3) {
      throw new IllegalArgumentException('\n' + "[ERROR] Wrong number of arguments: " + size + '\n');
    } else {
      registerOAuth2Credential(new File(args[0]), args[1], args[2]);
    }
  }

  /**
   * @param clientSecrets Null not allowed.
   * @param credentialDataStore Null not allowed.
   * @param dataStoreDirectory Null not allowed.
   * @throws IOException when file not available
   * @throws ParseException when file is not JSON
   */
  private static void registerOAuth2Credential(final File clientSecrets, final String credentialDataStore,
                                               final String dataStoreDirectory) throws IOException, ParseException {
    if (clientSecrets.exists()) {
      final ClientCredentials c = new ClientCredentials();
      c.setClientSecrets(clientSecrets);
      c.setCredentialDatastore(credentialDataStore);
      c.setDataStoreDirectory(dataStoreDirectory);
      final OAuth2CredentialFactory factory = (credentialFactory == null) ? new OAuth2CredentialFactoryImpl() : credentialFactory;
      final DataStore<StoredCredential> dataStore = factory.getDataStore(c.getCredentialDatastore(), c.getDataStoreDirectory());
      factory.getGoogleCredential(dataStore, c);
    } else {
      throw new IllegalArgumentException("The client-secrets file (YouTube OAuth) was not found: " + clientSecrets.getAbsolutePath());
    }
  }

  static void setCredentialFactory(final OAuth2CredentialFactory oAuth2CredentialFactory) {
    credentialFactory = oAuth2CredentialFactory;
  }
}
