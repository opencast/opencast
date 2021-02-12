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

package org.opencastproject.adopter.statistic;

import org.opencastproject.adopter.registration.Form;
import org.opencastproject.adopter.registration.Service;
import org.opencastproject.adopter.statistic.dto.GeneralData;
import org.opencastproject.adopter.statistic.dto.Host;
import org.opencastproject.adopter.statistic.dto.StatisticData;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.userdirectory.JpaUserAndRoleProvider;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * It collects and sends statistic data of an registered adopter.
 */
public class ScheduledDataCollector extends TimerTask {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ScheduledDataCollector.class);

  /** The property key containing the address of the external server where the statistic data will be send to. */
  private static final String PROP_KEY_STATISTIC_SERVER_ADDRESS = "org.opencastproject.adopter.registration.server.url";
  private static final String DEFAULT_STATISTIC_SERVER_ADDRESS = "https://register.opencast.org";

  private static final int ONE_DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;

  //================================================================================
  // OSGi properties
  //================================================================================

  /** Provides access to the adopter form information */
  private Service adopterFormService;

  /** Provides access to job and host information */
  private ServiceRegistry serviceRegistry;

  /** Provides access to recording information */
  private AssetManager assetManager;

  /** Provides access to series information */
  private SeriesService seriesService;

  /** User and role provider */
  protected JpaUserAndRoleProvider userAndRoleProvider;

  /** The security service */
  protected SecurityService securityService;


  //================================================================================
  // Properties
  //================================================================================

  /** Provides methods for sending statistic data */
  private Sender sender;

  /** The organisation of the system admin user */
  private Organization defaultOrganization;

  /** System admin user */
  private User systemAdminUser;


  //================================================================================
  // Scheduler methods
  //================================================================================

  /**
   * Entry point of the scheduler. Configured with the
   * activate parameter at OSGi component declaration.
   * @param ctx OSGi component context
   */
  public void activate(BundleContext ctx) {
    logger.info("Activating adopter statistic scheduler.");
    this.defaultOrganization = new DefaultOrganization();
    String systemAdminUserName = ctx.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    this.systemAdminUser = SecurityUtil.createSystemUser(systemAdminUserName, defaultOrganization);

    // We read this key for testing but don't ever expect this to be set.
    final String serverBaseUrl = ctx.getProperty(PROP_KEY_STATISTIC_SERVER_ADDRESS);
    if (serverBaseUrl != null) {
      logger.error("\nAdopter registration information are sent to a server other than register.opencast.org.\n"
          + "We cannot take any responsibility for what is done with the data.");
    }
    this.sender = new Sender(Objects.toString(serverBaseUrl, DEFAULT_STATISTIC_SERVER_ADDRESS));

    // Send data now. Repeat every 24h.
    new Timer().schedule(this, 0, ONE_DAY_IN_MILLISECONDS);
  }

  /**
   * The scheduled method. It collects statistic data
   * around Opencast and sends it via POST request.
   */
  @Override
  public void run() {
    logger.info("Executing adopter statistic scheduler task.");

    Form adopter;
    try {
      adopter = (Form) adopterFormService.retrieveFormData();
    } catch (Exception e) {
      logger.error("Couldn't retrieve adopter form data.", e);
      return;
    }

    if (adopter.isRegistered() && adopter.agreedToPolicy()) {
      try {
        String generalDataAsJson = collectGeneralData(adopter);
        sender.sendGeneralData(generalDataAsJson);
      } catch (Exception e) {
        logger.error("Error occurred while processing adopter general data.", e);
      }

      if (adopter.allowsStatistics()) {
        try {
          String statisticDataAsJson = collectStatisticData(adopter.getStatisticKey());
          sender.sendStatistics(statisticDataAsJson);
        } catch (Exception e) {
          logger.error("Error occurred while processing adopter statistic data.", e);
        }
      }
    }
  }


  //================================================================================
  // Data collecting methods
  //================================================================================

  /**
   * Just retrieves the form data of the adopter.
   * @param adopterRegistrationForm The adopter registration form.
   * @return The adopter form containing general data as JSON string.
   */
  private String collectGeneralData(Form adopterRegistrationForm) {
    GeneralData generalData = new GeneralData(adopterRegistrationForm);
    return generalData.jsonify();
  }

  /**
   * Gathers various statistic data.
   * @param statisticKey A Unique key per adopter for the statistic entry.
   * @return The statistic data as JSON string.
   * @throws Exception General exception that can occur while gathering data.
   */
  private String collectStatisticData(String statisticKey) throws Exception {
    StatisticData statisticData = new StatisticData(statisticKey);
    serviceRegistry.getHostRegistrations().forEach(host -> statisticData.addHost(new Host(host)));
    statisticData.setJobCount(serviceRegistry.count(null, null));

    AQueryBuilder q = assetManager.createQuery();
    SecurityUtil.runAs(this.securityService, this.defaultOrganization, this.systemAdminUser, () -> {
      AResult result = q.select(q.snapshot()).where(q.version().isLatest()).run();
      statisticData.setEventCount(result.getSize());
    });

    statisticData.setSeriesCount(seriesService.getSeriesCount());
    statisticData.setUserCount(userAndRoleProvider.countAllUsers());
    return statisticData.jsonify();
  }


  //================================================================================
  // OSGi setter
  //================================================================================

  /** OSGi setter for the adopter form service. */
  public void setAdopterFormService(Service adopterFormService) {
    this.adopterFormService = adopterFormService;
  }

  /** OSGi setter for the service registry. */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /** OSGi setter for the asset manager. */
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /** OSGi setter for the series service. */
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  /** OSGi setter for the user provider. */
  public void setUserAndRoleProvider(JpaUserAndRoleProvider userAndRoleProvider) {
    this.userAndRoleProvider = userAndRoleProvider;
  }

  /** OSGi callback for setting the security service. */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

}
