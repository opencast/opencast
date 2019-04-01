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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

import org.opencastproject.util.doc.rest.RestParameter
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestResponse
import org.opencastproject.util.doc.rest.RestService

import org.junit.Test
import java.lang.reflect.Method
import java.util.HashMap

import javax.servlet.http.HttpServletResponse
import javax.ws.rs.Consumes
import javax.ws.rs.HttpMethod
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * This test class tests the functionality of annotations used for documenting REST endpoints.
 */
class RestDocsAnnotationTest {

    /**
     * This tests the functionality of @RestService annotation type.
     */
    @Test
    fun testRestServiceDocs() {
        val restServiceAnnotation = TestServletSample::class.java.getAnnotation(RestService::class.java)

        // name, title and abstract text
        assertEquals("ingestservice", restServiceAnnotation.name)
        assertEquals("Ingest Service", restServiceAnnotation.title)
        assertEquals(
                "This service creates and augments Opencast media packages that include media tracks, metadata catalogs and attachments.",
                restServiceAnnotation.abstractText)

        // notes
        assertEquals(2, restServiceAnnotation.notes.size.toLong())
        assertEquals("All paths above are relative to the REST endpoint base (something like http://your.server/files)",
                restServiceAnnotation.notes[0])
        assertEquals(
                "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed",
                restServiceAnnotation.notes[1])

    }

