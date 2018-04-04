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
package org.opencastproject.event.handler;

import static org.opencastproject.util.OsgiUtil.getOptCfg;
import static org.opencastproject.util.OsgiUtil.getOptCfgAsBoolean;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.oaipmh.persistence.OaiPmhDatabase;
import org.opencastproject.oaipmh.persistence.QueryBuilder;
import org.opencastproject.oaipmh.persistence.SearchResult;
import org.opencastproject.oaipmh.persistence.SearchResultItem;
import org.opencastproject.publication.api.OaiPmhPublicationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Option;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

public class OaiPmhUpdatedEventHandler implements ManagedService {

  /** The logger */
  protected static final Logger logger = LoggerFactory.getLogger(OaiPmhUpdatedEventHandler.class);

  // config keys
  protected static final String CFG_PROPAGATE_EPISODE = "propagate.episode";
  protected static final String CFG_FLAVORS = "flavors";
  protected static final String CFG_TAGS = "tags";

  /** Whether to propagate episode meta data changes to OAI-PMH or not */
  private boolean propagateEpisode = false;

  /** List of flavors to redistribute */
  private Set<String> flavors = new HashSet<>();

  /** List of tags to redistribute */
  private Set<String> tags = new HashSet<>();

  /** The security service */
  private SecurityService securityService = null;

  /** The OAI-PMH database */
  private OaiPmhDatabase oaiPmhPersistence = null;

  /** The OAI-PMH publication service */
  private OaiPmhPublicationService oaiPmhPublicationService = null;

  /** The system account to use for running asynchronous events */
  protected String systemAccount = null;

  /**
   * OSGI callback for component activation.
   *
   * @param bundleContext
   *          the OSGI bundle context
   */
  protected void activate(BundleContext bundleContext) {
    this.systemAccount = bundleContext.getProperty("org.opencastproject.security.digest.user");
  }

  @Override
  public void updated(Dictionary<String, ?> dictionary) throws ConfigurationException {
    final Option<Boolean> propagateEpisode = getOptCfgAsBoolean(dictionary, CFG_PROPAGATE_EPISODE);
    if (propagateEpisode.isSome()) {
      this.propagateEpisode = propagateEpisode.get();
    }

    final Option<String> flavorsRaw = getOptCfg(dictionary, CFG_FLAVORS);
    if (flavorsRaw.isSome()) {
      final String[] flavorStrings = flavorsRaw.get().split("\\s*,\\s*");
      this.flavors = Collections.set(flavorStrings);
    } else {
      this.flavors = new HashSet<>();
    }

    final Option<String> tagsRaw = getOptCfg(dictionary, CFG_TAGS);
    if (tagsRaw.isSome()) {
      final String[] tags = tagsRaw.get().split("\\s*,\\s*");
      this.tags = Collections.set(tags);
    } else {
      this.tags = new HashSet<>();
    }
  }

  public void handleEvent(AssetManagerItem.TakeSnapshot snapshotItem) {
    if (!propagateEpisode) {
      logger.trace("Skipping automatic propagation of episode meta data to OAI-PMH since it is turned off.");
      return;
    }

    //An episode or its ACL has been updated. Construct the MediaPackage and publish it to OAI-PMH.
    logger.debug("Handling update event for media package {}",
            snapshotItem.getMediapackage().getIdentifier().compact());

    // We must be an administrative user to make a query to the OaiPmhPublicationService
    final User prevUser = securityService.getUser();
    final Organization prevOrg = securityService.getOrganization();

    try {
      securityService.setUser(SecurityUtil.createSystemUser(systemAccount, prevOrg));

      // Check weather the media package contains elements to republish
      MediaPackage snapshotMp = snapshotItem.getMediapackage();
      SimpleElementSelector mpeSelector = new SimpleElementSelector();
      for (String flavor : flavors) {
        mpeSelector.addFlavor(flavor);
      }
      for (String tag : tags) {
        mpeSelector.addTag(tag);
      }
      Collection<MediaPackageElement> elementsToUpdate = mpeSelector.select(snapshotMp, true);
      if (elementsToUpdate == null || elementsToUpdate.isEmpty()) {
        logger.debug("The media package {} does not contain any elements matching the given flavors and tags",
                snapshotMp.getIdentifier().compact());
        return;
      }

      SearchResult result = oaiPmhPersistence.search(QueryBuilder.query().mediaPackageId(snapshotMp)
              .isDeleted(false).build());
      for (SearchResultItem searchResultItem : result.getItems()) {
        try {
          Job job = oaiPmhPublicationService
                  .updateMetadata(snapshotMp, searchResultItem.getRepository(), flavors, tags, false);
          // we don't want to wait for job completion here because it will block the message queue
        } catch (Exception e) {
          logger.error("Unable to update OAI-PMH publication for the media package {} in repository {}",
                  snapshotItem.getMediapackage().getIdentifier().compact(), searchResultItem.getRepository(), e);
        }
      }
    } finally {
      securityService.setOrganization(prevOrg);
      securityService.setUser(prevUser);
    }
  }

  public void setOaiPmhPersistence(OaiPmhDatabase oaiPmhPersistence) {
    this.oaiPmhPersistence = oaiPmhPersistence;
  }

  public void setOaiPmhPublicationService(OaiPmhPublicationService oaiPmhPublicationService) {
    this.oaiPmhPublicationService = oaiPmhPublicationService;
  }

  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }
}
