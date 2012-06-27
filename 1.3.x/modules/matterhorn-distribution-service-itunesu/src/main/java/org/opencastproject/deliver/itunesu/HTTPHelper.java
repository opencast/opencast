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
package org.opencastproject.deliver.itunesu;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

/**
 * <p>This is the helper class that sends a GET or POST HTTP request with various form data,
 * including files.</p>
 */
public class HTTPHelper 
{
  /** HttpURLConnection instance */
  private HttpURLConnection connection;
  /** the output stream to write */
  private OutputStream outputStream = null;
  /** boundary of the multi-part form data */
  private String boundary;
  /** buffer size to read HTTP response */
  private static final int BUFFER_SIZE = 16 * 1024;
  /** buffer size to read file content */
  private static final int FILE_BUFFER_SIZE = 500 * 1024;

  /**
   * Constructor.
   *
   * @param urlString the string representation of the URL to send request to
   * @throws IOException
   */
  public HTTPHelper(String urlString) throws IOException {
    URL url = new URL(urlString);
  
    connection = (HttpURLConnection) url.openConnection();
    connection.setDoOutput(true);
  }

  /** 
   * Generates a random string.
   */
  protected static String randomString() {
    return Long.toString((new Random()).nextLong(), 36);
  }

  /**
   * Reads the HTTP response from the connection.
   *
   * @throws IOException
   * @return the response as string
   */
  private String readResponse() throws IOException {
    // read the response
    InputStream inputStream = null;
    StringBuffer stringBuffer = new StringBuffer();

    try {
      inputStream = connection.getInputStream();
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
      char [] buffer = new char[BUFFER_SIZE];
      for (int n = 0; n >= 0;) {
        n = reader.read(buffer, 0, buffer.length);
        if (n > 0) {
          stringBuffer.append(buffer, 0, n);
        }
      }
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    return stringBuffer.toString();
  }

  /**
   * Sends an HTTP POST request.
   *
   * @param request the encoded string
   * @throws IOException
   * @return the response as string
   */
  public String httpPost(String request) throws IOException {
    connection.setRequestMethod("POST");
    
    return retrieveDoc(request);
  }

  /**
   * Sends an HTTP GET request.
   *
   * @param request the encoded string
   * @throws IOException
   * @return the response as string
   */
  public String httpGet(String request) throws IOException {
    connection.setRequestMethod("GET");
    
    return retrieveDoc(request);
  }

  /**
   * Throws exception if status code is not 200.
   */
  private void checkStatusCode()
  {
    int statusCode = 0;
    String errorMessage = "";

    try {    
      statusCode = connection.getResponseCode();
      errorMessage = connection.getResponseMessage();
    } catch (IOException e) {
      throw new RuntimeException("Error in HTTP connection!");      
    }
 
    if (statusCode != HttpURLConnection.HTTP_OK) {
      switch (statusCode) {
      case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
        errorMessage = "Request Time-Out";
        break;
      case HttpURLConnection.HTTP_INTERNAL_ERROR:
        errorMessage = "Internal Server Error";
        break;
      case HttpURLConnection.HTTP_UNAVAILABLE:
        errorMessage = "Service Unavailable";
        break;
      }

      throw new RuntimeException(errorMessage);
    }
  }

  /**
   * Writes the request string to the connection and returns the response.
   *
   * @param request the encoded string
   * @throws IOException
   * @return the response as string
   */
  private String retrieveDoc(String request) throws IOException
  {
    connection.connect();
    OutputStream outputStream = connection.getOutputStream();
    outputStream.write(request.getBytes("UTF-8"));
    outputStream.flush();
    outputStream.close();

    String response = readResponse();
    
    // exception handling
    checkStatusCode();

    // clean
    connection.disconnect();

    return response;
  }

  /**
   * Sends a multi-part POST HTTP request with file data and returns the response as a string.
   *
   * @param fileName the name of the file to upload
   * @return the HTTP response
   * @throws IOException
   */
  public String uploadFile(String fileName) throws IOException {
    // boundary string
    boundary = "---------------------------" + randomString();

    // multi-part form data with randomly generated boundary string
    connection.setRequestProperty("Content-Type",
                                  "multipart/form-data; boundary=" + boundary);
    connection.connect();

    // get the output stream
    outputStream = connection.getOutputStream();

    StringBuffer stringBuffer = new StringBuffer();

    // form data starts
    stringBuffer.append("--");
    stringBuffer.append(boundary);
    stringBuffer.append("\r\n");

    // file name
    stringBuffer.append("Content-Disposition: form-data; name=\"file\"; filename=\"");
    stringBuffer.append(fileName);
    stringBuffer.append("\"\r\n");

    // content type
    stringBuffer.append("Content-Type: ");
    String type = connection.guessContentTypeFromName(fileName);
    if (type == null) {
      // default content type
      type = "application/octet-stream";
    }
    stringBuffer.append(type);
    stringBuffer.append("\r\n");
    stringBuffer.append("\r\n");

    outputStream.write(stringBuffer.toString().getBytes());

    // file data
    File file = new File(fileName);
    InputStream is = new FileInputStream(file);
    byte[] buf = new byte[FILE_BUFFER_SIZE];
    int nread;
    int navailable;
    int total = 0;
    synchronized (is) {
      while((nread = is.read(buf, 0, buf.length)) >= 0) {
        outputStream.write(buf, 0, nread);
        total += nread;
      }
    }
    outputStream.flush();

    // reset the string buffer
    stringBuffer.setLength(0);
    stringBuffer.append("\r\n");

    // end of form data
    stringBuffer.append("--");
    stringBuffer.append(boundary);
    stringBuffer.append("--");
    stringBuffer.append("\r\n");

    outputStream.write(stringBuffer.toString().getBytes());

    outputStream.close();

    String response = readResponse();

    // exception handling
    checkStatusCode();

    // clean
    connection.disconnect();

    return response;
  }
}
