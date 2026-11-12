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

import org.opencastproject.workspace.api.Workspace;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class GoogleSpeechTranscriptionServiceStorageTest {

  private CloseableHttpClient httpClient;
  private String size;
  private final String bucket = "bucket";
  private final String mpId = "mpId";
  private final String fileName = "myFile";
  private final String fileExtension = "flac";
  private File file;
  private String contentType;
  private String token;
  private Workspace workspace;
  private GoogleSpeechTranscriptionServiceStorage service;

  @Before
  public void setUp() throws Exception {
    httpClient = EasyMock.createNiceMock(CloseableHttpClient.class);
    service = new GoogleSpeechTranscriptionServiceStorage() {
      protected CloseableHttpClient makeHttpClient() {
        return httpClient;
      }
    };
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void startUploadTest() throws Exception {
    ClassicHttpResponse response = EasyMock.createNiceMock(ClassicHttpResponse.class);
    EasyMock.expect(response.getCode()).andReturn(HttpStatus.SC_OK).anyTimes();
    EasyMock.replay(response);

    Capture<HttpPost> capturedPost = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedPost)))
        .andReturn(CloseableHttpResponse.adapt(response)).anyTimes();
    EasyMock.replay(httpClient);

    // Check if correct url was invoked
    service.startUpload(httpClient, bucket, mpId, fileExtension, file, size, contentType, token);
    Assert.assertEquals("https://www.googleapis.com/upload/storage/v1/b/bucket/o?uploadType=resumable&name=mpId.flac",
            capturedPost.getValue().getUri().toString());
  }

  @Test
  public void deleteGoogleStorageFileTest() throws Exception {
    ClassicHttpResponse response = EasyMock.createNiceMock(ClassicHttpResponse.class);
    EasyMock.expect(response.getCode()).andReturn(HttpStatus.SC_NO_CONTENT).anyTimes();
    EasyMock.replay(response);

    Capture<HttpDelete> capturedDel = Capture.newInstance();
    EasyMock.expect(httpClient.execute(EasyMock.capture(capturedDel)))
        .andReturn(CloseableHttpResponse.adapt(response)).anyTimes();
    EasyMock.replay(httpClient);

    service.deleteGoogleStorageFile(httpClient, bucket, fileName, token);
    Assert.assertEquals("https://www.googleapis.com/storage/v1/b/bucket/o/myFile",
            capturedDel.getValue().getUri().toString());
  }

}
