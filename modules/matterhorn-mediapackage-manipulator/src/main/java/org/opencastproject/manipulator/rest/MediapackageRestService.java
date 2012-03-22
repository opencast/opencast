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
package org.opencastproject.manipulator.rest;

import java.net.URI;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.Catalog;
import org.opencastproject.mediapackage.CatalogImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.util.doc.rest.RestService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
@RestService(abstractText = "",
name = "mediapackage",
notes = {
  "All paths above are relative to the REST endpoint base (something like http://your.server/mediapackage)",
  "This Service doesn't manipulate data in the storage but simply changes the given XML data"
},
title = "Mediapackage manipulator RestService")
public class MediapackageRestService {

  private MediaPackageBuilderFactory factory;
  private static final Logger logger = LoggerFactory.getLogger(MediapackageRestService.class);
  private static final String REQUESTFIELD_MEDIAPACKAGE = "mediapackage";
  private static final String REQUESTFIELD_TRACK_URI = "trackUri";
  private static final String REQUESTFIELD_FLAVOR = "flavor";
  private static final String REQUESTFIELD_CONTRIBUTOR = "contributor";
  private static final String REQUESTFIELD_MIME_TYPE = "mimeType";
  private static final String REQUESTFIELD_TRACK_ID = "trackId";
  private static final String REQUESTFIELD_ATTACHMENT_ID = "attachmentId";
  private static final String REQUESTFIELD_ATTACHMENT_URI = "attachmentUri";
  private static final String REQUESTFIELD_CATALOG_URI = "catalogUri";
  private static final String REQUESTFIELD_CATALOG_ID = "catalogId";
  private static final String REQUESTFIELD_CREATOR = "creator";
  private static final String REQUESTFIELD_SUBJECT = "subject";

