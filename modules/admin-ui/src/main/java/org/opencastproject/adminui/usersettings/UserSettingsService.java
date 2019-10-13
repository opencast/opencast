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

package org.opencastproject.adminui.usersettings;

import org.opencastproject.adminui.usersettings.persistence.UserSettingDto;
import org.opencastproject.adminui.usersettings.persistence.UserSettingsServiceException;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.Log;

import org.osgi.service.component.ComponentContext;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

/**
 * Finds the user settings and message signatures from the current user.
 */
public class UserSettingsService {
  public static final String PERSISTENCE_UNIT = "org.opencastproject.adminui";

  /** Logging utilities */
  private static final Log logger = Log.mk(UserSettingsService.class);

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

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
  public void activate(ComponentContext cc) {
    logger.info("Activating persistence manager for user settings");
  }

  /** OSGi DI */
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /**
   * OSGi callback to set user directory service.
   *
   * @param userDirectoryService
   *          user directory service
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * OSGi callback to set the security service.
   *
   * @param securityService
   *          the security service
   */
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
    UserSettings userSettings = getUserSettings(limit, offset);
    if (userSettings == null) {
      userSettings = new UserSettings();
    }
    userSettings.setTotal(getUserSettingsTotal());
    userSettings.setLimit(limit);
    userSettings.setOffset(offset);
    return userSettings;
  }

  /**
   * @return Finds the total number of user settings for the current user.
   * @throws UserSettingsServiceException
   */
  private int getUserSettingsTotal() throws UserSettingsServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      String username = securityService.getUser().getUsername();
      Query q = em.createNamedQuery("UserSettings.countByUserName").setParameter("username", username).setParameter("org", orgId);
      Number countResult = (Number) q.getSingleResult();
      return countResult.intValue();
    }  catch (Exception e) {
      logger.error("Could not count message signatures:", e);
      throw new UserSettingsServiceException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  /**
   * @param offset
   *          The number of limits to page to.
   * @param limit
   *          The maximum number of settings to return.
   * @return Find all of the user settings for the current user.
   * @throws UserSettingsServiceException
   *           Thrown if there is a problem getting the user settings.
   */
  private UserSettings getUserSettings(int limit, int offset) throws UserSettingsServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      String username = securityService.getUser().getUsername();
      logger.debug("Getting user settings for '%s' in org '%s'", username, orgId);
      Query q = em.createNamedQuery("UserSettings.findByUserName").setParameter("username", username)
              .setParameter("org", orgId).setMaxResults(limit).setFirstResult(offset);
      List<UserSettingDto> result = q.getResultList();
      if (result.size() == 0) {
        logger.debug("Found no user settings.");
      }
      UserSettings userSettings = new UserSettings();
      for (UserSettingDto userSettingsDto : result) {
        UserSetting userSetting = userSettingsDto.toUserSetting();
        logger.debug("Found user setting id: %d key: %s value: %s", userSetting.getId(), userSetting.getKey()
                .toString(), userSetting.getValue().toString());
        userSettings.addUserSetting(userSetting);
      }
      return userSettings;
    } catch (Exception e) {
      logger.error("Could not get user settings:", e);
      throw new UserSettingsServiceException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
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
    EntityManager em = null;
    EntityTransaction tx = null;
    String orgId = "";
    String username = "";
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      orgId = securityService.getOrganization().getId();
      username = securityService.getUser().getUsername();
      UserSettingDto userSettingDto = new UserSettingDto();
      userSettingDto.setKey(key);
      userSettingDto.setValue(value);
      userSettingDto.setUsername(username);
      userSettingDto.setOrganization(orgId);
      em.persist(userSettingDto);
      tx.commit();
      return userSettingDto.toUserSetting();
    } catch (Exception e) {
      logger.error("Could not update user setting username '%s' org:'%s' key:'%s' value:'%s':",
        username, orgId, key, value, e);
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new UserSettingsServiceException(e);
    } finally {
      if (em != null) {
        em.close();
      }
    }
  }

  /**
   * Get all user settings based upon its key.
   * @param key The key to search for.
   * @return A {@link UserSettingDto} that matches the key.
   * @throws UserSettingsServiceException
   */
  private List<UserSettingDto> getUserSettingsByKey(String key) throws UserSettingsServiceException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      String orgId = securityService.getOrganization().getId();
      String username = securityService.getUser().getUsername();
      logger.debug("Getting user settings for '%s' in org '%s'", username, orgId);
      Query q = em.createNamedQuery("UserSettings.findByKey").setParameter("key", key).setParameter("username", username).setParameter("org", orgId);
      List<UserSettingDto> result = q.getResultList();
      if (result.size() == 0) {
        logger.debug("Found no user settings.");
        return null;
      }

      return result;
    } catch (Exception e) {
      logger.error("Could not get user setting", e);
      throw new UserSettingsServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Update a user setting that currently exists using its key to find it.
   * @param key The key for the user setting.
   * @param value The new value to set for the user setting.
   * @return An updated {@link UserSetting}
   * @throws UserSettingsServiceException
   */
  public UserSetting updateUserSetting(String key, String value, String oldValue) throws UserSettingsServiceException {
    UserSettingDto userSettingDto = null;
    List<UserSettingDto> userSettings = getUserSettingsByKey(key);
    for (UserSettingDto currentUserSetting : userSettings) {
      if (currentUserSetting.getKey().equalsIgnoreCase(key) && currentUserSetting.getValue().equalsIgnoreCase(oldValue)) {
        userSettingDto = currentUserSetting;
      }
    }
    if (userSettingDto == null) {
      throw new UserSettingsServiceException("Unable to find user setting with key " + key + " value " + value + " and old value " + oldValue);
    }
    return updateUserSetting(userSettingDto.getId(), key, value);
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
    EntityManager em = null;
    EntityTransaction tx = null;
    String orgId = "";
    String username = "";
    logger.debug("Updating user setting id: %d key: %s value: %s", id, key, value);
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      orgId = securityService.getOrganization().getId();
      username = securityService.getUser().getUsername();
      UserSettingDto userSettingDto = em.find(UserSettingDto.class, id);
      em.persist(userSettingDto);
      userSettingDto.setKey(key);
      userSettingDto.setValue(value);
      tx.commit();
      return userSettingDto.toUserSetting();
    } catch (Exception e) {
      logger.error("Could not update user setting username '%s' org:'%s' id:'%d' key:'%s' value:'%s':",
        username, orgId, id, key, value, e);
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new UserSettingsServiceException(e);
    } finally {
      if (em != null) {
        em.close();
      }
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
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      UserSettingDto userSettingsDto = em.find(UserSettingDto.class, id);
      tx = em.getTransaction();
      tx.begin();
      em.remove(userSettingsDto);
      tx.commit();
    } catch (Exception e) {
      logger.error("Could not delete user setting '%d':", id, e);
      if (tx.isActive())
        tx.rollback();
      throw new UserSettingsServiceException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }
}
