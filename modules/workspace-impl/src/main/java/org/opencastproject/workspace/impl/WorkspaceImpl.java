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

package org.opencastproject.workspace.impl;

import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.EqualsUtil.ne;
import static org.opencastproject.util.IoSupport.locked;
import static org.opencastproject.util.PathSupport.path;
import static org.opencastproject.util.RequireUtil.notNull;
import static org.opencastproject.util.data.Arrays.cons;
import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Prelude.sleep;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.HttpUtil;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.util.jmx.JmxUtil;
import org.opencastproject.workingfilerepository.api.PathMappable;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;
import org.opencastproject.workspace.impl.jmx.WorkspaceBean;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.params.BasicHttpParams;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.ObjectInstance;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.UriBuilder;

/**
 * Implements a simple cache for remote URIs. Delegates methods to {@link WorkingFileRepository} wherever possible.
 * <p>
 * Note that if you are running the workspace on the same machine as the singleton working file repository, you can save
 * a lot of space if you configure both root directories onto the same volume (that is, if your file system supports
 * hard links).
 *
 * TODO Implement cache invalidation using the caching headers, if provided, from the remote server.
 */
public final class WorkspaceImpl implements Workspace {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(WorkspaceImpl.class);

  /** Configuration key for the workspace root directory */
  public static final String WORKSPACE_DIR_KEY = "org.opencastproject.workspace.rootdir";
  /** Configuration key for the storage directory */
  public static final String STORAGE_DIR_KEY = "org.opencastproject.storage.dir";
  /** Configuration key for garbage collection period. */
  public static final String WORKSPACE_CLEANUP_PERIOD_KEY = "org.opencastproject.workspace.cleanup.period";
  /** Configuration key for garbage collection max age. */
  public static final String WORKSPACE_CLEANUP_MAX_AGE_KEY = "org.opencastproject.workspace.cleanup.max.age";

  /** Workspace JMX type */
  private static final String JMX_WORKSPACE_TYPE = "Workspace";

  /** Unknown file name string */
  private static final String UNKNOWN_FILENAME = "unknown";

  /** The JMX workspace bean */
  private WorkspaceBean workspaceBean = new WorkspaceBean(this);

  /** The JMX bean object instance */
  private ObjectInstance registeredMXBean;

  private final Object lock = new Object();

  /** The workspace root directory */
  private String wsRoot = null;

  /** If true, hardlinking can be done between working file repository and workspace */
  private boolean linkingEnabled = false;

  private TrustedHttpClient trustedHttpClient;

  /** The working file repository */
  private WorkingFileRepository wfr = null;

  /** The path mappable */
  private PathMappable pathMappable = null;

  private CopyOnWriteArraySet<String> staticCollections = new CopyOnWriteArraySet<String>();

  private boolean waitForResourceFlag = false;

  /** The workspce cleaner */
  private WorkspaceCleaner workspaceCleaner = null;

  public WorkspaceImpl() {
  }

  /**
   * Creates a workspace implementation which is located at the given root directory.
   * <p>
   * Note that if you are running the workspace on the same machine as the singleton working file repository, you can
   * save a lot of space if you configure both root directories onto the same volume (that is, if your file system
   * supports hard links).
   *
   * @param rootDirectory
   *          the repository root directory
   */
  public WorkspaceImpl(String rootDirectory, boolean waitForResource) {
    this.wsRoot = rootDirectory;
    this.waitForResourceFlag = waitForResource;
  }

  /**
   * Check is a property exists in a given bundle context.
   *
   * @param cc
   *          the OSGi component context
   * @param prop
   *          property to check for.
   */
  private boolean ensureContextProp(ComponentContext cc, String prop) {
    return cc != null && cc.getBundleContext().getProperty(prop) != null;
  }

