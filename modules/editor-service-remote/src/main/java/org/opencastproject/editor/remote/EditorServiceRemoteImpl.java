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
package org.opencastproject.editor.remote;

import org.opencastproject.editor.api.EditingData;
import org.opencastproject.editor.api.EditorService;
import org.opencastproject.editor.api.EditorServiceException;
import org.opencastproject.editor.api.ErrorStatus;
import org.opencastproject.serviceregistry.api.RemoteBase;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorServiceRemoteImpl extends RemoteBase implements EditorService {
  private static final Logger logger = LoggerFactory.getLogger(EditorServiceRemoteImpl.class);
  public static final String EDIT_JSON_SUFFIX = "/edit.json";

  /**
   * Creates a remote implementation for the given type of service.
   */
  public EditorServiceRemoteImpl() {
    super(EditorService.JOB_TYPE);
  }

  @Override
  public EditingData getEditData(String mediapackageId) throws EditorServiceException {
    logger.info("Editor Remote GET Request for mediapackage : '{}'", mediapackageId);
    HttpGet get = new HttpGet(mediapackageId + EDIT_JSON_SUFFIX);
    HttpResponse response = null;
    try {
      response = getResponse(get, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND);
      if (response == null || response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        switch (response.getStatusLine().getStatusCode()) {
          case HttpStatus.SC_NOT_FOUND:
            throw new EditorServiceException("Mediapackage not found", ErrorStatus.MEDIAPACKAGE_NOT_FOUND);
          case HttpStatus.SC_BAD_REQUEST:
            throw new EditorServiceException("Request invalid", ErrorStatus.UNKNOWN);
          default:
            throw new EditorServiceException("Editor Remote call failed", ErrorStatus.UNKNOWN);
        }
      } else {
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity != null) {
          return EditingData.parse(EntityUtils.toString(httpEntity));
        } else {
          throw new EditorServiceException("Editor Remote call failed", ErrorStatus.UNKNOWN);
        }
      }
    } catch (Exception e) {
      if (e instanceof EditorServiceException) {
        throw (EditorServiceException) e;
      } else {
        throw new EditorServiceException("Editor Remote call failed", ErrorStatus.UNKNOWN, e);
      }
    } finally {
      closeConnection(response);
    }
  }

  @Override
  public void setEditData(String mediapackageId, EditingData editingData) throws EditorServiceException {
    logger.info("Editor Remote POST Request for mediapackage : '{}'", mediapackageId);
    HttpPost post = new HttpPost(mediapackageId + EDIT_JSON_SUFFIX);
    HttpResponse response = null;
    try {
      StringEntity editJson = new StringEntity(editingData.toString());
      post.setEntity(editJson);
      post.setHeader("Content-type", "application/json");
      response = getResponse(post);
      if (response == null) {
        throw new EditorServiceException("No response for setEditData", ErrorStatus.UNKNOWN);
      }
    } catch (Exception e) {
      if (e instanceof EditorServiceException) {
        throw (EditorServiceException) e;
      } else {
        throw new EditorServiceException("Execution of setEditData failed", ErrorStatus.UNKNOWN, e);
      }
    } finally {
      closeConnection(response);
    }
  }
}
