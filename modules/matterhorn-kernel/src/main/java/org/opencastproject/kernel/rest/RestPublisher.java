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
package org.opencastproject.kernel.rest;

import org.opencastproject.rest.RestConstants;
import org.opencastproject.rest.StaticResource;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.util.NotFoundException;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.JSONProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.apache.http.HttpStatus;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Listens for JAX-RS annotated services and publishes them to the global URL space using a single shared HttpContext.
 */
public class RestPublisher implements RestConstants {

  /** The logger **/
  protected static final Logger logger = LoggerFactory.getLogger(RestPublisher.class);

  /** The rest publisher looks for any non-servlet with the 'opencast.service.path' property */
  public static final String JAX_RS_SERVICE_FILTER = "(&(!(objectClass=javax.servlet.Servlet))("
          + SERVICE_PATH_PROPERTY + "=*))";

  /** A map that sets default xml namespaces in {@link XMLStreamWriter}s */
  protected static final ConcurrentHashMap<String, String> NAMESPACE_MAP;

  /** The 404 Error page */
  protected String fourOhFour = null;

  @SuppressWarnings("unchecked")
  protected List providers = null;

  static {
    NAMESPACE_MAP = new ConcurrentHashMap<String, String>();
    NAMESPACE_MAP.put("http://www.w3.org/2001/XMLSchema-instance", "");
  }

  /** The rest publisher's OSGI declarative services component context */
  protected ComponentContext componentContext;

  /** A service tracker that monitors JAX-RS annotated services, (un)publishing servlets as they (dis)appear */
  protected ServiceTracker jaxRsTracker = null;

  /**
   * A bundle tracker that registers StaticResource servlets for bundles with the right headers.
   */
  protected BundleTracker bundleTracker = null;

  /** The base URL for this server */
  protected String baseServerUri;

  /** Holds references to servlets that this class publishes, so they can be unpublished later */
  protected Map<String, ServiceRegistration> servletRegistrationMap;

  /** Activates this rest publisher */
  @SuppressWarnings("unchecked")
  protected void activate(ComponentContext componentContext) {
    logger.debug("activate()");
    this.baseServerUri = componentContext.getBundleContext().getProperty("org.opencastproject.server.url");
    this.componentContext = componentContext;
    this.fourOhFour = "The resource you requested does not exist."; // TODO: Replace this with something a little nicer
    this.servletRegistrationMap = new ConcurrentHashMap<String, ServiceRegistration>();
    this.providers = new ArrayList();

    JSONProvider jsonProvider = new MatterhornJSONProvider();
    jsonProvider.setIgnoreNamespaces(true);
    jsonProvider.setNamespaceMap(NAMESPACE_MAP);

    providers.add(jsonProvider);
    providers.add(new ExceptionMapper<NotFoundException>() {
      public Response toResponse(NotFoundException e) {
        return Response.status(404).entity(fourOhFour).type(MediaType.TEXT_PLAIN).build();
      }
    });
    providers.add(new ExceptionMapper<UnauthorizedException>() {
      public Response toResponse(UnauthorizedException e) {
        return Response.status(HttpStatus.SC_UNAUTHORIZED).entity("unauthorized").type(MediaType.TEXT_PLAIN).build();
      };
    });
    providers.add(new RestDocRedirector());

    try {
      jaxRsTracker = new JaxRsServiceTracker();
      bundleTracker = new StaticResourceBundleTracker(componentContext.getBundleContext());
    } catch (InvalidSyntaxException e) {
      throw new IllegalStateException(e);
    }
    jaxRsTracker.open();
    bundleTracker.open();
  }

  /**
   * Deactivates the rest publisher
   */
  protected void deactivate() {
    logger.debug("deactivate()");
    jaxRsTracker.close();
    bundleTracker.close();
  }

