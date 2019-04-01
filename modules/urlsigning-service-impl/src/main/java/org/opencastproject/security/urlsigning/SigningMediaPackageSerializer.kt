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
package org.opencastproject.security.urlsigning

import org.opencastproject.mediapackage.MediaPackageSerializer
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.urlsigning.exception.UrlSigningException
import org.opencastproject.security.urlsigning.service.UrlSigningService
import org.opencastproject.security.urlsigning.utils.UrlSigningServiceOsgiUtil

import org.osgi.service.cm.ConfigurationException
import org.osgi.service.cm.ManagedService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.net.URI
import java.net.URISyntaxException
import java.util.Dictionary

/**
 * Implementation of a [MediaPackageSerializer] that will securely sign urls of a Mediapackage.
 */
/**
 * Creates a new and unconfigured package serializer that will not be able to perform any redirecting.
 */
class SigningMediaPackageSerializer : MediaPackageSerializer, ManagedService {

    /** Security service to use for the client's IP address  */
    private var securityService: SecurityService? = null

    /** URL Signing Service for Securing Content.  */
    private var urlSigningService: UrlSigningService? = null

    private var expireSeconds = UrlSigningServiceOsgiUtil.DEFAULT_URL_SIGNING_EXPIRE_DURATION

    private var signWithClientIP: Boolean? = UrlSigningServiceOsgiUtil.DEFAULT_SIGN_WITH_CLIENT_IP

    override val ranking: Int
        get() = RANKING

    val expirationSeconds: Long?
        get() = expireSeconds

    /** OSGi DI  */
    internal fun setSecurityService(securityService: SecurityService) {
        this.securityService = securityService
    }

    /** OSGi callback for UrlSigningService  */
    fun setUrlSigningService(urlSigningService: UrlSigningService) {
        this.urlSigningService = urlSigningService
    }

    /** OSGi callback if properties file is present  */
    @Throws(ConfigurationException::class)
    override fun updated(properties: Dictionary<*, *>) {
        expireSeconds = UrlSigningServiceOsgiUtil.getUpdatedSigningExpiration(properties, this.javaClass.simpleName)
        signWithClientIP = UrlSigningServiceOsgiUtil.getUpdatedSignWithClientIP(properties,
                this.javaClass.simpleName)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageSerializer.encodeURI
     */
    @Throws(URISyntaxException::class)
    override fun encodeURI(uri: URI): URI {
        if (uri == null)
            throw IllegalArgumentException("Argument uri is null")
        return uri
    }

    /**
     * {@inheritDoc}
     *
     * @see org.opencastproject.mediapackage.MediaPackageSerializer.decodeURI
     */
    @Throws(URISyntaxException::class)
    override fun decodeURI(uri: URI): URI {
        if (uri == null)
            throw IllegalArgumentException("Argument uri is null")
        return sign(uri)
    }

    override fun toString(): String {
        return "URL Signing MediaPackage Serializer"
    }

    /**
     * This method is signing the URI with a policy to expire it.
     *
     * @param uri
     * the URI to sign
     *
     * @return the signed URI
     * @throws URISyntaxException
     * if the input URI contains syntax errors
     */
    @Throws(URISyntaxException::class)
    private fun sign(uri: URI): URI {
        var path = uri.toString()
        if (urlSigningService != null && urlSigningService!!.accepts(path)) {
            try {
                var clientIP: String? = null
                if (signWithClientIP!!) {
                    clientIP = securityService!!.userIP
                }
                path = urlSigningService!!.sign(path, expireSeconds, null, clientIP!!)
            } catch (e: UrlSigningException) {
                logger.debug("Unable to sign url '$path' so not adding a signed query string.")
            }

        }
        return URI(path)
    }

    companion object {
        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(SigningMediaPackageSerializer::class.java)

        /** Signing of the URL should probably be something of the last things to do  */
        val RANKING = -1000
    }

}
