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

import static org.opencastproject.metadata.dublincore.DublinCore.PROPERTY_IDENTIFIER;

import org.opencastproject.archive.api.ArchiveException;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.archive.opencast.OpencastQueryBuilder;
import org.opencastproject.archive.opencast.OpencastResultSet;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.comments.Comment;
import org.opencastproject.comments.CommentException;
import org.opencastproject.comments.CommentParser;
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
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.exception.MetadataParsingException;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
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
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.EName;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.metadata.dublincore.DCMIPeriod;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
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
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesQuery;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.userdirectory.JpaGroupRoleProvider;
import org.opencastproject.userdirectory.UserIdRoleProvider;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.XmlNamespaceBinding;
import org.opencastproject.util.XmlNamespaceContext;
import org.opencastproject.util.data.Effect0;
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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class IndexServiceImpl implements IndexService {

  private static final String WORKFLOW_CONFIG_PREFIX = "org.opencastproject.workflow.config.";

  public static final String THEME_PROPERTY_NAME = "theme";

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);

  /** A parser for handling JSON documents inside the body of a request. **/
  private static final JSONParser parser = new JSONParser();

  private final List<EventCatalogUIAdapter> eventCatalogUIAdapters = new ArrayList<EventCatalogUIAdapter>();
  private final List<SeriesCatalogUIAdapter> seriesCatalogUIAdapters = new ArrayList<SeriesCatalogUIAdapter>();
  private EventCatalogUIAdapter eventCatalogUIAdapter;
  private SeriesCatalogUIAdapter seriesCatalogUIAdapter;

  private AclServiceFactory aclServiceFactory;
  private AuthorizationService authorizationService;
  private CaptureAgentStateService captureAgentStateService;
  private HttpMediaPackageElementProvider httpMediaPackageElementProvider;
  private IngestService ingestService;
  private OpencastArchive opencastArchive;
  private SchedulerService schedulerService;
  private SecurityService securityService;
  private JpaGroupRoleProvider jpaGroupRoleProvider;
  private SeriesService seriesService;
  private WorkflowService workflowService;
  private Workspace workspace;

  /** The single thread executor service */
  private ExecutorService executorService = Executors.newSingleThreadExecutor();

  /** OSGi DI. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  /** OSGi DI. */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /** OSGi DI. */
  public void setCaptureAgentStateService(CaptureAgentStateService captureAgentStateService) {
    this.captureAgentStateService = captureAgentStateService;
  }

  /** OSGi callback to add the event dublincore {@link EventCatalogUIAdapter} instance. */
  public void setCommonEventCatalogUIAdapter(CommonEventCatalogUIAdapter eventCatalogUIAdapter) {
    this.eventCatalogUIAdapter = eventCatalogUIAdapter;
  }

  /** OSGi callback to add the series dublincore {@link SeriesCatalogUIAdapter} instance. */
  public void setCommonSeriesCatalogUIAdapter(CommonSeriesCatalogUIAdapter seriesCatalogUIAdapter) {
    this.seriesCatalogUIAdapter = seriesCatalogUIAdapter;
  }

  /** OSGi callback to add {@link EventCatalogUIAdapter} instance. */
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi callback to remove {@link EventCatalogUIAdapter} instance. */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    eventCatalogUIAdapters.remove(catalogUIAdapter);
  }

  /** OSGi callback to add {@link SeriesCatalogUIAdapter} instance. */
  public void addCatalogUIAdapter(SeriesCatalogUIAdapter catalogUIAdapter) {
    seriesCatalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi callback to remove {@link SeriesCatalogUIAdapter} instance. */
  public void removeCatalogUIAdapter(SeriesCatalogUIAdapter catalogUIAdapter) {
    seriesCatalogUIAdapters.remove(catalogUIAdapter);
  }

  /** OSGi DI. */
  public void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider httpMediaPackageElementProvider) {
    this.httpMediaPackageElementProvider = httpMediaPackageElementProvider;
  }

  /** OSGi DI. */
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /** OSGi DI. */
  public void setOpencastArchive(OpencastArchive opencastArchive) {
    this.opencastArchive = opencastArchive;
  }

  /** OSGi DI. */
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  /** OSGi DI. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi DI. */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi DI. */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /** OSGi DI. */
  public void setGroupRoleProvider(JpaGroupRoleProvider jpaGroupRoleProvider) {
    this.jpaGroupRoleProvider = jpaGroupRoleProvider;
  }

  public AclService getAclService() {
    return aclServiceFactory.serviceFor(securityService.getOrganization());
  }

  private static final Fn2<EventCatalogUIAdapter, String, Boolean> eventOrganizationFilter = new Fn2<EventCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean ap(EventCatalogUIAdapter catalogUIAdapter, String organization) {
      return organization.equals(catalogUIAdapter.getOrganization());
    }
  };

  private static final Fn2<SeriesCatalogUIAdapter, String, Boolean> seriesOrganizationFilter = new Fn2<SeriesCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean ap(SeriesCatalogUIAdapter catalogUIAdapter, String organization) {
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
    return new ArrayList<EventCatalogUIAdapter>(getEventCatalogUIAdapters(securityService.getOrganization().getId()));
  }

  @Override
  public List<SeriesCatalogUIAdapter> getSeriesCatalogUIAdapters() {
    return new LinkedList<SeriesCatalogUIAdapter>(
            getSeriesCatalogUIAdapters(securityService.getOrganization().getId()));
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
                metadataJson = (JSONObject) parser.parse(metadata);
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
            } else {
              logger.warn("Unknown field name found {}", item.getFieldName());
            }
          }
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
      logger.error("Unable to create event: {}", ExceptionUtils.getStackTrace(e));
      throw new IndexServiceException(e.getMessage());
    }
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
      MetadataList metadataList = getMetadataListWithAllEventCatalogUIAdapters();
      metadataList.fromJSON(allEventMetadataJson.toJSONString());
      AbstractMetadataCollection eventMetadata = metadataList.getMetadataByAdapter(eventCatalogUIAdapter).get();

      JSONObject sourceMetadata = (JSONObject) source.get("metadata");
      if (sourceMetadata != null
              && (type.equals(SourceType.SCHEDULE_SINGLE) || type.equals(SourceType.SCHEDULE_MULTIPLE))) {
        try {
          MetadataField<?> current = eventMetadata.getOutputFields().get("location");
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

      Option<DublinCoreCatalog> dcOpt = DublinCoreUtil.loadEpisodeDublinCore(workspace, mp);
      if (dcOpt.isSome()) {
        dc = dcOpt.get();
        // make sure to bind the OC_PROPERTY namespace
        dc.addBindings(XmlNamespaceContext
                .mk(XmlNamespaceBinding.mk(DublinCores.OC_PROPERTY_NS_PREFIX, DublinCores.OC_PROPERTY_NS_URI)));
      } else {
        dc = DublinCores.mkOpencast();
      }

      if (sourceMetadata != null
              && (type.equals(SourceType.SCHEDULE_SINGLE) || type.equals(SourceType.SCHEDULE_MULTIPLE))) {
        Properties configuration;
        try {
          configuration = captureAgentStateService.getAgentConfiguration((String) sourceMetadata.get("device"));
        } catch (Exception e) {
          logger.warn("Unable to parse device {}: because: {}", sourceMetadata.get("device"),
                  ExceptionUtils.getStackTrace(e));
          throw new IllegalArgumentException("Unable to parse device");
        }
        caProperties.putAll(configuration);
        String agentTimeZone = configuration.getProperty("capture.device.timezone");
        dc.set(DublinCores.OC_PROPERTY_AGENT_TIMEZONE, agentTimeZone);

        Date start = new Date(DateTimeSupport.fromUTC((String) sourceMetadata.get("start")));
        Date end = new Date(DateTimeSupport.fromUTC((String) sourceMetadata.get("end")));

        String duration = (String) sourceMetadata.get("duration");
        if (StringUtils.isNotBlank(duration))
          dc.set(DublinCores.OC_PROPERTY_DURATION, duration);
        else
          throw new IllegalArgumentException("The duration property is missing.");

        DublinCoreValue period = EncodingSchemeUtils.encodePeriod(new DCMIPeriod(start, end), Precision.Second);
        String inputs = (String) sourceMetadata.get("inputs");

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
        URI uri = workspace.put(mp.getIdentifier().toString(), catalog.getIdentifier(), "dublincore.xml", inputStream);
        catalog.setURI(uri);
        // setting the URI to a new source so the checksum will most like be invalid
        catalog.setChecksum(null);
      } else {
        mp = ingestService.addCatalog(inputStream, "dublincore.xml", MediaPackageElements.EPISODE, mp);
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
        mp = authorizationService.setAcl(mp, AclScope.Episode, acl).getA();
        configuration.put("workflowDefinitionId", workflowTemplate);
        WorkflowInstance ingest = ingestService.ingest(mp, workflowTemplate, configuration);
        return mp.getIdentifier().compact();
      case SCHEDULE_SINGLE:
        ingestService.discardMediaPackage(mp);
        Long id = schedulerService.addEvent(dc, configuration);
        schedulerService.updateCaptureAgentMetadata(caProperties, Tuple.tuple(id, dc));
        schedulerService.updateAccessControlList(id, acl);
        return Long.toString(id);
      case SCHEDULE_MULTIPLE:
        ingestService.discardMediaPackage(mp);
        // try to create event and it's recurrences
        Long[] createdIDs = schedulerService.addReccuringEvent(dc, configuration);
        for (long createdId : createdIDs) {
          schedulerService.updateCaptureAgentMetadata(caProperties,
                  Tuple.tuple(createdId, schedulerService.getEventDublinCore(createdId)));
          schedulerService.updateAccessControlList(createdId, acl);
        }
        return StringUtils.join(createdIDs, ",");
      default:
        logger.warn("Unknown source type {}", type);
        throw new IllegalArgumentException("Unknown source type");
    }
  }

  @Override
  public MetadataList updateCommonEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, WorkflowDatabaseException, IndexServiceException, SearchIndexException,
          NotFoundException, UnauthorizedException {
    MetadataList metadataList;
    try {
      metadataList = getMetadataListWithCommonEventCatalogUIAdapter();
      metadataList.fromJSON(metadataJSON);
    } catch (Exception e) {
      logger.warn("Not able to parse the event metadata {}: {}", metadataJSON, ExceptionUtils.getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the event metadata " + metadataJSON, e);
    }
    return updateEventMetadata(id, metadataList, index);
  }

  @Override
  public MetadataList updateAllEventMetadata(String id, String metadataJSON, AbstractSearchIndex index)
          throws IllegalArgumentException, WorkflowDatabaseException, IndexServiceException, NotFoundException,
          SearchIndexException, UnauthorizedException {
    MetadataList metadataList;
    try {
      metadataList = getMetadataListWithAllEventCatalogUIAdapters();
      metadataList.fromJSON(metadataJSON);
    } catch (Exception e) {
      logger.warn("Not able to parse the event metadata {}: {}", metadataJSON, ExceptionUtils.getStackTrace(e));
      throw new IllegalArgumentException("Not able to parse the event metadata " + metadataJSON, e);
    }
    return updateEventMetadata(id, metadataList, index);
  }

  @Override
  public void removeCatalogByFlavor(Event event, MediaPackageElementFlavor flavor)
          throws WorkflowDatabaseException, IndexServiceException, NotFoundException, UnauthorizedException {
    Opt<MediaPackage> mpOpt = getEventMediapackage(event);
    if (mpOpt.isSome()) {
      Catalog[] catalogs = mpOpt.get().getCatalogs(flavor);
      if (catalogs.length == 0) {
        throw new NotFoundException(String.format("Cannot find a catalog with flavor '%s' for event with id '%s'.",
                flavor.toString(), event.getIdentifier()));
      }
      for (Catalog catalog : catalogs) {
        mpOpt.get().remove(catalog);
      }
      switch (getEventSource(event)) {
        case WORKFLOW:
          WorkflowInstance workflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
          workflowInstance.setMediaPackage(mpOpt.get());
          try {
            updateWorkflowInstance(workflowInstance);
          } catch (WorkflowException e) {
            logger.error("Unable to remove catalog with flavor {} by updating workflow event {} because {}",
                    new Object[] { flavor, event.getIdentifier(), ExceptionUtils.getStackTrace(e) });
            throw new IndexServiceException("Unable to update workflow event " + event.getIdentifier());
          }
          break;
        case ARCHIVE:
          opencastArchive.add(mpOpt.get());
          break;
        case SCHEDULE:
          // Ignoring as there are no mediapackages attached to scheduled items
          throw new IllegalStateException(
                  "Unable to remove a catalog from a Scheduled event as there is no mediapackage.");
        default:
          throw new IndexServiceException(
                  String.format("Unable to handle event source type '%s'", getEventSource(event)));
      }
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
          throws IllegalArgumentException, WorkflowDatabaseException, IndexServiceException, SearchIndexException,
          NotFoundException, UnauthorizedException {
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
            throw new IndexServiceException("No mediapackage found for workflow event {}!" + id);
          }
          mediaPackage = mpOpt.get();
          updateMediaPackageMetadata(mediaPackage, metadataList);
          WorkflowInstance workflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
          workflowInstance.setMediaPackage(mediaPackage);
          updateWorkflowInstance(workflowInstance);
        } catch (WorkflowDatabaseException e) {
          logger.error("Unable to update workflow event {} with metadata {} because {}", new Object[] { id,
                  RestUtils.getJsonStringSilent(metadataList.toJSON()), ExceptionUtils.getStackTrace(e) });
          throw new IndexServiceException("Unable to update workflow event " + id);
        } catch (WorkflowException e) {
          logger.error("Unable to update workflow event {} with metadata {} because {}", new Object[] { id,
                  RestUtils.getJsonStringSilent(metadataList.toJSON()), ExceptionUtils.getStackTrace(e) });
          throw new IndexServiceException("Unable to update workflow event " + id);
        }
        break;
      case ARCHIVE:
        if (mpOpt.isNone()) {
          logger.error("No mediapackage found for archived event {}!", id);
          throw new IndexServiceException("No mediapackage found for archived event {}!" + id);
        }
        mediaPackage = mpOpt.get();
        updateMediaPackageMetadata(mediaPackage, metadataList);
        opencastArchive.add(mediaPackage);
        break;
      case SCHEDULE:
        try {
          Long eventId = schedulerService.getEventId(event.getIdentifier());
          dc = schedulerService.getEventDublinCore(eventId);
          Opt<AbstractMetadataCollection> abstractMetadata = metadataList.getMetadataByAdapter(eventCatalogUIAdapter);
          if (abstractMetadata.isSome()) {
            DublinCoreMetadataUtil.updateDublincoreCatalog(dc, abstractMetadata.get());
          }
          schedulerService.updateEvent(eventId, dc, new HashMap<String, String>());
        } catch (SchedulerException e) {
          logger.error("Unable to update scheduled event {} with metadata {} because {}", new Object[] { id,
                  RestUtils.getJsonStringSilent(metadataList.toJSON()), ExceptionUtils.getStackTrace(e) });
          throw new IndexServiceException("Unable to update scheduled event " + id);
        }
        break;
      default:
        logger.error("Unkown event source!");
    }
    return metadataList;
  }

  @Override
  public AccessControlList updateEventAcl(String id, AccessControlList acl, AbstractSearchIndex index)
          throws IllegalArgumentException, WorkflowDatabaseException, IndexServiceException, SearchIndexException,
          NotFoundException {
    Opt<Event> optEvent = getEvent(id, index);
    if (optEvent.isNone())
      throw new NotFoundException("Cannot find an event with id " + id);

    Event event = optEvent.get();
    Opt<MediaPackage> mpOpt = getEventMediapackage(event);
    MediaPackage mediaPackage;
    switch (getEventSource(event)) {
      case WORKFLOW:
        // Not updating the acl as the workflow might have already passed the point of distribution.
        throw new IllegalArgumentException("Unable to update the ACL of this event as it is currently processing.");
      case ARCHIVE:
        if (mpOpt.isNone()) {
          logger.error("No mediapackage found for archived event {}!", id);
          throw new IndexServiceException("No mediapackage found for archived event {}!" + id);
        }
        mediaPackage = mpOpt.get();
        mediaPackage = authorizationService.setAcl(mediaPackage, AclScope.Episode, acl).getA();
        opencastArchive.add(mediaPackage);
        return acl;
      case SCHEDULE:
        try {
          Long eventId = schedulerService.getEventId(event.getIdentifier());
          schedulerService.updateAccessControlList(eventId, acl);
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
      return Opt.<Group> none();
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

  /**
   * Get a single event
   *
   * @param id
   *          the mediapackage id
   * @return an event or none if not found wrapped in an option
   * @throws SearchIndexException
   */
  @Override
  public Opt<Event> getEvent(String id, AbstractSearchIndex index) throws SearchIndexException {
    SearchResult<Event> result = index
            .getByQuery(new EventSearchQuery(securityService.getOrganization().getId(), securityService.getUser())
                    .withIdentifier(id));
    // If the results list if empty, we return already a response.
    if (result.getPageSize() == 0) {
      logger.debug("Didn't find event with id {}", id);
      return Opt.<Event> none();
    }
    return Opt.some(result.getItems()[0].getSource());
  }

  @Override
  public boolean removeEvent(String id) throws NotFoundException, UnauthorizedException {
    boolean unauthorizedScheduler = false;
    boolean notFoundScheduler = false;
    boolean removedScheduler = true;
    try {
      schedulerService.removeEvent(schedulerService.getEventId(id));
    } catch (NotFoundException e) {
      notFoundScheduler = true;
    } catch (UnauthorizedException e) {
      unauthorizedScheduler = true;
    } catch (SchedulerException e) {
      removedScheduler = false;
      logger.error("Unable to remove the event '{}' from scheduler service: {}", id, ExceptionUtils.getStackTrace(e));
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
      logger.error("Unable to remove the event '{}' because removing workflow failed: {}", id,
              ExceptionUtils.getStackTrace(e));
    } catch (WorkflowException e) {
      removedWorkflow = false;
      logger.error("Unable to remove the event '{}' because removing workflow failed: {}", id,
              ExceptionUtils.getStackTrace(e));
    }

    boolean unauthorizedArchive = false;
    boolean notFoundArchive = false;
    boolean removedArchive = true;
    try {
      OpencastResultSet archiveRes = opencastArchive.find(
              OpencastQueryBuilder.query().mediaPackageId(id).onlyLastVersion(true),
              httpMediaPackageElementProvider.getUriRewriter());
      if (archiveRes.size() > 0) {
        opencastArchive.delete(id);
      } else {
        notFoundArchive = true;
      }
    } catch (ArchiveException e) {
      if (e.isCauseNotAuthorized()) {
        unauthorizedArchive = true;
      } else if (e.isCauseNotFound()) {
        notFoundArchive = true;
      } else {
        removedArchive = false;
        logger.error("Unable to remove the event '{}' from the archive: {}", id, ExceptionUtils.getStackTrace(e));
      }
    }

    if (notFoundScheduler && notFoundWorkflow && notFoundArchive)
      throw new NotFoundException("Event id " + id + " not found.");

    if (unauthorizedScheduler || unauthorizedWorkflow || unauthorizedArchive)
      throw new UnauthorizedException("Not authorized to remove event id " + id);

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
  public Opt<MediaPackage> getEventMediapackage(Event event) throws WorkflowDatabaseException {
    switch (getEventSource(event)) {
      case WORKFLOW:
        WorkflowInstance currentWorkflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
        if (currentWorkflowInstance != null) {
          logger.debug("Found event in workflow with id {}", event.getIdentifier());
          return Opt.some(currentWorkflowInstance.getMediaPackage());
        }
        return Opt.none();
      case ARCHIVE:
        final OpencastResultSet archiveRes = opencastArchive.find(
                OpencastQueryBuilder.query().mediaPackageId(event.getIdentifier()).onlyLastVersion(true),
                httpMediaPackageElementProvider.getUriRewriter());
        if (archiveRes.size() > 0) {
          logger.debug("Found event in archive with id {}", event.getIdentifier());
          return Opt.some(archiveRes.getItems().get(0).getMediaPackage());
        }
        return Opt.none();
      case SCHEDULE:
        return Opt.none();
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
   * @throws IndexServiceException
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
  public WorkflowInstance getCurrentWorkflowInstance(String mpId) throws WorkflowDatabaseException {
    WorkflowQuery query = new WorkflowQuery().withMediaPackage(mpId);
    WorkflowSet workflowInstances = workflowService.getWorkflowInstances(query);
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

  private void updateMediaPackageMetadata(MediaPackage mp, MetadataList metadataList) {
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
  public String createSeries(MetadataList metadataList, Map<String, String> options, Opt<AccessControlList> optAcl,
          Opt<Long> optThemeId) throws IndexServiceException {
    DublinCoreCatalog dc = DublinCores.mkOpencast();
    dc.set(PROPERTY_IDENTIFIER, UUID.randomUUID().toString());
    dc.set(DublinCore.PROPERTY_CREATED, EncodingSchemeUtils.encodeDate(new Date(), Precision.Second));
    for (Entry<String, String> entry : options.entrySet()) {
      dc.set(new EName(DublinCores.OC_PROPERTY_NS_URI, entry.getKey()), entry.getValue());
    }

    Opt<AbstractMetadataCollection> seriesMetadata = metadataList
            .getMetadataByFlavor(MediaPackageElements.SERIES.toString());
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
      logger.error("Unable to create new series: {}", ExceptionUtils.getStackTrace(e));
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

    Opt<AbstractMetadataCollection> seriesMetadata = metadataList
            .getMetadataByFlavor(MediaPackageElements.SERIES.toString());
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
      return Opt.<Series> none();
    }
    return Opt.some(result.getItems()[0].getSource());
  }

  @Override
  public void removeSeries(String id) throws NotFoundException, SeriesException, UnauthorizedException {
    SeriesQuery seriesQuery = new SeriesQuery();
    seriesQuery.setSeriesId(id);
    DublinCoreCatalogList dublinCoreCatalogList = seriesService.getSeries(seriesQuery);
    if (dublinCoreCatalogList.size() == 0) {
      throw new NotFoundException();
    }
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
  public void updateCommentCatalog(final Event event, final List<Comment> comments) throws Exception {
    final Opt<MediaPackage> mpOpt = getEventMediapackage(event);
    if (mpOpt.isNone())
      return;

    final SecurityContext securityContext = new SecurityContext(securityService, securityService.getOrganization(),
            securityService.getUser());
    executorService.execute(new Runnable() {
      @Override
      public void run() {
        securityContext.runInContext(new Effect0() {
          @Override
          protected void run() {
            try {
              MediaPackage mediaPackage = mpOpt.get();
              updateMediaPackageCommentCatalog(mediaPackage, comments);
              switch (getEventSource(event)) {
                case WORKFLOW:
                  logger.info("Update workflow mediapacakge {} with updated comments catalog.", event.getIdentifier());
                  WorkflowInstance workflowInstance = getCurrentWorkflowInstance(event.getIdentifier());
                  workflowInstance.setMediaPackage(mediaPackage);
                  updateWorkflowInstance(workflowInstance);
                  break;
                case ARCHIVE:
                  logger.info("Update archive mediapacakge {} with updated comments catalog.", event.getIdentifier());
                  opencastArchive.add(mediaPackage);
                  break;
                default:
                  logger.error("Unkown event source {}!", event.getSource().toString());
              }
            } catch (Exception e) {
              logger.error("Unable to update event {} comment catalog: {}", event.getIdentifier(),
                      ExceptionUtils.getStackTrace(e));
            }
          }
        });
      }
    });
  }

  private void updateMediaPackageCommentCatalog(MediaPackage mediaPackage, List<Comment> comments)
          throws CommentException, IOException {
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
        String commentCatalog = CommentParser.getAsXml(comments);
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
          throws NotFoundException, SchedulerException, SearchIndexException {
    Opt<Event> optEvent = getEvent(eventId, index);
    if (optEvent.isNone())
      throw new NotFoundException("Cannot find an event with id " + eventId);

    schedulerService.updateOptOutStatus(eventId, optout);
    logger.debug("Setting event {} to opt out status of {}", eventId, optout);
  }

  public MetadataList updateSeriesMetadata(String seriesID, String metadataJSON, AbstractSearchIndex index,
          MetadataList metadataList)
                  throws IllegalArgumentException, IndexServiceException, NotFoundException, UnauthorizedException {
    try {
      Opt<Series> optSeries = getSeries(seriesID, index);
      if (optSeries.isNone())
        throw new NotFoundException("Cannot find a series with id " + seriesID);
    } catch (SearchIndexException e) {
      logger.error("Unable to get a series with id {} because: {}", seriesID, ExceptionUtils.getStackTrace(e));
      throw new IndexServiceException("Cannot use search service to find Series");
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
    metadataList.add(seriesCatalogUIAdapter.getFlavor(), seriesCatalogUIAdapter.getUITitle(),
            seriesCatalogUIAdapter.getRawFields());
    return metadataList;
  }

  /**
   * @return A {@link MetadataList} with all of the available CatalogUIAdapters empty {@link AbstractMetadataCollection}
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
