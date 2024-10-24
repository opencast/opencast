
@Capability(
    name = JAX_RS_WHITEBOARD_IMPLEMENTATION,
    namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE,
    version = JAX_RS_WHITEBOARD_SPECIFICATION_VERSION,
    uses = {
        javax.ws.rs.Path.class,
        javax.ws.rs.core.MediaType.class,
        javax.ws.rs.ext.Provider.class,
        javax.ws.rs.client.Entity.class,
        javax.ws.rs.container.PreMatching.class,
        javax.ws.rs.sse.Sse.class,
        org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.class
    }
)
package org.opencastproject.kernel;

import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_IMPLEMENTATION;
import static org.osgi.service.jaxrs.whiteboard.JaxrsWhiteboardConstants.JAX_RS_WHITEBOARD_SPECIFICATION_VERSION;

import org.osgi.annotation.bundle.Capability;
import org.osgi.namespace.implementation.ImplementationNamespace;