  @POST
  @Path("addTrack")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "Add a track to a mediapackage",
  name = "addTrack",
  returnDescription = "Returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "Mediapackage was manipulated successfully", responseCode = 200),
    @RestResponse(description = "something went wrong", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage to change", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the URI to the new track", isRequired = false, name = REQUESTFIELD_TRACK_URI, type = RestParameter.Type.STRING),
    @RestParameter(description = "the flavor of the track", isRequired = false, name = REQUESTFIELD_FLAVOR, type = RestParameter.Type.STRING)
  })
  public Response addTrack(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                           @FormParam(REQUESTFIELD_TRACK_URI) String trackUri,
                           @FormParam(REQUESTFIELD_FLAVOR) String flavor) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);
      MediaPackageElementFlavor flav = new MediaPackageElementFlavor(flavor.split("/")[0], flavor.split("/")[1]);
      URI uri = new URI(trackUri);
      mp.add(uri, MediaPackageElement.Type.Track, flav);
      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("removeTrack")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "remove a track from the given Mediapackage",
  name = "removeTrack",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the id of the track to remove", isRequired = false, name = REQUESTFIELD_TRACK_ID, type = RestParameter.Type.STRING)
  })
  public Response removeTrack(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                              @FormParam(REQUESTFIELD_TRACK_ID) String trackId) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);
      mp.remove(mp.getTrack(trackId));
      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("new")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "create a new mediapackage",
  name = "new",
  reponses = {
    @RestResponse(description = "if mediapackage was created successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  returnDescription = "Returns the new mediapackage or a 500 error")
  public Response getNew() {
    try {
      MediaPackage mp = factory.newMediaPackageBuilder().createNew();
      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("addContributor")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "Adds a contributor to the given Mediapackage",
  name = "addContributor",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the new contributor to add", isRequired = false, name = REQUESTFIELD_CONTRIBUTOR, type = RestParameter.Type.STRING)
  })
  public Response addContributor(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                                 @FormParam(REQUESTFIELD_CONTRIBUTOR) String contributor) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);
      mp.addContributor(contributor);
      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("removeContributor")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "remove a contributor from the given Mediapackage",
  name = "removeContributor",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the contributor to remove", isRequired = false, name = REQUESTFIELD_CONTRIBUTOR, type = RestParameter.Type.STRING)
  })
  public Response removeContributor(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                                    @FormParam(REQUESTFIELD_CONTRIBUTOR) String contributor) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);
      mp.removeContributor(contributor);
      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("addAttachment")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "Adds an attachment to the given Mediapackage",
  name = "addAttachment",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the flavor of the attachment", isRequired = false, name = REQUESTFIELD_FLAVOR, type = RestParameter.Type.STRING),
    @RestParameter(description = "the mimetype of the new attachment", isRequired = false, name = REQUESTFIELD_MIME_TYPE, type = RestParameter.Type.STRING),
    @RestParameter(description = "the uri to the new attachment", isRequired = false, name = REQUESTFIELD_ATTACHMENT_URI, type = RestParameter.Type.STRING)
  })
  public Response addAttachment(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                                @FormParam(REQUESTFIELD_FLAVOR) String flavor,
                                @FormParam(REQUESTFIELD_MIME_TYPE) String mimeType,
                                @FormParam(REQUESTFIELD_ATTACHMENT_URI) String attachmentUri) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);
      String[] flavors = flavor.split("/");
      String[] types = mimeType.split("/");

      Attachment a = new AttachmentImpl();
      a.setFlavor(new MediaPackageElementFlavor(flavors[0], flavors[1]));
      a.setMimeType(new MimeType(types[0], types[1]));
      a.setURI(new URI(attachmentUri));

      mp.add(a);
      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("removeAttachment")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "remove an attachment from the given Mediapackage",
  name = "removeAttachment",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the id of the attachment to remove", isRequired = false, name = REQUESTFIELD_ATTACHMENT_ID, type = RestParameter.Type.STRING)
  })
  public Response removeAttachment(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                                   @FormParam(REQUESTFIELD_ATTACHMENT_ID) String attachmentId) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);
      mp.remove(mp.getAttachment(attachmentId));
      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("addCatalog")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "add a catalog to the given Mediapackage",
  name = "addCatalog",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the uri to the catalog", isRequired = false, name = REQUESTFIELD_CATALOG_URI, type = RestParameter.Type.STRING)
  })
  public Response addCatalog(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                             @FormParam(REQUESTFIELD_CATALOG_URI) String catalogUri) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);

      Catalog c = CatalogImpl.fromURI(new URI(catalogUri));

      mp.add(c);
      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("removeCatalog")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "remove a catalog from the given Mediapackage",
  name = "removeCatalog",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the id of the catalog", isRequired = false, name = REQUESTFIELD_CATALOG_ID, type = RestParameter.Type.STRING)
  })
  public Response removeCatalog(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                                @FormParam(REQUESTFIELD_CATALOG_ID) String catalogId) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);
      mp.remove(mp.getCatalog(catalogId));

      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("addCreator")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "add a creator to the given Mediapackage",
  name = "addCreator",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the creator to add", isRequired = false, name = REQUESTFIELD_CREATOR, type = RestParameter.Type.STRING)
  })
  public Response addCreator(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                             @FormParam(REQUESTFIELD_CREATOR) String creator) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);

      mp.addCreator(creator);

      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("removeCreator")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "remove a creator from the given Mediapackage",
  name = "removeCreator",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the creator to remove", isRequired = false, name = REQUESTFIELD_CREATOR, type = RestParameter.Type.STRING)
  })
  public Response removeCreator(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                                @FormParam(REQUESTFIELD_CREATOR) String creator) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);

      mp.removeCreator(creator);

      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("addSubject")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "add a subject to the given Mediapackage",
  name = "addSubject",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the subject to add", isRequired = false, name = REQUESTFIELD_SUBJECT, type = RestParameter.Type.STRING)
  })
  public Response addSubject(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                             @FormParam(REQUESTFIELD_SUBJECT) String subject) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);

      mp.addSubject(subject);

      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  @POST
  @Path("removeSubject")
  @Produces(MediaType.APPLICATION_XML)
  @RestQuery(description = "remove a subject from the given Mediapackage",
  name = "removeSubject",
  returnDescription = "returns the new Mediapackage",
  reponses = {
    @RestResponse(description = "mediapackage was updated successfully", responseCode = 200),
    @RestResponse(description = "if an error occurred", responseCode = 500)
  },
  restParameters = {
    @RestParameter(description = "the mediapackage", isRequired = false, name = REQUESTFIELD_MEDIAPACKAGE, type = RestParameter.Type.TEXT),
    @RestParameter(description = "the subject to remove", isRequired = false, name = REQUESTFIELD_SUBJECT, type = RestParameter.Type.STRING)
  })
  public Response removeSubject(@FormParam(REQUESTFIELD_MEDIAPACKAGE) String mediapackage,
                                @FormParam(REQUESTFIELD_SUBJECT) String subject) {
    try {
      MediaPackage mp = getMediaPackage(mediapackage);

      mp.removeSubject(subject);
      
      return Response.ok(mp).build();
    } catch (MediaPackageException ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      return Response.status(500).build();
    }
  }

  protected void activate(ComponentContext cc) throws Exception {
    factory = MediaPackageBuilderFactory.newInstance();
  }

  private MediaPackage getMediaPackage(String mediapackage) throws MediaPackageException {
    return factory.newMediaPackageBuilder().loadFromXml(mediapackage);
  }
}
