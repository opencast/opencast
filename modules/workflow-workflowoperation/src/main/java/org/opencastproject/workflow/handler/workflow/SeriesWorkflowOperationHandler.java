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
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.job.api.JobContext;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.selector.AbstractMediaPackageElementSelector;
import org.opencastproject.mediapackage.selector.CatalogSelector;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.XmlNamespaceBinding;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationResult;
import org.opencastproject.workflow.api.WorkflowOperationResult.Action;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * The workflow definition for handling "series" operations
 */
public class SeriesWorkflowOperationHandler extends AbstractWorkflowOperationHandler {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(SeriesWorkflowOperationHandler.class);

  /** Name of the configuration option that provides the optional series identifier */
  public static final String SERIES_PROPERTY = "series";

  /** Name of the configuration option that provides the flavors of the series catalogs to attach */
  public static final String ATTACH_PROPERTY = "attach";

  /** Name of the configuration option that provides whether the ACL should be applied or not */
  public static final String APPLY_ACL_PROPERTY = "apply-acl";

  /** Name of the configuration key that specifies the list of series metadata to be copied to the episode */
  public static final String COPY_METADATA_PROPERTY = "copy-metadata";

  /** Name of the configuration key that specifies the default namespace for the metadata to be copied to the episode */
  public static final String DEFAULT_NS_PROPERTY = "default-namespace";

  /** The authorization service */
  private AuthorizationService authorizationService;

  /** The series service */
  private SeriesService seriesService;

  /** The workspace */
  private Workspace workspace;

  /** The security service */
  private SecurityService securityService;