  /**
   * Creates a REST endpoint for the JAX-RS annotated service.
   *
   * @param ref
   *          the osgi service reference
   * @param service
   *          The service itself
   */
  @SuppressWarnings("unchecked")
  protected void createEndpoint(ServiceReference ref, Object service) {
    RestServlet cxf = new RestServlet();
    ServiceRegistration reg = null;
    String serviceType = (String) ref.getProperty(SERVICE_TYPE_PROPERTY);
    String servicePath = (String) ref.getProperty(SERVICE_PATH_PROPERTY);
    boolean jobProducer = Boolean.parseBoolean((String) ref.getProperty(SERVICE_JOBPRODUCER_PROPERTY));
    try {
      Dictionary<String, Object> props = new Hashtable<String, Object>();
      props.put("contextId", RestConstants.HTTP_CONTEXT_ID);
      props.put("alias", servicePath);
      props.put(SERVICE_TYPE_PROPERTY, serviceType);
      props.put(SERVICE_PATH_PROPERTY, servicePath);
      props.put(SERVICE_JOBPRODUCER_PROPERTY, jobProducer);
      reg = componentContext.getBundleContext().registerService(Servlet.class.getName(), cxf, props);
    } catch (Exception e) {
      logger.info("Problem registering REST endpoint {} : {}", servicePath, e.getMessage());
      return;
    }
    servletRegistrationMap.put(servicePath, reg);

    // Wait for the servlet to be initialized as long as one minute. Since the servlet is published via the whiteboard,
    // this may happen asynchronously. However, after 30 seconds we expect the HTTP service and the whiteboard
    // implementation to be loaded and active.
    int count = 0;
    while (!cxf.isInitialized() && count < 300) {
      logger.debug("Waiting for the servlet at '{}' to be initialized", servicePath);
      try {
        Thread.sleep(100);
        count ++;
      } catch (InterruptedException e) {
        logger.warn("Interrupt while waiting for RestServlet initialization");
        break;
      }
    }

    // Was initialization successful
    if (!cxf.isInitialized()) {
      logger.error("Whiteboard implemenation failed to pick up REST endpoint declaration {}", serviceType);
      return;
    }

    // Set up cxf
    Bus bus = cxf.getBus();
    JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
    factory.setBus(bus);
    factory.setProviders(providers);

    // Set the service class
    factory.setServiceClass(service.getClass());
    factory.setResourceProvider(service.getClass(), new SingletonResourceProvider(service));

    // Set the address to '/', which will force the use of the http service
    factory.setAddress("/");

    // Use the cxf classloader itself to create the cxf server
    ClassLoader bundleClassLoader = Thread.currentThread().getContextClassLoader();
    ClassLoader delegateClassLoader = JAXRSServerFactoryBean.class.getClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(delegateClassLoader);
      factory.create();
    } finally {
      Thread.currentThread().setContextClassLoader(bundleClassLoader);
    }
    logger.info("Registered REST endpoint at " + servicePath);
  }

  /**
   * Removes an endpoint
   *
   * @param alias
   *          The URL space to reclaim
   */
  protected void destroyEndpoint(String alias) {
    ServiceRegistration reg = servletRegistrationMap.remove(alias);
    if (reg != null) {
      reg.unregister();
    }
  }

  /**
   * Extends the CXF JSONProvider for the grand purpose of removing '@' symbols from json and padded jsonp.
   */
  protected static class MatterhornJSONProvider extends JSONProvider {
    private static final Charset UTF8 = Charset.forName("utf-8");

    /**
     * {@inheritDoc}
     *
     * @see org.apache.cxf.jaxrs.provider.JSONProvider#createWriter(java.lang.Object, java.lang.Class,
     *      java.lang.reflect.Type, java.lang.String, java.io.OutputStream, boolean)
     */
    protected XMLStreamWriter createWriter(Object actualObject, Class<?> actualClass, Type genericType, String enc,
            OutputStream os, boolean isCollection) throws Exception {
      Configuration c = new Configuration(NAMESPACE_MAP);
      c.setSupressAtAttributes(true);
      MappedNamespaceConvention convention = new MappedNamespaceConvention(c);
      return new MappedXMLStreamWriter(convention, new OutputStreamWriter(os, UTF8)) {
        @Override
        public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void writeStartElement(String uri, String local) throws XMLStreamException {
          super.writeStartElement("", local, "");
        }

        @Override
        public void setPrefix(String pfx, String uri) throws XMLStreamException {
        }

        @Override
        public void setDefaultNamespace(String uri) throws XMLStreamException {
        }
      };
    }
  }

  /**
   * A custom ServiceTracker that published JAX-RS annotated services with the {@link RestPublisher#SERVICE_PROPERTY}
   * property set to some non-null value.
   */
  public class JaxRsServiceTracker extends ServiceTracker {

    JaxRsServiceTracker() throws InvalidSyntaxException {
      super(componentContext.getBundleContext(), componentContext.getBundleContext()
              .createFilter(JAX_RS_SERVICE_FILTER), null);
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
      String servicePath = (String) reference.getProperty(SERVICE_PATH_PROPERTY);
      destroyEndpoint(servicePath);
      super.removedService(reference, service);
    }

    @Override
    public Object addingService(ServiceReference reference) {
      Object service = componentContext.getBundleContext().getService(reference);
      if (service == null) {
        logger.info("JAX-RS service {} has not been instantiated yet, or has already been unregistered. Skipping "
                + "endpoint creation.", reference);
      } else {
        Path pathAnnotation = service.getClass().getAnnotation(Path.class);
        if (pathAnnotation == null) {
          logger.warn("{} was registered with '{}={}', but the service is not annotated with the JAX-RS "
                  + "@Path annotation",
                  new Object[] { service, SERVICE_PATH_PROPERTY, reference.getProperty(SERVICE_PATH_PROPERTY) });
        } else {
          createEndpoint(reference, service);
        }
      }
      return super.addingService(reference);
    }
  }

  /**
   * A classloader that delegates to an OSGI bundle for loading resources.
   */
  class StaticResourceClassLoader extends ClassLoader {
    private Bundle bundle = null;

    public StaticResourceClassLoader(Bundle bundle) {
      super();
      this.bundle = bundle;
    }

    @Override
    public URL getResource(String name) {
      URL url = bundle.getResource(name);
      logger.debug("{} found resource {} from name {}", new Object[] { this, url, name });
      return url;
    }
  }

  /**
   * Tracks bundles containing static resources to be exposed via HTTP URLs.
   */
  class StaticResourceBundleTracker extends BundleTracker {

    /**
     * Creates a new StaticResourceBundleTracker.
     *
     * @param context
     *          the bundle context
     */
    StaticResourceBundleTracker(BundleContext context) {
      super(context, Bundle.ACTIVE, null);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.osgi.util.tracker.BundleTracker#addingBundle(org.osgi.framework.Bundle, org.osgi.framework.BundleEvent)
     */
    @Override
    public Object addingBundle(Bundle bundle, BundleEvent event) {
      String classpath = (String) bundle.getHeaders().get(RestConstants.HTTP_CLASSPATH);
      String alias = (String) bundle.getHeaders().get(RestConstants.HTTP_ALIAS);
      String welcomeFile = (String) bundle.getHeaders().get(RestConstants.HTTP_WELCOME);

      if (classpath != null && alias != null) {
        Dictionary<String, String> props = new Hashtable<String, String>();
        props.put("alias", alias);
        props.put("contextId", RestConstants.HTTP_CONTEXT_ID);

        StaticResource servlet = new StaticResource(new StaticResourceClassLoader(bundle), classpath, alias,
                welcomeFile);

        // We use the newly added bundle's context to register this service, so when that bundle shuts down, it brings
        // down this servlet with it
        logger.debug("Registering servlet with alias {}", alias);
        bundle.getBundleContext().registerService(Servlet.class.getName(), servlet, props);
      }

      return super.addingBundle(bundle, event);
    }
  }

  /**
   * An HttpServlet that uses a JAX-RS service to handle requests.
   */
  public class RestServlet extends CXFNonSpringServlet {
    /** Serialization UID */
    private static final long serialVersionUID = -8963338160276371426L;

    /** Whether this servlet has been initialized by the http service */
    private boolean initialized = false;

    /**
     * Whether the http service has initialized this servlet.
     *
     * @return the initialization state
     */
    public boolean isInitialized() {
      return initialized;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
      super.init(servletConfig);
      initialized = true;
    }
  }

  public class RestDocRedirector implements RequestHandler {
    /**
     * {@inheritDoc}
     *
     * @see org.apache.cxf.jaxrs.ext.RequestHandler#handleRequest(org.apache.cxf.message.Message,
     *      org.apache.cxf.jaxrs.model.ClassResourceInfo)
     */
    @Override
    public Response handleRequest(Message m, ClassResourceInfo resourceClass) {
      String uri = (String) m.get(Message.REQUEST_URI);
      if (uri.endsWith("/docs")) {
        String[] pathSegments = uri.split("/");
        String path = "";
        for (int i = 1; i < pathSegments.length - 1; i++) {
          path += "/" + pathSegments[i].replace("/", "");
        }
        return Response.status(Status.MOVED_PERMANENTLY).type(MediaType.TEXT_PLAIN)
                .header("Location", "/docs.html?path=" + path).build();
      }
      return null;
    }
  }
}
