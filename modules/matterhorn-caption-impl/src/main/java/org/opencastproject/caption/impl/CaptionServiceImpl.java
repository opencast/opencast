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
package org.opencastproject.caption.impl;

import static org.opencastproject.util.MimeType.mimeType;

import org.opencastproject.caption.api.Caption;
import org.opencastproject.caption.api.CaptionConverter;
import org.opencastproject.caption.api.CaptionConverterException;
import org.opencastproject.caption.api.CaptionService;
import org.opencastproject.caption.api.UnsupportedCaptionFormatException;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.activation.FileTypeMap;

/**
 * Implementation of {@link CaptionService}. Uses {@link ComponentContext} to get all registered
 * {@link CaptionConverter}s. Converters are searched based on <code>caption.format</code> property. If there is no
 * match for specified input or output format {@link UnsupportedCaptionFormatException} is thrown.
 * 
 */
public class CaptionServiceImpl extends AbstractJobProducer implements CaptionService {

  /**
   * Creates a new caption service.
   */
  public CaptionServiceImpl() {
    super(JOB_TYPE);
  }

  /** Logging utility */
  private static final Logger logger = LoggerFactory.getLogger(CaptionServiceImpl.class);

  /** List of available operations on jobs */
  private enum Operation {
    Convert, ConvertWithLanguage
  };

  /** The collection name */
  public static final String COLLECTION = "captions";

  /** Reference to workspace */
  protected Workspace workspace;

  /** Reference to remote service manager */
  protected ServiceRegistry serviceRegistry;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** Component context needed for retrieving Converter Engines */
  protected ComponentContext componentContext = null;