  /**
   * OSGi service activation callback.
   *
   * @param cc
   *          the OSGi component context
   */
  public void activate(ComponentContext cc) {
    if (this.wsRoot == null) {
      if (ensureContextProp(cc, WORKSPACE_DIR_KEY)) {
        // use rootDir from CONFIG
        this.wsRoot = cc.getBundleContext().getProperty(WORKSPACE_DIR_KEY);
        logger.info("CONFIG " + WORKSPACE_DIR_KEY + ": " + this.wsRoot);
      } else if (ensureContextProp(cc, STORAGE_DIR_KEY)) {
        // create rootDir by adding "workspace" to the default data directory
        this.wsRoot = PathSupport.concat(cc.getBundleContext().getProperty(STORAGE_DIR_KEY), "workspace");
        logger.warn("CONFIG " + WORKSPACE_DIR_KEY + " is missing: falling back to " + this.wsRoot);
      } else {
        throw new IllegalStateException("Configuration '" + WORKSPACE_DIR_KEY + "' is missing");
      }
    }

    // Create the root directory
    File f = new File(this.wsRoot);
    if (!f.exists()) {
      try {
        FileUtils.forceMkdir(f);
      } catch (Exception e) {
        throw new IllegalStateException("Could not create workspace directory.", e);
      }
    }

    // Test whether hard linking between working file repository and workspace is possible
    if (pathMappable != null) {
      String wfrRoot = pathMappable.getPathPrefix();
      File srcFile = new File(wfrRoot, ".linktest");
      try {
        FileUtils.touch(srcFile);
      } catch (IOException e) {
        throw new IllegalStateException("The working file repository seems read-only", e);
      }

      // Create a unique target file
      File targetFile = null;
      try {
        targetFile = File.createTempFile(".linktest.", ".tmp", new File(wsRoot));
        targetFile.delete();
      } catch (IOException e) {
        throw new IllegalStateException("The workspace seems read-only", e);
      }

      // Test hard linking
      linkingEnabled = FileSupport.supportsLinking(srcFile, targetFile);

      // Clean up
      FileUtils.deleteQuietly(targetFile);

      if (linkingEnabled)
        logger.info("Hard links between the working file repository and the workspace enabled");
      else {
        logger.warn("Hard links between the working file repository and the workspace are not possible");
        logger.warn("This will increase the overall amount of disk space used");
      }
    }

    // Set up the garbage collection timer
    int garbageCollectionPeriodInSeconds = -1;
    if (ensureContextProp(cc, WORKSPACE_CLEANUP_PERIOD_KEY)) {
      String period = cc.getBundleContext().getProperty(WORKSPACE_CLEANUP_PERIOD_KEY);
      try {
        garbageCollectionPeriodInSeconds = Integer.parseInt(period);
      } catch (NumberFormatException e) {
        logger.warn("Invalid configuration for workspace garbage collection period ({}={})",
                WORKSPACE_CLEANUP_PERIOD_KEY, period);
        garbageCollectionPeriodInSeconds = -1;
      }
    }

    // Activate garbage collection
    int maxAgeInSeconds = -1;
    if (ensureContextProp(cc, WORKSPACE_CLEANUP_MAX_AGE_KEY)) {
      String age = cc.getBundleContext().getProperty(WORKSPACE_CLEANUP_MAX_AGE_KEY);
      try {
        maxAgeInSeconds = Integer.parseInt(age);
      } catch (NumberFormatException e) {
        logger.warn("Invalid configuration for workspace garbage collection max age ({}={})",
                WORKSPACE_CLEANUP_MAX_AGE_KEY, age);
        maxAgeInSeconds = -1;
      }
    }

    registeredMXBean = JmxUtil.registerMXBean(workspaceBean, JMX_WORKSPACE_TYPE);

    // Start cleanup scheduler if we have sensible cleanup values:
    if (garbageCollectionPeriodInSeconds > 0) {
      workspaceCleaner = new WorkspaceCleaner(this, garbageCollectionPeriodInSeconds, maxAgeInSeconds);
      workspaceCleaner.schedule();
    }

    // Initialize the list of static collections
    // TODO MH-12440 replace with a different mechanism that doesn't hardcode collection names
    staticCollections.add("archive");
    staticCollections.add("captions");
    staticCollections.add("composer");
    staticCollections.add("composite");
    staticCollections.add("coverimage");
    staticCollections.add("executor");
    staticCollections.add("inbox");
    staticCollections.add("ocrtext");
    staticCollections.add("sox");
    staticCollections.add("uploaded");
    staticCollections.add("videoeditor");
    staticCollections.add("videosegments");
    staticCollections.add("waveform");

  }

