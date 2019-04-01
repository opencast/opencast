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


package org.opencastproject.mediapackage

import org.opencastproject.mediapackage.elementbuilder.AttachmentBuilderPlugin
import org.opencastproject.mediapackage.elementbuilder.CatalogBuilderPlugin
import org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin
import org.opencastproject.mediapackage.elementbuilder.PublicationBuilderPlugin
import org.opencastproject.mediapackage.elementbuilder.TrackBuilderPlugin

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Node

import java.net.URI
import java.util.ArrayList

import javax.xml.xpath.XPath
import javax.xml.xpath.XPathExpressionException
import javax.xml.xpath.XPathFactory

/**
 * Default implementation for a media package element builder.
 */
class MediaPackageElementBuilderImpl : MediaPackageElementBuilder {

    /** The list of plugins  */
    private var plugins: MutableList<Class<out MediaPackageElementBuilderPlugin>>? = null

    // Create the list of available element builder pugins
    init {
        plugins = ArrayList()
        plugins!!.add(AttachmentBuilderPlugin::class.java)
        plugins!!.add(CatalogBuilderPlugin::class.java)
        plugins!!.add(TrackBuilderPlugin::class.java)
        plugins!!.add(PublicationBuilderPlugin::class.java)
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElementBuilder.elementFromURI
     */
    @Throws(UnsupportedElementException::class)
    override fun elementFromURI(uri: URI): MediaPackageElement {
        return elementFromURI(uri, null, null)
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElementBuilder.elementFromURI
     */
    @Throws(UnsupportedElementException::class)
    override fun elementFromURI(uri: URI, type: MediaPackageElement.Type?, flavor: MediaPackageElementFlavor?): MediaPackageElement {

        // Feed the file to the element builder plugins
        val candidates = ArrayList<MediaPackageElementBuilderPlugin>()
        run {
            var plugin: MediaPackageElementBuilderPlugin? = null
            for (pluginClass in plugins!!) {
                plugin = createPlugin(pluginClass)
                if (plugin.accept(uri, type, flavor))
                    candidates.add(plugin)
            }
        }

        // Check the plugins
        if (candidates.size == 0) {
            throw UnsupportedElementException("No suitable element builder plugin found for $uri")
        } else if (candidates.size > 1) {
            val buf = StringBuffer()
            for (plugin in candidates) {
                if (buf.length > 0)
                    buf.append(", ")
                buf.append(plugin.toString())
            }
            logger.debug("More than one element builder plugin with the same priority claims responsibilty for " + uri + ": "
                    + buf.toString())
        }

        // Create media package element depending on mime type flavor
        val builderPlugin = candidates[0]
        val element = builderPlugin.elementFromURI(uri)
        element.flavor = flavor
        builderPlugin.destroy()
        return element
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElementBuilder.elementFromManifest
     */
    @Throws(UnsupportedElementException::class)
    override fun elementFromManifest(node: Node, serializer: MediaPackageSerializer): MediaPackageElement {
        val candidates = ArrayList<MediaPackageElementBuilderPlugin>()
        for (pluginClass in plugins!!) {
            val plugin = createPlugin(pluginClass)
            if (plugin.accept(node)) {
                candidates.add(plugin)
            }
        }

        // Check the plugins
        if (candidates.size == 0) {
            throw UnsupportedElementException("No suitable element builder plugin found for node " + node.nodeName)
        } else if (candidates.size > 1) {
            val buf = StringBuffer()
            for (plugin in candidates) {
                if (buf.length > 0)
                    buf.append(", ")
                buf.append(plugin.toString())
            }
            val xpath = XPathFactory.newInstance().newXPath()
            val name = node.nodeName
            var elementFlavor: String? = null
            try {
                elementFlavor = xpath.evaluate("@type", node)
            } catch (e: XPathExpressionException) {
                elementFlavor = "(unknown)"
            }

            logger.debug("More than one element builder plugin claims responsability for " + name + " of flavor "
                    + elementFlavor + ": " + buf.toString())
        }

        // Create a new media package element
        val builderPlugin = candidates[0]
        val element = builderPlugin.elementFromManifest(node, serializer)
        builderPlugin.destroy()
        return element
    }

    /**
     * @see org.opencastproject.mediapackage.MediaPackageElementBuilder.newElement
     */
    override fun newElement(type: MediaPackageElement.Type, flavor: MediaPackageElementFlavor): MediaPackageElement? {
        val candidates = ArrayList<MediaPackageElementBuilderPlugin>()
        for (pluginClass in plugins!!) {
            val plugin = createPlugin(pluginClass)
            if (plugin.accept(type, flavor)) {
                candidates.add(plugin)
            }
        }

        // Check the plugins
        if (candidates.size == 0)
            return null
        else if (candidates.size > 1) {
            val buf = StringBuffer()
            for (plugin in candidates) {
                if (buf.length > 0)
                    buf.append(", ")
                buf.append(plugin.toString())
            }
            logger.debug("More than one element builder plugin claims responsibilty for $flavor: $buf")
        }

        // Create a new media package element
        val builderPlugin = candidates[0]
        val element = builderPlugin.newElement(type, flavor)
        builderPlugin.destroy()
        return element
    }

    /**
     * Creates and initializes a new builder plugin.
     */
    private fun createPlugin(clazz: Class<out MediaPackageElementBuilderPlugin>): MediaPackageElementBuilderPlugin {
        var plugin: MediaPackageElementBuilderPlugin? = null
        try {
            plugin = clazz.newInstance()
        } catch (e: InstantiationException) {
            throw RuntimeException("Cannot instantiate media package element builder plugin of type " + clazz.name
                    + ". Did you provide a parameterless constructor?", e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }

        try {
            plugin!!.init()
        } catch (e: Exception) {
            throw RuntimeException("An error occured while setting up media package element builder plugin " + plugin!!)
        }

        return plugin
    }

    companion object {

        /** the logging facility provided by log4j  */
        private val logger = LoggerFactory.getLogger(MediaPackageElementBuilderImpl::class.java!!.getName())
    }

}