    /**
     * This tests the functionality of @RestQuery, @RestParameter, @RestResponse, @Path, @Produces, @Consumes annotation
     * type.
     */
    @Test
    fun testRestQueryDocs() {
        val testMethod: Method?
        try {
            testMethod = TestServletSample::class.java.getMethod("methodA")
            if (testMethod != null) {
                val restQueryAnnotation = testMethod.getAnnotation(RestQuery::class.java) as RestQuery
                val pathAnnotation = testMethod.getAnnotation(Path::class.java) as Path
                val producesAnnotation = testMethod.getAnnotation(Produces::class.java) as Produces
                val consumesAnnotation = testMethod.getAnnotation(Consumes::class.java) as Consumes

                assertEquals(1, producesAnnotation.value().size.toLong())
                assertEquals(MediaType.TEXT_XML, producesAnnotation.value()[0])

                assertEquals(1, consumesAnnotation.value().size.toLong())
                assertEquals(MediaType.MULTIPART_FORM_DATA, consumesAnnotation.value()[0])

                assertEquals("addTrack", pathAnnotation.value())

                // we cannot hard code the GET.class or POST.class because we don't know which one is used.
                for (a in testMethod.annotations) {
                    val method = a.annotationType().getAnnotation(HttpMethod::class.java) as HttpMethod
                    if (method != null) {
                        assertEquals("POST", a.annotationType().getSimpleName())
                        assertEquals("POST", method.value())
                    }
                }

                // name, description and return description
                assertEquals("addTrackInputStream", restQueryAnnotation.name)
                assertEquals("Add a media track to a given media package using an input stream",
                        restQueryAnnotation.description)
                assertEquals("augmented media package", restQueryAnnotation.returnDescription)

                // path parameter
                assertTrue(restQueryAnnotation.pathParameters.size == 1)
                assertEquals("wdID", restQueryAnnotation.pathParameters[0].name)
                assertEquals("Workflow definition id", restQueryAnnotation.pathParameters[0].description)
                assertTrue(restQueryAnnotation.pathParameters[0].isRequired)
                assertEquals("", restQueryAnnotation.pathParameters[0].defaultValue)
                assertEquals(RestParameter.Type.STRING, restQueryAnnotation.pathParameters[0].type)

                // query parameters
                assertTrue(restQueryAnnotation.restParameters.size == 2)
                // #1
                assertEquals("flavor", restQueryAnnotation.restParameters[0].name)
                assertEquals("The kind of media track", restQueryAnnotation.restParameters[0].description)
                assertTrue(restQueryAnnotation.restParameters[0].isRequired)
                assertEquals("Default", restQueryAnnotation.restParameters[0].defaultValue)
                assertEquals(RestParameter.Type.STRING, restQueryAnnotation.restParameters[0].type)
                // #2
                assertEquals("mediaPackage", restQueryAnnotation.restParameters[1].name)
                assertEquals("The media package as XML", restQueryAnnotation.restParameters[1].description)
                assertFalse(restQueryAnnotation.restParameters[1].isRequired)
                assertEquals("", restQueryAnnotation.restParameters[1].defaultValue)
                assertEquals(RestParameter.Type.TEXT, restQueryAnnotation.restParameters[1].type)

                // body parameter
                assertEquals("BODY", restQueryAnnotation.bodyParameter.name)
                assertEquals("The media track file", restQueryAnnotation.bodyParameter.description)
                assertTrue(restQueryAnnotation.bodyParameter.isRequired)
                assertEquals("", restQueryAnnotation.bodyParameter.defaultValue)
                assertEquals(RestParameter.Type.FILE, restQueryAnnotation.bodyParameter.type)

                // responses
                assertTrue(restQueryAnnotation.reponses.size == 3)

                assertEquals(HttpServletResponse.SC_OK.toLong(), restQueryAnnotation.reponses[0].responseCode.toLong())
                assertEquals("Returns augmented media package", restQueryAnnotation.reponses[0].description)

                assertEquals(HttpServletResponse.SC_BAD_REQUEST.toLong(), restQueryAnnotation.reponses[1].responseCode.toLong())
                assertEquals("", restQueryAnnotation.reponses[1].description)

                assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR.toLong(), restQueryAnnotation.reponses[2].responseCode.toLong())
                assertEquals("", restQueryAnnotation.reponses[2].description)

            }
        } catch (e: SecurityException) {
            fail()
        } catch (e: NoSuchMethodException) {
            fail()
        }

    }

    /**
     * This tests the functionality of @RestQuery, @RestParameter, @RestResponse, @Path, @Produces, @Consumes annotation
     * type using a different technique.
     */
    @Test
    fun testRestQueryDocs2() {
        val testMethod: Method?
        try {
            testMethod = TestServletSample::class.java.getMethod("methodA")
            if (testMethod != null) {
                val restDocData = RestDocData("NAME", "TITLE", "URL", null, TestServletSample(),
                        HashMap())

                val restQueryAnnotation = testMethod.getAnnotation(RestQuery::class.java) as RestQuery
                val pathAnnotation = testMethod.getAnnotation(Path::class.java) as Path
                val producesAnnotation = testMethod.getAnnotation(Produces::class.java) as Produces

                var httpMethodString: String? = null

                // we cannot hard code the GET.class or POST.class because we don't know which one is used.
                for (a in testMethod.annotations) {
                    val method = a.annotationType().getAnnotation(HttpMethod::class.java) as HttpMethod
                    if (method != null) {
                        httpMethodString = method.value()
                    }
                }

                val endpoint = RestEndpointData(testMethod.returnType,
                        restQueryAnnotation.name, httpMethodString, "/" + pathAnnotation.value(),
                        restQueryAnnotation.description)
                if (!restQueryAnnotation.returnDescription.isEmpty()) {
                    endpoint.addNote("Return value description: " + restQueryAnnotation.returnDescription)
                }

                // name, description and return description
                assertEquals("addTrackInputStream", endpoint.name)
                assertEquals("Add a media track to a given media package using an input stream", endpoint.description)
                assertEquals(1, endpoint.notes.size.toLong())
                assertEquals("Return value description: augmented media package", endpoint.notes[0])

                // HTTP method
                assertEquals("POST", endpoint.method)
                assertEquals("/addTrack", endpoint.path)

                // @Produces
                if (producesAnnotation != null) {
                    for (format in producesAnnotation.value()) {
                        endpoint.addFormat(RestFormatData(format))
                    }
                }
                assertEquals(1, endpoint.formats.size.toLong())
                assertEquals(MediaType.TEXT_XML, endpoint.formats[0].name)

                // responses
                for (restResp in restQueryAnnotation.reponses) {
                    endpoint.addStatus(restResp)
                }
                assertEquals(3, endpoint.statuses.size.toLong())

                assertEquals(HttpServletResponse.SC_OK.toLong(), endpoint.statuses[0].code.toLong())
                assertEquals("Returns augmented media package", endpoint.statuses[0].description)

                assertEquals(HttpServletResponse.SC_BAD_REQUEST.toLong(), endpoint.statuses[1].code.toLong())
                assertEquals(null, endpoint.statuses[1].description)

                assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR.toLong(), endpoint.statuses[2].code.toLong())
                assertEquals(null, endpoint.statuses[2].description)

                // body parameter
                if (restQueryAnnotation.bodyParameter.type !== RestParameter.Type.NO_PARAMETER) {
                    endpoint.addBodyParam(restQueryAnnotation.bodyParameter)
                }
                assertEquals("BODY", endpoint.bodyParam!!.name)
                assertEquals("The media track file", endpoint.bodyParam!!.description)
                assertTrue(endpoint.bodyParam!!.isRequired)
                assertEquals(null, endpoint.bodyParam!!.defaultValue)
                assertTrue("FILE".equals(endpoint.bodyParam!!.type, ignoreCase = true))

                // path parameter
                for (restParam in restQueryAnnotation.pathParameters) {
                    endpoint.addPathParam(RestParamData(restParam))
                }
                assertEquals(1, endpoint.pathParams.size.toLong())
                assertEquals("wdID", endpoint.pathParams[0].name)
                assertEquals("Workflow definition id", endpoint.pathParams[0].description)
                assertTrue(endpoint.pathParams[0].isRequired)
                assertTrue(endpoint.pathParams[0].isPath)
                assertEquals(null, endpoint.pathParams[0].defaultValue)
                assertTrue("STRING".equals(endpoint.pathParams[0].type, ignoreCase = true))

                // query parameters
                for (restParam in restQueryAnnotation.restParameters) {
                    if (restParam.isRequired) {
                        endpoint.addRequiredParam(RestParamData(restParam))
                    } else {
                        endpoint.addOptionalParam(RestParamData(restParam))
                    }
                }
                // #1
                assertEquals(1, endpoint.requiredParams.size.toLong())
                assertEquals("flavor", endpoint.requiredParams[0].name)
                assertEquals("The kind of media track", endpoint.requiredParams[0].description)
                assertTrue(endpoint.requiredParams[0].isRequired)
                assertEquals("Default", endpoint.requiredParams[0].defaultValue)
                assertTrue("STRING".equals(endpoint.requiredParams[0].type, ignoreCase = true))

                // #2
                assertEquals(1, endpoint.optionalParams.size.toLong())
                assertEquals("mediaPackage", endpoint.optionalParams[0].name)
                assertEquals("The media package as XML", endpoint.optionalParams[0].description)
                assertFalse(endpoint.optionalParams[0].isRequired)
                assertEquals(null, endpoint.optionalParams[0].defaultValue)
                assertTrue("TEXT".equals(endpoint.optionalParams[0].type, ignoreCase = true))

            }
        } catch (e: SecurityException) {
            fail()
        } catch (e: NoSuchMethodException) {
            fail()
        }

    }

    @Test
    fun testPathPatternMatching() {
        assertTrue(RestDocData.isValidPath("/{seriesID:.+}"))
    }

    /**
     * This sample class simulates a annotated REST service class.
     */
    @RestService(name = "ingestservice", title = "Ingest Service", notes = ["All paths above are relative to the REST endpoint base (something like http://your.server/files)", "If the service is down or not working it will return a status 503, this means the the underlying service is not working and is either restarting or has failed"], abstractText = "This service creates and augments Opencast media packages that include media tracks, metadata catalogs and attachments.")
    inner class TestServletSample {

        val schema: String
            get() = "THIS IS SCHEMA"

        @POST
        @Produces(MediaType.TEXT_XML)
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Path("addTrack")
        @RestQuery(name = "addTrackInputStream", description = "Add a media track to a given media package using an input stream", pathParameters = [RestParameter(defaultValue = "", description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING)], restParameters = [RestParameter(defaultValue = "Default", description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(defaultValue = "", description = "The media package as XML", isRequired = false, name = "mediaPackage", type = RestParameter.Type.TEXT)], bodyParameter = RestParameter(defaultValue = "", description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "augmented media package")
        fun methodA(): Int {
            return 0
        }

        @POST
        @Produces(MediaType.TEXT_XML)
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @Path("addTrack")
        @RestQuery(name = "addTrackInputStream", description = "Add a media track to a given media package using an input stream", pathParameters = [RestParameter(defaultValue = "", description = "Workflow definition id", isRequired = true, name = "wdID", type = RestParameter.Type.STRING)], restParameters = [RestParameter(defaultValue = "Default", description = "The kind of media track", isRequired = true, name = "flavor", type = RestParameter.Type.STRING), RestParameter(defaultValue = "ADCD \${this.schema} XUHZSUFH \${somethingElse} UGGUH \${anotherthing} AIHID", description = "The media package as XML", isRequired = false, name = "mediaPackage", type = RestParameter.Type.TEXT)], bodyParameter = RestParameter(defaultValue = "", description = "The media track file", isRequired = true, name = "BODY", type = RestParameter.Type.FILE), reponses = [RestResponse(description = "Returns augmented media package", responseCode = HttpServletResponse.SC_OK), RestResponse(description = "", responseCode = HttpServletResponse.SC_BAD_REQUEST), RestResponse(description = "", responseCode = HttpServletResponse.SC_INTERNAL_SERVER_ERROR)], returnDescription = "augmented media package")
        fun methodB(): Int {
            return 0
        }
    }

}