  /** Callback from OSGi on service deactivation. */
  public void deactivate() {
    JmxUtil.unregisterMXBean(registeredMXBean);
    if (workspaceCleaner != null) {
      workspaceCleaner.shutdown();
    }
  }

  @Override
  public File get(final URI uri) throws NotFoundException, IOException {
    return get(uri, false);
  }

  @Override
  public File get(final URI uri, final boolean uniqueFilename) throws NotFoundException, IOException {
    File inWs = toWorkspaceFile(uri);

    if (uniqueFilename) {
      inWs = new File(FilenameUtils.removeExtension(inWs.getAbsolutePath()) + '-' + UUID.randomUUID() + '.'
              + FilenameUtils.getExtension(inWs.getName()));
      logger.debug("Created unique filename: {}", inWs);
    }

    if (pathMappable != null && StringUtils.isNotBlank(pathMappable.getPathPrefix())
            && StringUtils.isNotBlank(pathMappable.getUrlPrefix())) {
      if (uri.toString().startsWith(pathMappable.getUrlPrefix())) {
        final String localPath = uri.toString().substring(pathMappable.getUrlPrefix().length());
        final File wfrCopy = workingFileRepositoryFile(localPath);
        // does the file exist and is it up to date?
        logger.trace("Looking up {} at {}", uri.toString(), wfrCopy.getAbsolutePath());
        if (wfrCopy.isFile()) {
          final Long workspaceFileLastModified = inWs.isFile() ? inWs.lastModified() : 0L;
          // if the file exists in the workspace, but is older than the wfr copy, replace it
          if (workspaceFileLastModified < wfrCopy.lastModified()) {
            logger.debug("Replacing {} with an updated version from the file repository", inWs.getAbsolutePath());
            locked(inWs, copyOrLink(wfrCopy));
          } else {
            logger.debug("{} is up to date", inWs);
          }
          logger.debug("Getting {} directly from working file repository root at {}", uri, inWs);
          return new File(inWs.getAbsolutePath());
        } else {
          logger.warn("The working file repository and workspace paths don't match. Looking up {} at {} failed",
                  uri.toString(), wfrCopy.getAbsolutePath());
        }
      }
    }
    // do HTTP transfer
    return locked(inWs, downloadIfNecessary(uri));
  }

  @Override
  public InputStream read(final URI uri) throws NotFoundException, IOException {

    if (pathMappable != null) {
      if (uri.toString().startsWith(pathMappable.getUrlPrefix())) {
        final String localPath = uri.toString().substring(pathMappable.getUrlPrefix().length());
        final File wfrCopy = workingFileRepositoryFile(localPath);
        // does the file exist?
        logger.trace("Looking up {} at {} for read", uri, wfrCopy);
        if (wfrCopy.isFile()) {
          logger.debug("Getting {} directly from working file repository root at {} for read", uri, wfrCopy);
          return new FileInputStream(wfrCopy);
        }
        logger.warn("The working file repository URI and paths don't match. Looking up {} at {} failed", uri, wfrCopy);
      }
    }

    // fall back to get() which should download the file into local workspace if necessary
    return new DeleteOnCloseFileInputStream(get(uri, true));
  }

  /** Copy or link <code>src</code> to <code>dst</code>. */
  private void copyOrLink(final File src, final File dst) throws IOException {
    if (linkingEnabled) {
      FileUtils.deleteQuietly(dst);
      FileSupport.link(src, dst);
    } else {
      FileSupport.copy(src, dst);
    }
  }

  /** {@link #copyOrLink(java.io.File, java.io.File)} as an effect. <code>src -> dst -> ()</code> */
  private Effect<File> copyOrLink(final File src) {
    return new Effect.X<File>() {
      @Override
      protected void xrun(File dst) throws IOException {
        copyOrLink(src, dst);
      }
    };
  }

