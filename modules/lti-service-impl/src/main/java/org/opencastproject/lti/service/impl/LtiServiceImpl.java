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
package org.opencastproject.lti.service.impl;

import static org.opencastproject.util.MimeType.mimeType;
import static org.opencastproject.workflow.api.ConfiguredWorkflow.workflow;
import static org.opencastproject.workflow.api.WorkflowInstance.WorkflowState.SUCCEEDED;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.util.Workflows;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.exception.IndexServiceException;
import org.opencastproject.index.service.impl.index.AbstractSearchIndex;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.index.service.impl.index.event.EventSearchQuery;
import org.opencastproject.index.service.impl.index.event.EventUtils;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.lti.service.api.LtiFileUpload;
import org.opencastproject.lti.service.api.LtiJob;
import org.opencastproject.lti.service.api.LtiService;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.matterhorn.search.SearchResult;
import org.opencastproject.matterhorn.search.SearchResultItem;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.metadata.dublincore.DublinCore;
import org.opencastproject.metadata.dublincore.DublinCoreMetadataCollection;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.MetadataField;
import org.opencastproject.metadata.dublincore.MetadataJson;
import org.opencastproject.metadata.dublincore.MetadataList;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workflow.api.ConfiguredWorkflow;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowListener;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowUtil;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.data.json.SimpleSerializer;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The LTI service implementation
 */
public class LtiServiceImpl implements LtiService, ManagedService {
  private static final Logger logger = LoggerFactory.getLogger(LtiServiceImpl.class);

  private static final Gson gson = new Gson();
  private static final String NEW_MP_ID_KEY = "newMpId";
  private IndexService indexService;
  private IngestService ingestService;
  private SecurityService securityService;
  private WorkflowService workflowService;
  private AssetManager assetManager;
  private Workspace workspace;
  private AbstractSearchIndex searchIndex;
  private AuthorizationService authorizationService;
  private SeriesService seriesService;
  private String workflow;
  private String workflowConfiguration;
  private String retractWorkflowId;
  private String copyWorkflowId;
  private final List<EventCatalogUIAdapter> catalogUIAdapters = new ArrayList<>();

  /** OSGi DI */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  /** OSGI DI */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi DI */
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /** OSGi DI */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  /** OSGi DI */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /** OSGi DI */
  public void setSearchIndex(AbstractSearchIndex searchIndex) {
    this.searchIndex = searchIndex;
  }

