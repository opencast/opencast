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


package org.opencastproject.mediapackage.identifier

import org.opencastproject.util.ConfigurationException

/**
 * This class is used to create instances of an id builder. To specify your own implementation of the id builder, you
 * simply have to provide the class name of the desired implementation by setting the system property
 * `opencast.idbuilder` accordingly.
 */
class IdBuilderFactory
/**
 * Private method to create a new id builder factory.
 */
private constructor() {

    /** The default builder implementation  */
    private var builder: IdBuilder? = null

    init {
        val className = System.getProperty(PROPERTY_NAME)
        if (className != null) {
            builderClassName = className
        }
    }

    /**
     * Factory method that returns an instance of an id builder.
     *
     *
     * It uses the following ordered lookup procedure to determine which implementation of the [IdBuilder] interface
     * to use:
     *
     *  * Implementation specified using the `opencast.idbuilder` system property
     *  * Platform default implementation
     *
     *
     * @return the id builder
     * @throws ConfigurationException
     * If the builder cannot be instantiated
     */
    @Throws(ConfigurationException::class)
    fun newIdBuilder(): IdBuilder? {
        if (builder == null) {
            try {
                val builderClass = Class.forName(builderClassName)
                builder = builderClass.newInstance() as IdBuilder
            } catch (e: ClassNotFoundException) {
                throw ConfigurationException("Class not found while creating id builder: " + e.message, e)
            } catch (e: InstantiationException) {
                throw ConfigurationException("Instantiation exception while creating id builder: " + e.message, e)
            } catch (e: IllegalAccessException) {
                throw ConfigurationException("Access exception while creating id builder: " + e.message, e)
            }

        }
        return builder
    }

    companion object {

        /** Class name for the default id builder  */
        private val BUILDER_CLASS = "org.opencastproject.mediapackage.identifier.UUIDIdBuilderImpl"

        /** Name of the system property  */
        val PROPERTY_NAME = "opencast.idbuilder"

        /** The implementation class name  */
        private var builderClassName = BUILDER_CLASS

        /** The singleton instance of this factory  */
        private val factory = IdBuilderFactory()

        /**
         * Returns an instance of a HandleBuilderFactory.
         *
         * @return the id builder factory
         * @throws ConfigurationException
         * if the factory cannot be instantiated
         */
        @Throws(ConfigurationException::class)
        fun newInstance(): IdBuilderFactory {
            return factory
        }
    }

}
