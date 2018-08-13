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

package org.opencastproject.authorization.xacml;

import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_EPISODE;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_SERIES;
import static org.opencastproject.util.data.Collections.list;
import static org.opencastproject.util.data.Collections.mkString;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Prelude.unexhaustiveMatch;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.mediapackage.Attachment;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

/**
 * A XACML implementation of the {@link AuthorizationService}.
 */
public class XACMLAuthorizationService implements AuthorizationService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(XACMLAuthorizationService.class);

  /** The default filename for XACML attachments */
  public static final String XACML_FILENAME = "xacml.xml";

  /** The workspace */
  protected Workspace workspace;

  /** The security service */
  protected SecurityService securityService;

  /** The series service */
  protected SeriesService seriesService;

  @Override
  public Tuple<AccessControlList, AclScope> getActiveAcl(final MediaPackage mp) {
    logger.debug("getActiveACl for media package {}", mp.getIdentifier());
    return getAcl(mp, AclScope.Episode);
  }

  /** Returns an ACL based on a given file/inputstream. */
  public AccessControlList getAclFromInputStream(final InputStream in) throws IOException {
    logger.debug("Get ACL from inputstream");
    try {
      return XACMLUtils.parseXacml(in);
    } catch (XACMLParsingException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Tuple<AccessControlList, AclScope> getAcl(final MediaPackage mp, final AclScope scope) {
    List<Tuple<AclScope, MediaPackageElementFlavor>> scopes = new ArrayList<>(3);

    // Start with the requested scope but fall back to the less specific scope if it does not exist.
    // The order is: episode -> series -> general (deprecated) -> global
    if (AclScope.Episode.equals(scope)) {
      scopes.add(tuple(AclScope.Episode, XACML_POLICY_EPISODE));
    }
    if (AclScope.Episode.equals(scope) || AclScope.Series.equals(scope)) {
      scopes.add(tuple(AclScope.Series, XACML_POLICY_SERIES));
    }

    // hint: deprecated global flavor
    scopes.add(tuple(AclScope.Series, XACML_POLICY));

    for (Tuple<AclScope, MediaPackageElementFlavor> currentScope: scopes) {
      for (Attachment xacml : mp.getAttachments(currentScope.getB())) {
        Option<AccessControlList> acl = loadAcl(xacml.getURI());
        if (acl.isSome()) {
          return tuple(acl.get(), currentScope.getA());
        }
      }
    }

    logger.debug("Falling back to global default ACL");
    return tuple(new AccessControlList(), AclScope.Global);
  }

  @Override
  public Tuple<MediaPackage, Attachment> setAcl(final MediaPackage mp, final AclScope scope, final AccessControlList acl) {
    return withContextClassLoader(new Function0.X<Tuple<MediaPackage, Attachment>>() {
      @Override
      public Tuple<MediaPackage, Attachment> xapply() throws Exception {
        // Get XACML representation of these role + action tuples
        String xacmlContent;
        try {
          xacmlContent = XACMLUtils.getXacml(mp, acl);
        } catch (JAXBException e) {
          throw new MediaPackageException("Unable to generate xacml for mediapackage " + mp.getIdentifier());
        }

        // Remove the old xacml file(s)
        Attachment attachment = removeFromMediaPackageAndWorkspace(mp, toFlavors(scope)).getB();

        // add attachment
        final String elementId = toElementId(scope);
        URI uri;
        try (InputStream in = IOUtils.toInputStream(xacmlContent, "UTF-8")) {
          uri = workspace.put(mp.getIdentifier().toString(), elementId, XACML_FILENAME, in);
        } catch (IOException e) {
          throw new MediaPackageException("Error storing xacml for mediapackage " + mp.getIdentifier());
        }

        if (attachment == null) {
          attachment = (Attachment) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
                  .elementFromURI(uri, Attachment.TYPE, toFlavor(scope));
        }
        attachment.setURI(uri);
        attachment.setIdentifier(elementId);
        attachment.setMimeType(MimeTypes.XML);
        // setting the URI to a new source so the checksum will most like be invalid
        attachment.setChecksum(null);
        mp.add(attachment);

        logger.debug("Saved XACML as {}", uri);

        // return augmented mediapackage
        return tuple(mp, attachment);
      }
    });
  }

  @Override
  public MediaPackage removeAcl(MediaPackage mp, AclScope scope) {
    return removeFromMediaPackageAndWorkspace(mp, toFlavors(scope)).getA();
  }

  /** Apply function f within the context of the class loader of XACMLAuthorizationService. */
  private static <A> A withContextClassLoader(Function0<A> f) {
    Thread currentThread = Thread.currentThread();
    ClassLoader originalClassLoader = currentThread.getContextClassLoader();
    try {
      currentThread.setContextClassLoader(XACMLAuthorizationService.class.getClassLoader());
      return f.apply();
    } finally {
      Thread.currentThread().setContextClassLoader(originalClassLoader);
    }
  }

  /** Return an attachment of a given set of flavors only if there is exactly one. */
  private static Option<Attachment> getSingleAttachment(MediaPackage mp, List<MediaPackageElementFlavor> flavors) {
    final List<Attachment> as = getAttachments(mp, flavors);
    if (as.size() == 0) {
      logger.debug("No XACML attachment of type {} found in {}", mkString(flavors, ","), mp);
      return none();
    } else if (as.size() == 1) {
      return some(as.get(0));
    } else { // > 1
      logger.warn("More than one XACML attachment of type {} is attached to {}", mkString(flavors, ","), mp);
      return none();
    }
  }

  /** Return all attachments of the given flavors. */
  private static List<Attachment> getAttachments(MediaPackage mp, final List<MediaPackageElementFlavor> flavors) {
    return mlist(mp.getAttachments()).filter(new Function<Attachment, Boolean>() {
      @Override
      public Boolean apply(Attachment a) {
        return flavors.contains(a.getFlavor());
      }
    }).value();
  }

  /** Return the XACML attachment or none if the media package does not contain any XACMLs. */
  private Option<Attachment> getXacmlAttachment(MediaPackage mp) {
    Option<Attachment> attachment = getSingleAttachment(mp, list(XACML_POLICY_EPISODE));
    if (attachment.isNone()) {
      attachment = getSingleAttachment(mp, list(XACML_POLICY_SERIES, XACML_POLICY));
    }
    return attachment;
  }

  /**
   * Get <em>all</em> flavors associated with a scope. This method has to exist as long as the deprecated
   * {@link org.opencastproject.mediapackage.MediaPackageElements#XACML_POLICY} flavor exists.
   */
  private static List<MediaPackageElementFlavor> toFlavors(AclScope scope) {
    switch (scope) {
      case Episode:
        return list(XACML_POLICY_EPISODE);
      case Series:
        return list(XACML_POLICY_SERIES, XACML_POLICY);
      default:
        return list();
    }
  }

  /** Get the flavor associated with a scope. */
  private static MediaPackageElementFlavor toFlavor(AclScope scope) {
    switch (scope) {
      case Episode:
        return XACML_POLICY_EPISODE;
      case Series:
        return XACML_POLICY_SERIES;
      default:
        return unexhaustiveMatch();
    }
  }

  /** Get the element id associated with a scope. */
  private static String toElementId(AclScope scope) {
    switch (scope) {
      case Episode:
        return "security-policy-episode";
      case Series:
        return "security-policy-series";
      default:
        return unexhaustiveMatch();
    }
  }

  /**
   * Get a file from the workspace.
   *
   * @param uri
   *          The file uri
   * @return return the file if exists otherwise <code>null</code>
   */
  private File fromWorkspace(URI uri) {
    try {
      return workspace.get(uri, true);
    } catch (NotFoundException e) {
      logger.warn("XACML policy file not found '{}'.", uri);
      return null;
    } catch (IOException e) {
      logger.error("Unable to access XACML policy file. {}", uri, e);
      return null;
    }
  }

  /**
   * Remove all attachments of the given flavors from media package and workspace.
   *
   * @return the a tuple with the mutated (!) media package as A and the deleted Attachment as B
   */
  private Tuple<MediaPackage, Attachment> removeFromMediaPackageAndWorkspace(MediaPackage mp,
          List<MediaPackageElementFlavor> flavors) {
    Attachment attachment = null;
    for (Attachment a : getAttachments(mp, flavors)) {
      attachment = (Attachment) a.clone();
      try {
        workspace.delete(a.getURI());
      } catch (Exception e) {
        logger.warn("Unable to delete XACML file:", e);
      }
      mp.remove(a);
    }
    return Tuple.tuple(mp, attachment);
  }

  /** Load an ACL from the given URI. */
  private Option<AccessControlList> loadAcl(final URI uri) {
    logger.debug("Load Acl from {}", uri);
    try (InputStream is = workspace.read(uri)) {
      AccessControlList acl = XACMLUtils.parseXacml(is);
      if (acl != null) {
        return Option.option(acl);
      }
    } catch (NotFoundException e) {
      logger.debug("URI {} not found", uri);
    } catch (Exception e) {
      logger.warn("Unable to load or parse Acl", e);
    }
    return Option.none();
  }

  /**
   * {@inheritDoc}
   *
   * @see org.opencastproject.security.api.AuthorizationService#hasPolicy(org.opencastproject.mediapackage.MediaPackage)
   */
  @Override
  public boolean hasPolicy(MediaPackage mp) {
    return getXacmlAttachment(mp).isSome();
  }

  @Override
  public boolean hasPermission(final MediaPackage mp, final String action) {
    Option<Attachment> xacml = getXacmlAttachment(mp);
    if (xacml.isNone()) {
      logger.debug("No attached XACML. Denying access by default.");
      return false;
    }
    Attachment attachment = xacml.get();
    AccessControlList acl;
    try {
      acl = XACMLUtils.parseXacml(workspace.read(attachment.getURI()));
    } catch (XACMLParsingException | NotFoundException | IOException e) {
      logger.warn("Error reading XACML file {}", attachment.getURI(), e);
      return false;
    }
    boolean allowed = false;
    final User user = securityService.getUser();
    for (AccessControlEntry entry: acl.getEntries()) {
      // ignore entries for other actions
      if (!entry.getAction().equals(action)) {
        continue;
      }
      for (Role role : user.getRoles()) {
        if (entry.getRole().equals(role.getName())) {
          // immediately abort on matching deny rules
          // (never allow if a deny rule matches, even if another allow rule matches)
          if (!entry.isAllow()) {
            logger.debug("Access explicitely denied for role({}), action({})", role.getName(), action);
            return false;
          }
          allowed = true;
        }
      }
    }
    logger.debug("XACML file allowed access");
    return allowed;
  }

  /**
   * Sets the workspace to use for retrieving XACML policies
   *
   * @param workspace
   *          the workspace to set
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * Declarative services callback to set the security service.
   *
   * @param securityService
   *          the security service
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Declarative services callback to set the series service.
   *
   * @param seriesService
   *          the series service
   */
  protected void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

}
