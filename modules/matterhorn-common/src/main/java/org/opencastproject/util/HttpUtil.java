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
package org.opencastproject.util;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.functions.Misc.chuck;

/** Functions to support Apache httpcomponents. */
public final class HttpUtil {
  private HttpUtil() {
  }

 public static HttpPost post(NameValuePair... formParams) {
   final HttpPost post = new HttpPost();
   setFormParams(post, formParams);
   return post;
 }

 public static HttpPost post(String uri, NameValuePair... formParams) {
   final HttpPost post = new HttpPost(uri);
   setFormParams(post, formParams);
   return post;
 }

  private static void setFormParams(HttpEntityEnclosingRequest r, NameValuePair[] formParams) {
    final List<NameValuePair> params = list(formParams);
    try {
      r.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      chuck(e);
    }
  }

  public static NameValuePair param(String name, String value) {
    return new BasicNameValuePair(name, value);
  }
}
