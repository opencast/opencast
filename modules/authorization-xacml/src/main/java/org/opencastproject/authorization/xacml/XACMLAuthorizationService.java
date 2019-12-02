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

import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_EPISODE;
import static org.opencastproject.mediapackage.MediaPackageElements.XACML_POLICY_SERIES;
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
import org.opencastproject.util.data.Tuple;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBException;

/**
 * A XACML implementation of the {@link AuthorizationService}.
 */
public class XACMLAuthorizationService implements AuthorizationService, ManagedService {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(XACMLAuthorizationService.class);

  /** The default filename for XACML attachments */
  private static final String XACML_FILENAME = "xacml.xml";

  /** The workspace */
  protected Workspace workspace;

  /** The security service */
  protected SecurityService securityService;

  /** The series service */
  protected SeriesService seriesService;

  private static final String CONFIG_MERGE_MODE = "merge.mode";

  /** Definition of how merging of series and episode ACLs work */
  private static MergeMode mergeMode = MergeMode.OVERRIDE;

  enum MergeMode {
    OVERRIDE, ROLES, ACTIONS
  }

  public void activate(ComponentContext cc) {
    updated(cc.getProperties());
  }

  public void modified(Map<String, Object> config) {
    // this prevents the service from restarting on configuration updated.
    // updated() will handle the configuration update.
  }

  @Override
  public synchronized void updated(Dictionary<String, ?> properties) {
    if (properties == null) {
      mergeMode = MergeMode.OVERRIDE;
      logger.debug("Merge mode set to {}", mergeMode);
      return;
    }
    final String mode = StringUtils.defaultIfBlank((String) properties.get(CONFIG_MERGE_MODE),
            MergeMode.OVERRIDE.toString());
    try {
      mergeMode = MergeMode.valueOf(mode.toUpperCase());
    } catch (IllegalArgumentException e) {
      logger.warn("Invalid value set for ACL merge mode, defaulting to {}", MergeMode.OVERRIDE);
      mergeMode = MergeMode.OVERRIDE;
    }
    logger.debug("Merge mode set to {}", mergeMode);
  }

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
    Optional<AccessControlList> episode = Optional.empty();
    Optional<AccessControlList> series = Optional.empty();

    // Start with the requested scope but fall back to the less specific scope if it does not exist.
    // The order is: episode -> series -> general (deprecated) -> global
    if (AclScope.Episode.equals(scope) || AclScope.Merged.equals(scope)) {
      for (Attachment xacml : mp.getAttachments(XACML_POLICY_EPISODE)) {
        episode = loadAcl(xacml.getURI());
      }
    }
    if (Arrays.asList(AclScope.Episode, AclScope.Series, AclScope.Merged).contains(scope)) {
      for (Attachment xacml : mp.getAttachments(XACML_POLICY_SERIES)) {
        series = loadAcl(xacml.getURI());
      }
    }

    if (episode.isPresent() && series.isPresent()) {
      logger.debug("Found event and series ACL for media package {}", mp.getIdentifier());
      switch (mergeMode) {
        case ACTIONS:
          logger.debug("Merging ACLs based on individual actions");
          return tuple(series.get().mergeActions(episode.get()), AclScope.Merged);
        case ROLES:
          logger.debug("Merging ACLs based on roles");
          return tuple(series.get().merge(episode.get()), AclScope.Merged);
        default:
          logger.debug("Episode ACL overrides series ACL");
          return tuple(episode.get(), AclScope.Merged);
      }
    }
    if (episode.isPresent()) {
      logger.debug("Found event ACL for media package {}", mp.getIdentifier());
      return tuple(episode.get(), AclScope.Episode);
    }
    if (series.isPresent()) {
      logger.debug("Found series ACL for media package {}", mp.getIdentifier());
      return tuple(series.get(), AclScope.Series);
    }

    logger.debug("Falling back to global default ACL");
    return tuple(new AccessControlList(), AclScope.Global);
  }

  @Override
  public Tuple<MediaPackage, Attachment> setAcl(final MediaPackage mp, final AclScope scope, final AccessControlList acl)
          throws MediaPackageException {
    // Get XACML representation of these role + action tuples
    String xacmlContent;
    try {
      xacmlContent = XACMLUtils.getXacml(mp, acl);
    } catch (JAXBException e) {
      throw new MediaPackageException("Unable to generate xacml for media package " + mp.getIdentifier());
    }

    // Remove the old xacml file(s)
    Attachment attachment = removeFromMediaPackageAndWorkspace(mp, toFlavor(scope)).getB();

    // add attachment
    final String elementId = toElementId(scope);
    URI uri;
    try (InputStream in = IOUtils.toInputStream(xacmlContent, "UTF-8")) {
      uri = workspace.put(mp.getIdentifier().toString(), elementId, XACML_FILENAME, in);
    } catch (IOException e) {
      throw new MediaPackageException("Error storing xacml for media package " + mp.getIdentifier());
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

    // return augmented media package
    return tuple(mp, attachment);
  }

  @Override
  public MediaPackage removeAcl(MediaPackage mp, AclScope scope) {
    return removeFromMediaPackageAndWorkspace(mp, toFlavor(scope)).getA();
  }

  /** Get the flavor associated with a scope. */
  private static MediaPackageElementFlavor toFlavor(AclScope scope) {
    switch (scope) {
      case Episode:
        return XACML_POLICY_EPISODE;
      case Series:
        return XACML_POLICY_SERIES;
      default:
        throw new IllegalArgumentException("No flavors match the given ACL scope");
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
        throw new IllegalArgumentException("No element id matches the given ACL scope");
    }
  }

  /**
   * Remove all attachments of the given flavors from media package and workspace.
   *
   * @return the a tuple with the mutated (!) media package as A and the deleted Attachment as B
   */
  private Tuple<MediaPackage, Attachment> removeFromMediaPackageAndWorkspace(MediaPackage mp,
          MediaPackageElementFlavor flavor) {
    Attachment attachment = null;
    for (Attachment a : mp.getAttachments(flavor)) {
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
  private Optional<AccessControlList> loadAcl(final URI uri) {
    logger.debug("Load Acl from {}", uri);
    try (InputStream is = workspace.read(uri)) {
      AccessControlList acl = XACMLUtils.parseXacml(is);
      if (acl != null) {
        return Optional.of(acl);
      }
    } catch (NotFoundException e) {
      logger.debug("URI {} not found", uri);
    } catch (Exception e) {
      logger.warn("Unable to load or parse Acl from URI {}", uri, e);
    }
    return Optional.empty();
  }

  @Override
  public boolean hasPermission(final MediaPackage mp, final String action) {
    AccessControlList acl = getActiveAcl(mp).getA();
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
            logger.debug("Access explicitly denied for role({}), action({})", role.getName(), action);
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
