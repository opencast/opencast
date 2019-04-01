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

package org.opencastproject.composer.impl

import org.opencastproject.util.ReadinessIndicator.ARTIFACT

import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.composer.api.EncodingProfile.MediaType
import org.opencastproject.composer.api.EncodingProfileImpl
import org.opencastproject.util.ConfigurationException
import org.opencastproject.util.ReadinessIndicator

import org.apache.commons.lang3.StringUtils
import org.apache.felix.fileinstall.ArtifactInstaller
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FilenameFilter
import java.io.IOException
import java.util.ArrayList
import java.util.Dictionary
import java.util.HashMap
import java.util.Hashtable
import java.util.Properties

/**
 * This manager class tries to read encoding profiles from the classpath.
 */
class EncodingProfileScanner : ArtifactInstaller {

    /** OSGi bundle context  */
    private var bundleCtx: BundleContext? = null

    /** Sum of profiles files currently installed  */
    private var sumInstalledFiles = 0

    /** The profiles map  */
    private val profiles = HashMap<String, EncodingProfile>()

    /**
     * Returns the list of profiles.
     *
     * @return the profile definitions
     */
    fun getProfiles(): Map<String, EncodingProfile> {
        return profiles
    }

    /**
     * OSGi callback on component activation.
     *
     * @param ctx
     * the bundle context
     */
    internal fun activate(ctx: BundleContext) {
        this.bundleCtx = ctx
    }

    /**
     * Returns the encoding profile for the given identifier or `null` if no such profile has been configured.
     *
     * @param id
     * the profile identifier
     * @return the profile
     */
    fun getProfile(id: String): EncodingProfile {
        return profiles[id]
    }

    /**
     * Returns the list of profiles that are applicable for the given track type.
     *
     * @return the profile definitions
     */
    fun getApplicableProfiles(type: MediaType): Map<String, EncodingProfile> {
        val result = HashMap<String, EncodingProfile>()
        for ((key, profile) in profiles) {
            if (profile.isApplicableTo(type)) {
                result[key] = profile
            }
        }
        return result
    }

    /**
     * Reads the profiles from the given set of properties.
     *
     * @param artifact
     * the properties file
     * @return the profiles found in the properties
     */
    @Throws(IOException::class)
    internal fun loadFromProperties(artifact: File): Map<String, EncodingProfile> {
        // Format name
        val properties = Properties()
        FileInputStream(artifact).use { `in` -> properties.load(`in`) }

        // Find list of formats in properties
        val profileNames = ArrayList<String>()
        for (fullKey in properties.keys) {
            var key = fullKey.toString()
            if (key.startsWith(PROP_PREFIX) && key.endsWith(PROP_NAME)) {
                val separatorLocation = fullKey.toString().lastIndexOf('.')
                key = key.substring(PROP_PREFIX.length, separatorLocation)
                if (!profileNames.contains(key)) {
                    profileNames.add(key)
                } else {
                    throw ConfigurationException("Found duplicate definition for encoding profile '$key'")
                }
            }
        }

        // Load the formats
        val profiles = HashMap<String, EncodingProfile>()
        for (profileId in profileNames) {
            logger.debug("Enabling media format $profileId")
            val profile = loadProfile(profileId, properties, artifact)
            profiles[profileId] = profile
        }

        return profiles
    }

