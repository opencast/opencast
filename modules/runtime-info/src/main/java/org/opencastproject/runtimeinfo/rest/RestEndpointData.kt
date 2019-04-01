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

import org.opencastproject.util.JaxbXmlSchemaGenerator
import org.opencastproject.util.doc.DocData
import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestResponse

import org.apache.commons.lang3.StringEscapeUtils

import java.util.ArrayList
import java.util.Vector

class RestEndpointData
/**
 * Create a new basic endpoint, you should use the add methods to fill in the rest of the information about the
 * endpoint data
 *
 * @param returnType
 * the endpoint's return type
 * @param name
 * the endpoint's name (this should be unique in the same type of endpoints)
 * @param httpMethod
 * the HTTP method used for this endpoint
 * @param path
 * the path for this endpoint (e.g. /search OR /add/{id})
 * @param description
 * [optional] the description of this endpoint
 * @throws IllegalArgumentException
 * if name is null, name is not alphanumeric, method is null, path is null or path is not valid.
 */
@Throws(IllegalArgumentException::class)
constructor(returnType: Class<*>,
        /**
         * The name of the endpoint, which should be unique. In the documentation page, the same type of endpoints are shown
         * in ascending order of name.
         */
            /**
             * Gets the name of this endpoint.
             *
             * @return the name of this endpoint
             */
            val name: String, httpMethod: String?,
        /**
         * The path for this endpoint (e.g. /search OR /add/{id}).
         */
            /**
             * Gets the path for this endpoint.
             *
             * @return the path for this endpoint
             */
            val path: String,
        /**
         * The description of this endpoint.
         */
            /**
             * Gets the description of this endpoint.
             *
             * @return the description of this endpoint
             */
            val description: String) : Comparable<RestEndpointData> {

    /**
     * The HTTP method used to invoke the endpoint.
     */
    /**
     * Gets the name of HTTP method used to invoke this endpoint.
     *
     * @return the name of HTTP method used to invoke this endpoint
     */
    val method: String

    /**
     * The body parameter of this endpoint.
     */
    /**
     * Gets the body parameter of this endpoint.
     *
     * @return the body parameter of this endpoint
     */
    var bodyParam: RestParamData? = null
        private set

    /**
     * The list of path parameters of this endpoint.
     */
    private var pathParams: MutableList<RestParamData>? = null

    /**
     * The list of required query parameters of this endpoint.
     */
    private var requiredParams: MutableList<RestParamData>? = null

    /**
     * The list of optional query parameters of this endpoint.
     */
    private var optionalParams: MutableList<RestParamData>? = null

    /**
     * The list of notes (i.e. extra information) of this endpoint.
     */
    private var notes: MutableList<String>? = null

    /**
     * The list of formats returned by this endpoint.
     */
    private var formats: MutableList<RestFormatData>? = null

    /**
     * The list of HTTP responses returned by this endpoint.
     */
    private var statuses: MutableList<StatusData>? = null

    /**
     * The form for testing this endpoint in the documentation page.
     */
    /**
     * Gets the form for testing this endpoint in the documentation page.
     *
     * @return the form for testing this endpoint in the documentation page
     */
    var form: RestFormData? = null
        private set

    /** The XML schema for data returned by this endpoint.  */
    /**
     * @return the XML schema for this endpoint's return type
     */
    val returnTypeSchema: String? = null

    /**
     * Returns whether this endpoint's HTTP method is GET
     *
     * @return true if this endpoint method is GET, otherwise false
     */
    val isGetMethod: Boolean
        get() = "GET" == method

    /**
     * Returns the URL-encoded query string for a GET endpoint.
     *
     * @return the calculated query string for a GET endpoint (e.g. ?blah=1), will be urlencoded for html display
     */
    val queryString: String
        get() {
            var qs = ""
            if (isGetMethod) {
                if (optionalParams != null && !optionalParams!!.isEmpty()) {
                    val sb = StringBuilder()
                    sb.append("?")
                    for (p in optionalParams!!) {
                        if (sb.length > 2) {
                            sb.append("&")
                        }
                        sb.append(p.name)
                        sb.append("=")
                        if (p.defaultValue != null) {
                            sb.append(p.defaultValue)
                        } else {
                            sb.append("{")
                            sb.append(p.name)
                            sb.append("}")
                        }
                    }
                    qs = StringEscapeUtils.escapeHtml4(sb.toString())
                }
            }
            return qs
        }

    val escapedReturnTypeSchema: String?
        get() = StringEscapeUtils.escapeXml(returnTypeSchema)

    init {
        if (!DocData.isValidName(name)) {
            throw IllegalArgumentException("Name must not be null and must be alphanumeric.")
        }
        if (httpMethod == null || httpMethod.isEmpty()) {
            throw IllegalArgumentException("Method must not be null and must not be empty.")
        }
        if (!RestDocData.isValidPath(path)) {
            throw IllegalArgumentException(String.format("Path '%s' must not be null and must look something like " + "/a/b/{c}.", path))
        }
        this.returnTypeSchema = JaxbXmlSchemaGenerator.getXmlSchema(returnType)
        this.method = httpMethod.toUpperCase()
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object
     */
    override fun toString(): String {
        return ("ENDP:" + name + ":" + method + " " + path + " :body=" + bodyParam + " :req=" + requiredParams
                + " :opt=" + optionalParams + " :formats=" + formats + " :status=" + statuses + " :form=" + form)
    }

    /**
     * Adds a body parameter to this endpoint. Once added, the body parameter becomes a required parameter.
     *
     * @param restParam
     * a RestParameter annotation object corresponding to the body parameter
     *
     * @return the new RestParamData object in case you want to set attributes
     */
    fun addBodyParam(restParam: RestParameter): RestParamData {
        val type = RestParamData.Type.valueOf(restParam.type.name)
        val param = RestParamData("BODY", type, restParam.defaultValue,
                restParam.description, null)
        param.isRequired = true
        bodyParam = param
        return param
    }

    /**
     * Adds a path parameter for this endpoint, this would be a parameter which is passed as part of the path (e.g.
     * /my/path/{param}) and thus must use a name which is safe to be placed in a URL and does not contain a slash (/)
     *
     * @param param
     * the path parameter to add
     * @throws IllegalStateException
     * if the type of the path parameter is FILE or TEXT
     */
    @Throws(IllegalStateException::class)
    fun addPathParam(param: RestParamData) {
        if (RestParamData.Type.FILE.name == param.type || RestParamData.Type.TEXT.name == param.type) {
            throw IllegalStateException("Cannot add path param of type FILE or TEXT.")
        }
        param.isRequired = true
        param.isPath = true
        if (pathParams == null) {
            pathParams = Vector(3)
        }
        pathParams!!.add(param)
    }

    /**
     * Adds a required form parameter for this endpoint, this would be a parameter which is passed encoded as part of the
     * request body (commonly referred to as a post or form parameter). <br></br>
     * WARNING: This should generally be reserved for endpoints which are used for processing, it is better to use path
     * params unless the required parameter is not part of an identifier for the resource.
     *
     * @param param
     * the required parameter to add
     */
    @Throws(IllegalStateException::class)
    fun addRequiredParam(param: RestParamData) {
        param.isRequired = true
        param.isPath = false
        if (requiredParams == null) {
            requiredParams = Vector(3)
        }
        requiredParams!!.add(param)
    }

    /**
     * Adds an optional parameter for this endpoint, this would be a parameter which is passed in the query string (for
     * GET) or encoded as part of the body otherwise (often referred to as a post or form parameter).
     *
     * @param param
     * the optional parameter to add
     */
    fun addOptionalParam(param: RestParamData) {
        param.isRequired = false
        param.isPath = false
        if (optionalParams == null) {
            optionalParams = Vector(3)
        }
        optionalParams!!.add(param)
    }

    /**
     * Adds a format for the return data for this endpoint.
     *
     * @param format
     * a RestFormatData object
     */
    fun addFormat(format: RestFormatData) {
        if (formats == null) {
            formats = Vector(2)
        }
        formats!!.add(format)
    }

    /**
     * Adds a response status for this endpoint.
     *
     * @param restResponse
     * a RestResponse object containing the HTTP response code and description
     */
    fun addStatus(restResponse: RestResponse) {
        if (statuses == null) {
            statuses = Vector(3)
        }
        statuses!!.add(StatusData(restResponse))
    }

    /**
     * Adds a note for this endpoint.
     *
     * @param note
     * a string providing more information about this endpoint
     * @throws IllegalArgumentException
     * if note is blank (e.g. null, empty string)
     */
    @Throws(IllegalArgumentException::class)
    fun addNote(note: String) {
        if (DocData.isBlank(note)) {
            throw IllegalArgumentException("Note must not be null or blank.")
        }
        if (notes == null) {
            notes = Vector(3)
        }
        notes!!.add(note)
    }

    /**
     * Sets the test form for this endpoint, if this is null then no test form is rendered for this endpoint.
     *
     * @param form
     * the test form object (null to clear the form)
     */
    fun setTestForm(form: RestFormData) {
        this.form = form
    }

    /**
     * Gets the list of path parameters of this endpoint.
     *
     * @return the list of path parameters of this endpoint
     */
    fun getPathParams(): List<RestParamData> {
        if (pathParams == null) {
            pathParams = ArrayList(0)
        }
        return pathParams
    }

    /**
     * Gets the list of required parameters of this endpoint.
     *
     * @return the list of required parameters of this endpoint
     */
    fun getRequiredParams(): List<RestParamData> {
        if (requiredParams == null) {
            requiredParams = ArrayList(0)
        }
        return requiredParams
    }

    /**
     * Gets the list of optional parameters of this endpoint.
     *
     * @return list of optional parameters of this endpoint
     */
    fun getOptionalParams(): List<RestParamData> {
        if (optionalParams == null) {
            optionalParams = ArrayList(0)
        }
        return optionalParams
    }

    /**
     * Gets the list of formats returned by this endpoint.
     *
     * @return the list of formats returned by this endpoint
     */
    fun getFormats(): List<RestFormatData> {
        if (formats == null) {
            formats = ArrayList(0)
        }
        return formats
    }

    /**
     * Gets the list of HTTP responses returned by this endpoint.
     *
     * @return the list of HTTP responses returned by this endpoint
     */
    fun getStatuses(): List<StatusData> {
        if (statuses == null) {
            statuses = ArrayList(0)
        }
        return statuses
    }

    /**
     * Gets list of notes (i.e. extra information) of this endpoint.
     *
     * @return the list of notes (i.e. extra information) of this endpoint
     */
    fun getNotes(): List<String> {
        if (notes == null) {
            notes = ArrayList(0)
        }
        return notes
    }

    /**
     * Compares two RestEndpointData by their names so that the list of endpoints can be sorted.
     *
     * @param otherEndpoint
     * the other endpoint object to compare to
     *
     * @return a negative integer, zero, or a positive integer as the name of the supplied endpoint is greater than, equal
     * to, or less than this endpoint, ignoring case considerations.
     */
    override fun compareTo(otherEndpoint: RestEndpointData): Int {
        return name.compareTo(otherEndpoint.name, ignoreCase = true)
    }

}
