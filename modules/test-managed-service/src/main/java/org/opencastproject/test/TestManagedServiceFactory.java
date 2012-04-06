package org.opencastproject.test;

import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static org.opencastproject.util.data.Collections.dict;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.osgi.framework.Constants.SERVICE_PID;

/**
 * This class is to test the life cycle of a {@link ManagedServiceFactory}.
 * <p/>
 * The managed service gets updated each time a configuration object (file) in <code>$FELIX_HOME/load/</code> is
 * either created, modified or deleted. The config files must be named
 * <pre>
 * managed-service-pid ( "-" name )? ".cfg"</code>
 * </pre>
 * <code>"name"</code> can be chosen arbitrarily and does not have any further meaning nor will it be used somewhere.
 */
public class TestManagedServiceFactory implements ManagedServiceFactory {

  private static final Logger logger = LoggerFactory.getLogger(TestManagedServiceFactory.class);

  private ComponentContext cc;
  private String pid;

  // keep track of the services created and registered by this factory
  private final Map<String, ServiceRegistration> services = new HashMap<String, ServiceRegistration>();

  /**
   * Synchronize with {@link #updated(String, java.util.Dictionary)}.
   */
  public synchronized void activate(final ComponentContext cc) {
    this.cc = cc;
    pid = (String) cc.getProperties().get(SERVICE_PID);
    if (pid == null)
      throw new IllegalStateException("No " + SERVICE_PID + " assigned. Please configure one in your component.xml");
    logger.info("Activate pid={}", pid);
  }

  public void deactivate() {
    logger.info("Deactivate");
  }

  @Override
  public String getName() {
    return pid;
  }

  /**
   * Synchronize with {@link #activate(org.osgi.service.component.ComponentContext)}.
   * <p/>
   * The <code>properties</code> dictionary contains the following additional keys set by
   * the OSGi container.
   * <ul>
   *   <li><code>service.factoryPid</code> PID of the factory, i.e. the PID of this class</li>
   *   <li><code>service.pid</code> repeats method parameter <code>pid</code></li>
   * </ul>
   */
  @Override
  public synchronized void updated(String pid, Dictionary properties) throws ConfigurationException {
    logger.info("Updated pid={}, properties={}", pid, Util.mkString(properties));
    registerService(pid, new TestService());
  }

  @Override
  public void deleted(String pid) {
    logger.info("Deleted {}", pid);
    unregisterService(pid);
  }

  public <A> void registerService(String pid, A service) {
    String serviceClass = service.getClass().getName();
    // set property "service.pid"
    Dictionary<String, String> serviceProps = dict(tuple(SERVICE_PID, pid));
    ServiceRegistration reg = cc.getBundleContext().registerService(serviceClass, service, serviceProps);
    services.put(pid, reg);
    logger.info("Registered service {} pid={}", serviceClass, pid);
  }

  public <A> void unregisterService(String pid) {
    ServiceRegistration reg = services.remove(pid);
    if (reg != null) {
      reg.unregister();
      logger.info("Unregistered service {}={}", SERVICE_PID, pid);
    }
  }
}