  /** OSGi DI */
  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  /** OSGi DI */
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  /** OSGi DI */
  void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi DI. */
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi DI. */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.remove(catalogUIAdapter);
  }

  public void activate(ComponentContext cc) {
    workflowService.addWorkflowListener(new WorkflowListener() {
      @Override
      public void stateChanged(WorkflowInstance workflow) {
        if (!workflow.getTemplate().equals(copyWorkflowId)) {
          return;
        }

        if (workflow.getState().equals(SUCCEEDED)) {
          final String publishWorkflowName = "publish";
          logger.info("workflow '{}' succeeded for media package '{}', starting workflow '{}'", workflow.getTemplate(),
                  workflow.getMediaPackage().getIdentifier(), publishWorkflowName);
          final WorkflowDefinition wfd;
          try {
            wfd = workflowService.getWorkflowDefinitionById(publishWorkflowName);
            final Workflows workflows = new Workflows(assetManager, workflowService);
            final ConfiguredWorkflow newWorkflow = workflow(wfd);
            final String targetMpId = workflow.getConfiguration(NEW_MP_ID_KEY);
            final List<WorkflowInstance> workflowInstances = workflows
                    .applyWorkflowToLatestVersion(Collections.singleton(targetMpId), newWorkflow).toList();
            if (workflowInstances.isEmpty()) {
              throw new RuntimeException(
                      String.format("couldn't start workflow '%s' for event %s", publishWorkflowName, targetMpId));
            }
          } catch (WorkflowDatabaseException e) {
            logger.error(String.format("couldn't instantiate workflow '%s'", publishWorkflowName), e);
          } catch (NotFoundException e) {
            logger.error(String.format("couldn't find media package while starting workflow workflow '%s'",
                    publishWorkflowName), e);
          }
        }
      }
    });
  }

  @Override
  public List<LtiJob> listJobs(String seriesId) {
    final User user = securityService.getUser();
    final EventSearchQuery query = new EventSearchQuery(securityService.getOrganization().getId(), user)
            .withCreator(user.getName()).withSeriesId(StringUtils.trimToNull(seriesId));
    try {
      SearchResult<Event> results = this.searchIndex.getByQuery(query);
      ZonedDateTime startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
      return Arrays.stream(results.getItems())
              .map(SearchResultItem::getSource)
              .filter(e -> ZonedDateTime.parse(e.getCreated()).compareTo(startOfDay) > 0)
              .map(e -> new LtiJob(e.getTitle(), e.getDisplayableStatus(workflowService.getWorkflowStateMappings())))
              .collect(Collectors.toList());
    } catch (SearchIndexException e) {
      throw new RuntimeException("search index exception", e);
    }
  }

  @Override
  public void upsertEvent(
          final LtiFileUpload file,
          final String captions,
          final String eventId,
          final String seriesId,
          final String metadataJson) throws UnauthorizedException, NotFoundException {
    if (eventId != null) {
      updateEvent(eventId, metadataJson);
      return;
    }
    if (workflow == null || workflowConfiguration == null) {
      throw new RuntimeException("No workflow configured, cannot upload");
    }
    try {
      MediaPackage mp = ingestService.createMediaPackage();
      if (mp == null) {
        throw new RuntimeException("Unable to create media package for event");
      }
      if (captions != null) {
        final MediaPackageElementFlavor captionsFlavor = new MediaPackageElementFlavor("vtt+en", "captions");
        final MediaPackageElementBuilder elementBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
        final MediaPackageElement captionsMpe = elementBuilder
                .newElement(MediaPackageElement.Type.Attachment, captionsFlavor);
        captionsMpe.setMimeType(mimeType("text", "vtt"));
        captionsMpe.addTag("lang:en");
        mp.add(captionsMpe);
        final URI captionsUri = workspace
                .put(
                        mp.getIdentifier().toString(),
                        captionsMpe.getIdentifier(),
                        "captions.vtt",
                        new ByteArrayInputStream(captions.getBytes(StandardCharsets.UTF_8)));
        captionsMpe.setURI(captionsUri);
      }

      JSONArray metadataJsonArray = (JSONArray) new JSONParser().parse(metadataJson);

      final EventCatalogUIAdapter adapter = getEventCatalogUIAdapter();
      final DublinCoreMetadataCollection collection = adapter.getRawFields();

      JSONArray collectionJsonArray = MetadataJson.extractSingleCollectionfromListJson(metadataJsonArray);
      MetadataJson.fillCollectionFromJson(collection, collectionJsonArray);

      replaceField(collection, "isPartOf", seriesId);
      adapter.storeFields(mp, collection);

      AccessControlList accessControlList = null;

      // If series is set and it's ACL is not empty, use series' ACL as default
      if (StringUtils.isNotBlank(seriesId)) {
        accessControlList = seriesService.getSeriesAccessControl(seriesId);
      }

      if (accessControlList == null || accessControlList.getEntries().isEmpty()) {
        accessControlList = new AccessControlList(
          new AccessControlEntry("ROLE_ADMIN", "write", true),
          new AccessControlEntry("ROLE_ADMIN", "read", true),
          new AccessControlEntry("ROLE_OAUTH_USER", "write", true),
          new AccessControlEntry("ROLE_OAUTH_USER", "read", true));
      }

      this.authorizationService.setAcl(mp, AclScope.Episode, accessControlList);
      mp = ingestService.addTrack(file.getStream(), file.getSourceName(), MediaPackageElements.PRESENTER_SOURCE, mp);

      final Map<String, String> configuration = gson.fromJson(workflowConfiguration, Map.class);
      configuration.put("workflowDefinitionId", workflow);
      ingestService.ingest(mp, workflow, configuration);
    } catch (Exception e) {
      throw new RuntimeException("unable to create event", e);
    }
  }

  private static Map<String, String> createCopyWorkflowConfig(final String seriesId, final String newMpId) {
    final Map<String, String> result = new HashMap<>();
    result.put("numberOfEvents", "1");
    result.put("noSuffix", "true");
    result.put("setSeriesId", seriesId);
    result.put(NEW_MP_ID_KEY, newMpId);
    return result;
  }

  @Override
  public void copyEventToSeries(final String eventId, final String seriesId) {
    final String workflowId = copyWorkflowId;
    try {
      final WorkflowDefinition wfd = workflowService.getWorkflowDefinitionById(workflowId);
      final Workflows workflows = new Workflows(assetManager, workflowService);
      final ConfiguredWorkflow workflow = workflow(wfd, createCopyWorkflowConfig(seriesId, UUID.randomUUID().toString()));
      final List<WorkflowInstance> workflowInstances = workflows
              .applyWorkflowToLatestVersion(Collections.singleton(eventId), workflow).toList();
      if (workflowInstances.isEmpty()) {
        throw new RuntimeException(String.format("Couldn't start workflow '%s' for event %s", workflowId, eventId));
      }
    } catch (WorkflowDatabaseException | NotFoundException e) {
      logger.error("Unable to get workflow definition {}", workflowId, e);
      throw new RuntimeException(e);
    }
  }

  private EventCatalogUIAdapter getEventCatalogUIAdapter() {
    final MediaPackageElementFlavor flavor = new MediaPackageElementFlavor("dublincore", "episode");
    final EventCatalogUIAdapter adapter = catalogUIAdapters.stream().filter(e -> e.getFlavor().equals(flavor)).findAny()
            .orElse(null);
    if (adapter == null) {
      throw new RuntimeException("no adapter found");
    }
    return adapter;
  }

  private void updateEvent(final String eventId, final String metadata)
          throws NotFoundException, UnauthorizedException {
    final EventCatalogUIAdapter adapter = getEventCatalogUIAdapter();
    final DublinCoreMetadataCollection collection = adapter.getRawFields();
    try {
      final MetadataList metadataList = new MetadataList();
      metadataList.add(adapter, collection);
      MetadataJson.fillListFromJson(metadataList, (JSONArray) new JSONParser().parse(metadata));
      this.indexService.updateEventMetadata(eventId, metadataList, searchIndex);
      republishMetadata(eventId);
    } catch (IndexServiceException | SearchIndexException | ParseException e) {
      throw new RuntimeException(e);
    }
  }

  private void republishMetadata(final String eventId) {
    final String workflowId = "republish-metadata";
    try {
      final WorkflowDefinition wfd = workflowService.getWorkflowDefinitionById(workflowId);
      final Workflows workflows = new Workflows(assetManager, workflowService);
      final ConfiguredWorkflow workflow = workflow(wfd, Collections.emptyMap());
      if (workflows.applyWorkflowToLatestVersion(Collections.singleton(eventId), workflow).isEmpty()) {
        throw new RuntimeException(String.format("couldn't start workflow '%s' for event %s", workflowId, eventId));
      }
    } catch (WorkflowDatabaseException | NotFoundException e) {
      logger.error("Unable to get workflow definition {}", workflowId, e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getEventMetadata(final String eventId) throws NotFoundException, UnauthorizedException {
    final Opt<Event> optEvent;
    try {
      optEvent = indexService.getEvent(eventId, searchIndex);
    } catch (SearchIndexException e) {
      throw new RuntimeException(e);
    }

    if (optEvent.isNone())
      throw new NotFoundException("cannot find event with id '" + eventId + "'");

    final Event event = optEvent.get();

    final MetadataList metadataList = new MetadataList();
    final List<EventCatalogUIAdapter> catalogUIAdapters = this.indexService.getEventCatalogUIAdapters();
    catalogUIAdapters.remove(this.indexService.getCommonEventCatalogUIAdapter());
    final MediaPackage mediaPackage;
    try {
      mediaPackage = this.indexService.getEventMediapackage(event);
    } catch (IndexServiceException e) {
      if (e.getCause() instanceof NotFoundException) {
        throw new NotFoundException("Cannot retrieve metadata for event with id '" + eventId + "'");
      } else if (e.getCause() instanceof UnauthorizedException) {
        throw new UnauthorizedException("Not authorized to access event with id '" + eventId + "'");
      }
      throw new RuntimeException(e);
    }
    for (EventCatalogUIAdapter catalogUIAdapter : catalogUIAdapters) {
      metadataList.add(catalogUIAdapter, catalogUIAdapter.getFields(mediaPackage));
    }

    final DublinCoreMetadataCollection metadataCollection;
    try {
      metadataCollection = EventUtils.getEventMetadata(event, this.indexService.getCommonEventCatalogUIAdapter());
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    metadataList.add(this.indexService.getCommonEventCatalogUIAdapter(), metadataCollection);

    final String wfState = event.getWorkflowState();
    if (wfState != null && WorkflowUtil.isActive(WorkflowInstance.WorkflowState.valueOf(wfState)))
      metadataList.setLocked(MetadataList.Locked.WORKFLOW_RUNNING);
    return new SimpleSerializer().toJson(MetadataJson.listToJson(metadataList, true));
  }

  @Override
  public String getNewEventMetadata() {
    final MetadataList metadataList = this.indexService.getMetadataListWithAllEventCatalogUIAdapters();
    final DublinCoreMetadataCollection collection = metadataList
            .getMetadataByAdapter(this.indexService.getCommonEventCatalogUIAdapter());
    if (collection != null) {
      if (collection.getOutputFields().containsKey(DublinCore.PROPERTY_CREATED.getLocalName()))
        collection.removeField(collection.getOutputFields().get(DublinCore.PROPERTY_CREATED.getLocalName()));
      if (collection.getOutputFields().containsKey("duration"))
        collection.removeField(collection.getOutputFields().get("duration"));
      if (collection.getOutputFields().containsKey(DublinCore.PROPERTY_IDENTIFIER.getLocalName()))
        collection.removeField(collection.getOutputFields().get(DublinCore.PROPERTY_IDENTIFIER.getLocalName()));
      if (collection.getOutputFields().containsKey(DublinCore.PROPERTY_SOURCE.getLocalName()))
        collection.removeField(collection.getOutputFields().get(DublinCore.PROPERTY_SOURCE.getLocalName()));
      if (collection.getOutputFields().containsKey("startDate"))
        collection.removeField(collection.getOutputFields().get("startDate"));
      if (collection.getOutputFields().containsKey("startTime"))
        collection.removeField(collection.getOutputFields().get("startTime"));
      if (collection.getOutputFields().containsKey("location"))
        collection.removeField(collection.getOutputFields().get("location"));

      if (collection.getOutputFields().containsKey(DublinCore.PROPERTY_PUBLISHER.getLocalName())) {
        final MetadataField publisher = collection.getOutputFields().get(DublinCore.PROPERTY_PUBLISHER.getLocalName());
        final Map<String, String> users = publisher.getCollection() == null ? new HashMap<>() : publisher.getCollection();
        final String loggedInUser = this.securityService.getUser().getName();
        if (!users.containsKey(loggedInUser)) {
          users.put(loggedInUser, loggedInUser);
        }
        publisher.setValue(loggedInUser);
      }

      metadataList.add(this.indexService.getCommonEventCatalogUIAdapter(), collection);
    }
    return new SimpleSerializer().toJson(MetadataJson.listToJson(metadataList, true));
  }

  @Override
  public void setEventMetadataJson(final String eventId, final String metadataJson)
          throws NotFoundException, UnauthorizedException {
    try {
      this.indexService.updateAllEventMetadata(eventId, metadataJson, this.searchIndex);
    } catch (IndexServiceException | SearchIndexException e) {
      throw new RuntimeException(e);
    }
  }

  private void replaceField(DublinCoreMetadataCollection collection, String fieldName, String fieldValue) {
    final MetadataField field = collection.getOutputFields().get(fieldName);
    collection.removeField(field);
    collection.addField(MetadataJson.copyWithDifferentJsonValue(field, fieldValue));
  }

  @Override
  public void delete(String id) {
    try {
      final Opt<Event> event = indexService.getEvent(id, searchIndex);
      if (event.isNone()) {
        throw new RuntimeException("Event '" + id + "' not found");
      }
      final IndexService.EventRemovalResult eventRemovalResult = indexService.removeEvent(event.get(), () -> {
      }, retractWorkflowId);
      if (eventRemovalResult == IndexService.EventRemovalResult.GENERAL_FAILURE) {
        throw new RuntimeException("Error deleting event: " + eventRemovalResult);
      }
    } catch (WorkflowDatabaseException | SearchIndexException | UnauthorizedException | NotFoundException e) {
      throw new RuntimeException("Error deleting event", e);
    }
  }

  /** OSGi callback if properties file is present */
  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    // Ensure properties is not null
    if (properties == null) {
      throw new IllegalArgumentException("No configuration specified for events endpoint");
    }
    String workflowStr = (String) properties.get("workflow");
    if (workflowStr == null) {
      throw new IllegalArgumentException("Configuration is missing 'workflow' parameter");
    }
    String workflowConfigurationStr = (String) properties.get("workflow-configuration");
    if (workflowConfigurationStr == null) {
      throw new IllegalArgumentException("Configuration is missing 'workflow-configuration' parameter");
    }
    try {
      gson.fromJson(workflowConfigurationStr, Map.class);
      workflowConfiguration = workflowConfigurationStr;
      workflow = workflowStr;
      this.retractWorkflowId = Objects.toString(properties.get("retract-workflow-id"), "retract");
      this.copyWorkflowId = Objects.toString(properties.get("copy-workflow-id"), "copy-event-to-series");
    } catch (JsonSyntaxException e) {
      throw new IllegalArgumentException("Invalid JSON specified for workflow configuration");
    }
  }
}
