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

package org.opencastproject.runtimeinfo

import org.opencastproject.rest.RestConstants.SERVICES_FILTER
import org.opencastproject.rest.RestConstants.SERVICE_PATH_PROPERTY
import org.opencastproject.util.data.Option.none
import org.opencastproject.util.data.Option.some

import org.opencastproject.runtimeinfo.rest.RestDocData
import org.opencastproject.systems.OpencastConstants
import org.opencastproject.util.data.Option
import org.opencastproject.util.doc.DocUtil
import org.opencastproject.util.doc.rest.RestQuery
import org.opencastproject.util.doc.rest.RestService

import org.apache.commons.lang3.StringUtils
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.framework.InvalidSyntaxException
import org.osgi.framework.ServiceReference
import org.osgi.framework.ServiceRegistration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.IOException
import java.lang.reflect.Method
import java.util.Dictionary
import java.util.HashMap
import java.util.Hashtable

import javax.servlet.Servlet
import javax.servlet.ServletException
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.ws.rs.HttpMethod
import javax.ws.rs.Path
import javax.ws.rs.Produces

/** A bundle activator that registers the REST documentation servlet.  */
class Activator : HttpServlet(), BundleActivator {

    /** The OSGI bundle context  */
    protected var bundleContext: BundleContext

    /** The registration for the documentation servlet.  */
    protected var docServletRegistration: ServiceRegistration<*>? = null

    /** A map of global macro values for REST documentation.  */
    private var globalMacro: MutableMap<String, String>? = null

    private val restEndpointServices: Array<ServiceReference<*>>
        get() {
            try {
                return bundleContext.getAllServiceReferences(null, SERVICES_FILTER)
            } catch (e: InvalidSyntaxException) {
                logger.warn("Unable to query the OSGI service registry for all registered rest endpoints")
                return arrayOfNulls(0)
            }

        }

    @Throws(Exception::class)
    override fun start(bundleContext: BundleContext) {
        this.bundleContext = bundleContext
        val props = Hashtable<String, String>()
        props["alias"] = "/docs.html"
        prepareMacros()
        bundleContext.registerService(Servlet::class.java.name, this, props)
    }

    /** Add a list of global information, such as the server URL, to the globalMacro map.  */
    private fun prepareMacros() {
        globalMacro = HashMap()
        globalMacro!!["PING_BACK_URL"] = bundleContext.getProperty("org.opencastproject.anonymous.feedback.url")
        globalMacro!!["HOST_URL"] = bundleContext.getProperty(OpencastConstants.SERVER_URL_PROPERTY)
        globalMacro!!["LOCAL_STORAGE_DIRECTORY"] = bundleContext.getProperty("org.opencastproject.storage.dir")
    }

    @Throws(Exception::class)
    override fun stop(bundleContext: BundleContext) {
    }

    @Throws(ServletException::class, IOException::class)
    override fun doGet(req: HttpServletRequest, resp: HttpServletResponse) {
        val docPath = req.getParameter(PATH_PARAM)
        if (StringUtils.isBlank(docPath)) {
            resp.sendRedirect("rest_docs.html")
        } else {
            // write the details for this service
            writeServiceDocumentation(docPath, req, resp)
        }
    }

    @Throws(IOException::class)
    private fun writeServiceDocumentation(docPath: String, req: HttpServletRequest, resp: HttpServletResponse) {
        var reference: ServiceReference<*>? = null
        for (ref in restEndpointServices) {
            val alias = ref.getProperty(SERVICE_PATH_PROPERTY) as String
            if (docPath.equals(alias, ignoreCase = true)) {
                reference = ref
                break
            }
        }

        val docs = StringBuilder()

        if (reference == null) {
            docs.append("REST docs unavailable for ")
            docs.append(docPath)
        } else {
            val restService = bundleContext.getService<Any>(reference)
            findRestAnnotation(restService.javaClass).fold(object : Option.Match<RestService, Void> {
                override fun some(annotation: RestService?): Void? {
                    globalMacro!!["SERVICE_CLASS_SIMPLE_NAME"] = restService.javaClass.simpleName
                    val data = RestDocData(annotation!!.name, annotation.title, docPath, annotation.notes,
                            restService, globalMacro)
                    data.setAbstract(annotation.abstractText)

                    val producesClass = restService.javaClass.getAnnotation(Produces::class.java) as Produces

                    for (m in restService.javaClass.methods) {
                        val rq = m.getAnnotation(RestQuery::class.java) as RestQuery
                        var httpMethodString: String? = null
                        for (a in m.annotations) {
                            val httpMethod = a.annotationType().getAnnotation(HttpMethod::class.java) as HttpMethod
                            if (httpMethod != null) {
                                httpMethodString = httpMethod.value()
                            }
                        }
                        var produces: Produces? = m.getAnnotation(Produces::class.java) as Produces
                        if (produces == null) {
                            produces = producesClass
                        }
                        val path = m.getAnnotation(Path::class.java) as Path
                        val returnType = m.returnType
                        if (rq != null && httpMethodString != null && path != null) {
                            data.addEndpoint(rq, returnType, produces, httpMethodString, path)
                        }
                    }
                    val template = DocUtil.loadTemplate("/ui/restdocs/template.xhtml")
                    docs.append(DocUtil.generate(data, template))
                    return null
                }

                override fun none(): Void? {
                    docs.append("No documentation has been found for ").append(restService.javaClass.simpleName)
                    return null
                }
            })
        }

        resp.contentType = "text/html"
        resp.writer.write(docs.toString())
    }

    companion object {

        /** The logger  */
        private val logger = LoggerFactory.getLogger(Activator::class.java)

        /** The query string parameter used to specify a specific service  */
        private val PATH_PARAM = "path"

        /** java.io serialization UID  */
        private val serialVersionUID = 6930336096831297329L

        /** Try to find the RestService annotation starting at `endpointClass`.  */
        fun findRestAnnotation(endpointClass: Class<*>?): Option<RestService> {
            if (endpointClass == null) {
                return none()
            }
            val rs = endpointClass.getAnnotation(RestService::class.java)
            return rs?.let { some(it) } ?: findRestAnnotation(endpointClass.superclass)
        }
    }
}
