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

package org.opencastproject.coverimage.impl

import org.opencastproject.security.api.OrganizationDirectoryService
import org.opencastproject.security.api.SecurityService
import org.opencastproject.security.api.UserDirectoryService
import org.opencastproject.serviceregistry.api.ServiceRegistry
import org.opencastproject.workspace.api.Workspace

import org.apache.batik.ext.awt.image.GraphicsUtil
import org.apache.batik.ext.awt.image.codec.jpeg.JPEGRegistryEntry
import org.apache.batik.ext.awt.image.renderable.DeferRable
import org.apache.batik.ext.awt.image.renderable.Filter
import org.apache.batik.ext.awt.image.renderable.RedRable
import org.apache.batik.ext.awt.image.rendered.Any2sRGBRed
import org.apache.batik.ext.awt.image.rendered.CachableRed
import org.apache.batik.ext.awt.image.rendered.FormatRed
import org.apache.batik.ext.awt.image.spi.ImageTagRegistry
import org.apache.batik.util.ParsedURL
import org.osgi.service.component.ComponentContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.awt.image.WritableRaster
import java.io.IOException
import java.io.InputStream

import javax.imageio.ImageIO

/**
 * Implementation of [AbstractCoverImageService] for use in OSGi environment
 */
class CoverImageServiceOsgiImpl : AbstractCoverImageService() {

    /**
     * OSGi callback to set the service registry service
     *
     * @param serviceRegistry
     * the service registry service
     */
    protected override var serviceRegistry: ServiceRegistry
        get() = serviceRegistry
        set(serviceRegistry) {
            this.serviceRegistry = serviceRegistry
        }

    /**
     * OSGi callback to set the security service
     *
     * @param securityService
     * the security service
     */
    override var securityService: SecurityService
        get() = securityService
        set(securityService) {
            this.securityService = securityService
        }

    /**
     * OSGi callback to set the user directory service
     *
     * @param userDirectoryService
     * the user directory service
     */
    override var userDirectoryService: UserDirectoryService
        get() = userDirectoryService
        set(userDirectoryService) {
            this.userDirectoryService = userDirectoryService
        }

    /**
     * OSGi callback to set the organization directory service
     *
     * @param organizationDirectoryService
     * the organization directory service
     */
    override var organizationDirectoryService: OrganizationDirectoryService
        get() = organizationDirectoryService
        set(organizationDirectoryService) {
            this.organizationDirectoryService = organizationDirectoryService
        }

    /**
     * OSGi activation callback
     *
     * @param cc
     * the OSGi component context
     */
    override fun activate(cc: ComponentContext) {
        super.activate(cc)
        // See
        // http://www.stichlberger.com/software/workaround-for-batiks-noclassdeffounderrorclassnotfoundexception-truncatedfileexception/
        // ---------------
        // add this code before you use batik (make sure is runs only once)
        // via the lower priority this subclass is registered before JPEGRegistryEntry
        // and prevents JPEGRegistryEntry.handleStream from breaking when used on a non Sun/Oracle JDK
        val entry = object : JPEGRegistryEntry() {

            override fun getPriority(): Float {
                // higher than that of JPEGRegistryEntry (which is 1000)
                return 500f
            }

            /**
             * Decode the Stream into a RenderableImage
             *
             * @param inIS
             * The input stream that contains the image.
             * @param origURL
             * The original URL, if any, for documentation purposes only. This may be null.
             * @param needRawData
             * If true the image returned should not have any default color correction the file may specify applied.
             */
            override fun handleStream(inIS: InputStream, origURL: ParsedURL?, needRawData: Boolean): Filter {
                // Code from org.apache.batik.ext.awt.image.codec.jpeg.JPEGRegistryEntry#handleStream
                // Reading image with ImageIO to prevent NoClassDefFoundError on OpenJDK

                val dr = DeferRable()
                val errCode: String
                val errParam: Array<Any>
                if (origURL != null) {
                    errCode = ErrorConstants.ERR_URL_FORMAT_UNREADABLE
                    errParam = arrayOf("JPEG", origURL)
                } else {
                    errCode = ErrorConstants.ERR_STREAM_FORMAT_UNREADABLE
                    errParam = arrayOf("JPEG")
                }

                val t = object : Thread() {
                    override fun run() {
                        var filt: Filter
                        try {
                            var image: BufferedImage
                            image = ImageIO.read(inIS)
                            dr.setBounds(Rectangle2D.Double(0.0, 0.0, image.width.toDouble(), image.height.toDouble()))
                            var cr: CachableRed
                            cr = GraphicsUtil.wrap(image)
                            cr = Any2sRGBRed(cr)
                            cr = FormatRed(cr, GraphicsUtil.sRGB_Unpre)
                            val wr = cr.getData() as WritableRaster
                            val cm = cr.getColorModel()
                            image = BufferedImage(cm, wr, cm.isAlphaPremultiplied, null)
                            cr = GraphicsUtil.wrap(image)
                            filt = RedRable(cr)
                        } catch (ioe: IOException) {
                            // Something bad happened here...
                            filt = ImageTagRegistry.getBrokenLinkImage(this, errCode, errParam)
                        } catch (td: ThreadDeath) {
                            filt = ImageTagRegistry.getBrokenLinkImage(this, errCode, errParam)
                            dr.source = filt
                            throw td
                        } catch (t: Throwable) {
                            filt = ImageTagRegistry.getBrokenLinkImage(this, errCode, errParam)
                        }

                        dr.source = filt
                    }
                }
                t.start()
                return dr
            }
        }

        ImageTagRegistry.getRegistry().register(entry)

        logger.info("Cover image service activated")
    }

    /**
     * OSGi callback to set the workspace service.
     *
     * @param workspace
     * the workspace service
     */
    protected fun setWorkspace(workspace: Workspace) {
        this.workspace = workspace
    }

    companion object {

        /** The logging facility  */
        private val logger = LoggerFactory.getLogger(CoverImageServiceOsgiImpl::class.java)
    }

}
