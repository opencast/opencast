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

import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.eventstatuschange.EventStatusChangeItem;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base Endpoint for endpoints which perform asynchronous work.
 */
public abstract class AsynchronousEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(AsynchronousEndpoint.class);

  private ExecutorService executorService;
  protected SecurityService securityService;
  private MessageSender messageSender;

  /** OSGi callback to set the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback to set the message sender. */
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  protected void activate(BundleContext bundleContext) {
    executorService = Executors.newCachedThreadPool(new NamingThreadFactory());
  }

  protected void deactivate(BundleContext bundleContext) {
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  /**
   * @param runnable The runnable to execute in the background
   */
  protected void submit(WorkStartingRunnable runnable) {
    runnable.setOrganization(securityService.getOrganization());
    runnable.setUser(securityService.getUser());
    executorService.submit(runnable);
  }

  protected void reportEventStatusChange(EventStatusChangeItem.Type type, String message, List<String> eventIds) {
    messageSender.sendObjectMessage(
      EventStatusChangeItem.EVENT_STATUS_CHANGE_QUEUE,
      MessageSender.DestinationType.Queue,
      new EventStatusChangeItem(type, eventIds, message));
  }

  /**
   * Runnable which is used to do intensive work in the background.
   */
  protected abstract class WorkStartingRunnable implements Runnable {

    private User user;
    private Organization organization;
    protected List<String> eventIds;

    /**
     * @param eventIds
     *          The id(s) of the event(s) to work on
     */
    protected WorkStartingRunnable(List<String> eventIds) {
      this.eventIds = eventIds;
    }

    private void setUser(User user) {
      this.user = user;
    }

    private void setOrganization(Organization organization) {
      this.organization = organization;
    }

    @Override
    public void run() {
      securityService.setUser(user);
      securityService.setOrganization(organization);
      reportEventStatusChange(EventStatusChangeItem.Type.Starting,"Starting task", eventIds);
      try {
        doWork();
      } catch (Throwable t) {
        logger.warn("An unhandled error occurred while starting a task asynchronously", t);
        reportEventStatusChange(EventStatusChangeItem.Type.Failed,"Unhandled error", eventIds);
      }
    }

    protected abstract void doWork();
  }

  private class NamingThreadFactory implements ThreadFactory {
    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable runnable) {
      final Thread result = new Thread(runnable);
      result.setName(AsynchronousEndpoint.this.getClass().getName() + "-executor-" + counter.getAndIncrement());
      return result;
    }
  }
}
