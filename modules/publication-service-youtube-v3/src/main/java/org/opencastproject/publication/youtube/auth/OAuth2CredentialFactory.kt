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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.publication.youtube.auth

import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.util.store.DataStore

import java.io.IOException

/**
 * `OAuth2CredentialFactory` implementation is a factory class that returns
 * `GoogleCredential` objects.
 */
interface OAuth2CredentialFactory {

    /**
     * Returns a file-backed data store.
     *
     * @param id
     * unique identifier for the data store
     * @param dataStoreDirectory
     * name of the data store directory
     * @return file-backed data store
     * @throws IOException
     */
    @Throws(IOException::class)
    fun getDataStore(id: String, dataStoreDirectory: String): DataStore<StoredCredential>

    /**
     * Returns a `GoogleCredential` from a data store. If one
     * does not exist, a new `GoogleCredential` will be generated and
     * persisted in the data store.
     *
     * @param datastore
     * a file or memory backed data store
     * @param authContext
     * `ClientCredentials` object containing
     * the parameters needed to find an existing or create a new instance
     * of `GoogleCredential`
     * @return Google-specific subclass of `Credential`
     * @throws IOException if the default data store is not available
     */
    @Throws(IOException::class)
    fun getGoogleCredential(datastore: DataStore<StoredCredential>, authContext: ClientCredentials): GoogleCredential

    /**
     * Returns a `GoogleCredential` from the default data store. If one
     * does not exist, a new `GoogleCredential` will be generated and
     * persisted in the data store.
     *
     * @return Google-specific subclass of `Credential`
     * @throws IOException if the default data store is not available
     */
    @Throws(IOException::class)
    fun getGoogleCredential(credentials: ClientCredentials): GoogleCredential

}
