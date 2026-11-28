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
package org.opencastproject.transcription.googlespeech;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.FileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class GoogleSpeechTranscriptionServiceStorage {

  /**
   * The logger
   */
  private static final Logger logger = LoggerFactory.getLogger(GoogleSpeechTranscriptionService.class);

  private static final String GOOGLE_STORAGE_LINK = "https://www.googleapis.com/upload/storage/v1/b/";
  private static final String GOOGLE_STORAGE_MEDIA = "https://www.googleapis.com/storage/v1/b/";
  private static final int DEFAULT_CODE = 0;

  /**
   * Upload to Google Cloud Storage using Json API.
   * https://cloud.google.com/storage/docs/json_api/v1/how-tos/resumable-upload
   *
   * @param httpClient
   * @param bucket Google storage bucket name
   * @param mpId mediapackage ID
   * @param fileExtension
   * @param file
   * @param byteSize
   * @param contentType
   * @param accessToken
   * @return response code
   * @throws java.io.IOException
   */
  public int startUpload(CloseableHttpClient httpClient, String bucket, String mpId,
          String fileExtension, File file, String byteSize, String contentType, String accessToken) throws IOException {
    try {
      HttpPost httpPost = new HttpPost(GOOGLE_STORAGE_LINK + String.format(
              "%s/o?uploadType=resumable&name=%s.%s", bucket, mpId, fileExtension));
      logger.debug("Url to store media file on Google Cloud Storage : {}", httpPost.getRequestUri());
      httpPost.addHeader("Authorization", "Bearer " + accessToken);
      httpPost.addHeader("X-Upload-Content-Type", contentType);
      httpPost.addHeader("X-Upload-Content-Length", byteSize);
      httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
      record PostResult(int code, Header location) { }
      PostResult postResult = httpClient.execute(httpPost,
          response -> new PostResult(response.getCode(), response.getLastHeader("Location")));
      int postCode = postResult.code();
      if (postCode == HttpStatus.SC_OK) { // 200
        logger.info("Upload session has been successfully created");
        // Get upload session stored in location
        if (postResult.location() == null) {
          logger.warn("Google Storage did not return a location for the upload session");
          return DEFAULT_CODE;
        }
        String location = postResult.location().getValue();
        try {
          HttpPut httpPut = new HttpPut(location);
          httpPut.setHeader(HttpHeaders.CONTENT_TYPE, contentType);
          FileEntity entity = new FileEntity(file, ContentType.APPLICATION_OCTET_STREAM);
          httpPut.setEntity(entity);
          int putCode = httpClient.execute(httpPut, response -> response.getCode());

          switch (putCode) {
            case HttpStatus.SC_OK: // 200
            case HttpStatus.SC_CREATED: // 201
              logger.info("File {} uploaded to Google Storage", mpId + "." + fileExtension);
              return putCode;
            default:
              logger.warn("Unable to upload file to Google Storage, returned code: {}.", putCode);
              return putCode;
          }
        } catch (Exception e) {
          logger.warn("Exception when uploading file to Google Storage", e);
        }
      } else {
        logger.warn("Uploading file to Google Storage failed and returned, status: {}.", postCode);
        return postCode;
      }
    } catch (Exception e) {
      logger.warn("Exception when uploading file to Google Storage", e);
    } finally {
      try {
        httpClient.close();
      } catch (IOException e) {
      }
    }
    return DEFAULT_CODE;
  }

  /**
   * Delete file from Google Cloud Storage using json API.
   * https://cloud.google.com/storage/docs/deleting-objects
   *
   * @param httpClient
   * @param bucket
   * @param objectName
   * @param accessToken
   * @throws java.io.IOException
   */
  public void deleteGoogleStorageFile(
      CloseableHttpClient httpClient,
      String bucket,
      String objectName,
      String accessToken
  ) throws IOException {
    try {
      HttpDelete httpdelete = new HttpDelete(GOOGLE_STORAGE_MEDIA + String.format("%s/o/%s", bucket, objectName));
      logger.debug("Url to delete media file from Google Cloud Storage : {}", httpdelete.getRequestUri());
      httpdelete.addHeader("Authorization", "Bearer " + accessToken); // add the authorization header to the request;
      int code = httpClient.execute(httpdelete, response -> response.getCode());

      switch (code) {
        case HttpStatus.SC_NO_CONTENT: // 204
          logger.info("Media file: {} deleted from Google storage", objectName);
          break;
        default:
          logger.warn("Unable to delete meida file: {} from Google Storage", objectName);
          break;
      }
    } catch (Exception e) {
      logger.warn("Unable to delete meida file: {} from Google Storage", objectName, e);
    } finally {
      try {
        httpClient.close();
      } catch (IOException e) {
      }
    }
  }

}
