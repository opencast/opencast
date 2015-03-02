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

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.util.store.DataStore;

import java.io.IOException;

/**
 * <code>OAuth2CredentialFactory</code> implementation is a factory class that returns
 * <code>GoogleCredential</code> objects.
 */
public interface OAuth2CredentialFactory {

  /**
   * Returns a file-backed data store.
   *
   * @param id
   *          unique identifier for the data store
   * @param dataStoreDirectory
   *          name of the data store directory
   * @return file-backed data store
   * @throws IOException
   */
  DataStore<StoredCredential> getDataStore(String id, String dataStoreDirectory) throws IOException;

  /**
   * Returns a <code>GoogleCredential</code> from a data store. If one
   * does not exist, a new <code>GoogleCredential</code> will be generated and
   * persisted in the data store.
   *
   * @param datastore
   *          a file or memory backed data store
   * @param authContext
   *          <code>ClientCredentials</code> object containing
   *          the parameters needed to find an existing or create a new instance
   *          of <code>GoogleCredential</code>
   * @return Google-specific subclass of <code>Credential</code>
   * @throws IOException if the default data store is not available
   */
  GoogleCredential getGoogleCredential(DataStore<StoredCredential> datastore, ClientCredentials authContext) throws IOException;

  /**
   * Returns a <code>GoogleCredential</code> from the default data store. If one
   * does not exist, a new <code>GoogleCredential</code> will be generated and
   * persisted in the data store.
   *
   * @return Google-specific subclass of <code>Credential</code>
   * @throws IOException if the default data store is not available
   */
  GoogleCredential getGoogleCredential(final ClientCredentials credentials) throws IOException;

}
