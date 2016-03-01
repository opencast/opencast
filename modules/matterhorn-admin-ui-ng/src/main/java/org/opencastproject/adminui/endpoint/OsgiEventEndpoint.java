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

import org.opencastproject.adminui.impl.AdminUIConfiguration;
import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
<<<<<<< HEAD
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.Log;
import org.opencastproject.util.OsgiUtil;
=======
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.events.EventCatalogUIAdapter;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil;
import org.opencastproject.series.api.SeriesService;
>>>>>>> develop
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

<<<<<<< HEAD
import java.util.Dictionary;
=======
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
>>>>>>> develop

import javax.ws.rs.Path;

/** OSGi bound implementation. */
@Path("/")
public class OsgiEventEndpoint extends AbstractEventEndpoint implements ManagedService {

  private AclServiceFactory aclServiceFactory;
  private AdminUISearchIndex index;
  private AuthorizationService authorizationService;
  private CaptureAgentStateService captureAgentStateService;
  private EventCommentService eventCommentService;
  private HttpMediaPackageElementProvider httpMediaPackageElementProvider;
  private IndexService indexService;
  private JobEndpoint jobService;
  private OpencastArchive archive;
  private SchedulerService schedulerService;
  private SecurityService securityService;
<<<<<<< HEAD
=======
  private SeriesService seriesService;
  private UrlSigningService urlSigningService;
>>>>>>> develop
  private WorkflowService workflowService;
  private AdminUIConfiguration adminUIConfiguration;

<<<<<<< HEAD
  private long expireSeconds = DEFAULT_URL_SIGNING_EXPIRE_DURATION;
=======
  private final List<EventCatalogUIAdapter> catalogUIAdapters = new ArrayList<EventCatalogUIAdapter>();
  private long expireSeconds = UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION;
  private Boolean signWithClientIP = UrlSigningServiceOsgiUtil.DEFAULT_SIGN_WITH_CLIENT_IP;
>>>>>>> develop

  @Override
  public AdminUIConfiguration getAdminUIConfiguration() {
    return adminUIConfiguration;
  }

  /** OSGi DI. */
  public void setAdminUIConfiguration(AdminUIConfiguration adminUIConfiguration) {
    this.adminUIConfiguration = adminUIConfiguration;
  }


  @Override
  public OpencastArchive getArchive() {
    return archive;
  }

  /** OSGi DI. */
  public void setArchive(OpencastArchive archive) {
    this.archive = archive;
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
  public AclService getAclService() {
    return aclServiceFactory.serviceFor(securityService.getOrganization());
  }

  /** OSGi DI. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

  @Override
<<<<<<< HEAD
  public ParticipationManagementDatabase getPMPersistence() {
    return participationManagementDatabase;
  }

  /** OSGi DI. */
  public void setParticipationPersistence(ParticipationManagementDatabase participationManagementDatabase) {
    this.participationManagementDatabase = participationManagementDatabase;
=======
  public SeriesService getSeriesService() {
    return this.seriesService;
  }

  /** OSGi DI. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  @Override
  public DublinCoreCatalogService getDublinCoreService() {
    return dublinCoreCatalogService;
  }

  /** OSGi DI. */
  public void setDublinCoreCatalogService(DublinCoreCatalogService dublineCoreCatalogService) {
    this.dublinCoreCatalogService = dublineCoreCatalogService;
>>>>>>> develop
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

  @Override
<<<<<<< HEAD
  public void updated(@SuppressWarnings("rawtypes") Dictionary properties) throws ConfigurationException {
    Opt<Long> expiration = OsgiUtil.getOptCfg(properties, URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY).toOpt()
            .map(com.entwinemedia.fn.fns.Strings.toLongF);
    if (expiration.isSome()) {
      expireSeconds = expiration.get();
      logger.info("The property {} has been configured to expire signed URLs in {}.",
              URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY, Log.getHumanReadableTimeString(expireSeconds));
    } else {
      expireSeconds = DEFAULT_URL_SIGNING_EXPIRE_DURATION;
      logger.info(
              "The property {} has not been configured, so the default is being used to expire signed URLs in {}.",
              URL_SIGNING_EXPIRES_DURATION_SECONDS_KEY, Log.getHumanReadableTimeString(expireSeconds));
=======
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

  @Override
  public UrlSigningService getUrlSigningService() {
    return urlSigningService;
  }

  /** OSGi DI. */
  public void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  private static final Fn2<EventCatalogUIAdapter, String, Boolean> organizationFilter = new Fn2<EventCatalogUIAdapter, String, Boolean>() {
    @Override
    public Boolean ap(EventCatalogUIAdapter catalogUIAdapter, String organization) {
      return organization.equals(catalogUIAdapter.getOrganization());
>>>>>>> develop
    }
  }

  @Override
  public void updated(Dictionary properties) throws ConfigurationException {
    expireSeconds = UrlSigningServiceOsgiUtil.getUpdatedSigningExpiration(properties, this.getClass().getSimpleName());
    signWithClientIP = UrlSigningServiceOsgiUtil.getUpdatedSignWithClientIP(properties,
            this.getClass().getSimpleName());
  }

  @Override
  public long getUrlSigningExpireDuration() {
   return expireSeconds;
  }

  @Override
  public Boolean signWithClientIP() {
   return signWithClientIP;
  }

}
