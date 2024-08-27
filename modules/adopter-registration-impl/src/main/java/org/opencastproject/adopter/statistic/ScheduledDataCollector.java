/*
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
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.search.api.SearchQuery;
import org.opencastproject.search.api.SearchResult;
import org.opencastproject.search.api.SearchResultItem;
import org.opencastproject.search.api.SearchService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserProvider;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.userdirectory.JpaUserAndRoleProvider;
import org.opencastproject.userdirectory.JpaUserReferenceProvider;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

/**
 * It collects and sends statistic data of an registered adopter.
 */
@Component(
    immediate = true,
    service = ScheduledDataCollector.class,
    property = {
        "service.description=Adopter Statistics Scheduler"
    }
)
public class ScheduledDataCollector extends TimerTask {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(ScheduledDataCollector.class);

  /** The property key containing the address of the external server where the statistic data will be send to. */
  private static final String PROP_KEY_STATISTIC_SERVER_ADDRESS = "org.opencastproject.adopter.registration.server.url";
  private static final String DEFAULT_STATISTIC_SERVER_ADDRESS = "https://register.opencast.org";

  private static final int ONE_DAY_IN_MILLISECONDS = 1000 * 60 * 60 * 24;

  /* How many records to get from the search index at once */
  private static final int SEARCH_ITERATION_SIZE = 100;

  private static final Gson gson = new Gson();

  //================================================================================
  // OSGi properties
  //================================================================================

  /** Provides access to the adopter form information */
  private Service adopterFormService;

  /** Provides access to job and host information */
  private ServiceRegistry serviceRegistry;

  /** Provides access to CA counts */
  private CaptureAgentStateService caStateService;

  private OrganizationDirectoryService organizationDirectoryService;

  /** Provides access to recording information */
  private AssetManager assetManager;

  /** Provides access to series information */
  private SeriesService seriesService;

  /** Provides access to search information */
  private SearchService searchService;

  /** User and role provider */
  protected UserProvider userRefProvider;

  protected JpaUserAndRoleProvider userProvider;

  /** The security service */
  protected SecurityService securityService;

  private TobiraRemoteRequester tobiraRemoteRequester;

  protected TrustedHttpClient httpClient;


  //================================================================================
  // Properties
  //================================================================================

  /** Provides methods for sending statistic data */
  private Sender sender;

  /** The organisation of the system admin user */
  private Organization defaultOrganization;

  /** System admin user */
  private User systemAdminUser;

  /** The Opencast version this is running in */
  private String version;

  /** The timer for shutdown uses */
  private Timer timer;

  //================================================================================
  // Scheduler methods
  //================================================================================

  /**
   * Entry point of the scheduler. Configured with the
   * activate parameter at OSGi component declaration.
   * @param ctx OSGi component context
   */
  @Activate
  public void activate(BundleContext ctx) {
    logger.info("Activating adopter statistic scheduler.");
    this.defaultOrganization = new DefaultOrganization();
    String systemAdminUserName = ctx.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER);
    this.systemAdminUser = SecurityUtil.createSystemUser(systemAdminUserName, defaultOrganization);

    final Version ctxVersion = ctx.getBundle().getVersion();
    this.version = ctxVersion.toString();

    // We read this key for testing but don't ever expect this to be set.
    final String serverBaseUrl = ctx.getProperty(PROP_KEY_STATISTIC_SERVER_ADDRESS);
    if (serverBaseUrl != null) {
      logger.error("\nAdopter registration information are sent to a server other than register.opencast.org.\n"
          + "We cannot take any responsibility for what is done with the data.");
    }
    this.sender = new Sender(Objects.toString(serverBaseUrl, DEFAULT_STATISTIC_SERVER_ADDRESS));

