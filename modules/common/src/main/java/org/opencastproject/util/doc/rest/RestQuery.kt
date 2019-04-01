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

package org.opencastproject.util.doc.rest

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * This annotation type is used for annotating RESTful query(each java method, instead of the class). This annotation
 * type needs to be kept until runtime.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
annotation class RestQuery(
        /**
         * @return the name of the query.
         */
        val name: String,
        /**
         * @return a description of the query.
         */
        val description: String,
        /**
         * @return a description of what is returned.
         */
        val returnDescription: String,
        /**
         * @return a list of possible responses from this query.
         */
        val reponses: Array<RestResponse>,
        /**
         * @return a list of path parameters for this query.
         */
        val pathParameters: Array<RestParameter> = [],
        /**
         * @return a list of query parameters for this query.
         */
        val restParameters: Array<RestParameter> = [],
        /**
         * @return a body parameter for this query.
         */
        val bodyParameter: RestParameter = RestParameter(defaultValue = "", description = "", isRequired = false, name = "", type = RestParameter.Type.NO_PARAMETER))
