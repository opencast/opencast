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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.adminui.usersettings

import org.opencastproject.adminui.usersettings.persistence.UserSettingDto
import org.opencastproject.adminui.usersettings.persistence.UserSettingsServiceException
import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.util.Log

import org.apache.commons.lang3.exception.ExceptionUtils
import org.osgi.service.component.ComponentContext

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.Query

/**
 * Finds the user settings and message signatures from the current user.
 */
class UserSettingsService {

    /** Factory used to create [EntityManager]s for transactions  */
    protected var emf: EntityManagerFactory

    /** The user directory service  */
    protected var userDirectoryService: UserDirectoryService

    /** The organization directory service  */
    protected var organizationDirectoryService: OrganizationDirectoryService

    /** The security service  */
    protected var securityService: SecurityService

    /**
     * @return Finds the total number of user settings for the current user.
     * @throws UserSettingsServiceException
     */
    private val userSettingsTotal: Int
        @Throws(UserSettingsServiceException::class)
        get() {
            var em: EntityManager? = null
            try {
                em = emf.createEntityManager()
                val orgId = securityService.organization.id
                val username = securityService.user.username
                val q = em!!.createNamedQuery("UserSettings.countByUserName").setParameter("username", username).setParameter("org", orgId)
                val countResult = q.singleResult as Number
                return countResult.toInt()
            } catch (e: Exception) {
                logger.error("Could not count message signatures: %s", ExceptionUtils.getStackTrace(e))
                throw UserSettingsServiceException(e)
            } finally {
                em?.close()
            }
        }

    /**
     * Creates [EntityManagerFactory] using persistence provider and properties passed via OSGi.
     *
     * @param cc
     */
    fun activate(cc: ComponentContext) {
        logger.info("Activating persistence manager for user settings")
    }

    /** OSGi DI  */
    fun setEntityManagerFactory(emf: EntityManagerFactory) {
        this.emf = emf
    }

    /**
     * OSGi callback to set user directory service.
     *
     * @param userDirectoryService
     * user directory service
     */
    fun setUserDirectoryService(userDirectoryService: UserDirectoryService) {
        this.userDirectoryService = userDirectoryService
    }

    /**
     * OSGi callback to set the security service.
     *
     * @param securityService
     * the security service
     */
    fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /**
     * OSGi callback to set the organization directory service.
     *
     * @param organizationDirectoryService
     * the organization directory service
     */
    fun setOrganizationDirectoryService(organizationDirectoryService: OrganizationDirectoryService) {
        this.organizationDirectoryService = organizationDirectoryService
    }

    /**
     * Finds the user settings for the current user.
     *
     * @param limit
     * The maximum limit of results to return.
     * @param offset
     * The starting page offset.
     * @return The user settings for the current user.
     * @throws UserSettingsServiceException
     */
    @Throws(UserSettingsServiceException::class)
    fun findUserSettings(limit: Int, offset: Int): UserSettings {
        var userSettings: UserSettings? = getUserSettings(limit, offset)
        if (userSettings == null) {
            userSettings = UserSettings()
        }
        userSettings.total = userSettingsTotal
        userSettings.limit = limit
        userSettings.offset = offset
        return userSettings
    }

    /**
     * @param offset
     * The number of limits to page to.
     * @param limit
     * The maximum number of settings to return.
     * @return Find all of the user settings for the current user.
     * @throws UserSettingsServiceException
     * Thrown if there is a problem getting the user settings.
     */
    @Throws(UserSettingsServiceException::class)
    private fun getUserSettings(limit: Int, offset: Int): UserSettings {
        var em: EntityManager? = null
        try {
            em = emf.createEntityManager()
            val orgId = securityService.organization.id
            val username = securityService.user.username
            logger.debug("Getting user settings for '%s' in org '%s'", username, orgId)
            val q = em!!.createNamedQuery("UserSettings.findByUserName").setParameter("username", username)
                    .setParameter("org", orgId).setMaxResults(limit).setFirstResult(offset)
            val result = q.resultList
            if (result.size == 0) {
                logger.debug("Found no user settings.")
            }
            val userSettings = UserSettings()
            for (userSettingsDto in result) {
                val userSetting = userSettingsDto.toUserSetting()
                logger.debug("Found user setting id: %d key: %s value: %s", userSetting.id, userSetting.key
                        .toString(), userSetting.value.toString())
                userSettings.addUserSetting(userSetting)
            }
            return userSettings
        } catch (e: Exception) {
            logger.error("Could not get user settings: %s", ExceptionUtils.getStackTrace(e))
            throw UserSettingsServiceException(e)
        } finally {
            em?.close()
        }
    }

