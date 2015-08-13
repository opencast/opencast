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
import org.opencastproject.comments.events.EventCommentService;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.Log;
import org.opencastproject.util.OsgiUtil;
import org.opencastproject.workflow.api.WorkflowService;

import com.entwinemedia.fn.data.Opt;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;

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
  private ParticipationManagementDatabase participationManagementDatabase;
  private SchedulerService schedulerService;
  private SecurityService securityService;
  private WorkflowService workflowService;
  private AdminUIConfiguration adminUIConfiguration;

  private long expireSeconds = DEFAULT_URL_SIGNING_EXPIRE_DURATION;

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
  public ParticipationManagementDatabase getPMPersistence() {
    return participationManagementDatabase;
  }

  /** OSGi DI. */
  public void setParticipationPersistence(ParticipationManagementDatabase participationManagementDatabase) {
    this.participationManagementDatabase = participationManagementDatabase;
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
    }
  }

}
