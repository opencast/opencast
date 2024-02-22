/*
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

package org.opencastproject.graphql.execution.context;

import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.User;

import org.osgi.framework.BundleContext;

import java.util.Map;
import java.util.Objects;

public class OpencastContext {

  private BundleContext bundleContext;

  private Map<String, Object> arguments;

  private Organization organization;

  private User user;

  public <T> T getService(Class<T> clazz) {
    return bundleContext.getService(bundleContext.getServiceReference(clazz));
  }

  public static OpencastContext newContext(BundleContext bundleContext) {
    var context = new OpencastContext();
    context.setBundleContext(bundleContext);
    return context;
  }

  protected void setBundleContext(BundleContext bundleContext) {
    Objects.requireNonNull(bundleContext, "OSGi bundle context cannot be null");
    this.bundleContext = bundleContext;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

}
