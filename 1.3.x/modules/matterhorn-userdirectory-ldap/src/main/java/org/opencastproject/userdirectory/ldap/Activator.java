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
package org.opencastproject.userdirectory.ldap;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.Hashtable;

/**
 * A factory that provides a new {@link LdapUserProviderInstance} for each
 * <code>org.opencastproject.userdirectory.ldap</code> configuration provided to the
 * {@link org.osgi.service.cm.ConfigurationAdmin} service.
 */
public class Activator implements BundleActivator {

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    Hashtable<String, String> properties = new Hashtable<String, String>();
    properties.put("service.pid", "org.opencastproject.userdirectory.ldap");
    properties.put("service.description", "Provides ldap user directory instances");
    context.registerService(ManagedServiceFactory.class.getName(), new LdapUserProviderFactory(context), properties);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    // Nothing to do, the services registered using this context will be unregistered automatically
  }

}
