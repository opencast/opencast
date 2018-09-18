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

package org.opencastproject.index.service.impl;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;
import static org.opencastproject.assetmanager.api.AssetManager.DEFAULT_OWNER;
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;
import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.api.query.Predicate;
import org.opencastproject.assetmanager.util.WorkflowPropertiesUtil;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.event.comment.EventComment;
import org.opencastproject.event.comment.EventCommentException;
import org.opencastproject.event.comment.EventCommentParser;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataUtil;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.series.CommonSeriesCatalogUIAdapter;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventHttpServletRequest;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.group.Group;
import org.opencastproject.index.service.impl.index.group.GroupIndexSchema;
import org.opencastproject.index.service.impl.index.group.GroupSearchQuery;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.index.service.resources.list.query.GroupsListQuery;
import org.opencastproject.index.service.util.JSONUtils;
import org.opencastproject.index.service.util.RestUtils;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SortCriterion;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataCollection;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.MetadataParsingException;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.metadata.dublincore.SeriesCatalogUIAdapter;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.XmlNamespaceBinding;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowException;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowInstance.WorkflowState;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSet;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;
import com.entwinemedia.fn.data.Opt;

import net.fortuna.ical4j.model.Period;
import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.joda.time.DateTimeZone;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class IndexServiceImpl implements IndexService {

  private static final String WORKFLOW_CONFIG_PREFIX = "org.opencastproject.workflow.config.";

  public static final String THEME_PROPERTY_NAME = "theme";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);

  private final List<EventCatalogUIAdapter> eventCatalogUIAdapters = new ArrayList<>();
  private final List<SeriesCatalogUIAdapter> seriesCatalogUIAdapters = new ArrayList<>();

  /** A parser for handling JSON documents inside the body of a request. **/
  private static final JSONParser parser = new JSONParser();

  private String attachmentRegex = "^attachment.*";
  private String catalogRegex = "^catalog.*";
  private String trackRegex = "^track.*";
  private String numberedAssetRegex = "^\\*$";

  private boolean isOverwriteExistingAsset = true;
  private Pattern patternAttachment = Pattern.compile(attachmentRegex);
  private Pattern patternCatalog = Pattern.compile(catalogRegex);
  private Pattern patternTrack = Pattern.compile(trackRegex);
  private Pattern patternNumberedAsset = Pattern.compile(numberedAssetRegex);

  private EventCatalogUIAdapter eventCatalogUIAdapter;
  private SeriesCatalogUIAdapter seriesCatalogUIAdapter;

  private AclServiceFactory aclServiceFactory;
  private AuthorizationService authorizationService;
  private CaptureAgentStateService captureAgentStateService;
  private EventCommentService eventCommentService;
  private IngestService ingestService;
  private AssetManager assetManager;
  private SchedulerService schedulerService;
  private SecurityService securityService;
  private JpaGroupRoleProvider jpaGroupRoleProvider;
  private SeriesService seriesService;
  private UserDirectoryService userDirectoryService;
  private WorkflowService workflowService;
  private Workspace workspace;

  /** The single thread executor service */
  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  /**
   * OSGi DI.
   *
   * @param aclServiceFactory
   *          the factory to set
   */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /**
   * OSGi DI.
   *
   * @param authorizationService
   *          the service to set
   */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /**
   * OSGi DI.
   *
   * @param captureAgentStateService
   *          the service to set
   */
  public void setCaptureAgentStateService(CaptureAgentStateService captureAgentStateService) {
    this.captureAgentStateService = captureAgentStateService;
  }

  /**
   * OSGi callback for the event comment service.
   *
   * @param eventCommentService
   *          the service to set
   */
  public void setEventCommentService(EventCommentService eventCommentService) {
    this.eventCommentService = eventCommentService;
  }

  /**
   * OSGi callback to add the event dublincore {@link EventCatalogUIAdapter} instance.
   *
   * @param eventCatalogUIAdapter
   *          the adapter to set
   */
  public void setCommonEventCatalogUIAdapter(CommonEventCatalogUIAdapter eventCatalogUIAdapter) {
    this.eventCatalogUIAdapter = eventCatalogUIAdapter;
  }

  /**
   * OSGi callback to add the series dublincore {@link SeriesCatalogUIAdapter} instance.
   *
   * @param seriesCatalogUIAdapter
   *          the adapter to set
   */
  public void setCommonSeriesCatalogUIAdapter(CommonSeriesCatalogUIAdapter seriesCatalogUIAdapter) {
    this.seriesCatalogUIAdapter = seriesCatalogUIAdapter;
  }

  /**
   * OSGi callback to add {@link EventCatalogUIAdapter} instance.
   *
   * @param catalogUIAdapter
   *          the adapter to add
   */
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.add(catalogUIAdapter);
  }

  /**
   * OSGi callback to remove {@link EventCatalogUIAdapter} instance.
   *
   * @param catalogUIAdapter
   *          the adapter to remove
   */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.remove(catalogUIAdapter);
  }

  /**
   * OSGi callback to add {@link SeriesCatalogUIAdapter} instance.
   *
   * @param catalogUIAdapter
   *          the adapter to add
   */
  public void addCatalogUIAdapter(SeriesCatalogUIAdapter catalogUIAdapter) {
    seriesCatalogUIAdapters.add(catalogUIAdapter);
  }

  /**
   * OSGi callback to remove {@link SeriesCatalogUIAdapter} instance.
   *
   * @param catalogUIAdapter
   *          the adapter to remove
   */
  public void removeCatalogUIAdapter(SeriesCatalogUIAdapter catalogUIAdapter) {
    seriesCatalogUIAdapters.remove(catalogUIAdapter);
  }

  /**
   * OSGi DI.
   *
   * @param ingestService
   *          the service to set
   */
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /**
   * OSGi DI.
   *
   * @param assetManager
   *          the manager to set
   */
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /**
   * OSGi DI.
   *
   * @param schedulerService
   *          the service to set
   */
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  /**
   * OSGi DI.
   *
   * @param securityService
   *          the service to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi DI.
   *
   * @param seriesService
   *          the service to set
   */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * OSGi DI.
   *
   * @param workflowService
   *          the service to set
   */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /**
   * OSGi DI.
   *
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * OSGi DI.
   *
   * @param userDirectoryService
   *          the service to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * OSGi DI.
   *
   * @param jpaGroupRoleProvider
   *          the provider to set
   */
  public void setGroupRoleProvider(JpaGroupRoleProvider jpaGroupRoleProvider) {
    this.jpaGroupRoleProvider = jpaGroupRoleProvider;
  }

  /**
   *
   * @return the acl service
   */
  public AclService getAclService() {
    return aclServiceFactory.serviceFor(securityService.getOrganization());
  }

  private static final Fn2<EventCatalogUIAdapter, String, Boolean> eventOrganizationFilter = new Fn2<EventCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean apply(EventCatalogUIAdapter catalogUIAdapter, String organization) {
      return organization.equals(catalogUIAdapter.getOrganization());
    }
  };

  private static final Fn2<SeriesCatalogUIAdapter, String, Boolean> seriesOrganizationFilter = new Fn2<SeriesCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean apply(SeriesCatalogUIAdapter catalogUIAdapter, String organization) {
      return catalogUIAdapter.getOrganization().equals(organization);
    }
  };

  public List<EventCatalogUIAdapter> getEventCatalogUIAdapters(String organization) {
    return Stream.$(eventCatalogUIAdapters).filter(eventOrganizationFilter._2(organization)).toList();
  }

  /**
   * @param organization
   *          The organization to filter the results with.
   * @return A {@link List} of {@link SeriesCatalogUIAdapter} that provide the metadata to the front end.
   */
  public List<SeriesCatalogUIAdapter> getSeriesCatalogUIAdapters(String organization) {
    return Stream.$(seriesCatalogUIAdapters).filter(seriesOrganizationFilter._2(organization)).toList();
  }

  @Override
  public List<EventCatalogUIAdapter> getEventCatalogUIAdapters() {
    return new ArrayList<>(getEventCatalogUIAdapters(securityService.getOrganization().getId()));
  }

  @Override
  public List<SeriesCatalogUIAdapter> getSeriesCatalogUIAdapters() {
    return new LinkedList<>(getSeriesCatalogUIAdapters(securityService.getOrganization().getId()));
  }

  @Override
  public EventCatalogUIAdapter getCommonEventCatalogUIAdapter() {
    return eventCatalogUIAdapter;
  }

  @Override
  public SeriesCatalogUIAdapter getCommonSeriesCatalogUIAdapter() {
    return seriesCatalogUIAdapter;
  }

  @Override
  public String createEvent(HttpServletRequest request) throws IndexServiceException {
    JSONObject metadataJson = null;
    MediaPackage mp = null;
    // regex for form field name matching an attachment or a catalog
    // The first sub items identifies if the file is an attachment or catalog
    // The second is the item flavor
    // Example form field names:  "catalog/captions/timedtext" and "attachment/captions/vtt"
    // The prefix of field name for attachment and catalog
    List<String> assetList = new LinkedList<String>();
    try {
      if (ServletFileUpload.isMultipartContent(request)) {
        mp = ingestService.createMediaPackage();

        for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
          FileItemStream item = iter.next();
          String fieldName = item.getFieldName();
          if (item.isFormField()) {
            if ("metadata".equals(fieldName)) {
              String metadata = Streams.asString(item.openStream());
              try {
                metadataJson = (JSONObject) new JSONParser().parse(metadata);
                // in case of scheduling: Check if user has access to the CA
                if (metadataJson.containsKey("source")) {
                  final JSONObject sourceJson = (JSONObject) metadataJson.get("source");
                  if (sourceJson.containsKey("metadata")) {
                    final JSONObject sourceMetadataJson = (JSONObject) sourceJson.get("metadata");
                    if (sourceMetadataJson.containsKey("device")) {
                      SecurityUtil.checkAgentAccess(securityService, (String) sourceMetadataJson.get("device"));
                    }
                  }
                }
              } catch (Exception e) {
                logger.warn("Unable to parse metadata {}", metadata);
                throw new IllegalArgumentException("Unable to parse metadata");
              }
            }
          } else {
            if ("presenter".equals(item.getFieldName())) {
              mp = ingestService.addTrack(item.openStream(), item.getName(), MediaPackageElements.PRESENTER_SOURCE, mp);
            } else if ("presentation".equals(item.getFieldName())) {
              mp = ingestService.addTrack(item.openStream(), item.getName(), MediaPackageElements.PRESENTATION_SOURCE,
                      mp);
            } else if ("audio".equals(item.getFieldName())) {
              mp = ingestService.addTrack(item.openStream(), item.getName(),
                      new MediaPackageElementFlavor("presenter-audio", "source"), mp);
              // For dynamic uploads, cannot get flavor at this point, so saving with temporary flavor
            } else if (item.getFieldName().toLowerCase().matches(attachmentRegex)) {
              assetList.add(item.getFieldName());
              mp =  ingestService.addAttachment(item.openStream(), item.getName(),
                      new MediaPackageElementFlavor(item.getFieldName(), "*"), mp);
            } else if (item.getFieldName().toLowerCase().matches(catalogRegex)) {
              // Cannot get flavor at this point, so saving with temporary flavor
              assetList.add(item.getFieldName());
              mp =  ingestService.addCatalog(item.openStream(), item.getName(),
                      new MediaPackageElementFlavor(item.getFieldName(), "*"), mp);
            } else if (item.getFieldName().toLowerCase().matches(trackRegex)) {
              // Cannot get flavor at this point, so saving with temporary flavor
              assetList.add(item.getFieldName());
              mp = ingestService.addTrack(item.openStream(), item.getName(),
                      new MediaPackageElementFlavor(item.getFieldName(), "*"), mp);
              } else {
              logger.warn("Unknown field name found {}", item.getFieldName());
            }
          }
        }
        // MH-12085 update the flavors of any newly added assets.
        try {
          JSONArray assetMetadata = (JSONArray)((JSONObject) metadataJson.get("assets")).get("options");
          if (assetMetadata != null) {
            mp = updateMpAssetFlavor(assetList, mp, assetMetadata, isOverwriteExistingAsset);
           }
          } catch (Exception e) {
            // Assuming a parse error versus a file error and logging the error type
            logger.warn("Unable to process asset metadata {}", metadataJson.get("assets"), e);
            throw new IllegalArgumentException("Unable to parse metadata", e);
          }

      } else {
        throw new IllegalArgumentException("No multipart content");
      }

      // MH-10834 If there is only an audio track, change the flavor from presenter-audio/source to presenter/source.
      if (mp.getTracks().length == 1
              && mp.getTracks()[0].getFlavor().equals(new MediaPackageElementFlavor("presenter-audio", "source"))) {
        Track audioTrack = mp.getTracks()[0];
        mp.remove(audioTrack);
        audioTrack.setFlavor(MediaPackageElements.PRESENTER_SOURCE);
        mp.add(audioTrack);
      }

      return createEvent(metadataJson, mp);
    } catch (Exception e) {
      logger.error("Unable to create event: {}", getStackTrace(e));
      throw new IndexServiceException(e.getMessage());
    }
  }

  @Override
  public String updateEventAssets(MediaPackage mp, HttpServletRequest request) throws IndexServiceException {
    JSONObject metadataJson = null;
    // regex for form field name matching an attachment or a catalog
    // The first sub items identifies if the file is an attachment or catalog
    // The second is the item flavor
    // Example form field names:  "catalog/captions/timedtext" and "attachment/captions/vtt"
    // The prefix of field name for attachment and catalog
    // The metadata is expected to contain a workflow definition id and
    // asset metadata mapped to the asset field id.
    List<String> assetList = new LinkedList<String>();
    // 1. save assets with temporary flavors
    try {
      if (!ServletFileUpload.isMultipartContent(request)) {
        throw new IllegalArgumentException("No multipart content");
      }
      for (FileItemIterator iter = new ServletFileUpload().getItemIterator(request); iter.hasNext();) {
        FileItemStream item = iter.next();
        String fieldName = item.getFieldName();
        if (item.isFormField()) {
          if ("metadata".equals(fieldName)) {
            String metadata = Streams.asString(item.openStream());
            try {
              metadataJson = (JSONObject) parser.parse(metadata);
            } catch (Exception e) {
              logger.warn("Unable to parse metadata {}", metadata);
              throw new IllegalArgumentException("Unable to parse metadata");
            }
          }
        } else {
          if (item.getFieldName().toLowerCase().matches(attachmentRegex)) {
            assetList.add(item.getFieldName());
            // Add attachment with field name as temporary flavor
            mp =  ingestService.addAttachment(item.openStream(), item.getName(),
                    new MediaPackageElementFlavor(item.getFieldName(), "*"), mp);
          } else if (item.getFieldName().toLowerCase().matches(catalogRegex)) {

            assetList.add(item.getFieldName());
            // Add catalog with field name as temporary flavor
            mp =  ingestService.addCatalog(item.openStream(), item.getName(),
                    new MediaPackageElementFlavor(item.getFieldName(), "*"), mp);
          } else {
            logger.warn("Unknown field name found {}", item.getFieldName());
          }
        }
      }
      // 2. remove existing assets of the new flavor
      // and correct the temporary flavor to the new flavor.
      try {
        JSONArray assetMetadata = (JSONArray)((JSONObject) metadataJson.get("assets")).get("options");
        if (assetMetadata != null) {
          mp = updateMpAssetFlavor(assetList, mp, assetMetadata, isOverwriteExistingAsset);
        } else {
          logger.warn("The asset option mapping parameter was not found");
          throw new IndexServiceException("The asset option mapping parameter was not found");
        }
      } catch (Exception e) {
        // Assuming a parse error versus a file error and logging the error type
        logger.warn("Unable to process asset metadata {}", metadataJson.get("assets"), e);
        throw new IllegalArgumentException("Unable to parse metadata", e);
      }

      return startAddAssetWorkflow(metadataJson, mp);
    } catch (Exception e) {
      logger.error("Unable to create event: {}", getStackTrace(e));
      throw new IndexServiceException(e.getMessage());
    }
  }

  /**
   * Parses the processing information, including the workflowDefinitionId, from the metadataJson and starts the
   * workflow with the passed mediapackage.
   *
   * TODO NOTE: This checks for running workflows, then takes a snapshot prior to starting a new workflow. This causes a
   * potential race condition:
   *
   * 1. An existing workflow is running, the add asset workflow cannot start.
   *
   * 2. The snapshot(4x) archive(3x) is saved and the new workflow is started.
   *
   * 3. Possible race condition: No running workflow, a snapshot is saved but the workflow cannot start because another
   * workflow has started between the time of checking and starting running.
   *
   * 4. If race condition: the Admin UI shows error that the workflow could not start.
   *
   * 5. If race condition: The interim snapshot(4x) archive(3x) is updated(4x-3x) by the running workflow's snapshots
   * and resolves the inconsistency, eventually.
   *
   * Example of processing json:
   *
   * ...., "processing": { "workflow": "full", "configuration": { "videoPreview": "false", "trimHold": "false",
   * "captionHold": "false", "archiveOp": "true", "publishEngage": "true", "publishHarvesting": "true" } }, ....
   *
   * @param metadataJson
   * @param mp
   * @return the created workflow instance id
   * @throws IndexServiceException
   */
  private String startAddAssetWorkflow(JSONObject metadataJson, MediaPackage mediaPackage)
          throws IndexServiceException {
    String wfId = null;
    String mpId = mediaPackage.getIdentifier().toString();

    JSONObject processing = (JSONObject) metadataJson.get("processing");
    if (processing == null)
      throw new IllegalArgumentException("No processing field in metadata");

    String workflowDefId = (String) processing.get("workflow");
    if (workflowDefId == null)
      throw new IllegalArgumentException("No workflow definition field in processing metadata");

    JSONObject configJson = (JSONObject) processing.get("configuration");

    try {
      // 1. Check if any active workflows are running for this mediapackage id
      WorkflowSet workflowSet  = workflowService.getWorkflowInstances(new WorkflowQuery().withMediaPackage(mpId));
      for (WorkflowInstance wf : Arrays.asList(workflowSet.getItems())) {
        if (wf.isActive()) {
          logger.warn("Unable to start new workflow '{}' on archived media package '{}', existing workfow {} is running",
                  workflowDefId, mediaPackage, wf.getId());
          throw new IllegalArgumentException("A workflow is already active for mp " + mpId + ", cannot start this workflow.");
        }
      }
      // 2. Save the snapshot
      assetManager.takeSnapshot(mediaPackage);

      // 3. start the new workflow on the snapshot
      // Workflow params are assumed to be String (not mixed with Number)
      Map<String, String> params = new HashMap<String, String>();
      if (configJson != null) {
        for (Object key: configJson.keySet()) {
          params.put((String)key, (String) configJson.get(key));
        }
      }

      Set<String> mpIds = new HashSet<String>();
      mpIds.add(mpId);

      final Workflows workflows = new Workflows(assetManager, workspace, workflowService);
      List<WorkflowInstance> wfList = workflows
              .applyWorkflowToLatestVersion(mpIds,
                      ConfiguredWorkflow.workflow(workflowService.getWorkflowDefinitionById(workflowDefId), params))
              .toList();
      wfId = wfList.size() > 0 ? Long.toString(wfList.get(0).getId()) : "Unknown";
      logger.info("Asset update and publish workflow {} scheduled for mp {}",wfId, mpId);

    } catch (AssetManagerException e) {
      logger.warn("Unable to start workflow '{}' on archived media package '{}': {}",
              workflowDefId, mediaPackage, getStackTrace(e));
      throw new IndexServiceException("Unable to start workflow " + workflowDefId + " on " + mpId);
    } catch (WorkflowDatabaseException e) {
      logger.warn("Unable to load workflow '{}' from workflow service: {}", wfId, getStackTrace(e));
    } catch (NotFoundException e) {
      logger.warn("Workflow '{}' not found", wfId);
    }
    return wfId;
  }

  /**
   * Get the type of the source that is creating the event.
   *
   * @param source
   *          The source of the event e.g. upload, single scheduled, multi scheduled
   * @return The type of the source
   * @throws IllegalArgumentException
   *           Thrown if unable to get the source from the json object.
   */
  private SourceType getSourceType(JSONObject source) {
    SourceType type;
    try {
      type = SourceType.valueOf((String) source.get("type"));
    } catch (Exception e) {
      logger.error("Unknown source type '{}'", source.get("type"));
      throw new IllegalArgumentException("Unknown source type");
    }
    return type;
  }

  /**
   * Get the access control list from a JSON representation
   *
   * @param metadataJson
   *          The {@link JSONObject} that has the access json
   * @return An {@link AccessControlList}
   * @throws IllegalArgumentException
   *           Thrown if unable to parse the access control list
   */
  private AccessControlList getAccessControlList(JSONObject metadataJson) {
    AccessControlList acl = new AccessControlList();
    JSONObject accessJson = (JSONObject) metadataJson.get("access");
    if (accessJson != null) {
      try {
        acl = AccessControlParser.parseAcl(accessJson.toJSONString());
      } catch (Exception e) {
        logger.warn("Unable to parse access control list: {}", accessJson.toJSONString());
        throw new IllegalArgumentException("Unable to parse access control list!");
      }
    }
    return acl;
  }

  @Override
  public String createEvent(JSONObject metadataJson, MediaPackage mp) throws ParseException, IOException,
          MediaPackageException, IngestException, NotFoundException, SchedulerException, UnauthorizedException {
    if (metadataJson == null)
      throw new IllegalArgumentException("No metadata set");

    JSONObject source = (JSONObject) metadataJson.get("source");
    if (source == null)
      throw new IllegalArgumentException("No source field in metadata");

    JSONObject processing = (JSONObject) metadataJson.get("processing");
    if (processing == null)
      throw new IllegalArgumentException("No processing field in metadata");

    JSONArray allEventMetadataJson = (JSONArray) metadataJson.get("metadata");
    if (allEventMetadataJson == null)
      throw new IllegalArgumentException("No metadata field in metadata");

    AccessControlList acl = getAccessControlList(metadataJson);

    MetadataList metadataList = getMetadataListWithAllEventCatalogUIAdapters();
    try {
      metadataList.fromJSON(allEventMetadataJson.toJSONString());
    } catch (MetadataParsingException e) {
      logger.warn("Unable to parse event metadata {}", allEventMetadataJson.toJSONString());
      throw new IllegalArgumentException("Unable to parse metadata set");
    }

    EventHttpServletRequest eventHttpServletRequest = new EventHttpServletRequest();
    eventHttpServletRequest.setAcl(acl);
    eventHttpServletRequest.setMetadataList(metadataList);
    eventHttpServletRequest.setMediaPackage(mp);
    eventHttpServletRequest.setProcessing(processing);
    eventHttpServletRequest.setSource(source);

    return createEvent(eventHttpServletRequest);
  }

  @Override
  public String createEvent(EventHttpServletRequest eventHttpServletRequest) throws ParseException, IOException,
          MediaPackageException, IngestException, NotFoundException, SchedulerException, UnauthorizedException {
    // Preconditions
    if (eventHttpServletRequest.getAcl().isNone()) {
      throw new IllegalArgumentException("No access control list available to create new event.");
    }
    if (eventHttpServletRequest.getMediaPackage().isNone()) {
      throw new IllegalArgumentException("No mediapackage available to create new event.");
    }
    if (eventHttpServletRequest.getMetadataList().isNone()) {
      throw new IllegalArgumentException("No metadata list available to create new event.");
    }
    if (eventHttpServletRequest.getProcessing().isNone()) {
      throw new IllegalArgumentException("No processing metadata available to create new event.");
    }
    if (eventHttpServletRequest.getSource().isNone()) {
      throw new IllegalArgumentException("No source field metadata available to create new event.");
    }

    // Get Workflow
    String workflowTemplate = (String) eventHttpServletRequest.getProcessing().get().get("workflow");
    if (workflowTemplate == null)
      throw new IllegalArgumentException("No workflow template in metadata");

    // Get Type of Source
    SourceType type = getSourceType(eventHttpServletRequest.getSource().get());

    MetadataCollection eventMetadata = eventHttpServletRequest.getMetadataList().get()
            .getMetadataByAdapter(eventCatalogUIAdapter).get();

    Date currentStartDate = null;
    JSONObject sourceMetadata = (JSONObject) eventHttpServletRequest.getSource().get().get("metadata");
    if (sourceMetadata != null
            && (type.equals(SourceType.SCHEDULE_SINGLE) || type.equals(SourceType.SCHEDULE_MULTIPLE))) {
      try {
        MetadataField<?> current = eventMetadata.getOutputFields().get("location");
        eventMetadata.updateStringField(current, (String) sourceMetadata.get("device"));
      } catch (Exception e) {
        logger.warn("Unable to parse device {}", sourceMetadata.get("device"));
        throw new IllegalArgumentException("Unable to parse device");
      }
      if (StringUtils.isNotEmpty((String) sourceMetadata.get("start"))) {
        currentStartDate = EncodingSchemeUtils.decodeDate((String) sourceMetadata.get("start"));
      }
    }

    MetadataField<?> startDate = eventMetadata.getOutputFields().get("startDate");
    if (startDate != null && startDate.isUpdated() && startDate.getValue().isSome()) {
      SimpleDateFormat sdf = MetadataField.getSimpleDateFormatter(startDate.getPattern().get());
      currentStartDate = sdf.parse((String) startDate.getValue().get());
    } else if (currentStartDate != null) {
      eventMetadata.removeField(startDate);
      MetadataField<String> newStartDate = MetadataUtils.copyMetadataField(startDate);
      newStartDate.setValue(EncodingSchemeUtils.encodeDate(currentStartDate, Precision.Fraction).getValue());
      eventMetadata.addField(newStartDate);
    }

    MetadataField<?> created = eventMetadata.getOutputFields().get(DublinCore.PROPERTY_CREATED.getLocalName());
    if (created == null || !created.isUpdated() || created.getValue().isNone()) {
      eventMetadata.removeField(created);
      MetadataField<String> newCreated = MetadataUtils.copyMetadataField(created);
      if (currentStartDate != null) {
        newCreated.setValue(EncodingSchemeUtils.encodeDate(currentStartDate, Precision.Second).getValue());
      } else {
        newCreated.setValue(EncodingSchemeUtils.encodeDate(new Date(), Precision.Second).getValue());
      }
      eventMetadata.addField(newCreated);
    }

    // Get presenter usernames for use as technical presenters
    Set<String> presenterUsernames = new HashSet<>();
    Opt<Set<String>> technicalPresenters = updatePresenters(eventMetadata);
    if (technicalPresenters.isSome()) {
      presenterUsernames = technicalPresenters.get();
    }

    eventHttpServletRequest.getMetadataList().get().add(eventCatalogUIAdapter, eventMetadata);
    updateMediaPackageMetadata(eventHttpServletRequest.getMediaPackage().get(),
            eventHttpServletRequest.getMetadataList().get());

    DublinCoreCatalog dc = getDublinCoreCatalog(eventHttpServletRequest);
    String captureAgentId = null;
    TimeZone tz = null;
    org.joda.time.DateTime start = null;
    org.joda.time.DateTime end = null;
    long duration = 0L;
    Properties caProperties = new Properties();
    RRule rRule = null;
    if (sourceMetadata != null
            && (type.equals(SourceType.SCHEDULE_SINGLE) || type.equals(SourceType.SCHEDULE_MULTIPLE))) {
      Properties configuration;
      try {
        captureAgentId = (String) sourceMetadata.get("device");
        configuration = captureAgentStateService.getAgentConfiguration((String) sourceMetadata.get("device"));
      } catch (Exception e) {
        logger.warn("Unable to parse device {}: because: {}", sourceMetadata.get("device"), getStackTrace(e));
        throw new IllegalArgumentException("Unable to parse device");
      }

      String durationString = (String) sourceMetadata.get("duration");
      if (StringUtils.isBlank(durationString))
        throw new IllegalArgumentException("No duration in source metadata");

      // Create timezone based on CA's reported TZ.
      String agentTimeZone = configuration.getProperty("capture.device.timezone");
      if (StringUtils.isNotBlank(agentTimeZone)) {
        tz = TimeZone.getTimeZone(agentTimeZone);
        dc.set(DublinCores.OC_PROPERTY_AGENT_TIMEZONE, tz.getID());
      } else { // No timezone was present, assume the serve's local timezone.
        tz = TimeZone.getDefault();
        logger.debug(
                "The field 'capture.device.timezone' has not been set in the agent configuration. The default server timezone will be used.");
      }

      org.joda.time.DateTime now = new org.joda.time.DateTime(DateTimeZone.UTC);
      start = now.withMillis(DateTimeSupport.fromUTC((String) sourceMetadata.get("start")));
      end = now.withMillis(DateTimeSupport.fromUTC((String) sourceMetadata.get("end")));
      duration = Long.parseLong(durationString);
      DublinCoreValue period = EncodingSchemeUtils
              .encodePeriod(new DCMIPeriod(start.toDate(), start.plus(duration).toDate()), Precision.Second);
      String inputs = (String) sourceMetadata.get("inputs");

      caProperties.putAll(configuration);
      dc.set(DublinCore.PROPERTY_TEMPORAL, period);
      caProperties.put(CaptureParameters.CAPTURE_DEVICE_NAMES, inputs);
    }

    if (type.equals(SourceType.SCHEDULE_MULTIPLE)) {
      rRule = new RRule((String) sourceMetadata.get("rrule"));
    }

    Map<String, String> configuration = new HashMap<>();
    if (eventHttpServletRequest.getProcessing().get().get("configuration") != null) {
      configuration = new HashMap<>((JSONObject) eventHttpServletRequest.getProcessing().get().get("configuration"));

    }
    for (Entry<String, String> entry : configuration.entrySet()) {
      caProperties.put(WORKFLOW_CONFIG_PREFIX.concat(entry.getKey()), entry.getValue());
    }
    caProperties.put(CaptureParameters.INGEST_WORKFLOW_DEFINITION, workflowTemplate);

    eventHttpServletRequest.setMediaPackage(authorizationService.setAcl(eventHttpServletRequest.getMediaPackage().get(),
            AclScope.Episode, eventHttpServletRequest.getAcl().get()).getA());

    MediaPackage mediaPackage;
    switch (type) {
      case UPLOAD:
      case UPLOAD_LATER:
        eventHttpServletRequest
                .setMediaPackage(updateDublincCoreCatalog(eventHttpServletRequest.getMediaPackage().get(), dc));
        configuration.put("workflowDefinitionId", workflowTemplate);
        WorkflowInstance ingest = ingestService.ingest(eventHttpServletRequest.getMediaPackage().get(),
                workflowTemplate, configuration);
        return eventHttpServletRequest.getMediaPackage().get().getIdentifier().compact();
      case SCHEDULE_SINGLE:
        mediaPackage = updateDublincCoreCatalog(eventHttpServletRequest.getMediaPackage().get(), dc);
        eventHttpServletRequest.setMediaPackage(mediaPackage);
        try {
          schedulerService.addEvent(start.toDate(), start.plus(duration).toDate(), captureAgentId, presenterUsernames,
                  mediaPackage, configuration, (Map) caProperties, Opt.<Boolean> none(), Opt.<String> none(),
                  SchedulerService.ORIGIN);
        } finally {
          for (MediaPackageElement mediaPackageElement : mediaPackage.getElements()) {
            try {
              workspace.delete(mediaPackage.getIdentifier().toString(), mediaPackageElement.getIdentifier());
            } catch (NotFoundException | IOException e) {
              logger.warn("Failed to delete media package element", e);
            }
          }
        }
        return mediaPackage.getIdentifier().compact();
      case SCHEDULE_MULTIPLE:
        List<Period> periods = schedulerService.calculatePeriods(rRule, start.toDate(), end.toDate(), duration, tz);
        Map<String, Period> scheduled = new LinkedHashMap<>();
         scheduled = schedulerService.addMultipleEvents(rRule, start.toDate(), end.toDate(), duration, tz, captureAgentId,
                presenterUsernames, eventHttpServletRequest.getMediaPackage().get(), configuration, (Map) caProperties, Opt.none(), Opt.none(), SchedulerService.ORIGIN);
        return StringUtils.join(scheduled.keySet(), ",");
      default:
        logger.warn("Unknown source type {}", type);
        throw new IllegalArgumentException("Unknown source type");
    }
  }

  /**
   * Get the {@link DublinCoreCatalog} from an {@link EventHttpServletRequest}.
   *
   * @param eventHttpServletRequest
   *          The request to extract the {@link DublinCoreCatalog} from.
   * @return The {@link DublinCoreCatalog}
   */
  private DublinCoreCatalog getDublinCoreCatalog(EventHttpServletRequest eventHttpServletRequest) {
    DublinCoreCatalog dc;
    Opt<DublinCoreCatalog> dcOpt = DublinCoreUtil.loadEpisodeDublinCore(workspace,
            eventHttpServletRequest.getMediaPackage().get());
    if (dcOpt.isSome()) {
      dc = dcOpt.get();
      // make sure to bind the OC_PROPERTY namespace
      dc.addBindings(XmlNamespaceContext
              .mk(XmlNamespaceBinding.mk(DublinCores.OC_PROPERTY_NS_PREFIX, DublinCores.OC_PROPERTY_NS_URI)));
    } else {
      dc = DublinCores.mkOpencastEpisode().getCatalog();
    }
    return dc;
  }

  /**
   * Update the presenters field in the event {@link MetadataCollection} to have friendly names loaded by the
   * {@link UserDirectoryService} and return the usernames of the presenters.
   *
   * @param eventMetadata
   *          The {@link MetadataCollection} to update the presenters (creator field) with full names.
   * @return If the presenters (creator) field has been updated, the set of user names, if any, of the presenters. None
   *         if it wasn't updated.
   */
  private Opt<Set<String>> updatePresenters(MetadataCollection eventMetadata) {
    MetadataField<?> presentersMetadataField = eventMetadata.getOutputFields()
            .get(DublinCore.PROPERTY_CREATOR.getLocalName());
    if (presentersMetadataField.isUpdated()) {
      Set<String> presenterUsernames = new HashSet<>();
      Tuple<List<String>, Set<String>> updatedPresenters = getTechnicalPresenters(eventMetadata);
      presenterUsernames = updatedPresenters.getB();
      eventMetadata.removeField(presentersMetadataField);
      MetadataField<Iterable<String>> newPresentersMetadataField = MetadataUtils
              .copyMetadataField(presentersMetadataField);
      newPresentersMetadataField.setValue(updatedPresenters.getA());
      eventMetadata.addField(newPresentersMetadataField);
      return Opt.some(presenterUsernames);
    } else {
      return Opt.none();
    }
  }

  /**
   *
   * @param mp
   *          the mediapackage to update
   * @param dc
   *          the dublincore metadata to use to update the mediapackage
   * @return the updated mediapackage
   * @throws IOException
   *           Thrown if an IO error occurred adding the dc catalog file
   * @throws MediaPackageException
   *           Thrown if an error occurred updating the mediapackage
   * @throws IngestException
   *           Thrown if an error occurred attaching the catalog to the mediapackage
   */
  private MediaPackage updateDublincCoreCatalog(MediaPackage mp, DublinCoreCatalog dc)
          throws IOException, MediaPackageException, IngestException {
    try (InputStream inputStream = IOUtils.toInputStream(dc.toXmlString(), "UTF-8")) {
      // Update dublincore catalog
      Catalog[] catalogs = mp.getCatalogs(MediaPackageElements.EPISODE);
      if (catalogs.length > 0) {
        Catalog catalog = catalogs[0];
        URI uri = workspace.put(mp.getIdentifier().toString(), catalog.getIdentifier(), "dublincore.xml", inputStream);
        catalog.setURI(uri);
        // setting the URI to a new source so the checksum will most like be invalid
        catalog.setChecksum(null);
      } else {
        mp = ingestService.addCatalog(inputStream, "dublincore.xml", MediaPackageElements.EPISODE, mp);
      }
    }
    return mp;
  }

  /**
   * Update the flavor of newly added asset with the passed metadata
   *
   * @param assetList
   *          the list of assets to update
   * @param mp
   *          the mediapackage to update
   * @param assetMetadata
   *          a set of mapping metadata for the asset list
   * @param overwriteExisting
   *          true if the existing asset of the same flavor should be overwritten
   * @return mediapackage updated with assets
   */
  @SuppressWarnings("unchecked")
  protected MediaPackage updateMpAssetFlavor(List<String> assetList, MediaPackage mp, JSONArray assetMetadata, Boolean overwriteExisting) {
    // Create JSONObject data map
    JSONObject assetDataMap = new JSONObject();
    for (int i = 0; i < assetMetadata.size(); i++) {
      try {
        assetDataMap.put(((JSONObject) assetMetadata.get(i)).get("id"), assetMetadata.get(i));
      } catch (Exception e) {
        throw new IllegalArgumentException("Unable to parse metadata", e);
      }
    }
    // Find the correct flavor for each asset.
    for (String assetOrig: assetList) {
      // expecting file assets to contain postfix "track_trackpart.0"
      String asset = assetOrig;
      String assetNumber = null;
      String[] assetNameParts = asset.split(Pattern.quote("."));
      if (assetNameParts.length > 1) {
        asset = assetNameParts[0];
        assetNumber = assetNameParts[1];
      }
      try {
        if ((assetMetadata != null) && (assetDataMap.get(asset) != null)) {
          String type = (String)((JSONObject) assetDataMap.get(asset)).get("type");
          String flavorType = (String)((JSONObject) assetDataMap.get(asset)).get("flavorType");
          String flavorSubType = (String)((JSONObject) assetDataMap.get(asset)).get("flavorSubType");
          if (patternNumberedAsset.matcher(flavorSubType).matches() && (assetNumber != null)) {
            flavorSubType = assetNumber;
          }
          MediaPackageElementFlavor newElemflavor = new MediaPackageElementFlavor(flavorType, flavorSubType);
          if (patternAttachment.matcher(type).matches()) {
            if (overwriteExisting) {
              // remove existing attachments of the new flavor
              Attachment[] existing = mp.getAttachments(newElemflavor);
              for (int i = 0; i < existing.length; i++) {
                mp.remove(existing[i]);
                logger.info("Overwriting existing asset {} {}", type, newElemflavor);
              }
            }
            // correct the flavor of the new attachment
            Attachment[] elArray = mp.getAttachments(new MediaPackageElementFlavor(assetOrig, "*"));
            elArray[0].setFlavor(newElemflavor);
            logger.info("Updated asset {} {}", type, newElemflavor);
          } else if (patternCatalog.matcher(type).matches()) {
            if (overwriteExisting) {
              // remove existing catalogs of the new flavor
              Catalog[] existing = mp.getCatalogs(newElemflavor);
              for (int i = 0; i < existing.length; i++) {
                mp.remove(existing[i]);
                logger.info("Overwriting existing asset {} {}", type, newElemflavor);
              }
            }
            Catalog[] catArray = mp.getCatalogs(new MediaPackageElementFlavor(assetOrig, "*"));
            if (catArray.length > 1) {
              throw new IllegalArgumentException("More than one " + asset + " found, only one expected.");
            }
            catArray[0].setFlavor(newElemflavor);
            logger.info("Update asset {} {}", type, newElemflavor);
          } else if (patternTrack.matcher(type).matches()) {
            // Overwriting of existing tracks of same flavor is currently not allowed.
            // TODO: allow overwriting of existing tracks of same flavor
            Track[]  trackArray = mp.getTracks(new MediaPackageElementFlavor(assetOrig, "*"));
            if (trackArray.length > 1) {
              throw new IllegalArgumentException("More than one " + asset + " found, only one expected.");
            }
            trackArray[0].setFlavor(newElemflavor);
            logger.info("Update asset {} {}", type, newElemflavor);
          } else {
            logger.warn("Unknown asset type {} {} for field {}", type, newElemflavor, asset);
          }
        }
      } catch (Exception e) {
        // Assuming a parse error versus a file error and logging the error type
        logger.warn("Unable to process asset metadata {}", assetMetadata.toJSONString(), e);
        throw new IllegalArgumentException("Unable to parse metadata", e);
      }
    }
    return mp;
  }

  @Override
  public MetadataList updateCommonEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, SearchIndexException, NotFoundException,
          UnauthorizedException {
    MetadataList metadataList;
    try {
      metadataList = getMetadataListWithCommonEventCatalogUIAdapter();
      metadataList.fromJSON(metadataJSON);
    } catch (Exception e) {
      logger.warn("Not able to parse the event metadata {}: {}", metadataJSON, getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the event metadata " + metadataJSON, e);
    }
    return updateEventMetadata(id, metadataList, index);
  }

  @Override
  public MetadataList updateAllEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, NotFoundException, SearchIndexException,
          UnauthorizedException {
    MetadataList metadataList;
    try {
      metadataList = getMetadataListWithAllEventCatalogUIAdapters();
      metadataList.fromJSON(metadataJSON);
    } catch (Exception e) {
      logger.warn("Not able to parse the event metadata {}: {}", metadataJSON, getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the event metadata " + metadataJSON, e);
    }
    return updateEventMetadata(id, metadataList, index);
  }

  @Override
  public void removeCatalogByFlavor(Event event, MediaPackageElementFlavor flavor)
          throws IndexServiceException, NotFoundException, UnauthorizedException {
    MediaPackage mediaPackage = getEventMediapackage(event);
    Catalog[] catalogs = mediaPackage.getCatalogs(flavor);
    if (catalogs.length == 0) {
      throw new NotFoundException(String.format("Cannot find a catalog with flavor '%s' for event with id '%s'.",
              flavor.toString(), event.getIdentifier()));
    }
    for (Catalog catalog : catalogs) {
      mediaPackage.remove(catalog);
    }
    switch (getEventSource(event)) {
      case WORKFLOW:
        Opt<WorkflowInstance> workflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
        if (workflowInstance.isNone()) {
          logger.error("No workflow instance for event {} found!", event.getIdentifier());
          throw new IndexServiceException("No workflow instance found for event " + event.getIdentifier());
        }
        try {
          WorkflowInstance instance = workflowInstance.get();
          instance.setMediaPackage(mediaPackage);
          updateWorkflowInstance(instance);
        } catch (WorkflowException e) {
          logger.error("Unable to remove catalog with flavor {} by updating workflow event {} because {}",
                  flavor, event.getIdentifier(), getStackTrace(e));
          throw new IndexServiceException("Unable to update workflow event " + event.getIdentifier());
        }
        break;
      case ARCHIVE:
        assetManager.takeSnapshot(mediaPackage);
        break;
      case SCHEDULE:
        try {
          schedulerService.updateEvent(event.getIdentifier(), Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
                  Opt.<Set<String>> none(), Opt.some(mediaPackage), Opt.<Map<String, String>> none(),
                  Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
        } catch (SchedulerException e) {
          logger.error("Unable to remove catalog with flavor {} by updating scheduled event {} because {}",
                  flavor, event.getIdentifier(), getStackTrace(e));
          throw new IndexServiceException("Unable to update scheduled event " + event.getIdentifier());
        }
        break;
      default:
        throw new IndexServiceException(
                String.format("Unable to handle event source type '%s'", getEventSource(event)));
    }
  }

  @Override
  public void removeCatalogByFlavor(Series series, MediaPackageElementFlavor flavor)
          throws NotFoundException, IndexServiceException {
    if (series == null) {
      throw new IllegalArgumentException("The series cannot be null.");
    }
    if (flavor == null) {
      throw new IllegalArgumentException("The flavor cannot be null.");
    }
    boolean found = false;
    try {
      found = seriesService.deleteSeriesElement(series.getIdentifier(), flavor.getType());
    } catch (SeriesException e) {
      throw new IndexServiceException(String.format("Unable to delete catalog from series '%s' with type '%s'",
              series.getIdentifier(), flavor.getType()), e);
    }

    if (!found) {
      throw new NotFoundException(String.format("Unable to find a catalog for series '%s' with flavor '%s'",
              series.getIdentifier(), flavor));
    }
  }

  @Override
  public MetadataList updateEventMetadata(String id, MetadataList metadataList, AbstractSearchIndex index)
          throws IndexServiceException, SearchIndexException, NotFoundException, UnauthorizedException {
    Opt<Event> optEvent = getEvent(id, index);
    if (optEvent.isNone())
      throw new NotFoundException("Cannot find an event with id " + id);

    Event event = optEvent.get();
    MediaPackage mediaPackage = getEventMediapackage(event);
    Opt<Set<String>> presenters = Opt.none();
    Opt<MetadataCollection> eventCatalog = metadataList.getMetadataByAdapter(getCommonEventCatalogUIAdapter());
    if (eventCatalog.isSome()) {
      presenters = updatePresenters(eventCatalog.get());
    }
    updateMediaPackageMetadata(mediaPackage, metadataList);
    switch (getEventSource(event)) {
      case WORKFLOW:
        Opt<WorkflowInstance> workflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
        if (workflowInstance.isNone()) {
          logger.error("No workflow instance for event {} found!", event.getIdentifier());
          throw new IndexServiceException("No workflow instance found for event " + event.getIdentifier());
        }
        try {
          WorkflowInstance instance = workflowInstance.get();
          instance.setMediaPackage(mediaPackage);
          updateWorkflowInstance(instance);
        } catch (WorkflowException e) {
          logger.error("Unable to update workflow event {} with metadata {} because {}",
                  id, RestUtils.getJsonStringSilent(metadataList.toJSON()), getStackTrace(e));
          throw new IndexServiceException("Unable to update workflow event " + id);
        }
        break;
      case ARCHIVE:
        assetManager.takeSnapshot(mediaPackage);
        break;
      case SCHEDULE:
        try {
          schedulerService.updateEvent(id, Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(), presenters,
                  Opt.some(mediaPackage), Opt.<Map<String, String>> none(), Opt.<Map<String, String>> none(),
                  Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
        } catch (SchedulerException e) {
          logger.error("Unable to update scheduled event {} with metadata {} because {}",
                  id, RestUtils.getJsonStringSilent(metadataList.toJSON()), getStackTrace(e));
          throw new IndexServiceException("Unable to update scheduled event " + id);
        }
        break;
      default:
        logger.error("Unkown event source!");
    }
    return metadataList;
  }

  /**
   * Processes the combined usernames and free text entries of the presenters (creator) field into a list of presenters
   * using the full names of the users if available and adds the usernames to a set of technical presenters.
   *
   * @param eventMetadata
   *          The metadata list that has the presenter (creator) field to pull the list of presenters from.
   * @return A {@link Tuple} with a list of friendly presenter names and a set of user names if available for the
   *         presenters.
   */
  protected Tuple<List<String>, Set<String>> getTechnicalPresenters(MetadataCollection eventMetadata) {
    MetadataField<?> presentersMetadataField = eventMetadata.getOutputFields()
            .get(DublinCore.PROPERTY_CREATOR.getLocalName());
    List<String> presenters = new ArrayList<>();
    Set<String> technicalPresenters = new HashSet<>();
    for (String presenter : MetadataUtils.getIterableStringMetadata(presentersMetadataField)) {
      User user = userDirectoryService.loadUser(presenter);
      if (user == null) {
        presenters.add(presenter);
      } else {
        String fullname = StringUtils.isNotBlank(user.getName()) ? user.getName() : user.getUsername();
        presenters.add(fullname);
        technicalPresenters.add(user.getUsername());
      }
    }
    return Tuple.tuple(presenters, technicalPresenters);
  }

  @Override
  public AccessControlList updateEventAcl(String id, AccessControlList acl, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, SearchIndexException, NotFoundException,
          UnauthorizedException {
    Opt<Event> optEvent = getEvent(id, index);
    if (optEvent.isNone())
      throw new NotFoundException("Cannot find an event with id " + id);

    Event event = optEvent.get();
    MediaPackage mediaPackage = getEventMediapackage(event);
    switch (getEventSource(event)) {
      case WORKFLOW:
        // Not updating the acl as the workflow might have already passed the point of distribution.
        throw new IllegalArgumentException("Unable to update the ACL of this event as it is currently processing.");
      case ARCHIVE:
        mediaPackage = authorizationService.setAcl(mediaPackage, AclScope.Episode, acl).getA();
        assetManager.takeSnapshot(mediaPackage);
        return acl;
      case SCHEDULE:
        mediaPackage = authorizationService.setAcl(mediaPackage, AclScope.Episode, acl).getA();
        try {
          schedulerService.updateEvent(id, Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
                  Opt.<Set<String>> none(), Opt.some(mediaPackage), Opt.<Map<String, String>> none(),
                  Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(), SchedulerService.ORIGIN);
        } catch (SchedulerException e) {
          throw new IndexServiceException("Unable to update the acl for the scheduled event", e);
        }
        return acl;
      default:
        logger.error("Unknown event source '{}' unable to update ACL!", getEventSource(event));
        throw new IndexServiceException(
                String.format("Unable to update the ACL as '{}' is an unknown event source.", getEventSource(event)));
    }
  }

  @Override
  public boolean hasSnapshots(String eventId) {
    AQueryBuilder q = assetManager.createQuery();
    return !enrich(q.select(q.snapshot()).where(q.mediaPackageId(eventId).and(q.version().isLatest())).run()).getSnapshots().isEmpty();
  }

  @Override
  public Map<String, Map<String, String>> getEventWorkflowProperties(final List<String> eventIds) {
    return WorkflowPropertiesUtil.getLatestWorkflowPropertiesForEvents(assetManager, eventIds);
  }

  @Override
  public SearchResult<Group> getGroups(String filter, Opt<Integer> optLimit, Opt<Integer> optOffset,
          Opt<String> optSort, AbstractSearchIndex index) throws SearchIndexException {
    GroupSearchQuery query = new GroupSearchQuery(securityService.getOrganization().getId(), securityService.getUser());

    // Parse the filters
    if (StringUtils.isNotBlank(filter)) {
      for (String f : filter.split(",")) {
        String[] filterTuple = f.split(":");
        if (filterTuple.length < 2) {
          logger.info("No value for filter {} in filters list: {}", filterTuple[0], filter);
          continue;
        }

        String name = filterTuple[0];
        String value = filterTuple[1];

        if (GroupsListQuery.FILTER_NAME_NAME.equals(name))
          query.withName(value);
      }
    }

    if (optSort.isSome()) {
      Set<SortCriterion> sortCriteria = RestUtils.parseSortQueryParameter(optSort.get());
      for (SortCriterion criterion : sortCriteria) {
        switch (criterion.getFieldName()) {
          case GroupIndexSchema.NAME:
            query.sortByName(criterion.getOrder());
            break;
          case GroupIndexSchema.DESCRIPTION:
            query.sortByDescription(criterion.getOrder());
            break;
          case GroupIndexSchema.ROLE:
            query.sortByRole(criterion.getOrder());
            break;
          case GroupIndexSchema.MEMBERS:
            query.sortByMembers(criterion.getOrder());
            break;
          case GroupIndexSchema.ROLES:
            query.sortByRoles(criterion.getOrder());
            break;
          default:
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
      }
    }

    if (optLimit.isSome())
      query.withLimit(optLimit.get());
    if (optOffset.isSome())
      query.withOffset(optOffset.get());

    return index.getByQuery(query);
  }

  @Override
  public Opt<Group> getGroup(String id, AbstractSearchIndex index) throws SearchIndexException {
    SearchResult<Group> result = index
            .getByQuery(new GroupSearchQuery(securityService.getOrganization().getId(), securityService.getUser())
                    .withIdentifier(id));

    // If the results list if empty, we return already a response.
    if (result.getPageSize() == 0) {
      logger.debug("Didn't find event with id {}", id);
      return Opt.none();
    }
    return Opt.some(result.getItems()[0].getSource());
  }

  @Override
  public Response removeGroup(String id) throws NotFoundException {
    return jpaGroupRoleProvider.removeGroup(id);
  }

  @Override
  public Response updateGroup(String id, String name, String description, String roles, String members)
          throws NotFoundException {
    return jpaGroupRoleProvider.updateGroup(id, name, description, roles, members);
  }

  @Override
  public Response createGroup(String name, String description, String roles, String members) {
    if (StringUtils.isEmpty(roles))
      roles = "";
    if (StringUtils.isEmpty(members))
      members = "";
    return jpaGroupRoleProvider.createGroup(name, description, roles, members);
  }

  @Override
  public Opt<Event> getEvent(String id, AbstractSearchIndex index) throws SearchIndexException {
    SearchResult<Event> result = index
            .getByQuery(new EventSearchQuery(securityService.getOrganization().getId(), securityService.getUser())
                    .withIdentifier(id));
    // If the results list if empty, we return already a response.
    if (result.getPageSize() == 0) {
      logger.debug("Didn't find event with id {}", id);
      return Opt.none();
    }
    return Opt.some(result.getItems()[0].getSource());
  }

  @Override
  public boolean removeEvent(String id) throws NotFoundException, UnauthorizedException {
    boolean unauthorizedScheduler = false;
    boolean notFoundScheduler = false;
    boolean removedScheduler = true;
    try {
      schedulerService.removeEvent(id);
    } catch (NotFoundException e) {
      notFoundScheduler = true;
    } catch (UnauthorizedException e) {
      unauthorizedScheduler = true;
    } catch (SchedulerException e) {
      removedScheduler = false;
      logger.error("Unable to remove the event '{}' from scheduler service: {}", id, getStackTrace(e));
    }

    boolean unauthorizedWorkflow = false;
    boolean notFoundWorkflow = false;
    boolean removedWorkflow = true;
    try {
      WorkflowQuery workflowQuery = new WorkflowQuery().withMediaPackage(id);
      WorkflowSet workflowSet = workflowService.getWorkflowInstances(workflowQuery);
      if (workflowSet.size() == 0)
        notFoundWorkflow = true;
      for (WorkflowInstance instance : workflowSet.getItems()) {
        workflowService.stop(instance.getId());
        workflowService.remove(instance.getId());
      }
    } catch (NotFoundException e) {
      notFoundWorkflow = true;
    } catch (UnauthorizedException e) {
      unauthorizedWorkflow = true;
    } catch (WorkflowDatabaseException e) {
      removedWorkflow = false;
      logger.error("Unable to remove the event '{}' because removing workflow failed: {}", id, getStackTrace(e));
    } catch (WorkflowException e) {
      removedWorkflow = false;
      logger.error("Unable to remove the event '{}' because removing workflow failed: {}", id, getStackTrace(e));
    }

    boolean unauthorizedArchive = false;
    boolean notFoundArchive = false;
    boolean removedArchive = true;
    try {
      final AQueryBuilder q = assetManager.createQuery();
      final Predicate p = q.organizationId().eq(securityService.getOrganization().getId()).and(q.mediaPackageId(id));
      final AResult r = q.select(q.nothing()).where(p).run();
      if (r.getSize() > 0)
        q.delete(DEFAULT_OWNER, q.snapshot()).where(p).run();
    } catch (AssetManagerException e) {
      if (e.getCause() instanceof UnauthorizedException) {
        unauthorizedArchive = true;
      } else if (e.getCause() instanceof NotFoundException) {
        notFoundArchive = true;
      } else {
        removedArchive = false;
        logger.error("Unable to remove the event '{}' from the archive: {}", id, getStackTrace(e));
      }
    }

    if (notFoundScheduler && notFoundWorkflow && notFoundArchive)
      throw new NotFoundException("Event id " + id + " not found.");

    if (unauthorizedScheduler || unauthorizedWorkflow || unauthorizedArchive)
      throw new UnauthorizedException("Not authorized to remove event id " + id);

    try {
      eventCommentService.deleteComments(id);
    } catch (EventCommentException e) {
      logger.error("Unable to remove comments for event '{}': {}", id, getStackTrace(e));
    }

    return removedScheduler && removedWorkflow && removedArchive;
  }

  @Override
  public void updateWorkflowInstance(WorkflowInstance workflowInstance)
          throws WorkflowException, UnauthorizedException {
    // Only update the workflow if the instance is in a working state
    if (WorkflowInstance.WorkflowState.FAILED.equals(workflowInstance.getState())
            || WorkflowInstance.WorkflowState.FAILING.equals(workflowInstance.getState())
            || WorkflowInstance.WorkflowState.STOPPED.equals(workflowInstance.getState())
            || WorkflowInstance.WorkflowState.SUCCEEDED.equals(workflowInstance.getState())) {
      logger.info("Skip updating {} workflow mediapackage {} with updated comments catalog",
              workflowInstance.getState(), workflowInstance.getMediaPackage().getIdentifier().toString());
      return;
    }
    workflowService.update(workflowInstance);
  }

  @Override
  public MediaPackage getEventMediapackage(Event event) throws IndexServiceException {
    switch (getEventSource(event)) {
      case WORKFLOW:
        Opt<WorkflowInstance> currentWorkflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
        if (currentWorkflowInstance.isNone()) {
          logger.error("No workflow instance for event {} found!", event.getIdentifier());
          throw new IndexServiceException("No workflow instance found for event " + event.getIdentifier());
        }
        return currentWorkflowInstance.get().getMediaPackage();
      case ARCHIVE:
        final AQueryBuilder q = assetManager.createQuery();
        final AResult r = q.select(q.snapshot())
                .where(q.mediaPackageId(event.getIdentifier()).and(q.version().isLatest())).run();
        if (r.getSize() > 0) {
          logger.debug("Found event in archive with id {}", event.getIdentifier());
          return enrich(r).getSnapshots().head2().getMediaPackage();
        }
        logger.error("No event with id {} found from archive!", event.getIdentifier());
        throw new IndexServiceException("No archived event found with id " + event.getIdentifier());
      case SCHEDULE:
        try {
          MediaPackage mediaPackage = schedulerService.getMediaPackage(event.getIdentifier());
          logger.debug("Found event in scheduler with id {}", event.getIdentifier());
          return mediaPackage;
        } catch (NotFoundException e) {
          logger.error("No scheduled event with id {} found!", event.getIdentifier());
          throw new IndexServiceException(e.getMessage(), e);
        } catch (UnauthorizedException e) {
          logger.error("Unauthorized to get event with id {} from scheduler because {}", event.getIdentifier(),
                  getStackTrace(e));
          throw new IndexServiceException(e.getMessage(), e);
        } catch (SchedulerException e) {
          logger.error("Unable to get event with id {} from scheduler because {}", event.getIdentifier(),
                  getStackTrace(e));
          throw new IndexServiceException(e.getMessage(), e);
        }
      default:
        throw new IllegalStateException("Unknown event type!");
    }
  }

  /**
   * Determines in a very basic way what kind of source the event is
   *
   * @param event
   *          the event
   * @return the source type
   */
  @Override
  public Source getEventSource(Event event) {
    if (event.getWorkflowId() != null && isWorkflowActive(event.getWorkflowState()))
      return Source.WORKFLOW;

    if (event.getSchedulingStatus() != null && !event.hasRecordingStarted())
      return Source.SCHEDULE;

    if (event.getArchiveVersion() != null)
      return Source.ARCHIVE;

    if (event.getWorkflowId() != null)
      return Source.WORKFLOW;

    return Source.SCHEDULE;
  }

  @Override
  public Opt<WorkflowInstance> getCurrentWorkflowInstance(String mpId) throws IndexServiceException {
    WorkflowQuery query = new WorkflowQuery().withMediaPackage(mpId);
    WorkflowSet workflowInstances;
    try {
      workflowInstances = workflowService.getWorkflowInstances(query);
      if (workflowInstances.size() == 0) {
        logger.info("No workflow instance found for mediapackage {}.", mpId);
        return Opt.none();
      }
    } catch (WorkflowDatabaseException e) {
      logger.error("Unable to get workflows for event {} because {}", mpId, getStackTrace(e));
      throw new IndexServiceException("Unable to get current workflow for event " + mpId);
    }
    // Get the newest workflow instance
    // TODO This presuppose knowledge of the Database implementation and should be fixed sooner or later!
    WorkflowInstance workflowInstance = workflowInstances.getItems()[0];
    for (WorkflowInstance instance : workflowInstances.getItems()) {
      if (instance.getId() > workflowInstance.getId())
        workflowInstance = instance;
    }
    return Opt.some(workflowInstance);
  }

  private void updateMediaPackageMetadata(MediaPackage mp, MetadataList metadataList) {
    String oldSeriesId = mp.getSeries();
    for (EventCatalogUIAdapter catalogUIAdapter : getEventCatalogUIAdapters()) {
      Opt<MetadataCollection> metadata = metadataList.getMetadataByAdapter(catalogUIAdapter);
      if (metadata.isSome() && metadata.get().isUpdated()) {
        catalogUIAdapter.storeFields(mp, metadata.get());
      }
    }

    // update series catalogs
    if (!StringUtils.equals(oldSeriesId, mp.getSeries())) {
      List<String> seriesDcTags = new ArrayList<>();
      List<String> seriesAclTags = new ArrayList<>();
      Map<String, List<String>> seriesExtDcTags = new HashMap<>();
      if (StringUtils.isNotBlank(oldSeriesId)) {
        // remove series dublincore from the media package
        for (MediaPackageElement mpe : mp.getElementsByFlavor(MediaPackageElements.SERIES)) {
          mp.remove(mpe);
          for (String tag : mpe.getTags()) {
            seriesDcTags.add(tag);
          }
        }
        // remove series ACL from the media package
        for (MediaPackageElement mpe : mp.getElementsByFlavor(MediaPackageElements.XACML_POLICY_SERIES)) {
          mp.remove(mpe);
          for (String tag : mpe.getTags()) {
            seriesAclTags.add(tag);
          }
        }
        // remove series extended metadata from the media package
        try {
          Opt<Map<String, byte[]>> oldSeriesElementsOpt = seriesService.getSeriesElements(oldSeriesId);
          for (Map<String, byte[]> oldSeriesElements : oldSeriesElementsOpt) {
            for (String oldSeriesElementType : oldSeriesElements.keySet()) {
              for (MediaPackageElement mpe : mp
                      .getElementsByFlavor(MediaPackageElementFlavor.flavor(oldSeriesElementType, "series"))) {
                mp.remove(mpe);
                String elementType = mpe.getFlavor().getType();
                if (StringUtils.isNotBlank(elementType)) {
                  // remember the tags for this type of element
                  if (!seriesExtDcTags.containsKey(elementType)) {
                    // initialize the tags list on the first occurrence of this element type
                    seriesExtDcTags.put(elementType, new ArrayList<>());
                  }
                  for (String tag : mpe.getTags()) {
                    seriesExtDcTags.get(elementType).add(tag);
                  }
                }
              }
            }
          }
        } catch (SeriesException e) {
          logger.info("Unable to retrieve series element types from series service for the series {}", oldSeriesId, e);
        }
      }

      if (StringUtils.isNotBlank(mp.getSeries())) {
        // add updated series dublincore to the media package
        try {
          DublinCoreCatalog seriesDC = seriesService.getSeries(mp.getSeries());
          if (seriesDC != null) {
            mp.setSeriesTitle(seriesDC.getFirst(DublinCore.PROPERTY_TITLE));
            try (InputStream in = IOUtils.toInputStream(seriesDC.toXmlString(), "UTF-8")) {
              String elementId = UUID.randomUUID().toString();
              URI catalogUrl = workspace.put(mp.getIdentifier().compact(), elementId, "dublincore.xml", in);
              MediaPackageElement mpe = mp.add(catalogUrl, MediaPackageElement.Type.Catalog, MediaPackageElements.SERIES);
              mpe.setIdentifier(elementId);
              mpe.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, workspace.read(catalogUrl)));
              if (StringUtils.isNotBlank(oldSeriesId)) {
                for (String tag : seriesDcTags) {
                  mpe.addTag(tag);
                }
              } else {
                // add archive tag to the element if the media package had no series set before
                mpe.addTag("archive");
              }
            } catch (IOException e) {
              throw new IllegalStateException("Unable to add the series dublincore to the media package " + mp.getIdentifier(), e);
            }
          }
        } catch (SeriesException e) {
          throw new IllegalStateException("Unable to retrieve series dublincore catalog for the series " + mp.getSeries(), e);
        } catch (NotFoundException | UnauthorizedException e) {
          throw new IllegalArgumentException("Unable to retrieve series dublincore catalog for the series " + mp.getSeries(), e);
        }
        // add updated series ACL to the media package
        try {
          AccessControlList seriesAccessControl = seriesService.getSeriesAccessControl(mp.getSeries());
          if (seriesAccessControl != null) {
            mp = authorizationService.setAcl(mp, AclScope.Series, seriesAccessControl).getA();
            for (MediaPackageElement seriesAclMpe : mp.getElementsByFlavor(MediaPackageElements.XACML_POLICY_SERIES)) {
              if (StringUtils.isNotBlank(oldSeriesId)) {
                for (String tag : seriesAclTags) {
                  seriesAclMpe.addTag(tag);
                }
              } else {
                // add archive tag to the element if the media package had no series set before
                seriesAclMpe.addTag("archive");
              }
            }
          }
        } catch (SeriesException e) {
          throw new IllegalStateException("Unable to retrieve series ACL for series " + oldSeriesId, e);
        } catch (NotFoundException e) {
          logger.debug("There is no ACL set for the series {}", mp.getSeries());
        }
        // add updated series extended metadata to the media package
        try {
          Opt<Map<String, byte[]>> seriesElementsOpt = seriesService.getSeriesElements(mp.getSeries());
          for (Map<String, byte[]> seriesElements : seriesElementsOpt) {
            for (String seriesElementType : seriesElements.keySet()) {
              try (InputStream in = new ByteArrayInputStream(seriesElements.get(seriesElementType))) {
                String elementId = UUID.randomUUID().toString();
                URI catalogUrl = workspace.put(mp.getIdentifier().compact(), elementId, "dublincore.xml", in);
                MediaPackageElement mpe = mp.add(catalogUrl, MediaPackageElement.Type.Catalog,
                        MediaPackageElementFlavor.flavor(seriesElementType, "series"));
                mpe.setIdentifier(elementId);
                mpe.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, workspace.read(catalogUrl)));
                if (StringUtils.isNotBlank(oldSeriesId)) {
                  if (seriesExtDcTags.containsKey(seriesElementType)) {
                    for (String tag : seriesExtDcTags.get(seriesElementType)) {
                      mpe.addTag(tag);
                    }
                  }
                } else {
                  // add archive tag to the element if the media package had no series set before
                  mpe.addTag("archive");
                }
              } catch (IOException e) {
                throw new IllegalStateException(String.format("Unable to serialize series element %s for the series %s",
                        seriesElementType, mp.getSeries()), e);
              } catch (NotFoundException e) {
                throw new IllegalArgumentException("Unable to retrieve series element dublincore catalog for the series "
                        + mp.getSeries(), e);
              }
            }
          }
        } catch (SeriesException e) {
          throw new IllegalStateException("Unable to retrieve series elements for the series " + mp.getSeries(), e);
        }
      }
    }
  }

  @Override
  public String createSeries(MetadataList metadataList, Map<String, String> options, Opt<AccessControlList> optAcl,
          Opt<Long> optThemeId) throws IndexServiceException {
    DublinCoreCatalog dc = DublinCores.mkOpencastSeries().getCatalog();
    dc.set(PROPERTY_IDENTIFIER, UUID.randomUUID().toString());
    dc.set(DublinCore.PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(new Date(), Precision.Second));
    for (Entry<String, String> entry : options.entrySet()) {
      dc.set(new EName(DublinCores.OC_PROPERTY_NS_URI, entry.getKey()), entry.getValue());
    }

    Opt<MetadataCollection> seriesMetadata = metadataList.getMetadataByFlavor(MediaPackageElements.SERIES.toString());
    if (seriesMetadata.isSome()) {
      DublinCoreMetadataUtil.updateDublincoreCatalog(dc, seriesMetadata.get());
    }

    AccessControlList acl;
    if (optAcl.isSome()) {
      acl = optAcl.get();
    } else {
      acl = new AccessControlList();
    }

    String seriesId;
    try {
      DublinCoreCatalog createdSeries = seriesService.updateSeries(dc);
      seriesId = createdSeries.getFirst(PROPERTY_IDENTIFIER);
      seriesService.updateAccessControl(seriesId, acl);
      for (Long id : optThemeId)
        seriesService.updateSeriesProperty(seriesId, THEME_PROPERTY_NAME, Long.toString(id));
    } catch (Exception e) {
      logger.error("Unable to create new series: {}", getStackTrace(e));
      throw new IndexServiceException("Unable to create new series");
    }

    updateSeriesMetadata(seriesId, metadataList);

    return seriesId;
  }

  @Override
  public String createSeries(String metadata)
          throws IllegalArgumentException, IndexServiceException, UnauthorizedException {
    JSONObject metadataJson = null;
    try {
      metadataJson = (JSONObject) new JSONParser().parse(metadata);
    } catch (Exception e) {
      logger.warn("Unable to parse metadata {}", metadata);
      throw new IllegalArgumentException("Unable to parse metadata" + metadata);
    }

    if (metadataJson == null)
      throw new IllegalArgumentException("No metadata set to create series");

    JSONArray seriesMetadataJson = (JSONArray) metadataJson.get("metadata");
    if (seriesMetadataJson == null)
      throw new IllegalArgumentException("No metadata field in metadata");

    JSONObject options = (JSONObject) metadataJson.get("options");
    if (options == null)
      throw new IllegalArgumentException("No options field in metadata");

    Opt<Long> themeId = Opt.none();
    Long theme = (Long) metadataJson.get("theme");
    if (theme != null) {
      themeId = Opt.some(theme);
    }

    Map<String, String> optionsMap;
    try {
      optionsMap = JSONUtils.toMap(new org.codehaus.jettison.json.JSONObject(options.toJSONString()));
    } catch (JSONException e) {
      logger.warn("Unable to parse options to map: {}", getStackTrace(e));
      throw new IllegalArgumentException("Unable to parse options to map");
    }

    DublinCoreCatalog dc = DublinCores.mkOpencastSeries().getCatalog();
    dc.set(PROPERTY_IDENTIFIER, UUID.randomUUID().toString());
    dc.set(DublinCore.PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(new Date(), Precision.Second));
    for (Entry<String, String> entry : optionsMap.entrySet()) {
      dc.set(new EName(DublinCores.OC_PROPERTY_NS_URI, entry.getKey()), entry.getValue());
    }

    MetadataList metadataList;
    try {
      metadataList = getMetadataListWithAllSeriesCatalogUIAdapters();
      metadataList.fromJSON(seriesMetadataJson.toJSONString());
    } catch (Exception e) {
      logger.warn("Not able to parse the series metadata {}: {}", seriesMetadataJson, getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the series metadata");
    }

    Opt<MetadataCollection> seriesMetadata = metadataList.getMetadataByFlavor(MediaPackageElements.SERIES.toString());
    if (seriesMetadata.isSome()) {
      DublinCoreMetadataUtil.updateDublincoreCatalog(dc, seriesMetadata.get());
    }

    AccessControlList acl = getAccessControlList(metadataJson);

    String seriesId;
    try {
      DublinCoreCatalog createdSeries = seriesService.updateSeries(dc);
      seriesId = createdSeries.getFirst(PROPERTY_IDENTIFIER);
      seriesService.updateAccessControl(seriesId, acl);
      for (Long id : themeId)
        seriesService.updateSeriesProperty(seriesId, THEME_PROPERTY_NAME, Long.toString(id));
    } catch (Exception e) {
      logger.error("Unable to create new series: {}", getStackTrace(e));
      throw new IndexServiceException("Unable to create new series");
    }

    updateSeriesMetadata(seriesId, metadataList);

    return seriesId;
  }

  @Override
  public Opt<Series> getSeries(String seriesId, AbstractSearchIndex searchIndex) throws SearchIndexException {
    SearchResult<Series> result = searchIndex
            .getByQuery(new SeriesSearchQuery(securityService.getOrganization().getId(), securityService.getUser())
                    .withIdentifier(seriesId));
    // If the results list if empty, we return already a response.
    if (result.getPageSize() == 0) {
      logger.debug("Didn't find series with id {}", seriesId);
      return Opt.none();
    }
    return Opt.some(result.getItems()[0].getSource());
  }

  @Override
  public void removeSeries(String id) throws NotFoundException, SeriesException, UnauthorizedException {
    seriesService.deleteSeries(id);
  }

  @Override
  public MetadataList updateCommonSeriesMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, NotFoundException, UnauthorizedException {
    MetadataList metadataList = getMetadataListWithCommonSeriesCatalogUIAdapters();
    return updateSeriesMetadata(id, metadataJSON, index, metadataList);
  }

  @Override
  public MetadataList updateAllSeriesMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, IndexServiceException, NotFoundException, UnauthorizedException {
    MetadataList metadataList = getMetadataListWithAllSeriesCatalogUIAdapters();
    return updateSeriesMetadata(id, metadataJSON, index, metadataList);
  }

  @Override
  public MetadataList updateAllSeriesMetadata(String id, MetadataList metadataList, AbstractSearchIndex index)
          throws IndexServiceException, NotFoundException, UnauthorizedException {
    checkSeriesExists(id, index);
    updateSeriesMetadata(id, metadataList);
    return metadataList;
  }

  @Override
  public void updateCommentCatalog(final Event event, final List<EventComment> comments) throws Exception {
    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        securityContext.runInContext(new Effect0() {
          @Override
          protected void run() {
            try {
              MediaPackage mediaPackage = getEventMediapackage(event);
              updateMediaPackageCommentCatalog(mediaPackage, comments);
              switch (getEventSource(event)) {
                case WORKFLOW:
                  logger.info("Update workflow mediapacakge {} with updated comments catalog.", event.getIdentifier());
                  Opt<WorkflowInstance> workflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
                  if (workflowInstance.isNone()) {
                    logger.error("No workflow instance for event {} found!", event.getIdentifier());
                    throw new IndexServiceException("No workflow instance found for event " + event.getIdentifier());
                  }
                  WorkflowInstance instance = workflowInstance.get();
                  instance.setMediaPackage(mediaPackage);
                  updateWorkflowInstance(instance);
                  break;
                case ARCHIVE:
                  logger.info("Update archive mediapacakge {} with updated comments catalog.", event.getIdentifier());
                  assetManager.takeSnapshot(mediaPackage);
                  break;
                case SCHEDULE:
                  logger.info("Update scheduled mediapacakge {} with updated comments catalog.", event.getIdentifier());
                  schedulerService.updateEvent(event.getIdentifier(), Opt.<Date> none(), Opt.<Date> none(),
                          Opt.<String> none(), Opt.<Set<String>> none(), Opt.some(mediaPackage),
                          Opt.<Map<String, String>> none(), Opt.<Map<String, String>> none(), Opt.<Opt<Boolean>> none(),
                          SchedulerService.ORIGIN);
                  break;
                default:
                  logger.error("Unkown event source {}!", event.getSource().toString());
              }
            } catch (Exception e) {
              logger.error("Unable to update event {} comment catalog: {}", event.getIdentifier(), getStackTrace(e));
            }
          }
        });
      }
    });
  }

  private void updateMediaPackageCommentCatalog(MediaPackage mediaPackage, List<EventComment> comments)
          throws EventCommentException, IOException {
    // Get the comments catalog
    Catalog[] commentCatalogs = mediaPackage.getCatalogs(MediaPackageElements.COMMENTS);
    Catalog c = null;
    if (commentCatalogs.length == 1)
      c = commentCatalogs[0];

    if (comments.size() > 0) {
      // If no comments catalog found, create a new one
      if (c == null) {
        c = (Catalog) MediaPackageElementBuilderFactory.newInstance().newElementBuilder().newElement(Type.Catalog,
                MediaPackageElements.COMMENTS);
        c.setIdentifier(UUID.randomUUID().toString());
        mediaPackage.add(c);
      }

      // Update comments catalog
      InputStream in = null;
      try {
        String commentCatalog = EventCommentParser.getAsXml(comments);
        in = IOUtils.toInputStream(commentCatalog, "UTF-8");
        URI uri = workspace.put(mediaPackage.getIdentifier().toString(), c.getIdentifier(), "comments.xml", in);
        c.setURI(uri);
        // setting the URI to a new source so the checksum will most like be invalid
        c.setChecksum(null);
      } finally {
        IOUtils.closeQuietly(in);
      }
    } else {
      // Remove comments catalog
      if (c != null) {
        mediaPackage.remove(c);
        try {
          workspace.delete(c.getURI());
        } catch (NotFoundException e) {
          logger.warn("Comments catalog {} not found to delete!", c.getURI());
        }
      }
    }
  }

  @Override
  public void changeOptOutStatus(String eventId, boolean optout, AbstractSearchIndex index)
          throws NotFoundException, SchedulerException, SearchIndexException, UnauthorizedException {
    Opt<Event> optEvent = getEvent(eventId, index);
    if (optEvent.isNone())
      throw new NotFoundException("Cannot find an event with id " + eventId);

    schedulerService.updateEvent(eventId, Opt.<Date> none(), Opt.<Date> none(), Opt.<String> none(),
            Opt.<Set<String>> none(), Opt.<MediaPackage> none(), Opt.<Map<String, String>> none(),
            Opt.<Map<String, String>> none(), Opt.some(Opt.some(optout)), SchedulerService.ORIGIN);
    logger.debug("Setting event {} to opt out status of {}", eventId, optout);
  }

  /**
   * Checks to see if a given series exists.
   *
   * @param seriesID
   *          The id of the series.
   * @param index
   *          The index to check for the particular series.
   * @throws NotFoundException
   *           Thrown if unable to find the series.
   * @throws IndexServiceException
   *           Thrown if unable to access the index to get the series.
   */
  private void checkSeriesExists(String seriesID, AbstractSearchIndex index)
          throws NotFoundException, IndexServiceException {
    try {
      Opt<Series> optSeries = getSeries(seriesID, index);
      if (optSeries.isNone())
        throw new NotFoundException("Cannot find a series with id " + seriesID);
    } catch (SearchIndexException e) {
      logger.error("Unable to get a series with id {} because: {}", seriesID, getStackTrace(e));
      throw new IndexServiceException("Cannot use search service to find Series");
    }
  }

  private MetadataList updateSeriesMetadata(String seriesID, String metadataJSON, AbstractSearchIndex index,
          MetadataList metadataList)
                  throws IllegalArgumentException, IndexServiceException, NotFoundException, UnauthorizedException {
    checkSeriesExists(seriesID, index);
    try {
      metadataList.fromJSON(metadataJSON);
    } catch (Exception e) {
      logger.warn("Not able to parse the event metadata {}: {}", metadataJSON, getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the event metadata");
    }

    updateSeriesMetadata(seriesID, metadataList);
    return metadataList;
  }

  /**
   * @return A {@link MetadataList} with only the common SeriesCatalogUIAdapter's empty {@link MetadataCollection}
   *         available
   */
  private MetadataList getMetadataListWithCommonSeriesCatalogUIAdapters() {
    MetadataList metadataList = new MetadataList();
    metadataList.add(seriesCatalogUIAdapter.getFlavor(), seriesCatalogUIAdapter.getUITitle(),
            seriesCatalogUIAdapter.getRawFields());
    return metadataList;
  }

  /**
   * @return A {@link MetadataList} with all of the available CatalogUIAdapters empty {@link MetadataCollection}
   *         available
   */
  @Override
  public MetadataList getMetadataListWithAllSeriesCatalogUIAdapters() {
    MetadataList metadataList = new MetadataList();
    for (SeriesCatalogUIAdapter adapter : getSeriesCatalogUIAdapters()) {
      metadataList.add(adapter.getFlavor(), adapter.getUITitle(), adapter.getRawFields());
    }
    return metadataList;
  }

  private MetadataList getMetadataListWithCommonEventCatalogUIAdapter() {
    MetadataList metadataList = new MetadataList();
    metadataList.add(eventCatalogUIAdapter, eventCatalogUIAdapter.getRawFields());
    return metadataList;
  }

  @Override
  public MetadataList getMetadataListWithAllEventCatalogUIAdapters() {
    MetadataList metadataList = new MetadataList();
    for (EventCatalogUIAdapter catalogUIAdapter : getEventCatalogUIAdapters()) {
      metadataList.add(catalogUIAdapter, catalogUIAdapter.getRawFields());
    }
    return metadataList;
  }

  /**
   * Checks the list of metadata for updated fields and stores/updates them in the respective metadata catalog.
   *
   * @param seriesId
   *          The series identifier
   * @param metadataList
   *          The metadata list
   */
  private void updateSeriesMetadata(String seriesId, MetadataList metadataList) {
    for (SeriesCatalogUIAdapter adapter : seriesCatalogUIAdapters) {
      Opt<MetadataCollection> metadata = metadataList.getMetadataByFlavor(adapter.getFlavor());
      if (metadata.isSome() && metadata.get().isUpdated()) {
        adapter.storeFields(seriesId, metadata.get());
      }
    }
  }

  public boolean isWorkflowActive(String workflowState) {
    return WorkflowState.INSTANTIATED.toString().equals(workflowState)
            || WorkflowState.RUNNING.toString().equals(workflowState)
            || WorkflowState.PAUSED.toString().equals(workflowState);
  }

  @Override
  public boolean hasActiveTransaction(String eventId)
          throws NotFoundException, UnauthorizedException, IndexServiceException {
    try {
      return schedulerService.hasActiveTransaction(eventId);
    } catch (SchedulerException e) {
      logger.error("Unable to get active transaction for scheduled event {} because {}", eventId, getStackTrace(e));
      throw new IndexServiceException("Unable to get active transaction for scheduled event " + eventId);
    } catch (NotFoundException e) {
      logger.trace("The event was not found by the scheduler so it can't be in an active transaction.");
      return false;
    }
  }

}