  /** The list series catalog UI adapters */
  private final List<SeriesCatalogUIAdapter> seriesCatalogUIAdapters = new ArrayList<>();

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param authorizationService
   *          the authorization service
   */
  protected void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param seriesService
   *          the series service
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param workspace
   *          the workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Callback for the OSGi declarative services configuration.
   *
   * @param securityService
   *          the securityService
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback to add {@link SeriesCatalogUIAdapter} instance. */
  public void addCatalogUIAdapter(SeriesCatalogUIAdapter catalogUIAdapter) {
    seriesCatalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi callback to remove {@link SeriesCatalogUIAdapter} instance. */
  public void removeCatalogUIAdapter(SeriesCatalogUIAdapter catalogUIAdapter) {
    seriesCatalogUIAdapters.remove(catalogUIAdapter);
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.workflow.api.WorkflowOperationHandler#start(org.opencastproject.workflow.api.WorkflowInstance,
   *      JobContext)
   */
  @Override
  public WorkflowOperationResult start(final WorkflowInstance workflowInstance, JobContext context)
          throws WorkflowOperationException {
    logger.debug("Running series workflow operation");

    MediaPackage mediaPackage = workflowInstance.getMediaPackage();

    Opt<String> optSeries = getOptConfig(workflowInstance.getCurrentOperation(), SERIES_PROPERTY);
    Opt<String> optAttachFlavors = getOptConfig(workflowInstance.getCurrentOperation(), ATTACH_PROPERTY);
    Boolean applyAcl = getOptConfig(workflowInstance.getCurrentOperation(), APPLY_ACL_PROPERTY).map(toBoolean)
            .getOr(false);
    Opt<String> optCopyMetadata = getOptConfig(workflowInstance.getCurrentOperation(), COPY_METADATA_PROPERTY);
    String defaultNamespace = getOptConfig(workflowInstance.getCurrentOperation(), DEFAULT_NS_PROPERTY)
            .getOr(DublinCore.TERMS_NS_URI);
    logger.debug("Using default namespace: '{}'", defaultNamespace);

    if (optSeries.isSome() && !optSeries.get().equals(mediaPackage.getSeries())) {
      logger.info("Changing series id from '{}' to '{}'", StringUtils.trimToEmpty(mediaPackage.getSeries()),
              optSeries.get());
      mediaPackage.setSeries(optSeries.get());
    }

    String seriesId = mediaPackage.getSeries();
    if (seriesId == null) {
      logger.info("No series set, skip operation");
      return createResult(mediaPackage, Action.SKIP);
    }

    DublinCoreCatalog series;
    try {
      series = seriesService.getSeries(seriesId);
    } catch (NotFoundException e) {
      logger.info("No series with the identifier '{}' found, skip operation", seriesId);
      return createResult(mediaPackage, Action.SKIP);
    } catch (UnauthorizedException e) {
      logger.warn("Not authorized to get series with identifier '{}' found, skip operation", seriesId);
      return createResult(mediaPackage, Action.SKIP);
    } catch (SeriesException e) {
      logger.error("Unable to get series with identifier '{}', skip operation: {}", seriesId,
              ExceptionUtils.getStackTrace(e));
      throw new WorkflowOperationException(e);
    }

    mediaPackage.setSeriesTitle(series.getFirst(DublinCore.PROPERTY_TITLE));

    // Process extra metadata
    HashSet<EName> extraMetadata = new HashSet<>();
    if (optCopyMetadata.isSome()) {
      for (String strEName : optCopyMetadata.get().split(",+\\s*"))
        try {
          if (!strEName.isEmpty()) {
            extraMetadata.add(EName.fromString(strEName, defaultNamespace));
          }
        } catch (IllegalArgumentException iae) {
          logger.warn("Ignoring incorrect dublincore metadata property: '{}'", strEName);
        }
    }

    // Update the episode catalog
    for (Catalog episodeCatalog : mediaPackage.getCatalogs(MediaPackageElements.EPISODE)) {
      DublinCoreCatalog episodeDublinCore = DublinCoreUtil.loadDublinCore(workspace, episodeCatalog);
      // Make sure the MP catalog has bindings defined
      episodeDublinCore.addBindings(
              XmlNamespaceContext.mk(XmlNamespaceBinding.mk(DublinCore.TERMS_NS_PREFIX, DublinCore.TERMS_NS_URI)));
      episodeDublinCore.addBindings(XmlNamespaceContext
              .mk(XmlNamespaceBinding.mk(DublinCore.ELEMENTS_1_1_NS_PREFIX, DublinCore.ELEMENTS_1_1_NS_URI)));
      episodeDublinCore.addBindings(XmlNamespaceContext
              .mk(XmlNamespaceBinding.mk(DublinCores.OC_PROPERTY_NS_PREFIX, DublinCores.OC_PROPERTY_NS_URI)));
      episodeDublinCore.set(DublinCore.PROPERTY_IS_PART_OF, seriesId);
      for (EName property : extraMetadata) {
        if (!episodeDublinCore.hasValue(property) && series.hasValue(property)) {
          episodeDublinCore.set(property, series.get(property));
        }
      }
      try (InputStream in = IOUtils.toInputStream(episodeDublinCore.toXmlString(), "UTF-8")) {
        String filename = FilenameUtils.getName(episodeCatalog.getURI().toString());
        URI uri = workspace.put(mediaPackage.getIdentifier().toString(), episodeCatalog.getIdentifier(), filename, in);
        episodeCatalog.setURI(uri);
        // setting the URI to a new source so the checksum will most like be invalid
        episodeCatalog.setChecksum(null);
      } catch (Exception e) {
        logger.error("Unable to update episode catalog isPartOf field", e);
        throw new WorkflowOperationException(e);
      }
    }

    // Attach series catalogs
    if (optAttachFlavors.isSome()) {
      // Remove existing series catalogs
      AbstractMediaPackageElementSelector<Catalog> catalogSelector = new CatalogSelector();
      String[] seriesFlavors = StringUtils.split(optAttachFlavors.get(), ",");
      for (String flavor : seriesFlavors) {
        if ("*".equals(flavor)) {
          catalogSelector.addFlavor("*/*");
        } else {
          catalogSelector.addFlavor(flavor);
        }
      }
      for (Catalog c : catalogSelector.select(mediaPackage, false)) {
        if (MediaPackageElements.SERIES.equals(c.getFlavor()) || "series".equals(c.getFlavor().getSubtype())) {
          mediaPackage.remove(c);
        }
      }

      List<SeriesCatalogUIAdapter> adapters = getSeriesCatalogUIAdapters();
      for (String flavorString : seriesFlavors) {
        MediaPackageElementFlavor flavor;
        if ("*".equals(flavorString)) {
          flavor = MediaPackageElementFlavor.parseFlavor("*/*");
        } else {
          flavor = MediaPackageElementFlavor.parseFlavor(flavorString);
        }
        for (SeriesCatalogUIAdapter a : adapters) {
          MediaPackageElementFlavor adapterFlavor = MediaPackageElementFlavor.parseFlavor(a.getFlavor().toString());
          if (flavor.matches(adapterFlavor)) {
            if (MediaPackageElements.SERIES.eq(a.getFlavor().toString())) {
              addDublinCoreCatalog(series, MediaPackageElements.SERIES, mediaPackage);
            } else {
              try {
                Opt<byte[]> seriesElementData = seriesService.getSeriesElementData(seriesId, adapterFlavor.getType());
                if (seriesElementData.isSome()) {
                  DublinCoreCatalog catalog = DublinCores.read(new ByteArrayInputStream(seriesElementData.get()));
                  addDublinCoreCatalog(catalog, adapterFlavor, mediaPackage);
                } else {
                  logger.warn("No extended series catalog found for flavor '{}' and series '{}', skip adding catalog",
                          adapterFlavor.getType(), seriesId);
                }
              } catch (SeriesException e) {
                logger.error("Unable to load extended series metadata for flavor {}", adapterFlavor.getType());
                throw new WorkflowOperationException(e);
              }
            }
          }
        }
      }
    }

    if (applyAcl) {
      try {
        AccessControlList acl = seriesService.getSeriesAccessControl(seriesId);
        if (acl != null)
          authorizationService.setAcl(mediaPackage, AclScope.Series, acl);
      } catch (Exception e) {
        logger.error("Unable to update series ACL", e);
        throw new WorkflowOperationException(e);
      }
    }
    return createResult(mediaPackage, Action.CONTINUE);
  }

  /**
   * @param organization
   *          The organization to filter the results with.
   * @return A {@link List} of {@link SeriesCatalogUIAdapter} that provide the metadata to the front end.
   */
  private List<SeriesCatalogUIAdapter> getSeriesCatalogUIAdapters() {
    String organization = securityService.getOrganization().getId();
    return Stream.$(seriesCatalogUIAdapters).filter(seriesOrganizationFilter._2(organization)).toList();
  }

  private MediaPackage addDublinCoreCatalog(DublinCoreCatalog catalog, MediaPackageElementFlavor flavor,
          MediaPackage mediaPackage) throws WorkflowOperationException {
    try (InputStream in = IOUtils.toInputStream(catalog.toXmlString(), "UTF-8")) {
      String elementId = UUID.randomUUID().toString();
      URI catalogUrl = workspace.put(mediaPackage.getIdentifier().compact(), elementId, "dublincore.xml", in);
      logger.info("Adding catalog with flavor {} to mediapackage {}", flavor, mediaPackage);
      MediaPackageElement mpe = mediaPackage.add(catalogUrl, MediaPackageElement.Type.Catalog, flavor);
      mpe.setIdentifier(elementId);
      mpe.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, workspace.get(catalogUrl)));
      return mediaPackage;
    } catch (IOException | NotFoundException e) {
      throw new WorkflowOperationException(e);
    }
  }

  private static final Fn2<SeriesCatalogUIAdapter, String, Boolean> seriesOrganizationFilter = new Fn2<SeriesCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean apply(SeriesCatalogUIAdapter catalogUIAdapter, String organization) {
      return catalogUIAdapter.getOrganization().equals(organization);
    }
  };

  /** Convert a string into a boolean. */
  private static final Fn<String, Boolean> toBoolean = new Fn<String, Boolean>() {
    @Override
    public Boolean apply(String s) {
      return BooleanUtils.toBoolean(s);
    }
  };

}
