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

package org.opencastproject.workingfilerepository.remote;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;

import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * A remote service proxy for a working file repository
 */
@Component(
  property = {
    "service.description=Working File Repository Remote Service Proxy"
  },
  immediate = true,
  service = { WorkingFileRepository.class }
)
public class WorkingFileRepositoryRemoteImpl extends RemoteBase implements WorkingFileRepository {

  /** the logger */
  private static final Logger logger = LoggerFactory.getLogger(WorkingFileRepositoryRemoteImpl.class);

  public WorkingFileRepositoryRemoteImpl() {
    super(SERVICE_TYPE);
  }

  /**
   * Sets the trusted http client
   *
   * @param client
   */
  @Override
  @Reference
  public void setTrustedHttpClient(TrustedHttpClient client) {
    super.setTrustedHttpClient(client);
  }

  /**
   * Sets the remote service manager.
   *
   * @param remoteServiceManager
   */
  @Override
  @Reference
  public void setRemoteServiceManager(ServiceRegistry remoteServiceManager) {
    super.setRemoteServiceManager(remoteServiceManager);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#moveTo(java.lang.String, java.lang.String,
   *      java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public URI moveTo(String fromCollection, String fromFileName, String toMediaPackage, String toMediaPackageElement,
          String toFileName) throws IOException, NotFoundException {
    String urlSuffix = UrlSupport.concat(
            new String[] { "move", fromCollection, fromFileName, toMediaPackage, toMediaPackageElement, toFileName });
    HttpPost post = new HttpPost(urlSuffix);
    HttpResponse response = getResponse(post, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("File from collection to move not found: " + fromCollection + "/" + fromFileName);
        } else {
          URI uri = new URI(EntityUtils.toString(response.getEntity(), "UTF-8"));
          logger.info("Moved collection file {}/{} to {}", fromCollection, fromFileName, uri);
          return uri;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Unable to move file", e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to move file from collection");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#delete(java.lang.String, java.lang.String)
   */
  @Override
  public boolean delete(String mediaPackageID, String mediaPackageElementID) {
    String urlSuffix = UrlSupport
            .concat(new String[] { MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID });
    HttpDelete del = new HttpDelete(urlSuffix);
    HttpResponse response = getResponse(del, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        return HttpStatus.SC_OK == response.getStatusLine().getStatusCode();
      }
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Error removing file");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#get(java.lang.String, java.lang.String)
   */
  @Override
  public InputStream get(String mediaPackageID, String mediaPackageElementID) throws NotFoundException {
    String urlSuffix = UrlSupport
            .concat(new String[] { MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID });
    HttpGet get = new HttpGet(urlSuffix);
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException();
        } else {
          // Do not close this response. It will be closed when the caller closes the input stream
          return new HttpClientClosingInputStream(response);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException();
    }
    throw new RuntimeException("Error getting file");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getCollectionContents(java.lang.String)
   */
  @Override
  public URI[] getCollectionContents(String collectionId) throws NotFoundException {
    String urlSuffix = UrlSupport.concat(new String[] { "list", collectionId + ".json" });
    HttpGet get = new HttpGet(urlSuffix);
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException();
        } else {
          String json = EntityUtils.toString(response.getEntity());
          JSONArray jsonArray = (JSONArray) JSONValue.parse(json);
          URI[] uris = new URI[jsonArray.size()];
          for (int i = 0; i < jsonArray.size(); i++) {
            uris[i] = new URI((String) jsonArray.get(i));
          }
          return uris;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException();
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Error getting collection contents");
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

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#cleanupOldFilesFromCollection
   */
  @Override
  public boolean cleanupOldFilesFromCollection(String collectionId, long days) throws IOException {
    String url = UrlSupport.concat(new String[] { COLLECTION_PATH_PREFIX, collectionId, Long.toString(days) });
    HttpDelete del = new HttpDelete(url);
    HttpResponse response = getResponse(del, SC_NO_CONTENT, SC_NOT_FOUND);
    try {
      if (response != null)
        return SC_NO_CONTENT == response.getStatusLine().getStatusCode();
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Error removing older files from collection");
  }

  @Override
  public boolean cleanupOldFilesFromMediaPackage(long days) throws IOException {
    String url = UrlSupport.concat(new String[] { MEDIAPACKAGE_PATH_PREFIX, Long.toString(days) });
    HttpDelete del = new HttpDelete(url);
    HttpResponse response = getResponse(del, SC_NO_CONTENT);
    try {
      if (response != null) {
        return SC_NO_CONTENT == response.getStatusLine().getStatusCode();
      }
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Error removing older files from collection");
  }

  protected JSONObject getStorageReport() {
    String url = UrlSupport.concat(new String[] { "storage" });
    HttpGet get = new HttpGet(url);
    HttpResponse response = getResponse(get);
    try {
      if (response != null) {
        String json = EntityUtils.toString(response.getEntity());
        return (JSONObject) JSONValue.parse(json);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Error getting storage report");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getTotalSpace()
   */
  @Override
  public Option<Long> getTotalSpace() {
    return Option.some((Long) (getStorageReport().get("size")));
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
    HttpResponse response = getResponse(get);
    try {
      if (response != null) {
        return new URI(EntityUtils.toString(response.getEntity()));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to get collection URI");
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
    HttpResponse response = getResponse(get);
    try {
      if (response != null) {
        return new URI(EntityUtils.toString(response.getEntity()));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to get URI");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getUsableSpace()
   */
  @Override
  public Option<Long> getUsableSpace() {
    return Option.some((Long) (getStorageReport().get("usable")));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getUsedSpace()
   */
  @Override
  public Option<Long> getUsedSpace() {
    return Option.some((Long) (getStorageReport().get("used")));
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#put(java.lang.String, java.lang.String,
   *      java.lang.String, java.io.InputStream)
   */
  @Override
  public URI put(String mediaPackageID, String mediaPackageElementID, String filename, InputStream in) {
    String url = UrlSupport.concat(new String[] { MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID });
    HttpPost post = new HttpPost(url);
    MultipartEntity entity = new MultipartEntity();
    ContentBody body = new InputStreamBody(in, filename);
    entity.addPart("file", body);
    post.setEntity(entity);
    HttpResponse response = getResponse(post);
    try {
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        return new URI(content);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to put file");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#putInCollection(java.lang.String,
   *      java.lang.String, java.io.InputStream)
   */
  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) {
    String url = UrlSupport.concat(new String[] { COLLECTION_PATH_PREFIX, collectionId });
    HttpPost post = new HttpPost(url);
    MultipartEntity entity = new MultipartEntity();
    ContentBody body = new InputStreamBody(in, fileName);
    entity.addPart("file", body);
    post.setEntity(entity);
    HttpResponse response = getResponse(post);
    try {
      if (response != null) {
        String content = EntityUtils.toString(response.getEntity());
        return new URI(content);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Unable to put file in collection");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#deleteFromCollection(java.lang.String,
   *      java.lang.String,boolean)
   */
  @Override
  public boolean deleteFromCollection(String collectionId, String fileName, boolean removeCollection) {
    // The removeCollection parameter is ignored here, as this remote implementation is not currently used.
    String url = UrlSupport.concat(new String[] { COLLECTION_PATH_PREFIX, collectionId, fileName });
    HttpDelete del = new HttpDelete(url);
    HttpResponse response = getResponse(del, SC_NO_CONTENT, SC_NOT_FOUND);
    try {
      if (response != null)
        return SC_NO_CONTENT == response.getStatusLine().getStatusCode();
    } finally {
      closeConnection(response);
    }
    throw new RuntimeException("Error removing file from collection");
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#deleteFromCollection(java.lang.String,
   *      java.lang.String)
   */
  @Override
  public boolean deleteFromCollection(String collectionId, String fileName) {
    return deleteFromCollection(collectionId, fileName, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workingfilerepository.api.WorkingFileRepository#getBaseUri()
   */
  @Override
  public URI getBaseUri() {
    HttpGet get = new HttpGet("/baseUri");
    HttpResponse response = getResponse(get);
    try {
      if (response != null)
        return new URI(EntityUtils.toString(response.getEntity(), "UTF-8"));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to determine the base URI of the file repository");
    } finally {
      closeConnection(response);
    }
    throw new IllegalStateException("Unable to determine the base URI of the file repository");
  }

}
