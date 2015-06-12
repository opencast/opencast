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

package org.opencastproject.adminui.endpoint;

import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.comments.events.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.events.EventCatalogUIAdapter;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn2;
import com.entwinemedia.fn.Stream;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import javax.ws.rs.Path;

/** OSGi bound implementation. */
@Path("/")
public class OsgiEventEndpoint extends AbstractEventEndpoint implements ManagedService {

  private static final String OPT_PREVIEW_SUBTYPE = "preview.subtype";
  private AclServiceFactory aclServiceFactory;
  private AdminUISearchIndex index;
  private AuthorizationService authorizationService;
  private CaptureAgentStateService captureAgentStateService;
  private CommonEventCatalogUIAdapter eventCatalogUIAdapter;
  private DublinCoreCatalogService dublinCoreCatalogService;
  private EventCommentService eventCommentService;
  private HttpMediaPackageElementProvider httpMediaPackageElementProvider;
  private IndexService indexService;
  private IngestService ingestService;
  private JobEndpoint jobService;
  private ListProvidersService listProviderService;
  private OpencastArchive archive;
  private ParticipationManagementDatabase participationManagementDatabase;
  private SchedulerService schedulerService;
  private SecurityService securityService;
  private SeriesService seriesService;
  private WorkflowService workflowService;
  private Workspace workspace;
  private String previewSubtype;

  private final List<EventCatalogUIAdapter> catalogUIAdapters = new ArrayList<EventCatalogUIAdapter>();

  @Override
  public OpencastArchive getArchive() {
    return archive;
  }

  /** OSGi DI. */
  public void setArchive(OpencastArchive archive) {
    this.archive = archive;
  }

  @Override
  public Workspace getWorkspace() {
    return workspace;
  }

  /** OSGi DI. */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Override
  public WorkflowService getWorkflowService() {
    return workflowService;
  }

  /** OSGi DI. */
  public void setJobService(JobEndpoint jobService) {
    this.jobService = jobService;
  }

  @Override
  public JobEndpoint getJobService() {
    return jobService;
  }

  /** OSGi DI. */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @Override
  public HttpMediaPackageElementProvider getHttpMediaPackageElementProvider() {
    return httpMediaPackageElementProvider;
  }

  /** OSGi DI. */
  public void setHttpMediaPackageElementProvider(HttpMediaPackageElementProvider httpMediaPackageElementProvider) {
    this.httpMediaPackageElementProvider = httpMediaPackageElementProvider;
  }

  @Override
  public ListProvidersService getListProviderService() {
    return listProviderService;
  }

  /** OSGi DI. */
  public void setListProviderService(ListProvidersService listProviderService) {
    this.listProviderService = listProviderService;
  }

  @Override
  public AclService getAclService() {
    return aclServiceFactory.serviceFor(securityService.getOrganization());
  }

  /** OSGi DI. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  @Override
  public SeriesService getSeriesService() {
    return this.seriesService;
  }

  /** OSGi DI. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  @Override
  public ParticipationManagementDatabase getPMPersistence() {
    return participationManagementDatabase;
  }

  /** OSGi DI. */
  public void setParticipationPersistence(ParticipationManagementDatabase participationManagementDatabase) {
    this.participationManagementDatabase = participationManagementDatabase;
  }

  @Override
  public DublinCoreCatalogService getDublinCoreService() {
    return dublinCoreCatalogService;
  }

  /** OSGi DI. */
  public void setDublinCoreCatalogService(DublinCoreCatalogService dublineCoreCatalogService) {
    this.dublinCoreCatalogService = dublineCoreCatalogService;
  }

  @Override
  public EventCommentService getEventCommentService() {
    return eventCommentService;
  }

  /** OSGi DI. */
  public void setEventCommentService(EventCommentService eventCommentService) {
    this.eventCommentService = eventCommentService;
  }

  @Override
  public SecurityService getSecurityService() {
    return securityService;
  }

  /** OSGi DI. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  public IngestService getIngestService() {
    return ingestService;
  }

  /** OSGi DI. */
  public void setIngestService(IngestService ingestService) {
    this.ingestService = ingestService;
  }

  @Override
  public IndexService getIndexService() {
    return indexService;
  }

  /** OSGi DI. */
  public void setIndexService(IndexService indexService) {
    this.indexService = indexService;
  }

  @Override
  public AuthorizationService getAuthorizationService() {
    return authorizationService;
  }

  /** OSGi DI. */
  public void setAuthorizationService(AuthorizationService authorizationService) {
    this.authorizationService = authorizationService;
  }

  @Override
  public SchedulerService getSchedulerService() {
    return schedulerService;
  }

  /** OSGi DI. */
  public void setSchedulerService(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  @Override
  public CaptureAgentStateService getCaptureAgentStateService() {
    return captureAgentStateService;
  }

  /** OSGi DI. */
  public void setCaptureAgentStateService(CaptureAgentStateService captureAgentStateService) {
    this.captureAgentStateService = captureAgentStateService;
  }

  @Override
  public AdminUISearchIndex getIndex() {
    return index;
  }

  /** OSGi DI. */
  public void setIndex(AdminUISearchIndex index) {
    this.index = index;
  }

  /** OSGi DI. */
  public void setCommonEventCatalogUIAdapter(CommonEventCatalogUIAdapter eventCatalogUIAdapter) {
    this.eventCatalogUIAdapter = eventCatalogUIAdapter;
  }

  @Override
  public EventCatalogUIAdapter getEpisodeCatalogUIAdapter() {
    return eventCatalogUIAdapter;
  }

  /** OSGi DI. */
  public void addCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.add(catalogUIAdapter);
  }

  /** OSGi DI. */
  public void removeCatalogUIAdapter(EventCatalogUIAdapter catalogUIAdapter) {
    catalogUIAdapters.remove(catalogUIAdapter);
  }

  @Override
  public List<EventCatalogUIAdapter> getEventCatalogUIAdapters(String organization) {
    return Stream.$(catalogUIAdapters).filter(organizationFilter._2(organization)).toList();
  }

  private static final Fn2<EventCatalogUIAdapter, String, Boolean> organizationFilter = new Fn2<EventCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean ap(EventCatalogUIAdapter catalogUIAdapter, String organization) {
      return organization.equals(catalogUIAdapter.getOrganization());
    }
  };

  @Override
  public String getPreviewSubtype() {
    return previewSubtype;
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    if (properties == null)
      return;

    // Preview subtype
    previewSubtype = StringUtils.trimToNull((String)properties.get(OPT_PREVIEW_SUBTYPE));
    if (previewSubtype != null)
      logger.info("Preview subtype is '{}'", previewSubtype);
    else
      logger.warn("No preview ubtype configured");
  }

}
