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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.opencastproject.util.data.Collections;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * <code>ClientCredentials</code> class represents the set of parameters required to make an authorization
 * request.
 */
public final class ClientCredentials {

  private String clientId;
  private String credentialDatastore;
  private String dataStoreDirectory;
  private File clientSecrets;

  public String getClientId() {
    return clientId;
  }

  public String getCredentialDatastore() {
    return credentialDatastore;
  }

  public String getDataStoreDirectory() {
    return dataStoreDirectory;
  }

  public List<String> getScopes() {
    return Collections.list("https://www.googleapis.com/auth/youtube",
        "https://www.googleapis.com/auth/youtube.upload", "https://www.googleapis.com/auth/youtube.readonly");
  }

  public File getClientSecrets() {
    return clientSecrets;
  }

  public void setClientSecrets(final File clientSecrets) throws IOException, ParseException {
    this.clientSecrets = clientSecrets;
    this.clientId = getValueFromArray(clientSecrets);
  }

  public void setCredentialDatastore(final String credentialDatastore) {
    this.credentialDatastore = credentialDatastore;
  }

  public void setDataStoreDirectory(final String dataStoreDirectory) {
    this.dataStoreDirectory = dataStoreDirectory;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  /**
   * Parses a file and returns a value matching the keys provided if it exists.
   * The file is assumed to be composed of one array, with each array element
   * in turn being a JSONObject containing a name : value pair.
   *
   * @param file
   *          file to parse
   * @return matching value, or null if no match or there was a parse exception
   * @throws java.io.FileNotFoundException
   * @throws java.io.IOException
   */
  private String getValueFromArray(final File file) throws IOException, ParseException {
    final JSONParser parser = new JSONParser();
    final FileReader reader = new FileReader(file);
    final JSONObject jsonObject = (JSONObject) parser.parse(reader);
    final JSONObject array = (JSONObject) jsonObject.get("installed");
    return (String) array.get("client_id");
  }

}
