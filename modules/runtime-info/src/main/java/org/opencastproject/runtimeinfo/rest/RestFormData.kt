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

import java.util.ArrayList

class RestFormData
/**
 * Constructor which will auto-populate the form using the data in the endpoint, this will enable the ajax submit if
 * it is possible to do so.
 *
 * @param endpoint
 * a RestEndpointData object populated with all parameters it needs
 * @throws IllegalArgumentException
 * when endpoint is null
 */
@Throws(IllegalArgumentException::class)
constructor(endpoint: RestEndpointData?) {

    /**
     * This indicates whether the form submission should be an ajax submit or a normal submit.
     */
    /**
     * Returns whether the form submission should be an ajax submission or a normal submission.
     *
     * @return a boolean indicating whether the form submission should be an ajax submission or a normal submission
     */
    /**
     * Controls whether the form will be submitted via ajax and the content rendered on the page, NOTE that uploading any
     * files or downloading any content that is binary will require not using ajax submit, also note that there may be
     * other cases where ajax submission will fail to work OR where normal submission will fail to work (using PUT/DELETE)
     *
     * @param ajaxSubmit
     * a boolean indicating whether ajax submit is used
     */
    var isAjaxSubmit = false

    /**
     * If this is true then the upload contains a body parameter (i.e. a file upload option).
     */
    private var fileUpload = false

    /**
     * This indicates whether this test form is for a basic endpoint which has no parameters.
     */
    /**
     * Returns whether this form is for a basic endpoint which has no parameters.
     *
     * @return a boolean indicating whether this form is for a basic endpoint which has no parameters
     */
    var isBasic = false
        private set

    /**
     * The form parameters.
     */
    private val items: MutableList<RestParamData>?

    /**
     * Returns true if this form has no parameter in it, false if there is parameter in it.
     *
     * @return a boolean indicating whether this form contains any parameter
     */
    val isEmpty: Boolean
        get() {
            var empty = true
            if (items != null && !items.isEmpty()) {
                empty = false
            }
            return empty
        }

    /**
     * Returns whether the form contains a body parameter (i.e. a file upload option).
     *
     * @return a boolean indicating whether the form contains a body parameter (i.e. a file upload option)
     */
    /**
     * Set this to true if the file contains a file upload control, this will be determined automatically for
     * auto-generated forms.
     *
     * @param fileUpload
     * a boolean indicating whether there is file upload in this test
     */
    var isFileUpload: Boolean
        get() = fileUpload
        set(fileUpload) {
            this.fileUpload = fileUpload
            if (fileUpload) {
                isAjaxSubmit = false
            }
        }

    init {
        if (endpoint == null) {
            throw IllegalArgumentException("Endpoint must not be null.")
        }
        isAjaxSubmit = true
        items = ArrayList(3)
        var hasUpload = false
        if (endpoint.pathParams != null) {
            for (param in endpoint.pathParams) {
                param.isRequired = true
                items.add(param)
            }
        }
        if (endpoint.requiredParams != null) {
            for (param in endpoint.requiredParams) {
                param.isRequired = true
                if (RestParamData.Type.FILE.name.equals(param.type, ignoreCase = true)) {
                    hasUpload = true
                }
                items.add(param)
            }
        }
        if (endpoint.optionalParams != null) {
            for (param in endpoint.optionalParams) {
                param.isRequired = false
                if (RestParamData.Type.FILE.name.equals(param.type, ignoreCase = true)) {
                    hasUpload = true
                }
                items.add(param)
            }
        }
        if (endpoint.bodyParam != null) {
            val param = endpoint.bodyParam
            param!!.isRequired = true
            if (RestParamData.Type.FILE.name.equals(param.type, ignoreCase = true)) {
                hasUpload = true
            }
            items.add(param)
        }
        if (hasUpload) {
            fileUpload = true
            isAjaxSubmit = false
        }
        if (items.isEmpty() && endpoint.isGetMethod) {
            isBasic = true
        }
    }

    /**
     * Returns a string representation of this form.
     *
     * @return a string representation of this form
     */
    override fun toString(): String {
        return if (items == null) {
            "FORM:items=0"
        } else "FORM:items=" + items.size
    }

    /**
     * Returns the list of form parameters.
     *
     * @return a list of form parameters
     */
    fun getItems(): List<RestParamData>? {
        return items
    }

}
