/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencastproject.remotetest.server.resource;

import org.opencastproject.remotetest.Main;
import org.opencastproject.remotetest.util.TrustedHttpClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;

import java.io.File;

/**
 * Files REST resources
 */

public class FilesResources {

  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/files/";
  }

  public static HttpResponse getFile(TrustedHttpClient client, String mediaPackageID, String mediaPackageElementID) throws Exception {
    return client.execute(
            new HttpGet(getServiceUrl() + "mediapackage/" + mediaPackageID + '/' + mediaPackageElementID));
  }

  public static HttpResponse getFile(TrustedHttpClient client, String mediaPackageID, String mediaPackageElementID, String fileName)
          throws Exception {
    return client.execute(
            new HttpGet(getServiceUrl() + "mediapackage/" + mediaPackageID + '/' + mediaPackageElementID + "/"
                    + fileName));
  }

  // FIXME
  public static HttpResponse postFile(TrustedHttpClient client, String mediaPackageID, String mediaPackageElementID, String media)
          throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "mediapackage/" + mediaPackageElementID + "/" + mediaPackageElementID);
    MultipartEntity entity = new MultipartEntity();
    entity.addPart("file", new FileBody(new File(media)));
    post.setEntity(entity);
    return client.execute(post);
  }

  public static HttpResponse deleteFile(TrustedHttpClient client, String mediaPackageID, String mediaPackageElementID) throws Exception {
    HttpDelete delete = new HttpDelete(getServiceUrl() + "mediapackage/" + mediaPackageElementID + "/" + mediaPackageElementID);
    return client.execute(delete);
  }
}
