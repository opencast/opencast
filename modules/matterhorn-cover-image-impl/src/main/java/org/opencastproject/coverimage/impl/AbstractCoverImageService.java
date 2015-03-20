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
package org.opencastproject.coverimage.impl;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.opencastproject.coverimage.CoverImageException;
import org.opencastproject.coverimage.CoverImageService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.batik.apps.rasterizer.DestinationType;
import org.apache.batik.apps.rasterizer.SVGConverter;
import org.apache.batik.apps.rasterizer.SVGConverterException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Service for creating cover images
 */
public abstract class AbstractCoverImageService extends AbstractJobProducer implements CoverImageService {

  protected static final String COVERIMAGE_WORKSPACE_COLLECTION = "coverimage";

  /** List of available operations on jobs */
  protected enum Operation {
    Generate
  }

  /** The workspace service */
  protected Workspace workspace = null;

  /** The service registry service */
  protected ServiceRegistry serviceRegistry;

  /** The security service */
  protected SecurityService securityService;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The logging facility */
  private static final Logger log = LoggerFactory.getLogger(AbstractCoverImageService.class);

  /** Creates a new composer service instance. */
  public AbstractCoverImageService() {
    super(JOB_TYPE);
  }

  @Override
  protected String process(Job job) throws Exception {

    List<String> arguments = job.getArguments();
    String xml = arguments.get(0);
    String xsl = arguments.get(1);
    int width = Integer.valueOf(arguments.get(2));
    int height = Integer.valueOf(arguments.get(3));
    String posterImage = arguments.get(4);
    String targetFlavor = arguments.get(5);

    Operation op = null;
    op = Operation.valueOf(job.getOperation());
    switch (op) {
      case Generate:
        Attachment result = generateCoverImageInternal(job, xml, xsl, width, height, posterImage, targetFlavor);
        return MediaPackageElementParser.getAsXml(result);
      default:
        throw new IllegalStateException("Don't know how to handle operation '" + job.getOperation() + "'");
    }
  }

  @Override
  public Job generateCoverImage(String xml, String xsl, String width, String height, String posterImageUri,
          String targetFlavor) throws CoverImageException {

    // Null values are not passed to the arguments list
    if (posterImageUri == null)
      posterImageUri = "";

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.Generate.toString(),
              Arrays.asList(xml, xsl, width, height, posterImageUri, targetFlavor));
    } catch (ServiceRegistryException e) {
      throw new CoverImageException("Unable to create a job", e);
    }
  }

  protected Attachment generateCoverImageInternal(Job job, String xml, String xsl, int width, int height,
          String posterImage, String targetFlavor) throws CoverImageException {

    URI result;
    File tempSvg = null;
    File tempPng = null;
    StringReader xmlReader = null;

    try {
      Document xslDoc = parseXsl(xsl);

      // Create temp SVG file for transformation result
      tempSvg = createTempFile(job, ".svg");
      Result svg = new StreamResult(tempSvg);

      // Load Metadata (from resources)
      xmlReader = new StringReader(xml);
      Source xmlSource = new StreamSource(xmlReader);

      // Transform XML metadata with stylesheet to SVG
      transformSvg(svg, xmlSource, xslDoc, width, height, posterImage);

      // Rasterize SVG to PNG
      tempPng = createTempFile(job, ".png");
      rasterizeSvg(tempSvg, tempPng);

      FileInputStream in = null;
      try {
        in = new FileInputStream(tempPng);
        result = workspace.putInCollection(COVERIMAGE_WORKSPACE_COLLECTION, job.getId() + "_coverimage.png", in);
        log.debug("Put the cover image into the workspace ({})", result);
      } catch (FileNotFoundException e) {
        // should never happen...
        throw new CoverImageException(e);
      } catch (IOException e) {
        log.warn("Error while putting resulting image into workspace collection '{}': {}",
                COVERIMAGE_WORKSPACE_COLLECTION, e);
        throw new CoverImageException("Error while putting resulting image into workspace collection", e);
      } finally {
        IOUtils.closeQuietly(in);
      }
    } finally {
      FileUtils.deleteQuietly(tempSvg);
      FileUtils.deleteQuietly(tempPng);
      log.debug("Removed temporary files");

      IOUtils.closeQuietly(xmlReader);
    }

    return (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromURI(result, Type.Attachment, MediaPackageElementFlavor.parseFlavor(targetFlavor));
  }

  protected static Document parseXsl(String xsl) throws CoverImageException {
    if (StringUtils.isBlank(xsl))
      throw new IllegalArgumentException("XSL string must not be empty");

    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setNamespaceAware(true);
    Document xslDoc;
    try {
      log.debug("Parse given XSL to a org.w3c.dom.Document object");
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      xslDoc = dBuilder.parse(new InputSource(new ByteArrayInputStream(xsl.getBytes("utf-8"))));
    } catch (ParserConfigurationException e) {
      // this should never happen...
      throw new CoverImageException("The XSLT parser has serious configuration errors", e);
    } catch (SAXException e) {
      log.warn("Error while parsing the XSLT stylesheet: {}", e.getMessage());
      throw new CoverImageException("Error while parsing the XSLT stylesheet", e);
    } catch (IOException e) {
      log.warn("Error while reading the XSLT stylesheet: {}", e.getMessage());
      throw new CoverImageException("Error while reading the XSLT stylesheet", e);
    }
    return xslDoc;
  }

  protected static void transformSvg(Result svg, Source xmlSource, Document xslDoc, int width, int height,
          String posterImage) throws TransformerFactoryConfigurationError, CoverImageException {
    if (svg == null || xmlSource == null || xslDoc == null)
      throw new IllegalArgumentException("Neither svg nor xmlSource nor xslDoc must be null");

    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer;
    try {
      transformer = factory.newTransformer(new DOMSource(xslDoc));
    } catch (TransformerConfigurationException e) {
      // this should never happen...
      throw new CoverImageException("The XSL transformer factory has serious configuration errors", e);
    }
    transformer.setParameter("width", width);
    transformer.setParameter("height", height);
    if (isNotBlank(posterImage))
      transformer.setParameter("posterimage", posterImage);

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();
    thread.setContextClassLoader(AbstractCoverImageService.class.getClassLoader());
    try {
      log.debug("Transform XML source to SVG");
      transformer.transform(xmlSource, svg);
    } catch (TransformerException e) {
      log.warn("Error while transforming SVG to image: {}", e.getMessage());
      throw new CoverImageException("Error while transforming SVG to image", e);
    } finally {
      thread.setContextClassLoader(loader);
    }
  }

  protected File createTempFile(Job job, String suffix) throws CoverImageException {
    File tempFile;
    try {
      tempFile = File.createTempFile(COVERIMAGE_WORKSPACE_COLLECTION, Long.toString(job.getId()) + "_" + suffix);
      log.debug("Created temporary file {}", tempFile);
    } catch (IOException e) {
      log.warn("Error creating temporary file: {}", e);
      throw new CoverImageException("Error creating temporary file", e);
    }
    return tempFile;
  }

  protected static void rasterizeSvg(File svgSource, File pngResult) throws CoverImageException {
    SVGConverter converter = new SVGConverter();
    converter.setDestinationType(DestinationType.PNG);
    converter.setDst(pngResult);
    converter.setSources(new String[] { svgSource.getAbsolutePath() });
    try {
      log.debug("Start converting SVG to PNG");
      converter.execute();
    } catch (SVGConverterException e) {
      log.warn("Error while converting the SVG to a PNG: {}", e.getMessage());
      throw new CoverImageException("Error while converting the SVG to a PNG", e);
    }
  }
}