    /**
     * Create a new user setting key value pair.
     *
     * @param key
     * The key to use for the current user setting.
     * @param value
     * The value of the user setting.
     * @return A new user setting object
     * @throws UserSettingsServiceException
     */
    @Throws(UserSettingsServiceException::class)
    fun addUserSetting(key: String, value: String): UserSetting {
        var em: EntityManager? = null
        var tx: EntityTransaction? = null
        var orgId = ""
        var username = ""
        try {
            em = emf.createEntityManager()
            tx = em!!.transaction
            tx!!.begin()
            orgId = securityService.organization.id
            username = securityService.user.username
            val userSettingDto = UserSettingDto()
            userSettingDto.key = key
            userSettingDto.value = value
            userSettingDto.username = username
            userSettingDto.organization = orgId
            em.persist(userSettingDto)
            tx.commit()
            return userSettingDto.toUserSetting()
        } catch (e: Exception) {
            logger.error("Could not update user setting username '%s' org:'%s' key:'%s' value:'%s':%s", username, orgId, key, value, ExceptionUtils.getStackTrace(e))
            if (tx!!.isActive) {
                tx.rollback()
            }
            throw UserSettingsServiceException(e)
        } finally {
            em?.close()
        }
    }

    /**
     * Get all user settings based upon its key.
     * @param key The key to search for.
     * @return A [UserSettingDto] that matches the key.
     * @throws UserSettingsServiceException
     */
    @Throws(UserSettingsServiceException::class)
    private fun getUserSettingsByKey(key: String): List<UserSettingDto>? {
        var em: EntityManager? = null
        try {
            em = emf.createEntityManager()
            val orgId = securityService.organization.id
            val username = securityService.user.username
            logger.debug("Getting user settings for '%s' in org '%s'", username, orgId)
            val q = em!!.createNamedQuery("UserSettings.findByKey").setParameter("key", key).setParameter("username", username).setParameter("org", orgId)
            val result = q.resultList
            if (result.size == 0) {
                logger.debug("Found no user settings.")
                return null
            }

            return result
        } catch (e: Exception) {
            logger.error("Could not get user setting", e)
            throw UserSettingsServiceException(e)
        } finally {
            em?.close()
        }
    }

    /**
     * Update a user setting that currently exists using its key to find it.
     * @param key The key for the user setting.
     * @param value The new value to set for the user setting.
     * @return An updated [UserSetting]
     * @throws UserSettingsServiceException
     */
    @Throws(UserSettingsServiceException::class)
    fun updateUserSetting(key: String, value: String, oldValue: String): UserSetting {
        var userSettingDto: UserSettingDto? = null
        val userSettings = getUserSettingsByKey(key)
        for (currentUserSetting in userSettings!!) {
            if (currentUserSetting.key!!.equals(key, ignoreCase = true) && currentUserSetting.value!!.equals(oldValue, ignoreCase = true)) {
                userSettingDto = currentUserSetting
            }
        }
        if (userSettingDto == null) {
            throw UserSettingsServiceException("Unable to find user setting with key $key value $value and old value $oldValue")
        }
        return updateUserSetting(userSettingDto.id, key, value)
    }

    /**
     * Update a user setting that currently exists using its unique id to find it.
     * @param id The id for the user setting.
     * @param key The key for the user setting.
     * @param value The value for the user setting.
     * @return The updated [UserSetting].
     * @throws UserSettingsServiceException
     */
    @Throws(UserSettingsServiceException::class)
    fun updateUserSetting(id: Long, key: String, value: String): UserSetting {
        var em: EntityManager? = null
        var tx: EntityTransaction? = null
        var orgId = ""
        var username = ""
        logger.debug("Updating user setting id: %d key: %s value: %s", id, key, value)
        try {
            em = emf.createEntityManager()
            tx = em!!.transaction
            tx!!.begin()
            orgId = securityService.organization.id
            username = securityService.user.username
            val userSettingDto = em.find(UserSettingDto::class.java, id)
            em.persist(userSettingDto)
            userSettingDto.key = key
            userSettingDto.value = value
            tx.commit()
            return userSettingDto.toUserSetting()
        } catch (e: Exception) {
            logger.error("Could not update user setting username '%s' org:'%s' id:'%d' key:'%s' value:'%s':\n%s", username, orgId, id, key, value, ExceptionUtils.getStackTrace(e))
            if (tx!!.isActive) {
                tx.rollback()
            }
            throw UserSettingsServiceException(e)
        } finally {
            em?.close()
        }
    }

    /**
     * Delete a user setting by using a unique id to find it.
     *
     * @param id
     * The unique id for the user setting.
     * @throws UserSettingsServiceException
     */
    @Throws(UserSettingsServiceException::class)
    fun deleteUserSetting(id: Long) {
        var em: EntityManager? = null
        var tx: EntityTransaction? = null
        try {
            em = emf.createEntityManager()
            val userSettingsDto = em!!.find(UserSettingDto::class.java, id)
            tx = em.transaction
            tx!!.begin()
            em.remove(userSettingsDto)
            tx.commit()
        } catch (e: Exception) {
            logger.error("Could not delete user setting '%d': %s", id, ExceptionUtils.getStackTrace(e))
            if (tx!!.isActive)
                tx.rollback()
            throw UserSettingsServiceException(e)
        } finally {
            em?.close()
        }
    }

    companion object {
        val PERSISTENCE_UNIT = "org.opencastproject.adminui"

        /** Logging utilities  */
        private val logger = Log.mk(UserSettingsService::class.java)
    }
}