  /**
   * Handle the HTTP response.
   *
   * @return either a token to initiate a follow-up request or a file or none if the requested URI cannot be found
   * @throws IOException
   *           in case of any IO related issues
   */
  private Either<String, Option<File>> handleDownloadResponse(HttpResponse response, URI src, File dst)
          throws IOException {
    final String url = src.toString();
    final int status = response.getStatusLine().getStatusCode();
    switch (status) {
      case HttpServletResponse.SC_NOT_FOUND:
        return right(none(File.class));
      case HttpServletResponse.SC_NOT_MODIFIED:
        logger.debug("{} has not been modified.", url);
        return right(some(dst));
      case HttpServletResponse.SC_ACCEPTED:
        logger.debug("{} is not ready, try again later.", url);
        return left(response.getHeaders("token")[0].getValue());
      case HttpServletResponse.SC_OK:
        logger.info("Downloading {} to {}", url, dst.getAbsolutePath());
        return right(some(downloadTo(response, dst)));
      default:
        logger.warn(format("Received unexpected response status %s while trying to download from %s", status, url));
        FileUtils.deleteQuietly(dst);
        return right(none(File.class));
    }
  }

  /**
   * {@link #handleDownloadResponse(org.apache.http.HttpResponse, java.net.URI, java.io.File)} as a function.
   * <code>(URI, dst_file) -> HttpResponse -> Either token (Option File)</code>
   */
  private Function<HttpResponse, Either<String, Option<File>>> handleDownloadResponse(final URI src, final File dst) {
    return new Function.X<HttpResponse, Either<String, Option<File>>>() {
      @Override
      public Either<String, Option<File>> xapply(HttpResponse response) throws Exception {
        return handleDownloadResponse(response, src, dst);
      }
    };
  }

  /** Create a get request to the given URI. */
  private HttpGet createGetRequest(final URI src, final File dst, Tuple<String, String>... params) throws IOException {
    final String url = src.toString();
    final HttpGet get = new HttpGet(url);
    // if the destination file already exists add the If-None-Match header
    if (dst.isFile() && dst.length() > 0) {
      get.setHeader("If-None-Match", md5(dst));
    }
    for (final Tuple<String, String> a : params) {
      get.setParams(new BasicHttpParams().setParameter(a.getA(), a.getB()));
    }
    return get;
  }

  /**
   * Download content of <code>uri</code> to file <code>dst</code> only if necessary, i.e. either the file does not yet
   * exist in the workspace or a newer version is available at <code>uri</code>.
   *
   * @return the file
   */
  private File downloadIfNecessary(final URI src, final File dst) throws IOException, NotFoundException {
    HttpGet get = createGetRequest(src, dst);
    while (true) {
      // run the http request and handle its response
      final Either<Exception, Either<String, Option<File>>> result = trustedHttpClient
              .<Either<String, Option<File>>> runner(get).run(handleDownloadResponse(src, dst));
      // handle to result of response processing
      // right: there's an expected result
      for (Either<String, Option<File>> a : result.right()) {
        // right: either a file could be found or not
        for (Option<File> ff : a.right()) {
          for (File f : ff) {
            return f;
          }
          FileUtils.deleteQuietly(dst);
          // none
          throw new NotFoundException();
        }
        // left: file will be ready later
        for (String token : a.left()) {
          get = createGetRequest(src, dst, tuple("token", token));
          sleep(60000);
        }
      }
      // left: an exception occurred
      for (Exception e : result.left()) {
        logger.warn(format("Could not copy %s to %s: %s", src.toString(), dst.getAbsolutePath(), e.getMessage()));
        FileUtils.deleteQuietly(dst);
        throw new NotFoundException(e);
      }
    }
  }

  /**
   * {@link #downloadIfNecessary(java.net.URI, java.io.File)} as a function.
   * <code>src_uri -&gt; dst_file -&gt; dst_file</code>
   */
  private Function<File, File> downloadIfNecessary(final URI src) {
    return new Function.X<File, File>() {
      @Override
      public File xapply(final File dst) throws Exception {
        return downloadIfNecessary(src, dst);
      }
    };
  }

  /**
   * Download content of an HTTP response to a file.
   *
   * @return the destination file
   */
  private static File downloadTo(final HttpResponse response, final File dst) throws IOException {
    // ignore return value
    dst.createNewFile();
    InputStream in = null;
    OutputStream out = null;
    try {
      in = response.getEntity().getContent();
      out = new FileOutputStream(dst);
      IOUtils.copyLarge(in, out);
    } finally {
      IoSupport.closeQuietly(in);
      IoSupport.closeQuietly(out);
    }
    return dst;
  }

