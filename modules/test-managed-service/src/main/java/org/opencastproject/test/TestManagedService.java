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

package org.opencastproject.test;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

/**
 * This class is to test the life cycle of a {@link ManagedService}.
 * <p/>
 * A managed service can be configured by a set of properties typically
 * being maintained in a Java properties file located under
 * <code>$FELIX_HOME/conf/services/org.opencastproject.test.TestManagedService.properties</code>.
 * The name of the config file is derived from the PID of {@link TestManagedService} which is
 * set in the component declaration xml <code>OSGI-INF/test-managed-service.xml</code> like this
 * <code>&lt;property name="service.pid" value="org.opencastproject.test.TestManagedService"/&gt;</code>.
 * <p/>
 * A config file in <code>$FELIX_HOME/load/org.opencastproject.test.TestManagedService.cfg</code> has
 * precedence over the one in <code>$FELIX_HOME/conf/services</code> causing it to be ignored completely.
 * <p/>
 * If you provide config files named like
 * <pre>
 * $FELIX_HOME/load/org.opencastproject.test.TestManagedService-&lt;name&gt;.cfg
 * </pre>
 * you are able to spawn multiple instances of this managed service. This approach may have the advantage
 * over using a {@link org.osgi.service.cm.ManagedServiceFactory} to create and register multiple instances
 * of a service in that you can use declarative service (DS) configuration to manage the dependencies of
 * these services. Using a ManagedServiceFactory where you have to create and register the services
 * on your own you're also responsible for setting the dependencies.
 */
public class TestManagedService implements ManagedService {

  private static final Logger logger = LoggerFactory.getLogger(TestManagedService.class);

  /**
   * Synchronize with {@link #updated(java.util.Dictionary)}.
   */
  public synchronized void activate(final ComponentContext cc) {
    logger.info("Activate " + hashCode());
  }

  public void deactivate() {
    logger.info("Deactivate " + hashCode());
  }

  /**
   * Synchronize with {@link #activate(org.osgi.service.component.ComponentContext)}.
   */
  @Override
  public synchronized void updated(final Dictionary properties) throws ConfigurationException {
    logger.info("Updated " + hashCode() + ", " + Util.mkString(properties));
  }
}
