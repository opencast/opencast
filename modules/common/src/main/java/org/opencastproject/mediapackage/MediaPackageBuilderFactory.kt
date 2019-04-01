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


package org.opencastproject.mediapackage

import org.opencastproject.util.ConfigurationException

/**
 * Factory to retrieve instances of a media package builder. Use the static method [.newInstance] to obtain a
 * reference to a concrete implementation of a `MediaPackageBuilderFactory`. This instance can then be used
 * to create or load media packages.
 *
 *
 * The factory can be configured by specifying the concrete implementation class through the system property
 * `org.opencastproject.mediapackage.builder`.
 *
 */
class MediaPackageBuilderFactory
/**
 * Private method to create a new media package builder factory.
 */
private constructor() {

    init {
        val className = System.getProperty(PROPERTY_NAME)
        if (className != null) {
            builderClassName = className
        }
    }

    /**
     * Factory method that returns an instance of a media package builder.
     *
     *
     * It uses the following ordered lookup procedure to determine which implementation of the [MediaPackageBuilder]
     * interface to use:
     *
     *  * Implementation specified using the `org.opencastproject.mediapackage.builder` system property
     *  * Platform default implementation
     *
     *
     * @return the media package builder
     * @throws ConfigurationException
     * If the builder cannot be instantiated
     */
    @Throws(ConfigurationException::class)
    fun newMediaPackageBuilder(): MediaPackageBuilder? {
        var builder: MediaPackageBuilder? = null
        try {
            val builderClass = Class.forName(builderClassName)
            builder = builderClass.newInstance() as MediaPackageBuilder
        } catch (e: ClassNotFoundException) {
            throw ConfigurationException("Class not found while creating media package builder: " + e.message, e)
        } catch (e: InstantiationException) {
            throw ConfigurationException("Instantiation exception while creating media package builder: " + e.message, e)
        } catch (e: IllegalAccessException) {
            throw ConfigurationException("Access exception while creating media package builder: " + e.message, e)
        }

        return builder
    }

    companion object {

        /** Class name for the default media package builder  */
        private val BUILDER_CLASS = "org.opencastproject.mediapackage.MediaPackageBuilderImpl"

        /** Name of the system property  */
        val PROPERTY_NAME = "org.opencastproject.mediapackage.builder"

        /** The implementation class name  */
        private var builderClassName = BUILDER_CLASS

        /** The singleton instance of this factory  */
        private val factory = MediaPackageBuilderFactory()

        /**
         * Returns an instance of a MediaPackageBuilderFactory.
         *
         * @return the media package builder factory
         * @throws ConfigurationException
         * if the factory cannot be instantiated
         */
        @Throws(ConfigurationException::class)
        fun newInstance(): MediaPackageBuilderFactory {
            return factory
        }
    }

}
