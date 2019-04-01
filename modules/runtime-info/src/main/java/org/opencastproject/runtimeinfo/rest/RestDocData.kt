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

package org.opencastproject.runtimeinfo.rest

import org.opencastproject.util.doc.DocData
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse

import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Vector
import java.util.regex.Matcher
import java.util.regex.Pattern

import javax.ws.rs.Path
import javax.ws.rs.Produces

/**
 * This is the document model class which holds the data about a set of rest endpoints.
 */
class RestDocData
/**
 * Create the base data object for creating REST documentation.
 *
 * @param name
 * the name of the set of rest endpoints (must be alphanumeric (includes _) and no spaces or special chars)
 * @param title
 * [OPTIONAL] the title of the documentation
 * @param url
 * this is the absolute base URL for this endpoint, do not include the trailing slash (e.g. /workflow)
 * @param notes
 * [OPTIONAL] an array of notes to add into the end of the documentation
 * @throws IllegalArgumentException
 * if the url is null or empty
 */
@Throws(IllegalArgumentException::class)
constructor(name: String, title: String, url: String?, notes: Array<String>,
            /**
             * The service object which this RestDocData is about.
             */
            private val serviceObject: Any,
            /**
             * A map of macro values for REST documentation.
             */
            private val macros: Map<String, String>) : DocData(name, title, notes) {

    /**
     * List of RestEndpointHolderData which each stores a group of endpoints. Currently there are 2 groups, READ group and
     * WRITE group.
     */
    protected var holders: MutableList<RestEndpointHolderData>

    /**
     * Gets the path to the default template (a .xhtml file).
     *
     * @return the path to the default template file
     */
    override val defaultTemplatePath: String
        get() = DocData.TEMPLATE_DEFAULT

    init {
        if (url == null || "" == url) {
            throw IllegalArgumentException("URL cannot be blank.")
        }
        meta["url"] = url
        // create the endpoint holders
        holders = Vector(2)
        holders.add(RestEndpointHolderData(READ_ENDPOINT_HOLDER_NAME, "Read"))
        holders.add(RestEndpointHolderData(WRITE_ENDPOINT_HOLDER_NAME, "Write"))
    }

    /**
     * Verify the integrity of this object. If its data is verified to be okay, it return a map representation of this
     * RestDocData object.
     *
     * @return a map representation of this RestDocData object if this object passes the verification
     *
     * @throws IllegalStateException
     * if any path parameter is not present in the endpoint's path
     */
    @Throws(IllegalStateException::class)
    override fun toMap(): Map<String, Any> {
        val m = LinkedHashMap<String, Any>()
        m["meta"] = meta
        m["notes"] = notes
        // only pass through the holders with things in them
        val holdersList = ArrayList<RestEndpointHolderData>()
        for (holder in holders) {
            if (!holder.endpoints.isEmpty()) {
                for (endpoint in holder.endpoints) {
                    // Validate the endpoint path matches the specified path parameters.
                    // First, it makes sure that every path parameter is present in the endpoint's path.
                    if (!endpoint.pathParams.isEmpty()) {
                        for (param in endpoint.pathParams) {
                            // Some endpoints allow for arbitrary characters, including slashes, in their path parameters, so we
                            // must check for both {param} and {param:.*}.
                            if (!endpoint.path.contains("{" + param.name + "}") && !endpoint.path.contains("{" + param.name + ":")) {
                                throw IllegalStateException("Path (" + endpoint.path + ") does not match path parameter ("
                                        + param.name + ") for endpoint (" + endpoint.name
                                        + "), the path must contain all path parameter names.")
                            }
                        }
                    }
                    // Then, it makes sure that the number of path parameter patterns in the path is the same as the number of
                    // path parameters in the endpoint.
                    // The following part uses a regular expression to find patterns like {something}.
                    val pattern = Pattern.compile(PATH_PARAM_COUNTING_REGEX)
                    val matcher = pattern.matcher(endpoint.path)

                    var count = 0
                    while (matcher.find()) {
                        count++
                    }
                    if (count != endpoint.pathParams.size) {
                        throw IllegalStateException("Path (" + endpoint.path + ") does not match path parameters ("
                                + endpoint.pathParams + ") for endpoint (" + endpoint.name
                                + "), the path must contain the same number of path parameters (" + count
                                + ") as the pathParams list (" + endpoint.pathParams.size + ").")
                    }
                }
                holdersList.add(holder)
            }
        }
        m["endpointHolders"] = holdersList
        return m
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    override fun toString(): String {
        return "DOC:meta=$meta, notes=$notes, $holders"
    }

    /**
     * Add an endpoint to this documentation using and assign it to the correct type group (read/write).
     *
     * @param type
     * the type of this endpoint (RestEndpointData.Type.READ or RestEndpointData.Type.WRITE)
     * @param endpoint
     * the endpoint to be added
     * @throws IllegalStateException
     * if the endpoint cannot be assigned to a group
     */
    @Throws(IllegalStateException::class)
    private fun addEndpoint(type: String, endpoint: RestEndpointData) {
        var currentHolder: RestEndpointHolderData? = null
        for (holder in holders) {
            if (type.equals(holder.name, ignoreCase = true)) {
                currentHolder = holder
                break
            }
        }
        if (currentHolder == null) {
            throw IllegalStateException("Could not find holder of type: $type.")
        }
        currentHolder.addEndPoint(endpoint)
    }

    /**
     * Creates an abstract section which is displayed at the top of the documentation page.
     *
     * @param abstractText
     * any text to place at the top of the document, can be html markup but must be valid
     */
    fun setAbstract(abstractText: String) {
        if (isBlank(abstractText)) {
            meta.remove("abstract")
        } else {
            meta["abstract"] = abstractText
        }
    }

    /**
     * Add an endpoint to the Rest documentation.
     *
     * @param restQuery
     * the RestQuery annotation type storing information of an endpoint
     * @param returnType
     * the return type for this endpoint. If this is [javax.xml.bind.annotation.XmlRootElement] or
     * [javax.xml.bind.annotation.XmlRootElement], the XML schema for the class will be made available to
     * clients
     * @param produces
     * the return type(s) of this endpoint, values should be constants from [javax.ws.rs.core.MediaType](http://jackson.codehaus.org/javadoc/jax-rs/1.0/javax/ws/rs/core/MediaType.html) or ExtendedMediaType
     * (org.opencastproject.util.doc.rest.ExtendedMediaType).
     * @param httpMethodString
     * the HTTP method of this endpoint (e.g. GET, POST)
     * @param path
     * the path of this endpoint
     */
    fun addEndpoint(restQuery: RestQuery, returnType: Class<*>, produces: Produces?, httpMethodString: String,
                    path: Path) {
        val pathValue = if (path.value().startsWith("/")) path.value() else "/" + path.value()
        val endpoint = RestEndpointData(returnType, restQuery.name, httpMethodString, pathValue,
                restQuery.description)
        // Add return description if needed
        if (!restQuery.returnDescription.isEmpty()) {
            endpoint.addNote("Return value description: " + restQuery.returnDescription)
        }

        // Add formats
        if (produces != null) {
            for (format in produces.value()) {
                endpoint.addFormat(RestFormatData(format))
            }
        }

        // Add responses
        for (restResp in restQuery.reponses) {
            endpoint.addStatus(restResp)
        }

        // Add body parameter
        if (restQuery.bodyParameter.type !== RestParameter.Type.NO_PARAMETER) {
            endpoint.addBodyParam(restQuery.bodyParameter)
        }

        // Add path parameter
        for (pathParam in restQuery.pathParameters) {
            endpoint.addPathParam(RestParamData(pathParam))
        }

        // Add query parameter (required and optional)
        for (restParam in restQuery.restParameters) {
            if (restParam.isRequired) {
                endpoint.addRequiredParam(RestParamData(restParam))
            } else {
                endpoint.addOptionalParam(RestParamData(restParam))
            }
        }

        // Set the test form after all parameters are added.
        endpoint.setTestForm(RestFormData(endpoint))

        // Add the endpoint to the corresponding group based on its HTTP method
        if ("GET".equals(httpMethodString, ignoreCase = true) || "HEAD".equals(httpMethodString, ignoreCase = true)) {
            addEndpoint(READ_ENDPOINT_HOLDER_NAME, endpoint)
        } else if ("DELETE".equals(httpMethodString, ignoreCase = true) || "POST".equals(httpMethodString, ignoreCase = true)
                || "PUT".equals(httpMethodString, ignoreCase = true)) {
            addEndpoint(WRITE_ENDPOINT_HOLDER_NAME, endpoint)
        }
    }

    companion object {

        /**
         * The name to identify the endpoint holder for read endpoints (get/head).
         */
        private val READ_ENDPOINT_HOLDER_NAME = "READ"

        /**
         * The name to identify the endpoint holder for write endpoints (delete,post,put).
         */
        private val WRITE_ENDPOINT_HOLDER_NAME = "WRITE"

        /**
         * Regular expression used to count the number of path parameters in a path.
         */
        val PATH_PARAM_COUNTING_REGEX = "\\{(.+?)\\}"

        /**
         * Validates paths: VALID: /sample , /sample/{thing} , /{my}/{path}.xml , /my/fancy_path/is/{awesome}.{FORMAT}
         * INVALID: sample, /sample/, /sa#$%mple/path
         *
         * @param path
         * the path value to check
         * @return true if this path is valid, false otherwise
         */
        fun isValidPath(path: String?): Boolean {
            return path != null && path.matches("^/$|^/[\\w/{}|:.*+]*[\\w}.]$".toRegex())
        }
    }

}
