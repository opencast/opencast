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
package org.opencastproject.textanalyzer.impl;

import org.opencastproject.composer.api.ComposerService;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.dictionary.api.DictionaryService;
import org.opencastproject.dictionary.api.DictionaryService.DICT_TOKEN;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobBarrier;
import org.opencastproject.job.api.JobBarrier.Result;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.mpeg7.MediaTime;
import org.opencastproject.metadata.mpeg7.MediaTimeImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.metadata.mpeg7.SpatioTemporalDecomposition;
import org.opencastproject.metadata.mpeg7.TemporalDecomposition;
import org.opencastproject.metadata.mpeg7.Textual;
import org.opencastproject.metadata.mpeg7.TextualImpl;
import org.opencastproject.metadata.mpeg7.Video;
import org.opencastproject.metadata.mpeg7.VideoSegment;
import org.opencastproject.metadata.mpeg7.VideoText;
import org.opencastproject.metadata.mpeg7.VideoTextImpl;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.textanalyzer.api.TextAnalyzerException;
import org.opencastproject.textanalyzer.api.TextAnalyzerService;
import org.opencastproject.textextractor.api.TextExtractor;
import org.opencastproject.textextractor.api.TextExtractorException;
import org.opencastproject.textextractor.api.TextFrame;
import org.opencastproject.textextractor.api.TextLine;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Media analysis service that takes takes an image and returns text as extracted from that image.
 */
