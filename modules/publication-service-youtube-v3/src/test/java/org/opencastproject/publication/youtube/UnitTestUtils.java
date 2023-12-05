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

package org.opencastproject.publication.youtube;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public final class UnitTestUtils {

  private UnitTestUtils() {
  }

  public static File getMockClientSecretsFile(String clientId, File file) throws IOException {
    final String jsonContents = "{\"installed\":{\"auth_uri\":\"https://accounts.google.com/o/oauth2/auth\","
        + "\"client_secret\":\"dPjbjplNyt6sqHRrIpw2EBji\","
        + "\"token_uri\":\"https://accounts.google.com/o/oauth2/token\","
        + "\"client_email\":\"\",\"redirect_uris\":[\"urn:ietf:wg:oauth:2.0:oob\",\"oob\"],"
        + "\"client_x509_cert_url\":\"\",\"client_id\":\"" + clientId
        + "\",\"auth_provider_x509_cert_url\":\"https://www.googleapis.com/oauth2/v1/certs\"}}";
    FileUtils.writeStringToFile(file, jsonContents);
    return file;
  }
}
