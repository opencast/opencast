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
package org.opencastproject.distribution.download;

import static java.lang.String.format;
import static org.opencastproject.util.EqualsUtil.ne;
import static org.opencastproject.util.HttpUtil.waitForResource;
import static org.opencastproject.util.PathSupport.path;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.distribution.api.AbstractDistributionService;
import org.opencastproject.distribution.api.DistributionException;
import org.opencastproject.distribution.api.DistributionService;
import org.opencastproject.distribution.api.DownloadDistributionService;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.LoadUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.functions.Misc;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

/**
 * Distributes media to the local media delivery directory.
 */
public class DownloadDistributionServiceImpl extends AbstractDistributionService
        implements DistributionService, DownloadDistributionService, ManagedService {

  /** Logging facility */
  private static final Logger logger = LoggerFactory.getLogger(DownloadDistributionServiceImpl.class);

  /** List of available operations on jobs */
  private enum Operation {
    Distribute, Retract
  }

  /** Receipt type */
  public static final String JOB_TYPE = "org.opencastproject.distribution.download";

  /** Timeout in millis for checking distributed file request */
  private static final long TIMEOUT = 60000L;

  /** The load on the system introduced by creating a distribute job */
  public static final float DEFAULT_DISTRIBUTE_JOB_LOAD = 0.1f;

  /** The load on the system introduced by creating a retract job */
  public static final float DEFAULT_RETRACT_JOB_LOAD = 0.1f;

  /** The key to look for in the service configuration file to override the {@link DEFAULT_DISTRIBUTE_JOB_LOAD} */
  public static final String DISTRIBUTE_JOB_LOAD_KEY = "job.load.download.distribute";

  /** The key to look for in the service configuration file to override the {@link DEFAULT_RETRACT_JOB_LOAD} */
  public static final String RETRACT_JOB_LOAD_KEY = "job.load.download.retract";

  /** The load on the system introduced by creating a distribute job */
  private float distributeJobLoad = DEFAULT_DISTRIBUTE_JOB_LOAD;

  /** The load on the system introduced by creating a retract job */
  private float retractJobLoad = DEFAULT_RETRACT_JOB_LOAD;

  /** Interval time in millis for checking distributed file request */
  private static final long INTERVAL = 300L;

  private Gson gson = new Gson();

  /**
   * Creates a new instance of the download distribution service.
   */
  public DownloadDistributionServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * Activate method for this OSGi service implementation.
   *
   * @param cc
   *          the OSGi component context
   */
  @Override
  public void activate(ComponentContext cc) {
    super.activate(cc);
    serviceUrl = cc.getBundleContext().getProperty("org.opencastproject.download.url");
    if (serviceUrl == null)
      throw new IllegalStateException("Download url must be set (org.opencastproject.download.url)");
    logger.info("Download url is {}", serviceUrl);

    String ccDistributionDirectory = cc.getBundleContext().getProperty("org.opencastproject.download.directory");
    if (ccDistributionDirectory == null)
      throw new IllegalStateException("Distribution directory must be set (org.opencastproject.download.directory)");
    this.distributionDirectory = new File(ccDistributionDirectory);
    logger.info("Download distribution directory is {}", distributionDirectory);
    this.distributionChannel = OsgiUtil.getComponentContextProperty(cc, CONFIG_KEY_STORE_TYPE);
  }

  public String getDistributionType() {
    return this.distributionChannel;
  }

  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, String elementId)
          throws DistributionException, MediaPackageException {
    return distribute(channelId, mediapackage, elementId, true);
  }

  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, String elementId, boolean checkAvailability)
          throws DistributionException, MediaPackageException {
    Set<String> elementIds = new HashSet<String>();
    elementIds.add(elementId);
    return distribute(channelId, mediapackage, elementIds, checkAvailability, false);
  }

  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, Set<String> elementIds, boolean checkAvailability)
          throws DistributionException, MediaPackageException {
    return distribute(channelId, mediapackage, elementIds, checkAvailability, false);
  }

  @Override
  public Job distribute(String channelId, MediaPackage mediapackage, Set<String> elementIds, boolean checkAvailability, boolean preserveReference)
          throws DistributionException, MediaPackageException {
    notNull(mediapackage, "mediapackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");
    try {
      return serviceRegistry.createJob(
              JOB_TYPE,
              Operation.Distribute.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), gson.toJson(elementIds),
                      Boolean.toString(checkAvailability), Boolean.toString(preserveReference)), distributeJobLoad);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  /**
   * Distribute Mediapackage elements to the download distribution service.
   *
   * @param channelId
   #          The id of the publication channel to be distributed to.
   * @param mediapackage
   *          The media package that contains the elements to be distributed.
   * @param elementIds
   *          The ids of the elements that should be distributed contained within the media package.
   * @param checkAvailability
   *          Check the availability of the distributed element via http.
   * @return A reference to the MediaPackageElements that have been distributed.
   * @throws DistributionException
   *           Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
   *           cannot be copied or another unexpected exception occurs.
   */
  public MediaPackageElement[] distributeElements(String channelId, MediaPackage mediapackage, Set<String> elementIds,
          boolean checkAvailability) throws DistributionException {
    return distributeElements(channelId, mediapackage, elementIds, checkAvailability, false);
  }

  /**
   * Distribute Mediapackage elements to the download distribution service.
   *
   * @param channelId
   #          The id of the publication channel to be distributed to.
   * @param mediapackage
   *          The media package that contains the elements to be distributed.
   * @param elementIds
   *          The ids of the elements that should be distributed contained within the media package.
   * @param checkAvailability
   *          Check the availability of the distributed element via http.
   * @param preserveReference
   *          copy actual Reference to the new distributed element
   * @return A reference to the MediaPackageElements that have been distributed.
   * @throws DistributionException
   *           Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
   *           cannot be copied or another unexpected exception occurs.
   */
  public MediaPackageElement[] distributeElements(String channelId, MediaPackage mediapackage, Set<String> elementIds,
          boolean checkAvailability, boolean preserveReference) throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");

    final Set<MediaPackageElement> elements = getElements(channelId, mediapackage, elementIds);
    List<MediaPackageElement> distributedElements = new ArrayList<MediaPackageElement>();

    for (MediaPackageElement element : elements) {
      MediaPackageElement distributedElement = distributeElement(channelId, mediapackage, element, checkAvailability, preserveReference);
      distributedElements.add(distributedElement);
    }
    return distributedElements.toArray(new MediaPackageElement[distributedElements.size()]);
  }

  /**
   * Distribute a Mediapackage element to the download distribution service.
   *
   * @param channelId
   #          The id of the publication channel to be distributed to.
   * @param mediapackage
   *          The media package that contains the element to be distributed.
   * @param element
   *          The the element that should be distributed contained within the media package.
   * @param checkAvailability
   *          Check the availability of the distributed element via http.
   * @return A reference to the MediaPackageElement that has been distributed.
   * @throws DistributionException
   *           Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
   *           cannot be copied or another unexpected exception occurs.
   */
  public MediaPackageElement distributeElement(String channelId, MediaPackage mediapackage, MediaPackageElement element,
          boolean checkAvailability) throws DistributionException {
    return distributeElement(channelId, mediapackage, element, checkAvailability, false);
  }

  /**
   * Distribute a Mediapackage element to the download distribution service.
   *
   * @param channelId
   #          The id of the publication channel to be distributed to.
   * @param mediapackage
   *          The media package that contains the element to be distributed.
   * @param element
   *          The the element that should be distributed contained within the media package.
   * @param checkAvailability
   *          Check the availability of the distributed element via http.
   * @param preserveReference
   *           Copy existing Track-Reference to the new distributed Track
   * @return A reference to the MediaPackageElement that has been distributed.
   * @throws DistributionException
   *           Thrown if the parent directory of the MediaPackageElement cannot be created, if the MediaPackageElement
   *           cannot be copied or another unexpected exception occurs.
   */

  public MediaPackageElement distributeElement(String channelId, MediaPackage mediapackage, MediaPackageElement element,
          boolean checkAvailability, boolean preserveReference) throws DistributionException {

    final String mediapackageId = mediapackage.getIdentifier().compact();
    final String elementId = element.getIdentifier();

    try {
      File source;
      try {
        source = workspace.get(element.getURI());
      } catch (NotFoundException e) {
        throw new DistributionException("Unable to find " + element.getURI() + " in the workspace", e);
      } catch (IOException e) {
        throw new DistributionException("Error loading " + element.getURI() + " from the workspace", e);
      }

      // Try to find a duplicated element source
      try {
        source = findDuplicatedElementSource(source, mediapackageId);
      } catch (IOException e) {
        logger.warn("Unable to find duplicated source {}: {}", source, ExceptionUtils.getMessage(e));
      }

      File destination = getDistributionFile(channelId, mediapackage, element);
      if (!destination.equals(source)) {
        // Put the file in place if sourcesfile differs destinationfile
        try {
          FileUtils.forceMkdir(destination.getParentFile());
        } catch (IOException e) {
          throw new DistributionException("Unable to create " + destination.getParentFile(), e);
        }
        logger.debug("Distributing element {} of media package {} to publication channel {} ({})", elementId,
            mediapackageId, channelId, destination);

        try {
          FileSupport.link(source, destination, true);
        } catch (IOException e) {
          throw new DistributionException(format("Unable to copy %s to %s", source, destination), e);
        }
      }
      // Create a media package element representation of the distributed file
      MediaPackageElement distributedElement = (MediaPackageElement) element.clone();
      try {
        distributedElement.setURI(getDistributionUri(channelId, mediapackageId, element));
        if (preserveReference) {
          distributedElement.setReference(element.getReference());
        }
      } catch (URISyntaxException e) {
        throw new DistributionException("Distributed element produces an invalid URI", e);
      }

      logger.debug("Finished distributing element {} of media package {} to publication channel {}", elementId,
          mediapackageId, channelId);
      final URI uri = distributedElement.getURI();
      if (checkAvailability) {
        logger.debug("Checking availability of distributed artifact {} at {}", distributedElement, uri);
        waitForResource(trustedHttpClient, uri, HttpServletResponse.SC_OK, TIMEOUT, INTERVAL)
                .fold(Misc.<Exception, Void> chuck(), new Effect.X<Integer>() {
                  @Override
                  public void xrun(Integer status) throws Exception {
                    if (ne(status, HttpServletResponse.SC_OK)) {
                      logger.warn("Attempt to access distributed file {} returned code {}", uri, status);
                      throw new DistributionException("Unable to load distributed file " + uri.toString());
                    }
                  }
                });
      }
      return distributedElement;
    } catch (Exception e) {
      logger.warn("Error distributing " + element, e);
      if (e instanceof DistributionException) {
        throw (DistributionException) e;
      } else {
        throw new DistributionException(e);
      }
    }
  }

  @Override
  public Job retract(String channelId, MediaPackage mediapackage, String elementId) throws DistributionException {
    Set<String> elementIds = new HashSet();
    elementIds.add(elementId);
    return retract(channelId, mediapackage, elementIds);
  }

  @Override
  public Job retract(String channelId, MediaPackage mediapackage, Set<String> elementIds)
        throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Retract.toString(),
              Arrays.asList(channelId, MediaPackageParser.getAsXml(mediapackage), gson.toJson(elementIds)),
                   retractJobLoad);
    } catch (ServiceRegistryException e) {
      throw new DistributionException("Unable to create a job", e);
    }
  }

  @Override
  public List<MediaPackageElement> distributeSync(String channelId, MediaPackage mediapackage, Set<String> elementIds,
                                                  boolean checkAvailability) throws DistributionException {
    Job job = null;
    try {
      job = serviceRegistry
          .createJob(
              JOB_TYPE, Operation.Distribute.toString(), null, null, false, distributeJobLoad);
      job.setStatus(Job.Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      final MediaPackageElement[] mediaPackageElements = this.distributeElements(channelId, mediapackage, elementIds, checkAvailability);
      job.setStatus(Job.Status.FINISHED);
      return Arrays.asList(mediaPackageElements);
    } catch (ServiceRegistryException e) {
      throw new DistributionException(e);
    } catch (NotFoundException e) {
      throw new DistributionException("Unable to update distribution job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  @Override
  public List<MediaPackageElement> retractSync(String channelId, MediaPackage mediaPackage, Set<String> elementIds)
      throws DistributionException {
    Job job = null;
    try {
      job = serviceRegistry
          .createJob(
              JOB_TYPE, Operation.Retract.toString(), null, null, false, retractJobLoad);
      job.setStatus(Job.Status.RUNNING);
      job = serviceRegistry.updateJob(job);
      final MediaPackageElement[] mediaPackageElements = this.retractElements(channelId, mediaPackage, elementIds);
      job.setStatus(Job.Status.FINISHED);
      return Arrays.asList(mediaPackageElements);
    } catch (ServiceRegistryException e) {
      throw new DistributionException(e);
    } catch (NotFoundException e) {
      throw new DistributionException("Unable to update retraction job", e);
    } finally {
      finallyUpdateJob(job);
    }
  }

  /**
   * Retract a media package element from the distribution channel. The retracted element must not necessarily be the
   * one given as parameter <code>elementId</code>. Instead, the element's distribution URI will be calculated. This way
   * you are able to retract elements by providing the "original" element here.
   *
   * @param channelId
   *          the channel id
   * @param mediapackage
   *          the mediapackage
   * @param elementIds
   *          the element identifiers
   * @return the retracted element or <code>null</code> if the element was not retracted
   * @throws org.opencastproject.distribution.api.DistributionException
   *           in case of an error
   */
  protected MediaPackageElement[] retractElements(String channelId, MediaPackage mediapackage, Set<String> elementIds)
          throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(elementIds, "elementIds");
    notNull(channelId, "channelId");

    Set<MediaPackageElement> elements = getElements(channelId, mediapackage, elementIds);
    List<MediaPackageElement> retractedElements = new ArrayList<MediaPackageElement>();

    for (MediaPackageElement element : elements) {
      MediaPackageElement retractedElement = retractElement(channelId, mediapackage, element);
      retractedElements.add(retractedElement);
    }
    return retractedElements.toArray(new MediaPackageElement[retractedElements.size()]);
  }

  /**
   * Retract a media package element from the distribution channel. The retracted element must not necessarily be the
   * one given as parameter <code>elementId</code>. Instead, the element's distribution URI will be calculated. This way
   * you are able to retract elements by providing the "original" element here.
   *
   * @param channelId
   *          the channel id
   * @param mediapackage
   *          the mediapackage
   * @param element
   *          the element
   * @return the retracted element or <code>null</code> if the element was not retracted
   * @throws org.opencastproject.distribution.api.DistributionException
   *           in case of an error
   */
  protected MediaPackageElement retractElement(String channelId, MediaPackage mediapackage, MediaPackageElement element)
          throws DistributionException {
    notNull(mediapackage, "mediapackage");
    notNull(element, "element");
    notNull(channelId, "channelId");

    String mediapackageId = mediapackage.getIdentifier().compact();
    String elementId = element.getIdentifier();

    try {
      final File elementFile = getDistributionFile(channelId, mediapackage, element);
      final File mediapackageDir = getMediaPackageDirectory(channelId, mediapackage);
      // Does the file exist? If not, the current element has not been distributed to this channel
      // or has been removed otherwise
      if (!elementFile.exists()) {
        logger.info("Element {} from media package {} has already been removed or has never been distributed to "
            + "publication channel {}", elementId, mediapackageId, channelId);
        return element;
      }

      logger.debug("Retracting element {} ({})", element, elementFile);

      // Try to remove the file and its parent folder representing the mediapackage element id
      if (!FileUtils.deleteQuietly(elementFile.getParentFile())) {
        // TODO Removing a folder containing deleted files may fail on NFS volumes. This needs a cleanup strategy.
        logger.debug("Unable to delete folder {}", elementFile.getParentFile().getAbsolutePath());
      }

      if (mediapackageDir.isDirectory() && mediapackageDir.list().length == 0)
        FileSupport.delete(mediapackageDir);

      logger.debug("Finished retracting element {} of media package {} from publication channel {}", elementId,
          mediapackageId, channelId);
      return element;
    } catch (Exception e) {
      logger.warn("Error retracting element {} of media package {} from publication channel {}", elementId,
          mediapackageId, channelId, e);
      if (e instanceof DistributionException) {
        throw (DistributionException) e;
      } else {
        throw new DistributionException(e);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);
      String channelId = arguments.get(0);
      MediaPackage mediapackage = MediaPackageParser.getFromXml(arguments.get(1));
      Set<String> elementIds = gson.fromJson(arguments.get(2), new TypeToken<Set<String>>() { }.getType());

      switch (op) {
        case Distribute:
          Boolean checkAvailability = Boolean.parseBoolean(arguments.get(3));
          Boolean preserveReference = Boolean.parseBoolean(arguments.get(4));
          MediaPackageElement[] distributedElements = distributeElements(channelId, mediapackage, elementIds,
                  checkAvailability, preserveReference);
          return (distributedElements != null)
                  ? MediaPackageElementParser.getArrayAsXml(Arrays.asList(distributedElements)) : null;
        case Retract:
          MediaPackageElement[] retractedElements = retractElements(channelId, mediapackage, elementIds);
          return (retractedElements != null) ? MediaPackageElementParser.getArrayAsXml(Arrays.asList(retractedElements))
                  : null;
        default:
          throw new IllegalStateException("Don't know how to handle operation '" + operation + "'");
      }
    } catch (IllegalArgumentException e) {
      throw new ServiceRegistryException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ServiceRegistryException("This argument list for operation '" + op + "' does not meet expectations", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  private Set<MediaPackageElement> getElements(String channelId, MediaPackage mediapackage, Set<String> elementIds)
          throws IllegalStateException {
    final Set<MediaPackageElement> elements = new HashSet<MediaPackageElement>();
    for (String elementId : elementIds) {
       MediaPackageElement element = mediapackage.getElementById(elementId);
       if (element != null) {
         elements.add(element);
       } else {
         element = Arrays.stream(mediapackage.getPublications())
               .filter(p -> p.getChannel().equals(channelId))
               .flatMap(p -> Arrays.stream(p.getAttachments())
               .filter(a -> a.getIdentifier().equals(elementId)))
               .findAny()
               .orElseThrow(() ->
                       new IllegalStateException(format("No element %s found in mediapackage %s", elementId,
                           mediapackage.getIdentifier())));
         elements.add(element);
       }
    }
    return elements;
  }

  /**
   * Try to find the same file being already distributed in one of the other channels
   *
   * @param source
   *          the source file
   * @param mpId
   *          the element's mediapackage id
   * @return the found duplicated file or the given source if nothing has been found
   * @throws IOException
   *           if an I/O error occurs
   */
  private File findDuplicatedElementSource(final File source, final String mpId) throws IOException {
    String orgId = securityService.getOrganization().getId();
    final Path rootPath = Paths.get(distributionDirectory.getAbsolutePath(), orgId);

    if (!Files.exists(rootPath))
      return source;

    List<Path> mediaPackageDirectories = new ArrayList<>();
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(rootPath)) {
      for (Path path : directoryStream) {
        Path mpDir = path.resolve(mpId);
        if (Files.exists(mpDir)) {
          mediaPackageDirectories.add(mpDir);
        }
      }
    }

    if (mediaPackageDirectories.isEmpty())
      return source;

    final long size = Files.size(source.toPath());

    final File[] result = new File[1];
    for (Path p : mediaPackageDirectories) {
      Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (attrs.isDirectory())
            return FileVisitResult.CONTINUE;

          if (size != attrs.size())
            return FileVisitResult.CONTINUE;

          try (InputStream is1 = Files.newInputStream(source.toPath()); InputStream is2 = Files.newInputStream(file)) {
            if (!IOUtils.contentEquals(is1, is2))
              return FileVisitResult.CONTINUE;
          }
          result[0] = file.toFile();
          return FileVisitResult.TERMINATE;
        }
      });
      if (result[0] != null)
        break;
    }
    if (result[0] != null)
      return result[0];

    return source;
  }

  /**
   * Gets the destination file to copy the contents of a mediapackage element.
   *
   * @return The file to copy the content to
   */
  protected File getDistributionFile(String channelId, MediaPackage mp, MediaPackageElement element) {
    final String uriString = element.getURI().toString().split("\\?")[0];
    final String directoryName = distributionDirectory.getAbsolutePath();
    final String orgId = securityService.getOrganization().getId();
    if (uriString.startsWith(serviceUrl)) {
      String[] splitUrl = uriString.substring(serviceUrl.length() + 1).split("/");
      if (splitUrl.length < 5) {
        logger.warn("Malformed URI {}. Format must be .../{orgId}/{channelId}/{mediapackageId}/{elementId}/{fileName}."
                        + " Trying URI without channelId", uriString);
        return new File(path(directoryName, orgId, splitUrl[1], splitUrl[2], splitUrl[3]));
      } else {
        return new File(path(directoryName, orgId, splitUrl[1], splitUrl[2], splitUrl[3], splitUrl[4]));
      }
    }
    return new File(path(directoryName, orgId, channelId, mp.getIdentifier().compact(), element.getIdentifier(),
            FilenameUtils.getName(uriString)));
  }

  /**
   * Gets the directory containing the distributed files for this mediapackage.
   *
   * @return the filesystem directory
   */
  protected File getMediaPackageDirectory(String channelId, MediaPackage mp) {
    final String orgId = securityService.getOrganization().getId();
    return new File(distributionDirectory, path(orgId, channelId, mp.getIdentifier().compact()));
  }

  /**
   * Gets the URI for the element to be distributed.
   *
   * @param mediaPackageId
   *          the mediapackage identifier
   * @param element
   *          The mediapackage element being distributed
   * @return The resulting URI after distribution
   * @throws URISyntaxException
   *           if the concrete implementation tries to create a malformed uri
   */
  protected URI getDistributionUri(String channelId, String mediaPackageId, MediaPackageElement element)
          throws URISyntaxException {
    String elementId = element.getIdentifier();
    String fileName = FilenameUtils.getName(element.getURI().toString());
    String orgId = securityService.getOrganization().getId();
    String destinationURI = UrlSupport.concat(serviceUrl, orgId, channelId, mediaPackageId, elementId, fileName);
    return new URI(destinationURI);
  }

  @Override
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    distributeJobLoad = LoadUtil.getConfiguredLoadValue(properties, DISTRIBUTE_JOB_LOAD_KEY,
            DEFAULT_DISTRIBUTE_JOB_LOAD, serviceRegistry);
    retractJobLoad = LoadUtil.getConfiguredLoadValue(properties, RETRACT_JOB_LOAD_KEY, DEFAULT_RETRACT_JOB_LOAD,
            serviceRegistry);
  }

}