public class TextAnalyzerServiceImpl extends AbstractJobProducer implements TextAnalyzerService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TextAnalyzerServiceImpl.class);

  /** List of available operations on jobs */
  private enum Operation {
    Extract
  };

  /** Resulting collection in the working file repository */
  public static final String COLLECTION_ID = "ocrtext";

  /** Name of encoding profile used to create tiff images */
  public static final String TIFF_CONVERSION_PROFILE = "image-conversion.http";

  /** The text extraction implemenetation */
  private TextExtractor textExtractor = null;

  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry = null;

  /** The composer service */
  protected ComposerService composerService = null;

  /** The workspace to ue when retrieving remote media files */
  private Workspace workspace = null;

  /** The mpeg-7 service */
  protected Mpeg7CatalogService mpeg7CatalogService;

  /** The dictionary service */
  protected DictionaryService dictionaryService;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /**
   * Creates a new instance of the text analyzer service.
   */
  public TextAnalyzerServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * OSGi callback on component activation.
   * 
   * @param ctx
   *          the bundle context
   */
  void activate(BundleContext ctx) {
    logger.info("Activating Text analyser service");
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.textanalyzer.api.TextAnalyzerService#extract(org.opencastproject.mediapackage.Attachment)
   */
  @Override
  public Job extract(Attachment image) throws TextAnalyzerException, MediaPackageException {
    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Extract.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(image)));
    } catch (ServiceRegistryException e) {
      throw new TextAnalyzerException("Unable to create job", e);
    }
  }

  /**
   * Starts text extraction on the image and returns a receipt containing the final result in the form of an
   * Mpeg7Catalog.
   * 
   * @param image
   *          the element to analyze
   * @param block
   *          <code>true</code> to make this operation synchronous
   * @return a receipt containing the resulting mpeg-7 catalog
   * @throws TextAnalyzerException
   */
  @SuppressWarnings("unchecked")
  private Catalog extract(Job job, Attachment image) throws TextAnalyzerException, MediaPackageException {

    final Attachment attachment;
    final URI imageUrl;

    // Make sure the attachment is a tiff

    // Make sure this image is of type tif
    if (!image.getURI().getPath().endsWith(".tif")) {
      try {
        logger.info("Converting " + image + " to tif format");
        Job conversionJob = composerService.convertImage(image, TIFF_CONVERSION_PROFILE);
        JobBarrier barrier = new JobBarrier(serviceRegistry, conversionJob);
        Result result = barrier.waitForJobs();
        if (!result.isSuccess()) {
          throw new TextAnalyzerException("Unable to convert " + image + " to tiff");
        }
        conversionJob = serviceRegistry.getJob(conversionJob.getId()); // get the latest copy
        attachment = (Attachment) MediaPackageElementParser.getFromXml(conversionJob.getPayload());
        imageUrl = attachment.getURI();
      } catch (EncoderException e) {
        throw new TextAnalyzerException(e);
      } catch (NotFoundException e) {
        throw new TextAnalyzerException(e);
      } catch (ServiceRegistryException e) {
        throw new TextAnalyzerException(e);
      }
    } else {
      attachment = (Attachment) image;
      imageUrl = attachment.getURI();
    }

    File imageFile = null;
    try {
      Mpeg7CatalogImpl mpeg7 = Mpeg7CatalogImpl.newInstance();

      logger.info("Starting text extraction from {}", imageUrl);

      try {
        imageFile = workspace.get(imageUrl);
      } catch (NotFoundException e) {
        throw new TextAnalyzerException("Image " + imageUrl + " not found in workspace", e);
      } catch (IOException e) {
        throw new TextAnalyzerException("Unable to access " + imageUrl + " in workspace", e);
      }
      VideoText[] videoTexts = analyze(imageFile, image.getIdentifier());

      // Create a temporal decomposition
      MediaTime mediaTime = new MediaTimeImpl(0, 0);
      Video avContent = mpeg7.addVideoContent(image.getIdentifier(), mediaTime, null);
      TemporalDecomposition<VideoSegment> temporalDecomposition = (TemporalDecomposition<VideoSegment>) avContent
              .getTemporalDecomposition();

      // Add a segment
      VideoSegment videoSegment = temporalDecomposition.createSegment("segment-0");
      videoSegment.setMediaTime(mediaTime);

      // Add the video text to the spacio temporal decomposition of the segment
      SpatioTemporalDecomposition spatioTemporalDecomposition = videoSegment.createSpatioTemporalDecomposition(true,
              false);
      for (VideoText videoText : videoTexts) {
        spatioTemporalDecomposition.addVideoText(videoText);
      }

      logger.info("Text extraction of {} finished, {} lines found", attachment.getURI(), videoTexts.length);

      URI uri;
      try {
        uri = workspace.putInCollection(COLLECTION_ID, job.getId() + ".xml", mpeg7CatalogService.serialize(mpeg7));
      } catch (IOException e) {
        throw new TextAnalyzerException("Unable to put mpeg7 into the workspace", e);
      }
      Catalog catalog = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
              .newElement(Catalog.TYPE, MediaPackageElements.TEXTS);
      catalog.setURI(uri);

      logger.info("Finished text extraction of {}", imageUrl);

      return catalog;
    } catch (Exception e) {
      logger.warn("Error extracting text from " + imageUrl, e);
      if (e instanceof TextAnalyzerException) {
        throw (TextAnalyzerException) e;
      } else {
        throw new TextAnalyzerException(e);
      }
    } finally {
      try {
        workspace.delete(imageUrl);
      } catch (Exception e) {
        logger.warn("Unable to delete temporary text analysis image {}: {}", imageUrl, e);
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
      switch (op) {
        case Extract:
          Attachment element = (Attachment) MediaPackageElementParser.getFromXml(arguments.get(0));
          Catalog catalog = extract(job, element);
          return MediaPackageElementParser.getAsXml(catalog);
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

  /**
   * Returns the video text element for the given image.
   * 
   * @param imageFile
   *          the image
   * @param id
   *          the video text id
   * @return the video text found on the image
   * @throws IOException
   *           if accessing the image fails
   */
  protected VideoText[] analyze(File imageFile, String id) throws TextAnalyzerException {
    boolean languagesInstalled;
    if (dictionaryService.getLanguages().length == 0) {
      languagesInstalled = false;
      logger.warn("There are no language packs installed.  All text extracted from video will be considered valid.");
    } else {
      languagesInstalled = true;
    }

    List<VideoText> videoTexts = new ArrayList<VideoText>();
    TextFrame textFrame = null;
    try {
      textFrame = textExtractor.extract(imageFile);
    } catch (IOException e) {
      logger.warn("Error reading image file {}: {}", imageFile, e.getMessage());
      throw new TextAnalyzerException(e);
    } catch (TextExtractorException e) {
      logger.warn("Error extracting text from {}: {}", imageFile, e.getMessage());
      throw new TextAnalyzerException(e);
    }

    int i = 1;
    for (TextLine line : textFrame.getLines()) {
      VideoText videoText = new VideoTextImpl(id + "-" + i++);
      videoText.setBoundary(line.getBoundaries());
      Textual text = null;
      if (languagesInstalled) {
        String[] potentialWords = line.getText() == null ? new String[0] : line.getText().split("\\W");
        String[] languages = dictionaryService.detectLanguage(potentialWords);
        if (languages.length == 0) {
          // There are languages installed, but these words are part of one of those languages
          logger.debug("No languages found for '{}'.", line.getText());
          continue;
        } else {
          String language = languages[0];
          DICT_TOKEN[] tokens = dictionaryService.cleanText(potentialWords, language);
          StringBuilder cleanLine = new StringBuilder();
          for (int j = 0; j < potentialWords.length; j++) {
            if (tokens[j] == DICT_TOKEN.WORD) {
              if (cleanLine.length() > 0) {
                cleanLine.append(" ");
              }
              cleanLine.append(potentialWords[j]);
            }
          }
          // TODO: Ensure that the language returned by the dictionary is compatible with the MPEG-7 schema
          text = new TextualImpl(cleanLine.toString(), language);
        }
      } else {
        logger.debug("No languages installed.  For better results, please install at least one language pack");
        text = new TextualImpl(line.getText());
      }
      videoText.setText(text);
      videoTexts.add(videoText);
    }
    return videoTexts.toArray(new VideoText[videoTexts.size()]);
  }

  /**
   * Sets the receipt service
   * 
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * Sets the text extractor.
   * 
   * @param textExtractor
   *          a text extractor implementation
   */
  protected void setTextExtractor(TextExtractor textExtractor) {
    this.textExtractor = textExtractor;
  }

  /**
   * Sets the workspace
   * 
   * @param workspace
   *          an instance of the workspace
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Sets the mpeg7CatalogService
   * 
   * @param mpeg7CatalogService
   *          an instance of the mpeg7 catalog service
   */
  protected void setMpeg7CatalogService(Mpeg7CatalogService mpeg7CatalogService) {
    this.mpeg7CatalogService = mpeg7CatalogService;
  }

  /**
   * Sets the dictionary service
   * 
   * @param dictionaryService
   *          an instance of the dicitonary service
   */
  protected void setDictionaryService(DictionaryService dictionaryService) {
    this.dictionaryService = dictionaryService;
  }

  /**
   * OSGi callback to set the composer service.
   * 
   * @param composer
   *          the composer
   */
  protected void setComposerService(ComposerService composer) {
    this.composerService = composer;
  }

  /**
   * Callback for setting the security service.
   * 
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   * 
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   * 
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

}