  /**
   * Returns the md5 of a file
   *
   * @param file
   *          the source file
   * @return the md5 hash
   * @throws IOException
   *           if the file cannot be accessed
   * @throws IllegalArgumentException
   *           if <code>file</code> is <code>null</code>
   * @throws IllegalStateException
   *           if <code>file</code> does not exist or is not a regular file
   */
  protected String md5(File file) throws IOException, IllegalArgumentException, IllegalStateException {
    if (file == null)
      throw new IllegalArgumentException("File must not be null");
    if (!file.isFile())
      throw new IllegalArgumentException("File " + file.getAbsolutePath() + " can not be read");

    InputStream in = null;
    try {
      in = new FileInputStream(file);
      return DigestUtils.md5Hex(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  @Override
  public void delete(URI uri) throws NotFoundException, IOException {

    String uriPath = uri.toString();
    String[] uriElements = uriPath.split("/");
    String collectionId = null;
    boolean isMediaPackage = false;

    logger.trace("delete {}", uriPath);

    if (uriPath.startsWith(wfr.getBaseUri().toString())) {
      if (uriPath.indexOf(WorkingFileRepository.COLLECTION_PATH_PREFIX) > 0) {
        if (uriElements.length > 2) {
          collectionId = uriElements[uriElements.length - 2];
          String filename = uriElements[uriElements.length - 1];
          wfr.deleteFromCollection(collectionId, filename);
        }
      } else if (uriPath.indexOf(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX) > 0) {
        isMediaPackage = true;
        if (uriElements.length >= 3) {
          String mediaPackageId = uriElements[uriElements.length - 3];
          String elementId = uriElements[uriElements.length - 2];
          wfr.delete(mediaPackageId, elementId);
        }
      }
    }

    // Remove the file and optionally its parent directory if empty
    File f = toWorkspaceFile(uri);
    if (f.isFile()) {
      synchronized (lock) {
        File mpElementDir = f.getParentFile();
        FileUtils.forceDelete(f);

        // Remove containing folder if a mediapackage element or a not a static collection
        if (isMediaPackage || !isStaticCollection(collectionId))
          FileSupport.delete(mpElementDir);

        // Also delete mediapackage itself when empty
        if (isMediaPackage)
          FileSupport.delete(mpElementDir.getParentFile());
      }
    }

    // wait for WFR
    waitForResource(uri, HttpServletResponse.SC_NOT_FOUND, "File %s does not disappear in WFR");
  }

  @Override
  public void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException {
    // delete locally
    final File f = workspaceFile(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, mediaPackageID, mediaPackageElementID);
    FileUtils.deleteQuietly(f);
    FileSupport.delete(f.getParentFile());
    // delete in WFR
    wfr.delete(mediaPackageID, mediaPackageElementID);
    // todo check in WFR
  }

  @Override
  public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in)
          throws IOException {
    String safeFileName = PathSupport.toSafeName(fileName);
    final URI uri = wfr.getURI(mediaPackageID, mediaPackageElementID, fileName);
    notNull(in, "in");

    // Determine the target location in the workspace
    File workspaceFile = null;
    FileOutputStream out = null;
    synchronized (lock) {
      workspaceFile = toWorkspaceFile(uri);
      FileUtils.touch(workspaceFile);
    }

    // Try hard linking first and fall back to tee-ing to both the working file repository and the workspace
    if (linkingEnabled) {
      // The WFR stores an md5 hash along with the file, so we need to use the API and not try to write (link) the file
      // there ourselves
      wfr.put(mediaPackageID, mediaPackageElementID, fileName, in);
      File workingFileRepoDirectory = workingFileRepositoryFile(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
              mediaPackageID, mediaPackageElementID);
      File workingFileRepoCopy = new File(workingFileRepoDirectory, safeFileName);
      FileSupport.link(workingFileRepoCopy, workspaceFile, true);
    } else {
      InputStream tee = null;
      try {
        out = new FileOutputStream(workspaceFile);
        tee = new TeeInputStream(in, out, true);
        wfr.put(mediaPackageID, mediaPackageElementID, fileName, tee);
      } finally {
        IOUtils.closeQuietly(tee);
        IOUtils.closeQuietly(out);
      }
    }
    // wait until the file appears on the WFR node
    waitForResource(uri, HttpServletResponse.SC_OK, "File %s does not appear in WFR");
    return uri;
  }

  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in) throws IOException {
    String safeFileName = PathSupport.toSafeName(fileName);
    URI uri = wfr.getCollectionURI(collectionId, fileName);

    // Determine the target location in the workspace
    InputStream tee = null;
    File tempFile = null;
    FileOutputStream out = null;
    try {
      synchronized (lock) {
        tempFile = toWorkspaceFile(uri);
        FileUtils.touch(tempFile);
        out = new FileOutputStream(tempFile);
      }

      // Try hard linking first and fall back to tee-ing to both the working file repository and the workspace
      if (linkingEnabled) {
        tee = in;
        wfr.putInCollection(collectionId, fileName, tee);
        FileUtils.forceMkdir(tempFile.getParentFile());
        File workingFileRepoDirectory = workingFileRepositoryFile(WorkingFileRepository.COLLECTION_PATH_PREFIX,
                collectionId);
        File workingFileRepoCopy = new File(workingFileRepoDirectory, safeFileName);
        FileSupport.link(workingFileRepoCopy, tempFile, true);
      } else {
        tee = new TeeInputStream(in, out, true);
        wfr.putInCollection(collectionId, fileName, tee);
      }
    } catch (IOException e) {
      FileUtils.deleteQuietly(tempFile);
      throw e;
    } finally {
      IoSupport.closeQuietly(tee);
      IoSupport.closeQuietly(out);
    }
    waitForResource(uri, HttpServletResponse.SC_OK, "File %s does not appear in WFR");
    return uri;
  }

  @Override
  public URI getURI(String mediaPackageID, String mediaPackageElementID) {
    return wfr.getURI(mediaPackageID, mediaPackageElementID);
  }

  @Override
  public URI getURI(String mediaPackageID, String mediaPackageElementID, String filename) {
    return wfr.getURI(mediaPackageID, mediaPackageElementID, filename);
  }

  @Override
  public URI getCollectionURI(String collectionID, String fileName) {
    return wfr.getCollectionURI(collectionID, fileName);
  }

  @Override
  public URI copyTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
          throws NotFoundException, IOException {
    String path = collectionURI.toString();
    String filename = FilenameUtils.getName(path);
    String collection = getCollection(collectionURI);

    // Copy the local file
    final File original = toWorkspaceFile(collectionURI);
    if (original.isFile()) {
      URI copyURI = wfr.getURI(toMediaPackage, toMediaPackageElement, filename);
      File copy = toWorkspaceFile(copyURI);
      FileUtils.forceMkdir(copy.getParentFile());
      FileSupport.link(original, copy);
    }

    // Tell working file repository
    final URI wfrUri = wfr.copyTo(collection, filename, toMediaPackage, toMediaPackageElement, toFileName);
    // wait for WFR
    waitForResource(wfrUri, SC_OK, "File %s does not appear in WFR");
    return wfrUri;
  }

  @Override
  public URI moveTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
          throws NotFoundException, IOException {
    String path = collectionURI.toString();
    String filename = FilenameUtils.getName(path);
    String collection = getCollection(collectionURI);
    logger.debug("Moving {} from {} to {}/{}", filename, collection, toMediaPackage, toMediaPackageElement);
    // move locally
    File original = toWorkspaceFile(collectionURI);
    if (original.isFile()) {
      URI copyURI = wfr.getURI(toMediaPackage, toMediaPackageElement, toFileName);
      File copy = toWorkspaceFile(copyURI);
      FileUtils.forceMkdir(copy.getParentFile());
      FileUtils.deleteQuietly(copy);
      FileUtils.moveFile(original, copy);
      if (!isStaticCollection(collection))
        FileSupport.delete(original.getParentFile());
    }
    // move in WFR
    final URI wfrUri = wfr.moveTo(collection, filename, toMediaPackage, toMediaPackageElement, toFileName);
    // wait for WFR
    waitForResource(wfrUri, SC_OK, "File %s does not appear in WFR");
    return wfrUri;
  }

  @Override
  public URI[] getCollectionContents(String collectionId) throws NotFoundException {
    return wfr.getCollectionContents(collectionId);
  }

  @Override
  public void deleteFromCollection(String collectionId, String fileName, boolean removeCollection) throws NotFoundException, IOException {
    // local delete
    final File f = workspaceFile(WorkingFileRepository.COLLECTION_PATH_PREFIX, collectionId,
            PathSupport.toSafeName(fileName));
    FileUtils.deleteQuietly(f);
    if (removeCollection) {
      FileSupport.delete(f.getParentFile());
    }
    // delete in WFR
    try {
      wfr.deleteFromCollection(collectionId, fileName, removeCollection);
    } catch (IllegalArgumentException e) {
      throw new NotFoundException(e);
    }
    // wait for WFR
    waitForResource(wfr.getCollectionURI(collectionId, fileName), SC_NOT_FOUND, "File %s does not disappear in WFR");
  }

  @Override
  public void deleteFromCollection(String collectionId, String fileName) throws NotFoundException, IOException {
    deleteFromCollection(collectionId, fileName, false);
  }

  /**
   * Transforms a URI into a workspace File. If the file comes from the working file repository, the path in the
   * workspace mirrors that of the repository. If the file comes from another source, directories are created for each
   * segment of the URL. Sub-directories may be created as needed.
   *
   * @param uri
   *          the uri
   * @return the local file representation
   */
  File toWorkspaceFile(URI uri) {
    // MH-11497: Fix for compatibility with stream security: the query parameters are deleted.
    // TODO Refactor this class to use the URI class and methods instead of String for handling URIs
    String uriString = UriBuilder.fromUri(uri).replaceQuery(null).build().toString();
    String wfrPrefix = wfr.getBaseUri().toString();
    String serverPath = FilenameUtils.getPath(uriString);
    if (uriString.startsWith(wfrPrefix)) {
      serverPath = serverPath.substring(wfrPrefix.length());
    } else {
      serverPath = serverPath.replaceAll(":/*", "_");
    }
    String wsDirectoryPath = PathSupport.concat(wsRoot, serverPath);
    File wsDirectory = new File(wsDirectoryPath);
    wsDirectory.mkdirs();

    String safeFileName = PathSupport.toSafeName(FilenameUtils.getName(uriString));
    if (StringUtils.isBlank(safeFileName))
      safeFileName = UNKNOWN_FILENAME;
    return new File(wsDirectory, safeFileName);
  }

  /** Return a file object pointing into the workspace. */
  private File workspaceFile(String... path) {
    return new File(path(cons(String.class, wsRoot, path)));
  }

  /** Return a file object pointing into the working file repository. */
  private File workingFileRepositoryFile(String... path) {
    return new File(path(cons(String.class, pathMappable.getPathPrefix(), path)));
  }

  /**
   * Returns the working file repository collection.
   * <p>
   *
   * <pre>
   * http://localhost:8080/files/collection/&lt;collection&gt;/ -> &lt;collection&gt;
   * </pre>
   *
   * @param uri
   *          the working file repository collection uri
   * @return the collection name
   */
  private String getCollection(URI uri) {
    String path = uri.toString();
    if (path.indexOf(WorkingFileRepository.COLLECTION_PATH_PREFIX) < 0)
      throw new IllegalArgumentException(uri + " must point to a working file repository collection");

    String collection = FilenameUtils.getPath(path);
    if (collection.endsWith("/"))
      collection = collection.substring(0, collection.length() - 1);
    collection = collection.substring(collection.lastIndexOf("/"));
    collection = collection.substring(collection.lastIndexOf("/") + 1, collection.length());
    return collection;
  }

  private boolean isStaticCollection(String collection) {
    return staticCollections.contains(collection);
  }

  @Override
  public Option<Long> getTotalSpace() {
    return some(new File(wsRoot).getTotalSpace());
  }

  @Override
  public Option<Long> getUsableSpace() {
    return some(new File(wsRoot).getUsableSpace());
  }

  @Override
  public Option<Long> getUsedSpace() {
    return some(FileUtils.sizeOfDirectory(new File(wsRoot)));
  }

  @Override
  public URI getBaseUri() {
    return wfr.getBaseUri();
  }

  public void setRepository(WorkingFileRepository repo) {
    this.wfr = repo;
    if (repo instanceof PathMappable) {
      this.pathMappable = (PathMappable) repo;
      logger.info("Mapping workspace to working file repository using {}", pathMappable.getPathPrefix());
    }
  }

  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.trustedHttpClient = trustedHttpClient;
  }

