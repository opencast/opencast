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
 * Factory to retreive instances of a media package element builder. Use the static method [.newInstance] to
 * obtain a reference to a concrete implementation of a `MediaPackageElementBuilderFactory`. This instance
 * can then be used to create or load media package elements.
 *
 *
 * The factory can be configured by specifying the concrete implementation class through the system property
 * `org.opencastproject.mediapackage.elementbuilder`.
 *
 */
class MediaPackageElementBuilderFactory {

    /** The default builder implementation  */
    private var builder: MediaPackageElementBuilder? = null

    /**
     * Factory method that returns an instance of a media package element builder.
     *
     *
     * It uses the following ordered lookup procedure to determine which implementation of the
     * [MediaPackageElementBuilder] interface to use:
     *
     *  * Implementation specified using the `org.opencastproject.mediapackage.elementbuilder` system property
     *
     *  * Platform default implementation
     *
     *
     * @return the media package element builder
     * @throws ConfigurationException
     * If the builder cannot be instantiated
     */
    @Throws(ConfigurationException::class)
    fun newElementBuilder(): MediaPackageElementBuilder? {
        if (builder == null) {
            try {
                val builderClass = Class.forName(builderClassName, true,
                        MediaPackageElementBuilderFactory::class.java!!.getClassLoader())
                builder = builderClass.newInstance() as MediaPackageElementBuilder
            } catch (e: ClassNotFoundException) {
                throw ConfigurationException("Class not found while creating element builder: " + e.message, e)
            } catch (e: InstantiationException) {
                throw ConfigurationException("Instantiation exception while creating element builder: " + e.message, e)
            } catch (e: IllegalAccessException) {
                throw ConfigurationException("Access exception while creating element builder: " + e.message, e)
            } catch (e: Exception) {
                throw ConfigurationException("Exception while creating element builder: " + e.message, e)
            }

        }
        return builder
    }

    companion object {

        /** The implementation class name  */
        private val builderClassName = "org.opencastproject.mediapackage.MediaPackageElementBuilderImpl"

        /** The singleton instance of this factory  */
        private val factory = MediaPackageElementBuilderFactory()

        /**
         * Returns an instance of a MediaPackageElementBuilderFactory.
         *
         * @return the media package element builder factory
         * @throws ConfigurationException
         * if the factory cannot be instantiated
         */
        @Throws(ConfigurationException::class)
        fun newInstance(): MediaPackageElementBuilderFactory {
            return factory
        }
    }

}
