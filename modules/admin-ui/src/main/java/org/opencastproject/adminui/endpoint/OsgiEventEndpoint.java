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
import org.opencastproject.adminui.index.AdminUISearchIndex;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil;
import org.opencastproject.workflow.api.WorkflowService;

import org.apache.commons.lang3.BooleanUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;
import java.util.Objects;

import javax.ws.rs.Path;

/** OSGi bound implementation. */
@Path("/")
public class OsgiEventEndpoint extends AbstractEventEndpoint implements ManagedService {

  private AclServiceFactory aclServiceFactory;
  private AssetManager assetManager;
  private AdminUISearchIndex index;
  private AuthorizationService authorizationService;
  private CaptureAgentStateService captureAgentStateService;
  private EventCommentService eventCommentService;
  private IndexService indexService;
  private JobEndpoint jobService;
  private SeriesEndpoint seriesEndpoint;
  private SchedulerService schedulerService;
  private SecurityService securityService;
  private UrlSigningService urlSigningService;
  private WorkflowService workflowService;
  private AdminUIConfiguration adminUIConfiguration;

  private long expireSeconds = UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION;
  private Boolean signWithClientIP = UrlSigningServiceOsgiUtil.DEFAULT_SIGN_WITH_CLIENT_IP;

  public static final String EVENTMODAL_ONLYSERIESWITHWRITEACCESS_KEY = "eventModal.onlySeriesWithWriteAccess";
  public static final String EVENTSTAB_ONLYEVENTSWITHWRITEACCESS_KEY = "eventsTab.onlyEventsWithWriteAccess";
  private Boolean onlySeriesWithWriteAccessEventModal = false;
  private Boolean onlyEventsWithWriteAccessEventsTab = false;

  @Override
  public AdminUIConfiguration getAdminUIConfiguration() {
    return adminUIConfiguration;
  }

  /** OSGi DI. */
  public void setAdminUIConfiguration(AdminUIConfiguration adminUIConfiguration) {
    this.adminUIConfiguration = adminUIConfiguration;
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
  public void setSeriesEndpoint(SeriesEndpoint seriesEndpoint) {
    this.seriesEndpoint = seriesEndpoint;
  }

  @Override
  public SeriesEndpoint getSeriesEndpoint() {
    return seriesEndpoint;
  }

  /** OSGi DI. */
  public void setWorkflowService(WorkflowService workflowService) {
    this.workflowService = workflowService;
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
  public AssetManager getAssetManager() {
    return assetManager;
  }

  /** OSGi DI. */
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
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
  public UrlSigningService getUrlSigningService() {
    return urlSigningService;
  }

  /** OSGi DI. */
  public void setUrlSigningService(UrlSigningService urlSigningService) {
    this.urlSigningService = urlSigningService;
  }

  @Override
  public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
    if (properties == null) {
      logger.info("No configuration available, using defaults");
      return;
    }

    expireSeconds = UrlSigningServiceOsgiUtil.getUpdatedSigningExpiration(properties, this.getClass().getSimpleName());
    signWithClientIP = UrlSigningServiceOsgiUtil.getUpdatedSignWithClientIP(properties,
            this.getClass().getSimpleName());

    Object dictionaryValue = properties.get(EVENTMODAL_ONLYSERIESWITHWRITEACCESS_KEY);
    onlySeriesWithWriteAccessEventModal = BooleanUtils.toBoolean(Objects.toString(dictionaryValue, "true"));

    dictionaryValue = properties.get(EVENTSTAB_ONLYEVENTSWITHWRITEACCESS_KEY);
    onlyEventsWithWriteAccessEventsTab = BooleanUtils.toBoolean(Objects.toString(dictionaryValue, "true"));
  }

  @Override
  public long getUrlSigningExpireDuration() {
    return expireSeconds;
  }

  @Override
  public Boolean signWithClientIP() {
    return signWithClientIP;
  }

  @Override
  public Boolean getOnlySeriesWithWriteAccessEventModal() {
    return onlySeriesWithWriteAccessEventModal;
  }

  @Override
  public Boolean getOnlyEventsWithWriteAccessEventsTab() {
    return onlyEventsWithWriteAccessEventsTab;
  }

}
