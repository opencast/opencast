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
package org.opencastproject.index.service.impl;

import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;

import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.archive.opencast.OpencastQueryBuilder;
import org.opencastproject.archive.opencast.OpencastResultSet;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.AbstractMetadataCollection;
import org.opencastproject.index.service.catalog.adapter.DublinCoreMetadataUtil;
import org.opencastproject.index.service.catalog.adapter.MetadataField;
import org.opencastproject.index.service.catalog.adapter.MetadataList;
import org.opencastproject.index.service.catalog.adapter.MetadataUtils;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.events.EventCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.series.CommonSeriesCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.series.SeriesCatalogUIAdapter;
import org.opencastproject.index.service.exception.InternalServerErrorException;
import org.opencastproject.index.service.exception.MetadataParsingException;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesSearchQuery;
import org.opencastproject.index.service.util.JSONUtils;
import org.opencastproject.ingest.api.IngestException;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCoreValue;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EncodingSchemeUtils;
import org.opencastproject.metadata.dublincore.Precision;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.userdirectory.UserIdRoleProvider;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.XmlNamespaceBinding;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
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

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jettison.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

public class IndexServiceImpl implements IndexService {

  private static final String WORKFLOW_CONFIG_PREFIX = "org.opencastproject.workflow.config.";

  public static final String THEME_PROPERTY_NAME = "theme";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);
  /** A parser for handling JSON documents inside the body of a request. **/
  private static final JSONParser parser = new JSONParser();

  private AclServiceFactory aclServiceFactory;
  private AuthorizationService authorizationService;
  private CaptureAgentStateService captureAgentStateService;
  private CommonEventCatalogUIAdapter eventCatalogUIAdapter;
  private final List<EventCatalogUIAdapter> eventCatalogUIAdapters = new ArrayList<EventCatalogUIAdapter>();
  private HttpMediaPackageElementProvider httpMediaPackageElementProvider;
  private IngestService ingestService;
  private OpencastArchive opencastArchive;
  private SchedulerService schedulerService;
  private SecurityService securityService;
  private final List<SeriesCatalogUIAdapter> seriesCatalogUIAdapters = new ArrayList<SeriesCatalogUIAdapter>();
  private SeriesCatalogUIAdapter commonSeriesCatalogUIAdapter;
  private SeriesService seriesService;
  private WorkflowService workflowService;
  private Workspace workspace;

  public AclService getAclService() {
    return aclServiceFactory.serviceFor(securityService.getOrganization());
  }

  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  public AuthorizationService getAuthorizationService() {
    return authorizationService;
  }

  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  public CaptureAgentStateService getCaptureAgentStateService() {
    return captureAgentStateService;
  }

  public void setCaptureAgentStateService(CaptureAgentStateService captureAgentStateService) {
    this.captureAgentStateService = captureAgentStateService;
  }

  /** OSGi DI. */
  public void setCommonEventCatalogUIAdapter(CommonEventCatalogUIAdapter eventCatalogUIAdapter) {
    this.eventCatalogUIAdapter = eventCatalogUIAdapter;
  }

  public EventCatalogUIAdapter getEpisodeCatalogUIAdapter() {
    return eventCatalogUIAdapter;
  }

  /** OSGi DI. */
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi DI. */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.remove(catalogUIAdapter);
  }

  public List<EventCatalogUIAdapter> getEventCatalogUIAdapters(String organization) {
    return Stream.$(eventCatalogUIAdapters).filter(eventOrganizationFilter._2(organization)).toList();
  }

  private static final Fn2<EventCatalogUIAdapter, String, Boolean> eventOrganizationFilter = new Fn2<EventCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean ap(EventCatalogUIAdapter catalogUIAdapter, String organization) {
      return organization.equals(catalogUIAdapter.getOrganization());
    }
  };

  public HttpMediaPackageElementProvider getHttpMediaPackageElementProvider() {
    return httpMediaPackageElementProvider;
  }

  public void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider httpMediaPackageElementProvider) {
    this.httpMediaPackageElementProvider = httpMediaPackageElementProvider;
  }

  public IngestService getIngestService() {
    return ingestService;
  }

  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  public OpencastArchive getOpencastArchive() {
    return opencastArchive;
  }

  public void setOpencastArchive(OpencastArchive opencastArchive) {
    this.opencastArchive = opencastArchive;
  }

  public SchedulerService getSchedulerService() {
    return schedulerService;
  }

  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback to add the series dublincore {@link SeriesCatalogUIAdapter} instance. */
  public void setCommonSeriesCatalogUIAdapter(CommonSeriesCatalogUIAdapter commonSeriesCatalogUIAdapter) {
    this.commonSeriesCatalogUIAdapter = commonSeriesCatalogUIAdapter;
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
   * @param organization
   *          The organization to filter the results with.
   * @return A {@link List} of {@link SeriesCatalogUIAdapter} that provide the metadata to the front end.
   */
  public List<SeriesCatalogUIAdapter> getSeriesCatalogUIAdapters(String organization) {
    return Stream.$(seriesCatalogUIAdapters).filter(seriesOrganizationFilter._2(organization)).toList();
  }

  private static final Fn2<SeriesCatalogUIAdapter, String, Boolean> seriesOrganizationFilter = new Fn2<SeriesCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean ap(SeriesCatalogUIAdapter catalogUIAdapter, String organization) {
      return catalogUIAdapter.getOrganization().equals(organization);
    }
  };

  public SeriesService getSeriesService() {
    return seriesService;
  }

  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  public WorkflowService getWorkflowService() {
    return workflowService;
  }

  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public String createEvent(HttpServletRequest request) throws InternalServerErrorException, IllegalArgumentException {
    JSONObject metadataJson = null;
    MediaPackage mp = null;
    try {
      if (ServletFileUpload.isMultipartContent(request)) {
        mp = getIngestService().createMediaPackage();

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
            if ("presenter".equals(item.getFieldName())) {
              mp = getIngestService().addTrack(item.openStream(), item.getName(),
                      MediaPackageElements.PRESENTER_SOURCE, mp);
            } else if ("presentation".equals(item.getFieldName())) {
              mp = getIngestService().addTrack(item.openStream(), item.getName(),
                      MediaPackageElements.PRESENTATION_SOURCE, mp);
            } else if ("audio".equals(item.getFieldName())) {
              mp = getIngestService().addTrack(item.openStream(), item.getName(),
                      new MediaPackageElementFlavor("presenter-audio", "source"), mp);
            } else {
              logger.warn("Unknown field name found {}", item.getFieldName());
            }
          }
        }
      } else {
        throw new IllegalArgumentException("No multipart content");
      }

      return createEvent(metadataJson, mp);
    } catch (Exception e) {
      logger.error("Unable to create event: {}", ExceptionUtils.getStackTrace(e));
      throw new InternalServerErrorException(e.getMessage());
    }
  }

  protected String createEvent(JSONObject metadataJson, MediaPackage mp) throws ParseException, IOException,
          MediaPackageException, IngestException, NotFoundException, SchedulerException, UnauthorizedException {
    if (metadataJson == null)
      throw new IllegalArgumentException("No metadata set");

    JSONObject source = (JSONObject) metadataJson.get("source");
    if (source == null)
      throw new IllegalArgumentException("No source field in metadata");

    JSONObject processing = (JSONObject) metadataJson.get("processing");
    if (processing == null)
      throw new IllegalArgumentException("No processing field in metadata");

    String workflowTemplate = (String) processing.get("workflow");
    if (workflowTemplate == null)
      throw new IllegalArgumentException("No workflow template in metadata");

    JSONArray allEventMetadataJson = (JSONArray) metadataJson.get("metadata");
    if (allEventMetadataJson == null)
      throw new IllegalArgumentException("No metadata field in metadata");

    SourceType type;
    try {
      type = SourceType.valueOf((String) source.get("type"));
    } catch (Exception e) {
      logger.error("Unknown source type '{}'", source.get("type"));
      throw new IllegalArgumentException("Unkown source type");
    }

    DublinCoreCatalog dc;
    InputStream inputStream = null;
    Properties caProperties = new Properties();
    try {
      MetadataList metadataList = getMetadatListWithAllEventCatalogUIAdapters();
      metadataList.fromJSON(allEventMetadataJson.toJSONString());
      AbstractMetadataCollection eventMetadata = metadataList.getMetadataByAdapter(eventCatalogUIAdapter).get();

      JSONObject sourceMetadata = (JSONObject) source.get("metadata");
      if (sourceMetadata != null
              && (type.equals(SourceType.SCHEDULE_SINGLE) || type.equals(SourceType.SCHEDULE_MULTIPLE))) {
        try {
          MetadataField<?> current = eventMetadata.getOutputFields().get("agent");
          eventMetadata.updateStringField(current, (String) sourceMetadata.get("device"));
        } catch (Exception e) {
          logger.warn("Unable to parse device {}", sourceMetadata.get("device"));
          throw new IllegalArgumentException("Unable to parse device");
        }
      }

      MetadataField<?> created = eventMetadata.getOutputFields().get("created");
      if (created == null || !created.isUpdated() || created.getValue().isNone()) {
        eventMetadata.removeField(created);
        MetadataField<String> newCreated = MetadataUtils.copyMetadataField(created);
        newCreated.setValue(EncodingSchemeUtils.encodeDate(new Date(), Precision.Second).getValue());
        eventMetadata.addField(newCreated);
      }

      metadataList.add(eventCatalogUIAdapter, eventMetadata);
      updateMediaPackageMetadata(mp, metadataList);

      Option<DublinCoreCatalog> dcOpt = DublinCoreUtil.loadEpisodeDublinCore(getWorkspace(), mp);
      if (dcOpt.isSome()) {
        dc = dcOpt.get();
        // make sure to bind the OC_PROPERTY namespace
        dc.addBindings(XmlNamespaceContext.mk(XmlNamespaceBinding.mk(DublinCores.OC_PROPERTY_NS_PREFIX,
                DublinCores.OC_PROPERTY_NS_URI)));
      } else {
        dc = DublinCores.mkOpencast();
      }

      if (sourceMetadata != null
              && (type.equals(SourceType.SCHEDULE_SINGLE) || type.equals(SourceType.SCHEDULE_MULTIPLE))) {
        Date start = new Date(DateTimeSupport.fromUTC((String) sourceMetadata.get("start")));
        Date end = new Date(DateTimeSupport.fromUTC((String) sourceMetadata.get("end")));
        DublinCoreValue period = EncodingSchemeUtils.encodePeriod(new DCMIPeriod(start, end), Precision.Second);
        String inputs = (String) sourceMetadata.get("inputs");

        Properties configuration;
        try {
          configuration = getCaptureAgentStateService().getAgentConfiguration((String) sourceMetadata.get("device"));
        } catch (Exception e) {
          logger.warn("Unable to parse device {}: because: {}", sourceMetadata.get("device"),
                  ExceptionUtils.getStackTrace(e));
          throw new IllegalArgumentException("Unable to parse device");
        }
        caProperties.putAll(configuration);
        String agentTimeZone = configuration.getProperty("capture.device.timezone.offset");
        dc.set(DublinCores.OC_PROPERTY_AGENT_TIMEZONE, agentTimeZone);
        dc.set(DublinCore.PROPERTY_TEMPORAL, period);
        caProperties.put(CaptureParameters.CAPTURE_DEVICE_NAMES, inputs);
      }

      if (type.equals(SourceType.SCHEDULE_MULTIPLE)) {
        String rrule = (String) sourceMetadata.get("rrule");
        dc.set(DublinCores.OC_PROPERTY_RECURRENCE, rrule);
      }

      inputStream = IOUtils.toInputStream(dc.toXmlString(), "UTF-8");

      // Update dublincore catalog
      Catalog[] catalogs = mp.getCatalogs(MediaPackageElements.EPISODE);
      if (catalogs.length > 0) {
        Catalog catalog = catalogs[0];
        URI uri = getWorkspace().put(mp.getIdentifier().toString(), catalog.getIdentifier(), "dublincore.xml",
                inputStream);
        catalog.setURI(uri);
        // setting the URI to a new source so the checksum will most like be invalid
        catalog.setChecksum(null);
      } else {
        mp = getIngestService().addCatalog(inputStream, "dublincore.xml", MediaPackageElements.EPISODE, mp);
      }
    } catch (MetadataParsingException e) {
      logger.warn("Unable to parse event metadata {}", allEventMetadataJson.toJSONString());
      throw new IllegalArgumentException("Unable to parse metadata set");
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    Map<String, String> configuration = new HashMap<String, String>((JSONObject) processing.get("configuration"));
    for (Entry<String, String> entry : configuration.entrySet()) {
      caProperties.put(WORKFLOW_CONFIG_PREFIX.concat(entry.getKey()), entry.getValue());
    }
    caProperties.put(CaptureParameters.INGEST_WORKFLOW_DEFINITION, workflowTemplate);

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

    switch (type) {
      case UPLOAD:
      case UPLOAD_LATER:
        mp = getAuthorizationService().setAcl(mp, AclScope.Episode, acl).getA();
        configuration.put("workflowDefinitionId", workflowTemplate);
        WorkflowInstance ingest = getIngestService().ingest(mp, workflowTemplate, configuration);
        return Long.toString(ingest.getId());
      case SCHEDULE_SINGLE:
        getIngestService().discardMediaPackage(mp);
        Long id = getSchedulerService().addEvent(dc, configuration);
        getSchedulerService().updateCaptureAgentMetadata(caProperties, Tuple.tuple(id, dc));
        getSchedulerService().updateAccessControlList(id, acl);
        return Long.toString(id);
      case SCHEDULE_MULTIPLE:
        getIngestService().discardMediaPackage(mp);
        // try to create event and it's recurrences
        Long[] createdIDs = getSchedulerService().addReccuringEvent(dc, configuration);
        for (long createdId : createdIDs) {
          getSchedulerService().updateCaptureAgentMetadata(caProperties,
                  Tuple.tuple(createdId, getSchedulerService().getEventDublinCore(createdId)));
          getSchedulerService().updateAccessControlList(createdId, acl);
        }
        return StringUtils.join(createdIDs, ",");
      default:
        logger.warn("Unknown source type {}", type);
        throw new IllegalArgumentException("Unknown source type");
    }
  }

  @Override
  public MetadataList updateCommonEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, InternalServerErrorException, NotFoundException, UnauthorizedException {
    MetadataList metadataList;
    try {
      metadataList = getMetadatListWithCommonEventCatalogUIAdapter();
      metadataList.fromJSON(metadataJSON);
    } catch (Exception e) {
      logger.warn("Not able to parse the event metadata {}: {}", metadataJSON, ExceptionUtils.getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the event metadata " + metadataJSON, e);
    }
    return updateEventMetadata(id, metadataJSON, index, metadataList);
  }

  @Override
  public MetadataList updateAllEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, InternalServerErrorException, NotFoundException, UnauthorizedException {
    MetadataList metadataList;
    try {
      metadataList = getMetadatListWithAllEventCatalogUIAdapters();
      metadataList.fromJSON(metadataJSON);
    } catch (Exception e) {
      logger.warn("Not able to parse the event metadata {}: {}", metadataJSON, ExceptionUtils.getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the event metadata " + metadataJSON, e);
    }
    return updateEventMetadata(id, metadataJSON, index, metadataList);
  }

  protected MetadataList updateEventMetadata(String id, String metadataJSON, AbstractSearchIndex index,
          MetadataList metadataList) throws IllegalArgumentException, InternalServerErrorException, NotFoundException,
          UnauthorizedException {
    Opt<Event> optEvent = getEvent(id, index);
    if (optEvent.isNone())
      throw new NotFoundException("Cannot find an event with id " + id);

    Event event = optEvent.get();
    Opt<MediaPackage> mpOpt = getEventMediapackage(event);
    MediaPackage mediaPackage;
    DublinCoreCatalog dc;
    switch (getEventSource(event)) {
      case WORKFLOW:
        try {
          if (mpOpt.isNone()) {
            logger.error("No mediapackage found for workflow event {}!", id);
            throw new InternalServerErrorException("No mediapackage found for workflow event {}!" + id);
          }
          mediaPackage = mpOpt.get();
          updateMediaPackageMetadata(mediaPackage, metadataList);
          WorkflowInstance workflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
          workflowInstance.setMediaPackage(mediaPackage);
          updateWorkflowInstance(workflowInstance);
        } catch (WorkflowDatabaseException e) {
          logger.error("Unable to update workflow event {} with metadata {} because {}", new Object[] { id,
                  metadataJSON, ExceptionUtils.getStackTrace(e) });
          throw new InternalServerErrorException("Unable to update workflow event " + id);
        } catch (WorkflowException e) {
          logger.error("Unable to update workflow event {} with metadata {} because {}", new Object[] { id,
                  metadataJSON, ExceptionUtils.getStackTrace(e) });
          throw new InternalServerErrorException("Unable to update workflow event " + id);
        }
        break;
      case ARCHIVE:
        if (mpOpt.isNone()) {
          logger.error("No mediapackage found for archived event {}!", id);
          throw new InternalServerErrorException("No mediapackage found for archived event {}!" + id);
        }

        mediaPackage = mpOpt.get();
        updateMediaPackageMetadata(mediaPackage, metadataList);
        getOpencastArchive().add(mediaPackage);
        break;
      case SCHEDULE:
        try {
          Long eventId = getSchedulerService().getEventId(event.getIdentifier());
          dc = getSchedulerService().getEventDublinCore(eventId);
          Opt<AbstractMetadataCollection> abstractMetadata = metadataList
                  .getMetadataByAdapter(getEpisodeCatalogUIAdapter());
          if (abstractMetadata.isSome()) {
            DublinCoreMetadataUtil.updateDublincoreCatalog(dc, abstractMetadata.get());
          }
          getSchedulerService().updateEvent(eventId, dc, new HashMap<String, String>());
        } catch (SchedulerException e) {
          logger.error("Unable to update scheduled event {} with metadata {} because {}", new Object[] { id,
                  metadataJSON, ExceptionUtils.getStackTrace(e) });
          throw new InternalServerErrorException("Unable to update scheduled event " + id);
        }
        break;
      default:
        logger.error("Unkown event source!");
    }
    return metadataList;
  }

  /**
   * Get a single event
   *
   * @param id
   *          the mediapackage id
   * @return an event or none if not found wrapped in an option
   * @throws SearchIndexException
   */
  @Override
  public Opt<Event> getEvent(String id, AbstractSearchIndex index) throws InternalServerErrorException {
    SearchResult<Event> result;
    try {
      result = index.getByQuery(new EventSearchQuery(getSecurityService().getOrganization().getId(),
              getSecurityService().getUser()).withIdentifier(id));
      // If the results list if empty, we return already a response.
      if (result.getPageSize() == 0) {
        logger.debug("Didn't find event with id {}", id);
        return Opt.<Event> none();
      }
      return Opt.some(result.getItems()[0].getSource());
    } catch (IllegalStateException e) {
      logger.error("Unable to get event with id {} because {}", id, ExceptionUtils.getStackTrace(e));
      throw new InternalServerErrorException(e.getMessage(), e);
    } catch (SearchIndexException e) {
      logger.error("Unable to get event with id {} because {}", id, ExceptionUtils.getStackTrace(e));
      throw new InternalServerErrorException(e.getMessage(), e);
    }
  }

  @Override
  public void updateWorkflowInstance(WorkflowInstance workflowInstance) throws WorkflowException, UnauthorizedException {
    // Only update the workflow if the instance is in a working state
    if (WorkflowInstance.WorkflowState.FAILED.equals(workflowInstance.getState())
            || WorkflowInstance.WorkflowState.FAILING.equals(workflowInstance.getState())
            || WorkflowInstance.WorkflowState.STOPPED.equals(workflowInstance.getState())
            || WorkflowInstance.WorkflowState.SUCCEEDED.equals(workflowInstance.getState())) {
      logger.info("Skip updating {} workflow mediapackage {} with updated comments catalog",
              workflowInstance.getState(), workflowInstance.getMediaPackage().getIdentifier().toString());
      return;
    }
    getWorkflowService().update(workflowInstance);
  }

  @Override
  public Opt<MediaPackage> getEventMediapackage(Event event) throws InternalServerErrorException {
    switch (getEventSource(event)) {
      case WORKFLOW:
        WorkflowInstance currentWorkflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
        if (currentWorkflowInstance != null) {
          logger.debug("Found event in workflow with id {}", event.getIdentifier());
          return Opt.some(currentWorkflowInstance.getMediaPackage());
        }
        return Opt.none();
      case ARCHIVE:
        final OpencastResultSet archiveRes = getOpencastArchive().find(
                OpencastQueryBuilder.query().mediaPackageId(event.getIdentifier()).onlyLastVersion(true),
                getHttpMediaPackageElementProvider().getUriRewriter());
        if (archiveRes.size() > 0) {
          logger.debug("Found event in archive with id {}", event.getIdentifier());
          return Opt.some(archiveRes.getItems().get(0).getMediaPackage());
        }
        return Opt.none();
      case SCHEDULE:
        return Opt.none();
      default:
        throw new InternalServerErrorException("Unknown event type!");
    }
  }

  /**
   * Determines in a very basic way what kind of source the event is
   *
   * @param event
   *          the event
   * @return the source type
   * @throws InternalServerErrorException
   *           Thrown if there is an issue getting the workflow for the event.
   */
  @Override
  public Source getEventSource(Event event) {
    if (event.getWorkflowId() != null && isWorkflowActive(event.getWorkflowState()))
      return Source.WORKFLOW;

    if (event.getArchiveVersion() != null)
      return Source.ARCHIVE;

    if (event.getWorkflowId() != null)
      return Source.WORKFLOW;

    return Source.SCHEDULE;
  }

  @Override
  public WorkflowInstance getCurrentWorkflowInstance(String mpId) throws InternalServerErrorException {
    WorkflowQuery query = new WorkflowQuery().withMediaPackage(mpId);
    try {
      WorkflowSet workflowInstances = getWorkflowService().getWorkflowInstances(query);
      if (workflowInstances.size() == 0) {
        logger.info("No workflow instance found for mediapackage {}.", mpId);
        return null;
      }

      // Get the newest workflow instance
      // TODO This presuppose knowledge of the Database implementation and should be fixed sooner or later!
      WorkflowInstance workflowInstance = workflowInstances.getItems()[0];
      for (WorkflowInstance instance : workflowInstances.getItems()) {
        if (instance.getId() > workflowInstance.getId())
          workflowInstance = instance;
      }
      return workflowInstance;
    } catch (WorkflowDatabaseException e) {
      throw new InternalServerErrorException("Unable to get the current workflow instance for " + mpId, e);
    }
  }

  private MetadataList getMetadatListWithCommonEventCatalogUIAdapter() {
    MetadataList metadataList = new MetadataList();
    metadataList.add(eventCatalogUIAdapter, eventCatalogUIAdapter.getRawFields());
    return metadataList;
  }

  private MetadataList getMetadatListWithAllEventCatalogUIAdapters() {
    MetadataList metadataList = new MetadataList();
    for (EventCatalogUIAdapter catalogUIAdapter : getEventCatalogUIAdapters()) {
      metadataList.add(catalogUIAdapter, catalogUIAdapter.getRawFields());
    }
    return metadataList;
  }

  private static Function<ManagedAcl, AccessControlList> toAcl = new Function<ManagedAcl, AccessControlList>() {
    @Override
    public AccessControlList apply(ManagedAcl a) {
      return a.getAcl();
    }
  };

  private static Function2<String, AccessControlList, AccessControlList> extendAclWithCurrentUser = new Function2<String, AccessControlList, AccessControlList>() {
    @Override
    public AccessControlList apply(String username, AccessControlList acl) {
      String userIdRole = UserIdRoleProvider.getUserIdRole(username);
      acl = AccessControlUtil.extendAcl(acl, userIdRole, Permissions.Action.READ.toString(), true);
      acl = AccessControlUtil.extendAcl(acl, userIdRole, Permissions.Action.WRITE.toString(), true);
      return acl;
    }
  };

  private List<EventCatalogUIAdapter> getEventCatalogUIAdapters() {
    return new ArrayList<EventCatalogUIAdapter>(getEventCatalogUIAdapters(getSecurityService().getOrganization()
            .getId()));
  }

  @Override
  public void updateMediaPackageMetadata(MediaPackage mp, MetadataList metadataList) {
    List<EventCatalogUIAdapter> catalogUIAdapters = getEventCatalogUIAdapters();
    if (catalogUIAdapters.size() > 0) {
      for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
        Opt<AbstractMetadataCollection> metadata = metadataList.getMetadataByAdapter(catalogUIAdapter);
        if (metadata.isSome() && metadata.get().isUpdated()) {
          catalogUIAdapter.storeFields(mp, metadata.get());
        }
      }
    }
  }

  @Override
  public String createSeries(String metadata) throws IllegalArgumentException, InternalServerErrorException,
          UnauthorizedException {
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

    Opt<Long> themeId = Opt.<Long> none();
    Long theme = (Long) metadataJson.get("theme");
    if (theme != null) {
      themeId = Opt.some(theme);
    }

    Map<String, String> optionsMap;
    try {
      optionsMap = JSONUtils.toMap(new org.codehaus.jettison.json.JSONObject(options.toJSONString()));
    } catch (JSONException e) {
      logger.warn("Unable to parse options to map: {}", ExceptionUtils.getStackTrace(e));
      throw new IllegalArgumentException("Unable to parse options to map");
    }

    DublinCoreCatalog dc = DublinCores.mkOpencast();
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
      logger.warn("Not able to parse the series metadata {}: {}", seriesMetadataJson, ExceptionUtils.getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the series metadata");
    }

    Opt<AbstractMetadataCollection> seriesMetadata = metadataList.getMetadataByFlavor(MediaPackageElements.SERIES
            .toString());
    if (seriesMetadata.isSome()) {
      DublinCoreMetadataUtil.updateDublincoreCatalog(dc, seriesMetadata.get());
    }

    AccessControlList acl = new AccessControlList();
    JSONObject access = (JSONObject) metadataJson.get("access");
    if (access != null) {
      try {
        acl = AccessControlParser.parseAcl(access.toJSONString());
      } catch (Exception e) {
        logger.warn("Unable to parse access control list: {}", access.toJSONString());
        throw new IllegalArgumentException("Unable to parse access control list!");
      }
    }

    String seriesId;
    try {
      DublinCoreCatalog createdSeries = seriesService.updateSeries(dc);
      seriesId = createdSeries.getFirst(PROPERTY_IDENTIFIER);
      seriesService.updateAccessControl(seriesId, acl);
      for (Long id : themeId)
        seriesService.updateSeriesProperty(seriesId, THEME_PROPERTY_NAME, Long.toString(id));
    } catch (Exception e) {
      logger.error("Unable to create new series: {}", ExceptionUtils.getStackTrace(e));
      throw new InternalServerErrorException("Unable to create new series");
    }

    updateSeriesMetadata(seriesId, metadataList);

    return seriesId;
  }

  @Override
  public Opt<Series> getSeries(String seriesId, AbstractSearchIndex searchIndex) throws SearchIndexException {
    SearchResult<Series> result = searchIndex.getByQuery(new SeriesSearchQuery(securityService.getOrganization()
            .getId(), securityService.getUser()).withIdentifier(seriesId));
    // If the results list if empty, we return already a response.
    if (result.getPageSize() == 0) {
      logger.debug("Didn't find series with id {}", seriesId);
      return Opt.<Series> none();
    }
    return Opt.some(result.getItems()[0].getSource());
  }

  @Override
  public MetadataList updateCommonSeriesMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, InternalServerErrorException, NotFoundException, UnauthorizedException {
    MetadataList metadataList = getMetadataListWithCommonSeriesCatalogUIAdapters();
    return updateSeriesMetadata(id, metadataJSON, index, metadataList);
  }

  @Override
  public MetadataList updateAllSeriesMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, InternalServerErrorException, NotFoundException, UnauthorizedException {
    MetadataList metadataList = getMetadataListWithAllSeriesCatalogUIAdapters();
    return updateSeriesMetadata(id, metadataJSON, index, metadataList);
  }

  public MetadataList updateSeriesMetadata(String seriesID, String metadataJSON, AbstractSearchIndex index,
          MetadataList metadataList) throws IllegalArgumentException, InternalServerErrorException, NotFoundException,
          UnauthorizedException {
    try {
      Opt<Series> optSeries = getSeries(seriesID, index);
      if (optSeries.isNone())
        throw new NotFoundException("Cannot find a series with id " + seriesID);
    } catch (SearchIndexException e) {
      logger.error("Unable to get a series with id {} because: {}", seriesID, ExceptionUtils.getStackTrace(e));
      throw new InternalServerErrorException("Cannot use search service to find Series");
    }
    try {
      metadataList.fromJSON(metadataJSON);
    } catch (Exception e) {
      logger.warn("Not able to parse the event metadata {}: {}", metadataJSON, ExceptionUtils.getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the event metadata");
    }

    updateSeriesMetadata(seriesID, metadataList);
    return metadataList;
  }

  /**
   * @return A {@link MetadataList} with only the common SeriesCatalogUIAdapter's empty
   *         {@link AbstractMetadataCollection} available
   */
  private MetadataList getMetadataListWithCommonSeriesCatalogUIAdapters() {
    MetadataList metadataList = new MetadataList();
    metadataList.add(commonSeriesCatalogUIAdapter.getFlavor(), commonSeriesCatalogUIAdapter.getUITitle(),
            commonSeriesCatalogUIAdapter.getRawFields());
    return metadataList;
  }

  /**
   * @return A {@link MetadataList} with all of the available CatalogUIAdapters empty {@link AbstractMetadataCollection}
   *         available
   */
  private MetadataList getMetadataListWithAllSeriesCatalogUIAdapters() {
    MetadataList metadataList = new MetadataList();
    for (SeriesCatalogUIAdapter adapter : getSeriesCatalogUIAdapters(securityService.getOrganization().getId())) {
      metadataList.add(adapter.getFlavor(), adapter.getUITitle(), adapter.getRawFields());
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
      Opt<AbstractMetadataCollection> metadata = metadataList.getMetadataByFlavor(adapter.getFlavor());
      if (metadata.isSome() && metadata.get().isUpdated()) {
        adapter.storeFields(seriesId, metadata.get());
      }
    }
  }

  public boolean isWorkflowActive(String workflowState) {
    if (WorkflowState.INSTANTIATED.toString().equals(workflowState)
            || WorkflowState.RUNNING.toString().equals(workflowState)
            || WorkflowState.PAUSED.toString().equals(workflowState)) {
      return true;
    }
    return false;
  }

}
