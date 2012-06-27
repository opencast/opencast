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
package org.opencastproject.workingfilerepository.remote;

import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A remote service proxy for a working file repository
 */
public class WorkingFileRepositoryRemoteImpl extends RemoteBase implements WorkingFileRepository {

  /** the logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryRemoteImpl.class);

  public WorkingFileRepositoryRemoteImpl() {
    super(SERVICE_TYPE);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#copyTo(java.lang.String, java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI copyTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement,
          String toFileName) throws IOException {
    String urlSuffix = UrlSupport.concat(new String[] { "copy", fromCollection, fromFileName, toMediaPackage,
            toMediaPackageElement, toFileName });
    HttpPost post = new HttpPost(urlSuffix);
    HttpResponse response = getResponse(post);
    if (response == null) {
      throw new RuntimeException();
    }
    try {
      URI uri = new URI(EntityUtils.toString(response.getEntity(), "UTF-8"));
      logger.info("Copied collection file {}/{} to {}", new Object[] { fromCollection, fromFileName, uri });
      return uri;
    } catch (Exception e) {
      throw new IOException("Unable to copy file", e);
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#moveTo(java.lang.String, java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI moveTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement,
          String toFileName) throws IOException {
    String urlSuffix = UrlSupport.concat(new String[] { "move", fromCollection, fromFileName, toMediaPackage,
            toMediaPackageElement });
    HttpPost post = new HttpPost(urlSuffix);
    HttpResponse response = getResponse(post);
    if (response == null) {
      throw new RuntimeException();
    }
    try {
      URI uri = new URI(EntityUtils.toString(response.getEntity(), "UTF-8"));
      logger.info("Moved collection file {}/{} to {}", new Object[] { fromCollection, fromFileName, uri });
      return uri;
    } catch (Exception e) {
      throw new IOException("Unable to move file", e);
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#delete(java.lang.String, java.lang.String)
   */
  @Override
  public boolean delete(String mediaPackageID, String mediaPackageElementID) {
    String urlSuffix = UrlSupport.concat(new String[] { mediaPackageID, mediaPackageElementID });
    HttpDelete del = new HttpDelete(urlSuffix);
    HttpResponse response = null;
    try {
      response = getResponse(del, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND);
      if (response == null)
        throw new RuntimeException("Error removing file");
      return HttpStatus.SC_OK == response.getStatusLine().getStatusCode();
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#get(java.lang.String, java.lang.String)
   */
  @Override
  public InputStream get(String mediaPackageID, String mediaPackageElementID) throws NotFoundException {
    String urlSuffix = UrlSupport.concat(new String[] { mediaPackageID, mediaPackageElementID });
    HttpGet get = new HttpGet(urlSuffix);
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response == null) {
        throw new NotFoundException();
      }
      return new HttpClientClosingInputStream(response);
    } catch (Exception e) {
      throw new RuntimeException();
    }
    // Do not close this response. It will be closed when the caller closes the input stream
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionContents(java.lang.String)
   */
  @Override
  public URI[] getCollectionContents(String collectionId) {
    String urlSuffix = UrlSupport.concat(new String[] { "list", collectionId + ".json" });
    HttpGet get = new HttpGet(urlSuffix);
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response == null) {
        throw new RuntimeException();
      }
      String json = EntityUtils.toString(response.getEntity());
      JSONArray jsonArray = (JSONArray) JSONValue.parse(json);
      URI[] uris = new URI[jsonArray.size()];
      for (int i = 0; i < jsonArray.size(); i++) {
        uris[i] = new URI((String) jsonArray.get(i));
      }
      return uris;
    } catch (Exception e) {
      throw new RuntimeException();
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionSize(java.lang.String)
   */
  @Override
  public long getCollectionSize(String id) throws NotFoundException {
    return getCollectionContents(id).length;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getDiskSpace()
   */
  @Override
  public String getDiskSpace() {
    return (String) getStorageReport().get("summary");
  }

  protected JSONObject getStorageReport() {
    String url = UrlSupport.concat(new String[] { "storage" });
    HttpGet get = new HttpGet(url);
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response == null) {
        throw new RuntimeException();
      }
      String json = EntityUtils.toString(response.getEntity());
      return (JSONObject) JSONValue.parse(json);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getFromCollection(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public InputStream getFromCollection(String collectionId, String fileName) throws NotFoundException {
    String url = UrlSupport.concat(new String[] { "collection", collectionId, fileName });
    HttpGet get = new HttpGet(url);
    HttpResponse response = null;
    try {
      response = getResponse(get, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND);
      if (response == null)
        throw new RuntimeException();
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND)
        throw new NotFoundException();
      return new HttpClientClosingInputStream(response);
    } catch (Exception e) {
      throw new RuntimeException();
    }
    // Do not close this response. It will be closed when the caller closes the input stream
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getTotalSpace()
   */
  @Override
  public long getTotalSpace() {
    return (Long) (getStorageReport().get("size"));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionURI(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public URI getCollectionURI(String collectionID, String fileName) {
    String url = UrlSupport.concat(new String[] { "collectionuri", collectionID, fileName });
    HttpGet get = new HttpGet(url);
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response == null)
        throw new RuntimeException();
      return new URI(EntityUtils.toString(response.getEntity()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getURI(java.lang.String, java.lang.String)
   */
  @Override
  public URI getURI(String mediaPackageID, String mediaPackageElementID) {
    return getURI(mediaPackageID, mediaPackageElementID, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getURI(java.lang.String, java.lang.String,
   *      java.lang.String)
   */
  @Override
  public URI getURI(String mediaPackageID, String mediaPackageElementID, String fileName) {
    String url = UrlSupport.concat(new String[] { "uri", mediaPackageID, mediaPackageElementID });
    if (fileName != null)
      url = UrlSupport.concat(url, fileName);
    HttpGet get = new HttpGet(url);
    HttpResponse response = null;
    try {
      response = getResponse(get);
      if (response == null) {
        throw new RuntimeException();
      }
      return new URI(EntityUtils.toString(response.getEntity()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getUsableSpace()
   */
  @Override
  public long getUsableSpace() {
    return (Long) (getStorageReport().get("usable"));
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#put(java.lang.String, java.lang.String,
   *      java.lang.String, java.io.InputStream)
   */
  @Override
  public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in) {
    String url = UrlSupport.concat(new String[] { "mp", mediaPackageID, mediaPackageElementID });
    HttpPost post = new HttpPost(url);
    MultipartEntity entity = new MultipartEntity();
    ContentBody body = new InputStreamBody(in, filename);
    entity.addPart("file", body);
    post.setEntity(entity);
    HttpResponse response = null;
    try {
      response = getResponse(post);
      String content = EntityUtils.toString(response.getEntity());
      return new URI(content);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#putInCollection(java.lang.String,
   *      java.lang.String, java.io.InputStream)
   */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) {
    String url = UrlSupport.concat(new String[] { "collection", collectionId });
    HttpPost post = new HttpPost(url);
    MultipartEntity entity = new MultipartEntity();
    ContentBody body = new InputStreamBody(in, fileName);
    entity.addPart("file", body);
    post.setEntity(entity);
    HttpResponse response = null;
    try {
      response = getResponse(post);
      String content = EntityUtils.toString(response.getEntity());
      return new URI(content);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#deleteFromCollection(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public boolean deleteFromCollection(String collectionId, String fileName) {
    String url = UrlSupport.concat(new String[] { "collection", collectionId, fileName });
    System.out.println("");
    HttpDelete del = new HttpDelete(url);
    HttpResponse response = null;
    try {
      response = getResponse(del, HttpStatus.SC_OK, HttpStatus.SC_NOT_FOUND);
      if (response == null)
        throw new RuntimeException("Error removing file");
      return HttpStatus.SC_OK == response.getStatusLine().getStatusCode();
    } finally {
      closeConnection(response);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getBaseUri()
   */
  @Override
  public URI getBaseUri() {
    HttpGet get = new HttpGet("/baseUri");
    HttpResponse response = null;
    try {
      response = getResponse(get);
      return new URI(EntityUtils.toString(response.getEntity(), "UTF-8"));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to determine the base URI of the file repository");
    } finally {
      closeConnection(response);
    }
  }

}