    /**
     * Reads the profile from the given properties
     *
     * @param profile
     * @param properties
     * @param artifact
     * @return the loaded profile or null if profile
     * @throws RuntimeException
     */
    @Throws(ConfigurationException::class)
    private fun loadProfile(profile: String, properties: Properties, artifact: File): EncodingProfile {
        val defaultProperties = ArrayList<String>(10)

        val name = getDefaultProperty(profile, PROP_NAME, properties, defaultProperties)
        if (StringUtils.isBlank(name)) {
            throw ConfigurationException("Distribution profile '$profile' is missing a name ($PROP_NAME).")
        }

        val df = EncodingProfileImpl(profile, name, artifact)

        // Output Type
        val type = getDefaultProperty(profile, PROP_OUTPUT, properties, defaultProperties)
        if (StringUtils.isBlank(type))
            throw ConfigurationException("Output type ($PROP_OUTPUT) of profile '$profile' is missing")
        try {
            df.outputType = MediaType.parseString(StringUtils.trimToEmpty(type))
        } catch (e: IllegalArgumentException) {
            throw ConfigurationException("Output type (" + PROP_OUTPUT + ") '" + type + "' of profile '" + profile
                    + "' is unknown")
        }

        //Suffixes with tags?
        val tags = getTags(profile, properties, defaultProperties)
        if (tags.size > 0) {
            for (tag in tags) {
                val prop = "$PROP_SUFFIX.$tag"
                val suffixObj = getDefaultProperty(profile, prop, properties, defaultProperties)
                df.setSuffix(tag, StringUtils.trim(suffixObj))
            }
        } else {
            // Suffix old stile, without tags
            val suffixObj = getDefaultProperty(profile, PROP_SUFFIX, properties, defaultProperties)
            if (StringUtils.isBlank(suffixObj))
                throw ConfigurationException("Suffix ($PROP_SUFFIX) of profile '$profile' is missing")
            df.suffix = StringUtils.trim(suffixObj)
        }

        // Applicable to the following track categories
        val applicableObj = getDefaultProperty(profile, PROP_APPLICABLE, properties, defaultProperties)
        if (StringUtils.isBlank(applicableObj))
            throw ConfigurationException("Input type ($PROP_APPLICABLE) of profile '$profile' is missing")
        df.setApplicableType(MediaType.parseString(StringUtils.trimToEmpty(applicableObj)))

        val jobLoad = getDefaultProperty(profile, PROP_JOBLOAD, properties, defaultProperties)
        if (!StringUtils.isBlank(jobLoad)) {
            df.jobLoad = java.lang.Float.valueOf(jobLoad!!)
            logger.debug("Setting job load for profile {} to {}", profile, jobLoad)
        }

        // Look for extensions
        val extensionKey = "$PROP_PREFIX$profile."
        for ((key1, value) in properties) {
            val key = key1.toString()
            if (key.startsWith(extensionKey) && !defaultProperties.contains(key)) {
                val k = key.substring(extensionKey.length)
                val v = StringUtils.trimToEmpty(value.toString())
                df.addExtension(k, v)
            }
        }

        return df
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactListener.canHandle
     */
    override fun canHandle(artifact: File): Boolean {
        return "encoding" == artifact.parentFile.name && artifact.name.endsWith(".properties")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactInstaller.install
     */
    @Throws(Exception::class)
    override fun install(artifact: File) {
        logger.info("Registering encoding profiles from {}", artifact)
        try {
            val profileMap = loadFromProperties(artifact)
            for ((key, value) in profileMap) {
                logger.info("Installed profile {}", value.identifier)
                profiles[key] = value
            }
            sumInstalledFiles++
        } catch (e: Exception) {
            logger.error("Encoding profiles could not be read from {}: {}", artifact, e.message)
        }

        // Determine the number of available profiles
        val filesInDirectory = artifact.parentFile.list { arg0, name -> name.endsWith(".properties") }

        // Once all profiles have been loaded, announce readiness
        if (filesInDirectory!!.size == sumInstalledFiles) {
            val properties = Hashtable<String, String>()
            properties[ARTIFACT] = "encodingprofile"
            logger.debug("Indicating readiness of encoding profiles")
            bundleCtx!!.registerService(ReadinessIndicator::class.java.name, ReadinessIndicator(), properties)
            logger.info("All {} encoding profiles installed", filesInDirectory.size)
        } else {
            logger.debug("{} of {} encoding profiles installed", sumInstalledFiles, filesInDirectory.size)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactInstaller.uninstall
     */
    @Throws(Exception::class)
    override fun uninstall(artifact: File) {
        val iter = profiles.values.iterator()
        while (iter.hasNext()) {
            val profile = iter.next()
            if (artifact == profile.source) {
                logger.info("Uninstalling profile {}", profile.identifier)
                iter.remove()
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactInstaller.update
     */
    @Throws(Exception::class)
    override fun update(artifact: File) {
        uninstall(artifact)
        install(artifact)
    }

    companion object {

        /** Prefix for encoding profile property keys  */
        private val PROP_PREFIX = "profile."

        /* Property names */
        private val PROP_NAME = ".name"
        private val PROP_APPLICABLE = ".input"
        private val PROP_OUTPUT = ".output"
        private val PROP_SUFFIX = ".suffix"
        private val PROP_JOBLOAD = ".jobload"

        /** The logging instance  */
        private val logger = LoggerFactory.getLogger(EncodingProfileScanner::class.java)

        /**
         * Returns the default property and registers the property key in the list.
         *
         * @param profile
         * the profile identifier
         * @param keySuffix
         * the key suffix, like ".name"
         * @param properties
         * the properties
         * @param list
         * the list of default property keys
         * @return the property value or `null`
         */
        private fun getDefaultProperty(profile: String, keySuffix: String, properties: Properties, list: MutableList<String>): String? {
            val key = PROP_PREFIX + profile + keySuffix
            list.add(key)
            return StringUtils.trimToNull(properties.getProperty(key))
        }

        /**
         * Get any tags that might follow the PROP_SUFFIX
         * @param profile
         * the profile identifier
         * @param properties
         * the properties
         * @param list
         * the list of default property keys
         * @return A list of tags for output files
         */

        private fun getTags(profile: String, properties: Properties, list: MutableList<String>): List<String> {
            val keys = properties.keys
            val key = PROP_PREFIX + profile + PROP_SUFFIX

            val tags = ArrayList<String>()
            for (o in keys) {
                val k = o.toString()
                if (k.startsWith(key)) {
                    if (k.substring(key.length).length > 0) {
                        list.add(k)
                        tags.add(k.substring(key.length + 1))
                    }
                }
            }
            return tags
        }
    }

}
