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

package org.opencastproject.feed.scanner

import org.opencastproject.util.ReadinessIndicator.ARTIFACT

import org.opencastproject.feed.api.FeedGenerator
import org.opencastproject.search.api.SearchService
import org.opencastproject.series.api.SeriesService
import org.opencastproject.util.ReadinessIndicator

import org.apache.commons.io.IOUtils
import org.apache.felix.fileinstall.ArtifactInstaller
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.FilenameFilter
import java.util.Dictionary
import java.util.HashMap
import java.util.Hashtable
import java.util.Properties

/**
 * Installs feeds matching "*.properties" in the feeds watch directory.
 */
class FeedRegistrationScanner : ArtifactInstaller {

    /** A map to keep track of each feed registration file and feed generator it produces  */
    protected var generators: MutableMap<File, ServiceRegistration<*>> = HashMap()

    /** The search service to use in each feed generator  */
    protected var searchService: SearchService

    /** The series service to be used by the series feeds  */
    protected var seriesService: SeriesService

    /** The bundle context for this osgi component  */
    protected var bundleContext: BundleContext? = null

    /** Sum of profiles files currently installed  */
    private var sumInstalledFiles = 0

    /** Sets the search service  */
    fun setSearchService(searchService: SearchService) {
        this.searchService = searchService
    }

    /** Sets the series service  */
    fun setSeriesService(seriesService: SeriesService) {
        this.seriesService = seriesService
    }

    /**
     * Activates the component
     *
     * @param cc
     * the component's context
     */
    protected fun activate(cc: ComponentContext) {
        this.bundleContext = cc.bundleContext
    }

    /**
     * Deactivates the component
     */
    protected fun deactivate() {
        this.bundleContext = null
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactListener.canHandle
     */
    override fun canHandle(artifact: File): Boolean {
        return "feeds" == artifact.parentFile.name && artifact.name.endsWith(".properties")
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactInstaller.install
     */
    @Throws(Exception::class)
    override fun install(artifact: File) {
        logger.info("Installing a feed from '{}'", artifact.name)
        val props = Properties()
        var `in`: FileInputStream? = null
        try {
            `in` = FileInputStream(artifact)
            props.load(`in`)
        } finally {
            IOUtils.closeQuietly(`in`)
        }
        // Always include the server URL obtained from the bundle context
        props["org.opencastproject.server.url"] = bundleContext!!.getProperty("org.opencastproject.server.url")
        val clazz = javaClass.classLoader.loadClass(props.getProperty(FEED_CLASS))
        val generator = clazz.newInstance() as FeedGenerator
        generator.setSearchService(searchService)
        generator.setSeriesService(seriesService)
        generator.initialize(props)
        val reg = bundleContext!!.registerService(FeedGenerator::class.java.name, generator, null)
        generators[artifact] = reg
        sumInstalledFiles++

        // Determine the number of available profiles
        val filesInDirectory = artifact.parentFile.list { arg0, name -> name.endsWith(".properties") }

        // Once all profiles have been loaded, announce readiness
        if (filesInDirectory!!.size == sumInstalledFiles) {
            val properties = Hashtable<String, String>()
            properties[ARTIFACT] = "feed"
            logger.debug("Indicating readiness of feed")
            bundleContext!!.registerService(ReadinessIndicator::class.java.name, ReadinessIndicator(), properties)
            logger.info("All {} feeds installed", filesInDirectory.size)
        } else {
            logger.debug("{} of {} feeds installed", sumInstalledFiles, filesInDirectory.size)
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactInstaller.uninstall
     */
    @Throws(Exception::class)
    override fun uninstall(artifact: File) {
        val reg = generators[artifact]
        if (reg != null) {
            reg.unregister()
            generators.remove(artifact)
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
        val FEED_CLASS = "feed.class"
        val FEED_URI = "feed.uri"
        val FEED_SELECTOR = "feed.selector"
        val FEED_ENTRY = "feed.entry"

        private val logger = LoggerFactory.getLogger(FeedRegistrationScanner::class.java)
    }
}