  /**
   * Activate this service implementation via the OSGI service component runtime.
   * 
   * @param componentContext
   *          the component context
   */
  public void activate(ComponentContext componentContext) {
    this.componentContext = componentContext;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionService#convert(org.opencastproject.mediapackage.Catalog,
   *      java.lang.String, java.lang.String)
   */
  @Override
  public Job convert(Catalog input, String inputFormat, String outputFormat) throws UnsupportedCaptionFormatException,
          CaptionConverterException, MediaPackageException {

    if (input == null)
      throw new IllegalArgumentException("Input catalog can't be null");
    if (StringUtils.isBlank(inputFormat))
      throw new IllegalArgumentException("Input format is null");
    if (StringUtils.isBlank(outputFormat))
      throw new IllegalArgumentException("Output format is null");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Convert.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(input), inputFormat, outputFormat));
    } catch (ServiceRegistryException e) {
      throw new CaptionConverterException("Unable to create a job", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionService#convert(org.opencastproject.mediapackage.Catalog,
   *      java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Job convert(Catalog input, String inputFormat, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, CaptionConverterException, MediaPackageException {

    if (input == null)
      throw new IllegalArgumentException("Input catalog can't be null");
    if (StringUtils.isBlank(inputFormat))
      throw new IllegalArgumentException("Input format is null");
    if (StringUtils.isBlank(outputFormat))
      throw new IllegalArgumentException("Output format is null");
    if (StringUtils.isBlank(language))
      throw new IllegalArgumentException("Language format is null");

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.ConvertWithLanguage.toString(),
              Arrays.asList(MediaPackageElementParser.getAsXml(input), inputFormat, outputFormat, language));
    } catch (ServiceRegistryException e) {
      throw new CaptionConverterException("Unable to create a job", e);
    }
  }

  /**
   * Converts the captions and returns them in a new catalog.
   * 
   * @return the converted catalog
   */
  protected Catalog convert(Job job, Catalog input, String inputFormat, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, CaptionConverterException, MediaPackageException {
    try {

      // check parameters
      if (input == null)
        throw new IllegalArgumentException("Input catalog can't be null");
      if (StringUtils.isBlank(inputFormat))
        throw new IllegalArgumentException("Input format is null");
      if (StringUtils.isBlank(outputFormat))
        throw new IllegalArgumentException("Output format is null");

      // get input file
      File captionsFile;
      try {
        captionsFile = workspace.get(input.getURI());
      } catch (NotFoundException e) {
        throw new CaptionConverterException("Requested media package element " + input + " could not be found.");
      } catch (IOException e) {
        throw new CaptionConverterException("Requested media package element " + input + "could not be accessed.");
      }

      logger.debug("Atempting to convert from {} to {}...", inputFormat, outputFormat);

      List<Caption> collection = null;
      try {
        collection = importCaptions(captionsFile, inputFormat, language);
        logger.debug("Parsing to collection succeeded.");
      } catch (UnsupportedCaptionFormatException e) {
        throw new UnsupportedCaptionFormatException(inputFormat);
      } catch (CaptionConverterException e) {
        throw e;
      }

      URI exported;
      try {
        exported = exportCaptions(collection,
                job.getId() + "." + FilenameUtils.getExtension(captionsFile.getAbsolutePath()), outputFormat, language);
        logger.debug("Exporting captions succeeding.");
      } catch (UnsupportedCaptionFormatException e) {
        throw new UnsupportedCaptionFormatException(outputFormat);
      } catch (IOException e) {
        throw new CaptionConverterException("Could not export caption collection.", e);
      }

      // create catalog and set properties
      MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
      Catalog catalog = (Catalog) elementBuilder.elementFromURI(exported, Catalog.TYPE, new MediaPackageElementFlavor(
              "captions", outputFormat));
      String[] mimetype = FileTypeMap.getDefaultFileTypeMap().getContentType(exported.getPath()).split("/");
      catalog.setMimeType(mimeType(mimetype[0], mimetype[1]));
      catalog.addTag("lang:" + language);

      return catalog;

    } catch (Exception e) {
      logger.warn("Error converting captions in " + input, e);
      if (e instanceof CaptionConverterException) {
        throw (CaptionConverterException) e;
      } else if (e instanceof UnsupportedCaptionFormatException) {
        throw (UnsupportedCaptionFormatException) e;
      } else {
        throw new CaptionConverterException(e);
      }
    }
  }

  /**
   * 
   * {@inheritDoc}
   * 
   * @see org.opencastproject.caption.api.CaptionService#getLanguageList(org.opencastproject.mediapackage.MediaPackageElement,
   *      java.lang.String)
   */
  @Override
  public String[] getLanguageList(Catalog input, String format) throws UnsupportedCaptionFormatException,
          CaptionConverterException {

    if (format == null) {
      throw new UnsupportedCaptionFormatException("<null>");
    }
    CaptionConverter converter = getCaptionConverter(format);
    if (converter == null) {
      throw new UnsupportedCaptionFormatException(format);
    }

    File captions;
    try {
      captions = workspace.get(input.getURI());
    } catch (NotFoundException e) {
      throw new CaptionConverterException("Requested media package element " + input + " could not be found.");
    } catch (IOException e) {
      throw new CaptionConverterException("Requested media package element " + input + "could not be accessed.");
    }

    FileInputStream stream = null;
    String[] languageList;
    try {
      stream = new FileInputStream(captions);
      languageList = converter.getLanguageList(stream);
    } catch (FileNotFoundException e) {
      throw new CaptionConverterException("Requested file " + captions + "could not be found.");
    } finally {
      IoSupport.closeQuietly(stream);
    }

    return languageList == null ? new String[0] : languageList;
  }

  /**
   * Returns all registered {@link CaptionFormat}s.
   */
  protected HashMap<String, CaptionConverter> getAvailableCaptionConverters() {
    HashMap<String, CaptionConverter> captionConverters = new HashMap<String, CaptionConverter>();
    ServiceReference[] refs = null;
    try {
      refs = componentContext.getBundleContext().getServiceReferences(CaptionConverter.class.getName(), null);
    } catch (InvalidSyntaxException e) {
      // should not happen since it is called with null argument
    }

    if (refs != null) {
      for (ServiceReference ref : refs) {
        CaptionConverter converter = (CaptionConverter) componentContext.getBundleContext().getService(ref);
        String format = (String) ref.getProperty("caption.format");
        if (captionConverters.containsKey(format)) {
          logger.warn("Caption converter with format {} has already been registered. Ignoring second definition.",
                  format);
        } else {
          captionConverters.put((String) ref.getProperty("caption.format"), converter);
        }
      }
    }

    return captionConverters;
  }

  /**
   * Returns specific {@link CaptionConverter}. Registry is searched based on formatName, so in order for
   * {@link CaptionConverter} to be found, it has to have <code>caption.format</code> property set with
   * {@link CaptionConverter} format. If none is found, null is returned, if more than one is found then the first
   * reference is returned.
   * 
   * @param formatName
   *          name of the caption format
   * @return {@link CaptionConverter} or null if none is found
   */
  protected CaptionConverter getCaptionConverter(String formatName) {
    ServiceReference[] ref = null;
    try {
      ref = componentContext.getBundleContext().getServiceReferences(CaptionConverter.class.getName(),
              "(caption.format=" + formatName + ")");
    } catch (InvalidSyntaxException e) {
      throw new RuntimeException(e);
    }
    if (ref == null) {
      logger.warn("No caption format available for {}.", formatName);
      return null;
    }
    if (ref.length > 1)
      logger.warn("Multiple references for caption format {}! Returning first service reference.", formatName);
    CaptionConverter converter = (CaptionConverter) componentContext.getBundleContext().getService(ref[0]);
    return converter;
  }

  /**
   * Imports captions using registered converter engine and specified language.
   * 
   * @param input
   *          file containing captions
   * @param inputFormat
   *          format of imported captions
   * @param language
   *          (optional) captions' language
   * @return {@link List} of parsed captions
   * @throws UnsupportedCaptionFormatException
   *           if there is no registered engine for given format
   * @throws IllegalCaptionFormatException
   *           if parser encounters exception
   */
  private List<Caption> importCaptions(File input, String inputFormat, String language)
          throws UnsupportedCaptionFormatException, CaptionConverterException {
    // get input format
    CaptionConverter converter = getCaptionConverter(inputFormat);
    if (converter == null) {
      logger.error("No available caption format found for {}.", inputFormat);
      throw new UnsupportedCaptionFormatException(inputFormat);
    }

    FileInputStream fileStream = null;
    try {
      fileStream = new FileInputStream(input);
      List<Caption> collection = converter.importCaption(fileStream, language);
      return collection;
    } catch (FileNotFoundException e) {
      throw new CaptionConverterException("Could not locate file " + input);
    } finally {
      IOUtils.closeQuietly(fileStream);
    }
  }

  /**
   * Exports captions {@link List} to specified format. Extension is added to exported file name. Throws
   * {@link UnsupportedCaptionFormatException} if format is not supported.
   * 
   * @param captions
   *          {@link {@link List} to be exported
   * @param outputName
   *          name under which exported captions will be stored
   * @param outputFormat
   *          format of exported collection
   * @param language
   *          (optional) captions' language
   * @throws UnsupportedCaptionFormatException
   *           if there is no registered engine for given format
   * @return location of converted captions
   * @throws IOException
   *           if exception occurs while writing to output stream
   */
  private URI exportCaptions(List<Caption> captions, String outputName, String outputFormat, String language)
          throws UnsupportedCaptionFormatException, IOException {
    CaptionConverter converter = getCaptionConverter(outputFormat);
    if (converter == null) {
      logger.error("No available caption format found for {}.", outputFormat);
      throw new UnsupportedCaptionFormatException(outputFormat);
    }

    // TODO instead of first writing it all in memory, write it directly to disk
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      converter.exportCaption(outputStream, captions, language);
    } catch (IOException e) {
      // since we're writing to memory, this should not happen
    }
    ByteArrayInputStream in = new ByteArrayInputStream(outputStream.toByteArray());
    return workspace.putInCollection(COLLECTION, outputName + "." + converter.getExtension(), in);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#process(Job)
   */
  @Override
  protected String process(Job job) throws Exception {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    try {
      op = Operation.valueOf(operation);

      Catalog catalog = (Catalog) MediaPackageElementParser.getFromXml(arguments.get(0));
      String inputFormat = arguments.get(1);
      String outputFormat = arguments.get(2);

      Catalog resultingCatalog = null;

      switch (op) {
        case Convert:
          resultingCatalog = convert(job, catalog, inputFormat, outputFormat, null);
          return MediaPackageElementParser.getAsXml(resultingCatalog);
        case ConvertWithLanguage:
          String language = arguments.get(3);
          resultingCatalog = convert(job, catalog, inputFormat, outputFormat, language);
          return MediaPackageElementParser.getAsXml(resultingCatalog);
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
   * Setter for workspace via declarative activation
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Setter for remote service manager via declarative activation
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
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
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
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
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

}
