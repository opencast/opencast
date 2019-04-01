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
package org.opencastproject.email.template.impl

import org.apache.commons.io.IOUtils
import org.apache.felix.fileinstall.ArtifactInstaller
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

class EmailTemplateScanner : ArtifactInstaller {

    /** The templates map  */
    private val templates = ConcurrentHashMap<String, String>()

    /**
     * Returns the list of templates.
     *
     * @return the email templates
     */
    fun getTemplates(): Map<String, String> {
        return templates
    }

    /**
     * OSGi callback on component activation.
     *
     * @param ctx
     * the bundle context
     */
    internal fun activate(ctx: BundleContext) {
        logger.info("EmailTemplateScanner activated")
    }

    /**
     * Returns the email template for the given file name or `null` if no such one.
     *
     * @param fileName
     * the template file name
     * @return the email template text
     */
    fun getTemplate(fileName: String): String {
        return templates[fileName]
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactListener.canHandle
     */
    override fun canHandle(artifact: File): Boolean {
        return "email" == artifact.parentFile.name
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactInstaller.install
     */
    @Throws(Exception::class)
    override fun install(artifact: File) {
        val `is` = FileInputStream(artifact)
        val template = IOUtils.toString(`is`)
        templates[artifact.name] = template
        logger.info("Template {} installed", artifact.name)
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactInstaller.uninstall
     */
    @Throws(Exception::class)
    override fun uninstall(artifact: File) {
        val iter = templates.values.iterator()
        while (iter.hasNext()) {
            val temp = iter.next()
            if (artifact.name == temp) {
                logger.info("Uninstalling template {}", temp)
                iter.remove()
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.apache.felix.fileinstall.ArtifactInstaller.update
     */
    @Throws(Exception::class)
    override fun update(artifact: File) {
        uninstall(artifact)
        install(artifact)
    }

    companion object {

        /** The logging instance  */
        private val logger = LoggerFactory.getLogger(EmailTemplateScanner::class.java)
    }

}
