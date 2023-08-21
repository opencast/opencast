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

package org.opencastproject.adminui.usersettings;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.adminui.usersettings.persistence.UserSettingDto;
import org.opencastproject.adminui.usersettings.persistence.UserSettingsServiceException;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;

import org.apache.commons.lang3.tuple.Pair;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Finds the user settings and message signatures from the current user.
 */
@Component(
  immediate = true,
  service = UserSettingsService.class,
  property = {
    "service.description=Admin UI - Users Settings Service",
    "opencast.service.type=org.opencastproject.adminui.usersettings.UserSettingsService"
  }
)
public class UserSettingsService {
  public static final String PERSISTENCE_UNIT = "org.opencastproject.adminui";

  /** Logging utilities */
  private static final Logger logger = LoggerFactory.getLogger(UserSettingsService.class);

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService;

  /** The security service */
  protected SecurityService securityService;

  /**
   * Creates {@link EntityManagerFactory} using persistence provider and properties passed via OSGi.
   *
   * @param cc
   */
  @Activate
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for user settings");
    db = dbSessionFactory.createSession(emf);
  }

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.adminui)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /**
   * OSGi callback to set user directory service.
   *
   * @param userDirectoryService
   *          user directory service
   */
  @Reference
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the security service
   */
  @Reference
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the organization directory service.
   *
   * @param organizationDirectoryService
   *          the organization directory service
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  /**
   * Finds the user settings for the current user.
   *
   * @param limit
   *          The maximum limit of results to return.
   * @param offset
   *          The starting page offset.
   * @return The user settings for the current user.
   * @throws UserSettingsServiceException
   */
  public UserSettings findUserSettings(int limit, int offset) throws UserSettingsServiceException {
    try {
      UserSettings userSettings = db.exec(getUserSettingsQuery(limit, offset));
      userSettings.setTotal(db.exec(getUserSettingsTotalQuery()));
      userSettings.setLimit(limit);
      userSettings.setOffset(offset);
      return userSettings;
    } catch (Exception e) {
      logger.error("Could not get user settings:", e);
      throw new UserSettingsServiceException(e);
    }
  }

  /**
   * @return Function that finds the total number of user settings for the current user.
   */
  private Function<EntityManager, Integer> getUserSettingsTotalQuery() {
    String orgId = securityService.getOrganization().getId();
    String username = securityService.getUser().getUsername();
    return namedQuery.find(
        "UserSettings.countByUserName",
        Number.class,
        Pair.of("username", username),
        Pair.of("org", orgId)
    ).andThen(Number::intValue);
  }

  /**
   * @param offset
   *          The number of limits to page to.
   * @param limit
   *          The maximum number of settings to return.
   * @return Function that finds all of the user settings for the current user.
   */
  private Function<EntityManager, UserSettings> getUserSettingsQuery(int limit, int offset) {
    return em -> {
      String orgId = securityService.getOrganization().getId();
      String username = securityService.getUser().getUsername();
      logger.debug("Getting user settings for '%s' in org '%s'", username, orgId);

      List<UserSettingDto> result = em
          .createNamedQuery("UserSettings.findByUserName", UserSettingDto.class)
          .setParameter("username", username)
          .setParameter("org", orgId)
          .setMaxResults(limit)
          .setFirstResult(offset)
          .getResultList();
      if (result.size() == 0) {
        logger.debug("Found no user settings.");
      }

      UserSettings userSettings = new UserSettings();
      for (UserSettingDto userSettingsDto : result) {
        UserSetting userSetting = userSettingsDto.toUserSetting();
        logger.debug("Found user setting id: %d key: %s value: %s", userSetting.getId(), userSetting.getKey(),
            userSetting.getValue());
        userSettings.addUserSetting(userSetting);
      }
      return userSettings;
    };
  }

  /**
   * Create a new user setting key value pair.
   *
   * @param key
   *          The key to use for the current user setting.
   * @param value
   *          The value of the user setting.
   * @return A new user setting object
   * @throws UserSettingsServiceException
   */
  public UserSetting addUserSetting(String key, String value) throws UserSettingsServiceException {
    String orgId = securityService.getOrganization().getId();
    String username = securityService.getUser().getUsername();
    try {
      return db.execTx(em -> {
        UserSettingDto userSettingDto = new UserSettingDto();
        userSettingDto.setKey(key);
        userSettingDto.setValue(value);
        userSettingDto.setUsername(username);
        userSettingDto.setOrganization(orgId);
        em.persist(userSettingDto);
        return userSettingDto.toUserSetting();
      });
    } catch (Exception e) {
      logger.error("Could not update user setting username '%s' org:'%s' key:'%s' value:'%s':", username, orgId, key,
          value, e);
      throw new UserSettingsServiceException(e);
    }
  }

  /**
   * Get all user settings based upon its key.
   * @param key The key to search for.
   * @return A function returning {@link UserSettingDto} that matches the key.
   */
  private Function<EntityManager, List<UserSettingDto>> getUserSettingsByKeyQuery(String key) {
    String orgId = securityService.getOrganization().getId();
    String username = securityService.getUser().getUsername();
    logger.debug("Getting user settings for '%s' in org '%s'", username, orgId);
    return namedQuery.findAll(
        "UserSettings.findByKey",
        UserSettingDto.class,
        Pair.of("key", key),
        Pair.of("username", username),
        Pair.of("org", orgId)
    );
  }

  /**
   * Update a user setting that currently exists using its key to find it.
   * @param key The key for the user setting.
   * @param value The new value to set for the user setting.
   * @return An updated {@link UserSetting}
   * @throws UserSettingsServiceException
   */
  public UserSetting updateUserSetting(String key, String value, String oldValue) throws UserSettingsServiceException {
    try {
      UserSettingDto userSettingDto = db.exec(getUserSettingsByKeyQuery(key)).stream()
          .filter(setting -> setting.getKey().equalsIgnoreCase(key) && setting.getValue().equalsIgnoreCase(oldValue))
          .findFirst()
          .orElseThrow(() -> new UserSettingsServiceException("Unable to find user setting with key " + key + " value "
              + value + " and old value " + oldValue));

      return updateUserSetting(userSettingDto.getId(), key, value);
    } catch (Exception e) {
      logger.error("Could not update user setting", e);
      throw new UserSettingsServiceException(e);
    }
  }

  /**
   * Update a user setting that currently exists using its unique id to find it.
   * @param id The id for the user setting.
   * @param key The key for the user setting.
   * @param value The value for the user setting.
   * @return The updated {@link UserSetting}.
   * @throws UserSettingsServiceException
   */
  public UserSetting updateUserSetting(long id, String key, String value) throws UserSettingsServiceException {
    String orgId = securityService.getOrganization().getId();
    String username = securityService.getUser().getUsername();
    logger.debug("Updating user setting id: %d key: %s value: %s", id, key, value);

    try {
      return db.execTx(em -> {
        UserSettingDto userSettingDto = em.find(UserSettingDto.class, id);
        userSettingDto.setKey(key);
        userSettingDto.setValue(value);
        em.persist(userSettingDto);
        return userSettingDto.toUserSetting();
      });
    } catch (Exception e) {
      logger.error("Could not update user setting username '%s' org:'%s' id:'%d' key:'%s' value:'%s':",
        username, orgId, id, key, value, e);
      throw new UserSettingsServiceException(e);
    }
  }

  /**
   * Delete a user setting by using a unique id to find it.
   *
   * @param id
   *          The unique id for the user setting.
   * @throws UserSettingsServiceException
   */
  public void deleteUserSetting(long id) throws UserSettingsServiceException {
    try {
      db.execTx(em -> {
        UserSettingDto userSettingsDto = em.find(UserSettingDto.class, id);
        em.remove(userSettingsDto);
      });
    } catch (Exception e) {
      logger.error("Could not delete user setting '%d':", id, e);
      throw new UserSettingsServiceException(e);
    }
  }
}