    // Send data now. Repeat every 24h.
    timer = new Timer();
    timer.schedule(this, 0, ONE_DAY_IN_MILLISECONDS);
  }

  @Deactivate
  public void deactivate() {
    timer.cancel();
  }

  /**
   * The scheduled method. It collects statistic data
   * around Opencast and sends it via POST request.
   */
  @Override
  public void run() {
    logger.info("Executing adopter statistic scheduler task.");

    this.tobiraRemoteRequester = new TobiraRemoteRequester();
    this.tobiraRemoteRequester.setRemoteServiceManager(serviceRegistry);
    this.tobiraRemoteRequester.setTrustedHttpClient(httpClient);

    Form adopter;
    try {
      adopter = (Form) adopterFormService.retrieveFormData();
    } catch (Exception e) {
      logger.error("Couldn't retrieve adopter form data.", e);
      return;
    }

    if (adopter.shouldDelete()) {
      //Sanitize the data we're sending to delete things
      Form f = new Form();
      f.setAdopterKey(adopter.getAdopterKey());
      GeneralData gd = new GeneralData(f);
      gd.setAdopterKey(adopter.getAdopterKey());
      StatisticData sd = new StatisticData(adopter.getStatisticKey());

      try {
        sender.deleteStatistics(sd.jsonify());
        sender.deleteGeneralData(gd.jsonify());
        adopterFormService.deleteRegistration();
      } catch (IOException e) {
        logger.warn("Error occurred while deleting registration data, will retry", e);
      }
      return;
    }
    // Don't send data unless they've agreed to the latest (at time of writing) terms.
    // Pre April 2022 doesn't allow collection of a bunch of things, and doens't allow linking stat data to org
    // so rather than burning time turning various things off (after figuring out what needs to be turned off)
    // we just don't send anything.  By the time we need to update the ToU again this whole thing would need reworking
    // anyway, so we'll run with this for now.
    if (adopter.isRegistered() && adopter.getTermsVersionAgreed() == Form.TERMSOFUSEVERSION.APRIL_2022) {
      try {
        String generalDataAsJson = collectGeneralData(adopter);
        sender.sendGeneralData(generalDataAsJson);
        //Note: save the form (unmodified) to update the dates.  Old dates cause warnings to the user!
        adopterFormService.saveFormData(adopter);
      } catch (Exception e) {
        logger.error("Error occurred while processing adopter general data.", e);
      }

      if (adopter.allowsStatistics()) {
        try {
          StatisticData statisticData = collectStatisticData(adopter.getAdopterKey(), adopter.getStatisticKey());
          sender.sendStatistics(statisticData.jsonify());
          JsonObject tobiraJson = tobiraRemoteRequester.getStats();
          // This is null in the case that Tobira hasn't sent any stats yet.
          // This could be due to Tobira not existing, or because we've just rebooted.
          if (null != tobiraJson) {
            sender.sendTobiraData(
                "{ \"statistic_key\": \"" + statisticData.getStatisticKey()
                  + "\", \"data\": " + tobiraJson.toString() + " }");
          }
          //Note: save the form (unmodified) (again!) to update the dates.  Old dates cause warnings to the user!
          adopterFormService.saveFormData(adopter);
        } catch (Exception e) {
          logger.error("Error occurred while processing adopter statistic data.", e);
        }
      }
    }
  }

  public String getRegistrationDataAsString() throws Exception {
    Form adopter = (Form) adopterFormService.retrieveFormData();
    String generalJson = collectGeneralData(adopter);
    String statsJson;
    if (adopter.allowsStatistics()) {
      statsJson = collectStatisticData(adopter.getAdopterKey(), adopter.getStatisticKey()).jsonify();
    } else {
      statsJson = "{}";
    }
    JsonObject tobiraJson = tobiraRemoteRequester.getStats();

    if (null != tobiraJson) {
      //It's not stupid if it works!
      return "{ \"general\":" + generalJson + ", \"statistics\":" + statsJson + ", \"tobira\":" + tobiraJson + "}";
    } else {
      return "{ \"general\":" + generalJson + ", \"statistics\":" + statsJson + "}";
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
  private StatisticData collectStatisticData(String adopterKey, String statisticKey) throws Exception {
    StatisticData statisticData = new StatisticData(statisticKey);
    statisticData.setAdopterKey(adopterKey);
    serviceRegistry.getHostRegistrations().forEach(host -> {
      Host h = new Host(host);
      try {
        String services = serviceRegistry.getServiceRegistrationsByHost(host.getBaseUrl())
            .stream()
            .map(sr -> sr.getServiceType())
            .collect(Collectors.joining(",\n"));
        h.setServices(services);
      } catch (ServiceRegistryException e) {
        logger.warn("Error gathering services for {}", host.getBaseUrl(), e);
      }
      statisticData.addHost(h);
    });
    statisticData.setJobCount(serviceRegistry.count(null, null));

    statisticData.setSeriesCount(seriesService.getSeriesCount());
    SearchQuery sq = new SearchQuery();
    sq.withId("");
    sq.withElementTags(new String[0]);
    sq.withElementFlavors(new MediaPackageElementFlavor[0]);
    sq.signURLs(false);
    sq.includeEpisodes(true);
    sq.includeSeries(false);
    sq.withLimit(SEARCH_ITERATION_SIZE);

    List<Organization> orgs = organizationDirectoryService.getOrganizations();
    statisticData.setTenantCount(orgs.size());

    for (Organization org : orgs) {
      SecurityUtil.runAs(securityService, org, systemAdminUser, () -> {
        statisticData.setEventCount(statisticData.getEventCount() + assetManager.countEvents(org.getId()));

        //Calculate the number of attached CAs for this org, add it to the total
        long current = statisticData.getCACount();
        int orgCAs = caStateService.getKnownAgents().size();
        statisticData.setCACount(current + orgCAs);

        //Calculate the total number of minutes for this org, add it to the total
        current = statisticData.getTotalMinutes();
        long orgDuration = 0L;
        long total = 0;
        int offset = 0;
        try {
          do {
            sq.withOffset(offset);
            SearchResult sr = searchService.getForAdministrativeRead(sq);
            offset += SEARCH_ITERATION_SIZE;
            total = sr.getTotalSize();
            orgDuration = Arrays.stream(sr.getItems())
                                       .map(SearchResultItem::getMediaPackage)
                                       .map(MediaPackage::getDuration)
                                       .mapToLong(Long::valueOf)
                                       .sum() / 1000L;
          } while (offset + SEARCH_ITERATION_SIZE <= total);
        } catch (UnauthorizedException e) {
          //This should never happen, but...
          logger.warn("Unable to calculate total minutes, unauthorized");
        }
        statisticData.setTotalMinutes(current + orgDuration);

        //Add the users for each org
        long currentUsers = statisticData.getUserCount();
        statisticData.setUserCount(currentUsers + userProvider.countUsers() + userRefProvider.countUsers());
      });
    }
    statisticData.setVersion(version);
    return statisticData;
  }


  //================================================================================
  // OSGi setter
  //================================================================================

  /** OSGi setter for the adopter form service. */
  @Reference
  public void setAdopterFormService(Service adopterFormService) {
    this.adopterFormService = adopterFormService;
  }

  /** OSGi setter for the service registry. */
  @Reference
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Reference
  public void setCaptureAdminService(CaptureAgentStateService stateService) {
    this.caStateService = stateService;
  }

  /** OSGi setter for the asset manager. */
  @Reference
  public void setAssetManager(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  /** OSGi setter for the series service. */
  @Reference
  public void setSeriesService(SeriesService seriesService) {
    this.seriesService = seriesService;
  }

  @Reference
  public void setSearchService(SearchService searchService) {
    this.searchService = searchService;
  }

  /** OSGi setter for the userref provider. */
  @Reference
  public void setUserRefProvider(JpaUserReferenceProvider userRefProvider) {
    this.userRefProvider = userRefProvider;
  }

  /* OSGi setter for the user provider. */
  @Reference
  public void setUserAndRoleProvider(JpaUserAndRoleProvider userProvider) {
    this.userProvider = userProvider;
  }

  /** OSGi callback for setting the security service. */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi callback for setting the org directory service. */
  @Reference
  public void setOrganizationDirectoryService(OrganizationDirectoryService orgDirServ) {
    this.organizationDirectoryService = orgDirServ;
  }

  @Reference
  public void setTrustedHttpClient(TrustedHttpClient trustedHttpClient) {
    this.httpClient = trustedHttpClient;
  }

  private class TobiraRemoteRequester extends RemoteBase {
    private static final String SERVICE_TYPE = "org.opencastproject.tobira";
    TobiraRemoteRequester() {
      super(SERVICE_TYPE);
    }

    public JsonObject getStats() throws IOException {
      HttpGet get = new HttpGet("/stats");
      HttpResponse response = this.getResponse(get);
      try {
        if (response != null) {
          String json = IOUtils.toString(response.getEntity().getContent());
          return gson.fromJson(json, JsonElement.class).getAsJsonObject();
        }
      } finally {
        this.closeConnection(response);
      }
      return null;
    }
  }
}
