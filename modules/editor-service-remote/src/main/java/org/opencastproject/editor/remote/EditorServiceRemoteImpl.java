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
package org.opencastproject.editor.remote;

import org.opencastproject.editor.api.EditingData;
import org.opencastproject.editor.api.EditorService;
import org.opencastproject.editor.api.EditorServiceException;
import org.opencastproject.editor.api.ErrorStatus;
import org.opencastproject.editor.api.LockData;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
    property = {
        "service.description=Editor Service Remote Proxy"
    },
    immediate = true,
    service = EditorService.class
)
public class EditorServiceRemoteImpl extends RemoteBase implements EditorService {
  private static final Logger logger = LoggerFactory.getLogger(EditorServiceRemoteImpl.class);
  public static final String EDIT_SUFFIX = "/edit.json";
  public static final String LOCK_SUFFIX = "/lock";
  public static final String METADATA_SUFFIX = "/metadata.json";

  /**
   * Creates a remote implementation for the given type of service.
   */
  public EditorServiceRemoteImpl() {
    super(EditorService.JOB_TYPE);
  }

  @Override
  public EditingData getEditData(String mediaPackageId)
          throws EditorServiceException {
    return EditingData.parse(doGetForMediaPackage(mediaPackageId, EDIT_SUFFIX));
  }

  @Override
  public void setEditData(String mediaPackageId, EditingData editingData) throws EditorServiceException {
    doPostForMediaPackage(mediaPackageId, EDIT_SUFFIX, editingData.toString());
  }

  @Override
  public String getMetadata(String mediaPackageId) throws EditorServiceException {
    return doGetForMediaPackage(mediaPackageId, METADATA_SUFFIX);
  }

  @Override
  public void lockMediaPackage(String mediaPackageId, LockData lockData) throws EditorServiceException {
    Map<String, String> params = new HashMap<>();
    params.put("uuid", lockData.getUUID());
    params.put("user", lockData.getUser());
    doPostFormForMediaPackage(mediaPackageId, LOCK_SUFFIX, params);
  }

  @Override
  public void unlockMediaPackage(String mediaPackageId, LockData lockData) throws EditorServiceException {
    doDeleteForMediaPackage(mediaPackageId, LOCK_SUFFIX + "/" + lockData.getUUID());
  }

  protected String doDeleteForMediaPackage(String mediaPackageId, final String urlSuffix)
          throws EditorServiceException {
    logger.debug("Editor Remote Lock POST Request for mediaPackage : '{}'", mediaPackageId);
    HttpDelete delete = new HttpDelete(mediaPackageId + urlSuffix);
    HttpResponse response = null;
    try {
      response = getResponse(delete, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_CONFLICT);
      if (response == null || response.getStatusLine() == null) {
        throw new EditorServiceException("Editor Remote call failed to respond", ErrorStatus.UNKNOWN);
      }
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        evaluateResponseCode(response);
      }
      return null;
    } catch (Exception e) {
      throw new EditorServiceException("Editor Remote call failed", ErrorStatus.UNKNOWN, e);
    } finally {
      closeConnection(response);
    }
  }

  protected String doGetForMediaPackage(final String mediaPackageId, final String urlSuffix)
          throws EditorServiceException {
    logger.debug("Editor Remote GET Request for mediaPackage: '{}' to url: '{}'", mediaPackageId, urlSuffix);
    HttpGet get = new HttpGet(mediaPackageId + urlSuffix);
    HttpResponse response = null;
    try {
      response = getResponse(get, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_CONFLICT,
        HttpStatus.SC_BAD_REQUEST);
      if (response == null || response.getStatusLine() == null) {
        throw new EditorServiceException("HTTP Request failed", ErrorStatus.UNKNOWN);
      }
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        evaluateResponseCode(response);
        return null;
      } else {
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity != null) {
          return EntityUtils.toString(httpEntity);
        } else {
          throw new EditorServiceException("Editor Remote call failed", ErrorStatus.UNKNOWN);
        }
      }
    } catch (IOException e) {
      throw new EditorServiceException("Editor Remote call failed", ErrorStatus.UNKNOWN, e);
    } finally {
      closeConnection(response);
    }
  }

  protected void doPostForMediaPackage(final String mediaPackageId, final String urlSuffix, final String data)
          throws EditorServiceException {
    logger.debug("Editor Remote POST Request for mediaPackage : '{}' to url: '{}'", mediaPackageId, urlSuffix);
    HttpPost post = new HttpPost(mediaPackageId + urlSuffix);
    HttpResponse response = null;
    try {
      StringEntity editJson = new StringEntity(data, StandardCharsets.UTF_8);
      post.setEntity(editJson);
      post.setHeader("Content-type", "application/json");
      response = getResponse(post, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_BAD_REQUEST,
              HttpStatus.SC_CONFLICT);
      if (response == null || response.getStatusLine() == null) {
        throw new EditorServiceException("No response for setEditData", ErrorStatus.UNKNOWN);
      }
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        evaluateResponseCode(response);
      }
    } finally {
      closeConnection(response);
    }
  }

  protected void doPostFormForMediaPackage(final String mediaPackageId, final String urlSuffix,
          final Map<String, String> params)
          throws EditorServiceException {
    logger.debug("Editor Remote POST Request for mediaPackage : '{}' to url: '{}'", mediaPackageId, urlSuffix);
    HttpPost post = new HttpPost(mediaPackageId + urlSuffix);
    HttpResponse response = null;
    try {
      List<BasicNameValuePair> formParams = new ArrayList<>();
      for (String field : params.keySet()) {
        formParams.add(new BasicNameValuePair(field, params.get(field)));
      }
      post.setEntity(new UrlEncodedFormEntity(formParams, "UTF-8"));
      response = getResponse(post, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND, HttpStatus.SC_BAD_REQUEST,
              HttpStatus.SC_CONFLICT);
      if (response == null || response.getStatusLine() == null) {
        throw new EditorServiceException("No response for setEditData", ErrorStatus.UNKNOWN);
      }
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        evaluateResponseCode(response);
      }
    } catch (UnsupportedEncodingException e) {
      throw new EditorServiceException(e.getMessage(), ErrorStatus.UNKNOWN);
    } finally {
      closeConnection(response);
    }
  }

  protected void evaluateResponseCode(HttpResponse response) throws EditorServiceException {
    switch (response.getStatusLine().getStatusCode()) {
      case HttpStatus.SC_NOT_FOUND:
        throw new EditorServiceException("MediaPackage not found", ErrorStatus.MEDIAPACKAGE_NOT_FOUND);
      case HttpStatus.SC_CONFLICT:
        throw new EditorServiceException(response.getEntity().toString(), ErrorStatus.MEDIAPACKAGE_LOCKED);
      case HttpStatus.SC_BAD_REQUEST:
        throw new EditorServiceException("Request invalid", ErrorStatus.UNKNOWN);
      default:
        throw new EditorServiceException("Editor Remote call failed", ErrorStatus.UNKNOWN);
    }
  }

  @Reference
  public void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  @Reference
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    this.remoteServiceManager = remoteServiceManager;
  }
}
