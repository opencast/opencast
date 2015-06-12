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

package org.opencastproject.index.service.message;

import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.index.service.impl.index.series.Series;
import org.opencastproject.index.service.impl.index.series.SeriesIndexUtils;
import org.opencastproject.index.service.util.AccessInformationUtil;
import org.opencastproject.matterhorn.search.SearchIndexException;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.series.SeriesItem;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.User;
import org.opencastproject.util.data.Option;

import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Strings;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SeriesMessageReceiverImpl extends BaseMessageReceiverImpl<SeriesItem> {

  private static final String THEME_PROPERTY_NAME = "theme";

  private static final Logger logger = LoggerFactory.getLogger(SeriesMessageReceiverImpl.class);

  private AclServiceFactory aclServiceFactory;

  /**
   * Creates a new message receiver that is listening to the admin ui destination of the series queue.
   */
  public SeriesMessageReceiverImpl() {
    super(MessageSender.DestinationType.Queue);
  }

  @Override
  protected void execute(SeriesItem seriesItem) {
    Series series = null;
    String organization = getSecurityService().getOrganization().getId();
    User user = getSecurityService().getUser();
    switch (seriesItem.getType()) {
      case UpdateCatalog:
        logger.debug("Received Update Series");

        DublinCoreCatalog dc = seriesItem.getSeries();
        String seriesId = dc.getFirst(DublinCoreCatalog.PROPERTY_IDENTIFIER);

        // Load or create the corresponding series
        try {
          series = SeriesIndexUtils.getOrCreate(seriesId, organization, user, getSearchIndex());
          series.setCreator(getSecurityService().getUser().getName());
          SeriesIndexUtils.updateSeries(series, dc);
        } catch (SearchIndexException e) {
          logger.error("Error retrieving series {} from the search index: {}", seriesId,
                  ExceptionUtils.getStackTrace(e));
          return;
        }

        // Update the event series titles if they changed
        try {
          SeriesIndexUtils.updateEventSeriesTitles(series, organization, getSecurityService().getUser(),
                  getSearchIndex());
        } catch (SearchIndexException e) {
          logger.error("Error updating the series name of series {} from the associated events: {}",
                  series.getIdentifier(), ExceptionUtils.getStackTrace(e));
        }

        // Persist the series
        update(seriesItem.getSeriesId(), series);
        break;
      case UpdateAcl:
        logger.debug("Received Update Series ACL");

        // Load or create the corresponding series
        try {
          series = SeriesIndexUtils.getOrCreate(seriesItem.getSeriesId(), organization, user, getSearchIndex());

          List<ManagedAcl> acls = aclServiceFactory.serviceFor(getSecurityService().getOrganization()).getAcls();
          Option<ManagedAcl> managedAcl = AccessInformationUtil.matchAcls(acls, seriesItem.getAcl());
          if (managedAcl.isSome())
            series.setManagedAcl(managedAcl.get().getName());

          series.setAccessPolicy(AccessControlParser.toJsonSilent(seriesItem.getAcl()));
        } catch (SearchIndexException e) {
          logger.error("Error retrieving series {} from the search index: {}", seriesItem.getSeriesId(),
                  ExceptionUtils.getStackTrace(e));
          return;
        }

        // Persist the updated series
        update(seriesItem.getSeriesId(), series);
        break;
      case UpdateOptOut:
        logger.debug("Received update opt out status of series {}", seriesItem.getSeriesId());

        // Load or create the corresponding series
        try {
          series = SeriesIndexUtils.getOrCreate(seriesItem.getSeriesId(), organization, user, getSearchIndex());
          series.setOptOut(seriesItem.getOptOut());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving series {} from the search index: {}", seriesItem.getSeriesId(),
                  ExceptionUtils.getStackTrace(e));
          return;
        }

        // Persist the updated series
        update(seriesItem.getSeriesId(), series);
        break;
      case UpdateProperty:
        logger.debug("Received update property of series {}", seriesItem.getSeriesId());

        if (!THEME_PROPERTY_NAME.equals(seriesItem.getPropertyName()))
          break;

        // Load or create the corresponding series
        try {
          series = SeriesIndexUtils.getOrCreate(seriesItem.getSeriesId(), organization, user, getSearchIndex());
          series.setTheme(Opt.nul(seriesItem.getPropertyValue()).bind(Strings.toLong).orNull());
        } catch (SearchIndexException e) {
          logger.error("Error retrieving series {} from the search index: {}", seriesItem.getSeriesId(),
                  ExceptionUtils.getStackTrace(e));
          return;
        }

        // Persist the updated series
        update(seriesItem.getSeriesId(), series);
        break;
      case Delete:
        logger.debug("Received Delete Series Event {}", seriesItem.getSeriesId());

        // Remove the series from the search index
        try {
          getSearchIndex().delete(Series.DOCUMENT_TYPE, seriesItem.getSeriesId().concat(organization));
          logger.debug("Series {} removed from adminui search index", seriesItem.getSeriesId());
        } catch (SearchIndexException e) {
          logger.error("Error deleting the series {} from the search index: {}", seriesItem.getSeriesId(),
                  ExceptionUtils.getStackTrace(e));
          return;
        }
        return;
      default:
        throw new IllegalArgumentException("Unhandled type of SeriesItem");
    }
  }

  private void update(String seriesId, Series series) {
    try {
      getSearchIndex().addOrUpdate(series);
      logger.debug("Series {} updated in the adminui search index", seriesId);
    } catch (SearchIndexException e) {
      logger.error("Error storing the series {} to the search index: {}", seriesId, ExceptionUtils.getStackTrace(e));
    }
  }

  /** OSGi callback for acl services. */
  public void setAclServiceFactory(AclServiceFactory aclServiceFactory) {
    this.aclServiceFactory = aclServiceFactory;
  }

}