  private static final long TIMEOUT = 2L * 60L * 1000L;
  private static final long INTERVAL = 1000L;

  private void waitForResource(final URI uri, final int expectedStatus, final String errorMsg) throws IOException {
    if (waitForResourceFlag) {
      HttpUtil.waitForResource(trustedHttpClient, uri, expectedStatus, TIMEOUT, INTERVAL)
              .fold(Misc.<Exception, Void> chuck(), new Effect.X<Integer>() {
                @Override
                public void xrun(Integer status) throws Exception {
                  if (ne(status, expectedStatus)) {
                    final String msg = format(errorMsg, uri.toString());
                    logger.warn(msg);
                    throw new IOException(msg);
                  }
                }
              });
    }
  }

  @Override
  public void cleanup(final int maxAgeInSeconds) {
    // Cancel cleanup if we do not have a valid setting for the maximum file age
    if (maxAgeInSeconds < 0) {
      logger.debug("Canceling cleanup of workspace due to maxAge ({}) <= 0", maxAgeInSeconds);
      return;
    }

    // Warn if time is very short since this operation is dangerous and *should* only be a fallback for if stuff
    // remained in the workspace due to some errors. If we have a very short maxAge, we may delete file which are
    // currently being processed. The warn value is 2 days:
    if (maxAgeInSeconds < 60 * 60 * 24 * 2) {
      logger.warn("The max age for the workspace cleaner is dangerously low. Please consider increasing the value to "
              + "avoid deleting data in use by running workflows.");
    }

    // Get workspace root directly
    final File workspaceDirectory = new File(wsRoot);
    logger.info("Starting cleanup of workspace at {}", workspaceDirectory);

    long now = new Date().getTime();
    for (File file: FileUtils.listFiles(workspaceDirectory, null, true)) {
      long fileLastModified = file.lastModified();
      // Ensure file/dir is older than maxAge
      long fileAgeInSeconds = (now - fileLastModified) / 1000;
      if (fileLastModified == 0 || fileAgeInSeconds < maxAgeInSeconds) {
        logger.debug("File age ({}) < max age ({}) or unknown: Skipping {} ", fileAgeInSeconds, maxAgeInSeconds, file);
        continue;
      }

      // Delete old files
      if (FileUtils.deleteQuietly(file)) {
        logger.info("Deleted {}", file);
      } else {
        logger.warn("Could not delete {}", file);
      }
    }
    logger.info("Finished cleanup of workspace");
  }

  @Override
  public void cleanup(Id mediaPackageId) throws IOException {
    cleanup(mediaPackageId, false);
  }

  @Override
  public void cleanup(Id mediaPackageId, boolean filesOnly) throws IOException {
    final File mediaPackageDir = workspaceFile(WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX, mediaPackageId.toString());

    if (filesOnly) {
      logger.debug("Clean workspace media package directory {} (files only)", mediaPackageDir);
      FileSupport.delete(mediaPackageDir, FileSupport.DELETE_FILES);
    }
    else {
      logger.debug("Clean workspace media package directory {}", mediaPackageDir);
      FileUtils.deleteDirectory(mediaPackageDir);
    }
  }

  @Override
  public String rootDirectory() {
    return wsRoot;
  }

  private class DeleteOnCloseFileInputStream extends FileInputStream {
    private File file;

    DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
      super(file);
      this.file = file;
    }

    public void close() throws IOException {
      try {
        super.close();
      } finally {
        if (file != null) {
          logger.debug("Cleaning up {}", file);
          file.delete();
          file = null;
        }
      }
    }
  }
}
