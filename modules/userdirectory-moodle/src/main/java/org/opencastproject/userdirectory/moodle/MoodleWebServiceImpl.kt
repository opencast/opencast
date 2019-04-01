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

package org.opencastproject.userdirectory.moodle

import org.apache.http.NameValuePair
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicNameValuePair
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URI
import java.net.URISyntaxException
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedList

/**
 * Implementation of the Moodle web service client.
 */
class MoodleWebServiceImpl
/**
 * Constructs a new Moodle web service client.
 *
 * @param url   URL of the Moodle instance
 * @param token Web service token
 */
(
        /**
         * The URL of the Moodle instance.
         */
        private val url: URI,
        /**
         * The token used to call Moodle REST webservices.
         */
        private val token: String) : MoodleWebService {

    /**
     * {@inheritDoc}
     */
    @Throws(URISyntaxException::class, IOException::class, MoodleWebServiceException::class, ParseException::class)
    override fun coreUserGetUsersByField(filter: MoodleWebService.CoreUserGetUserByFieldFilters, values: List<String>): List<MoodleUser> {
        logger.debug("coreUserGetUsersByField(({}, {}))", filter, values)

        val params = ArrayList<NameValuePair>()
        params.add(BasicNameValuePair("field", filter.toString()))

        for (i in values.indices)
            params.add(BasicNameValuePair("values[$i]", values[i]))

        val resp = executeMoodleRequest(MoodleWebService.MOODLE_FUNCTION_CORE_USER_GET_USERS_BY_FIELD, params)

        // Parse response
        if (resp == null || resp !is JSONArray)
            throw MoodleWebServiceException("Moodle responded in unexpected format")

        val respArray = resp as JSONArray?
        val users = ArrayList<MoodleUser>(respArray!!.size)

        for (userObj in respArray) {
            if (userObj !is JSONObject)
                throw MoodleWebServiceException("Moodle responded in unexpected format")

            val user = MoodleUser()

            if (userObj.containsKey("id"))
                user.id = userObj["id"].toString()
            if (userObj.containsKey("username"))
                user.username = userObj["username"].toString()
            if (userObj.containsKey("fullname"))
                user.fullname = userObj["fullname"].toString()
            if (userObj.containsKey("idnumber"))
                user.idnumber = userObj["idnumber"].toString()
            if (userObj.containsKey("email"))
                user.email = userObj["email"].toString()
            if (userObj.containsKey("auth"))
                user.auth = userObj["auth"].toString()

            users.add(user)
        }

        return users
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.userdirectory.moodle.MoodleWebService.toolOpencastGetCoursesForInstructor
     */
    @Throws(URISyntaxException::class, IOException::class, MoodleWebServiceException::class, ParseException::class)
    override fun toolOpencastGetCoursesForInstructor(username: String): List<String> {
        logger.debug("toolOpencastGetCoursesForInstructor({})", username)

        val params = listOf(BasicNameValuePair("username", username) as NameValuePair)

        return parseIdList(executeMoodleRequest(MoodleWebService.MOODLE_FUNCTION_TOOL_OPENCAST_GET_COURSES_FOR_INSTRUCTOR, params))
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.userdirectory.moodle.MoodleWebService.toolOpencastGetCoursesForLearner
     */
    @Throws(URISyntaxException::class, IOException::class, MoodleWebServiceException::class, ParseException::class)
    override fun toolOpencastGetCoursesForLearner(username: String): List<String> {
        logger.debug("toolOpencastGetCoursesForLearner({})", username)

        val params = listOf(BasicNameValuePair("username", username) as NameValuePair)

        return parseIdList(executeMoodleRequest(MoodleWebService.MOODLE_FUNCTION_TOOL_OPENCAST_GET_COURSES_FOR_LEARNER, params))
    }

    @Throws(URISyntaxException::class, IOException::class, MoodleWebServiceException::class, ParseException::class)
    override fun toolOpencastGetGroupsForLearner(username: String): List<String> {
        logger.debug("toolOpencastGetGroupsForLearner({})", username)

        val params = listOf(BasicNameValuePair("username", username) as NameValuePair)

        return parseIdList(executeMoodleRequest(MoodleWebService.MOODLE_FUNCTION_TOOL_OPENCAST_GET_GROUPS_FOR_LEARNER, params))
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.userdirectory.moodle.MoodleWebService.getURL
     */
    override fun getURL(): String {
        return url.toString()
    }

    /**
     * Parses the returned Moodle response for a list of IDs.
     *
     * @param resp The Moodle response. It should be of type [JSONArray].
     * @return A list of Moodle IDs.
     * @throws MoodleWebServiceException If the parsing failed because the response format was unexpected.
     */
    @Throws(MoodleWebServiceException::class)
    private fun parseIdList(resp: Any?): List<String> {
        if (resp == null)
            return LinkedList()

        if (resp !is JSONArray)
            throw MoodleWebServiceException("Moodle responded in unexpected format")

        val respArray = resp as JSONArray?
        val ids = ArrayList<String>(respArray!!.size)

        for (courseObj in respArray) {
            if (courseObj !is JSONObject || courseObj["id"] == null)
                throw MoodleWebServiceException("Moodle responded in unexpected format")

            ids.add(courseObj["id"].toString())
        }

        return ids
    }

    /**
     * Executes a Moodle webservice request.
     *
     * @param function The function to execute.
     * @param params   Additional parameters to pass.
     * @return A JSON object, array, String, Number, Boolean, or null.
     * @throws URISyntaxException        In case the URL cannot be constructed.
     * @throws IOException               In case of an IO error.
     * @throws MoodleWebServiceException In case Moodle returns an error.
     * @throws ParseException            In case the Moodle response cannot be parsed.
     */
    @Throws(URISyntaxException::class, IOException::class, MoodleWebServiceException::class, ParseException::class)
    private fun executeMoodleRequest(function: String, params: List<NameValuePair>): Any? {
        // Build URL
        val url = URIBuilder(this.url)
        url.addParameters(params)
        url.addParameter("wstoken", token)
        url.addParameter("wsfunction", function)
        url.addParameter("moodlewsrestformat", "json")

        // Execute request
        val get = HttpGet(url.build())
        get.setHeader("User-Agent", OC_USERAGENT)

        HttpClients.createDefault().use { client ->
            client.execute(get).use { resp ->
                // Parse response
                val reader = BufferedReader(InputStreamReader(resp.entity.content))
                val parser = JSONParser()
                val obj = parser.parse(reader)

                // Check for errors
                if (obj is JSONObject) {
                    if (obj.containsKey("exception") || obj.containsKey("errorcode"))
                        throw MoodleWebServiceException("Moodle returned an error: " + obj.toJSONString())
                }

                return obj
            }
        }
    }

    companion object {
        /**
         * The logger.
         */
        private val logger = LoggerFactory.getLogger(MoodleUserProviderInstance::class.java)

        /**
         * HTTP user agent when performing requests.
         */
        private val OC_USERAGENT = "Opencast"
    }
}
