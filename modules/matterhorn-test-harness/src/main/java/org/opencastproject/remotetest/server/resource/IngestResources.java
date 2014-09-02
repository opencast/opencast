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
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.message.BasicNameValuePair;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Ingest REST resources
 *
 * @author jamiehodge
 *
 */

public class IngestResources {

  private static final String getServiceUrl() {
    return Main.getBaseUrl() + "/ingest/";
  }

  public static HttpResponse createMediaPackage(TrustedHttpClient client) throws Exception {
    return client.execute(new HttpGet(getServiceUrl() + "createMediaPackage"));
  }

  /**
   *
   * @param type
   *          Type of media to add: Track, Catalog, Attachment
   *
   */
  public static HttpResponse add(TrustedHttpClient client, String type, String url, String flavor, String mediaPackage) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "add" + type);
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("url", url));
    params.add(new BasicNameValuePair("flavor", flavor));
    params.add(new BasicNameValuePair("mediaPackage", mediaPackage));
    post.setEntity(new UrlEncodedFormEntity(params));
    return client.execute(post);
  }

  /**
   *
   * @param type
   *          Type of media to add: Track, Catalog, Attachment
   *
   */
  public static HttpResponse addTrack(TrustedHttpClient client, String type, InputStream media, String flavor, String mediaPackage)
          throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "add" + type);
    MultipartEntity entity = new MultipartEntity();
    entity.addPart("flavor", new StringBody(flavor));
    entity.addPart("mediaPackage", new StringBody(mediaPackage));
    post.setEntity(entity);
    return client.execute(post);
  }

  // TODO addMediaPackage

  public static HttpResponse addZippedMediaPackage(TrustedHttpClient client, InputStream mediaPackage) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "addZippedMediaPackage");
    MultipartEntity entity = new MultipartEntity();
    entity.addPart("mediaPackage", new InputStreamBody(mediaPackage, "mediapackage.zip"));
    post.setEntity(entity);
    return client.execute(post);
  }

  public static HttpResponse ingest(TrustedHttpClient client, String mediaPackageId) throws Exception {
    HttpPost post = new HttpPost(getServiceUrl() + "ingest");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("mediaPackage", mediaPackageId));
    post.setEntity(new UrlEncodedFormEntity(params));
    return client.execute(post);
  }

  public static HttpResponse getWorkflowInstance(TrustedHttpClient client, String id) throws Exception {
    return client.execute(new HttpGet(Main.getBaseUrl() + "/workflow/instance/" + id + ".xml"));
  }

}
