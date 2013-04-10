/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package org.opencastproject.mediapackage;

import org.opencastproject.mediapackage.elementbuilder.AttachmentBuilderPlugin;
import org.opencastproject.mediapackage.elementbuilder.CatalogBuilderPlugin;
import org.opencastproject.mediapackage.elementbuilder.MediaPackageElementBuilderPlugin;
import org.opencastproject.mediapackage.elementbuilder.PublicationBuilderPlugin;
import org.opencastproject.mediapackage.elementbuilder.TrackBuilderPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Default implementation for a media package element builder.
 */
public class MediaPackageElementBuilderImpl implements MediaPackageElementBuilder {

  /** The list of plugins */
  private List<Class<? extends MediaPackageElementBuilderPlugin>> plugins = null;

  /** the logging facility provided by log4j */
  private static final Logger logger = LoggerFactory.getLogger(MediaPackageElementBuilderImpl.class.getName());

  // Create the list of available element builder pugins
  public MediaPackageElementBuilderImpl() {
    plugins = new ArrayList<Class<? extends MediaPackageElementBuilderPlugin>>();
    plugins.add(AttachmentBuilderPlugin.class);
    plugins.add(CatalogBuilderPlugin.class);
    plugins.add(TrackBuilderPlugin.class);
    plugins.add(PublicationBuilderPlugin.class);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElementBuilder#elementFromURI(URI)
   */
  public MediaPackageElement elementFromURI(URI uri) throws UnsupportedElementException {
    return elementFromURI(uri, null, null);
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElementBuilder#elementFromURI(URI,
   *      org.opencastproject.mediapackage.MediaPackageElement.Type ,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement elementFromURI(URI uri, MediaPackageElement.Type type, MediaPackageElementFlavor flavor)
          throws UnsupportedElementException {

    // Feed the file to the element builder plugins
    List<MediaPackageElementBuilderPlugin> candidates = new ArrayList<MediaPackageElementBuilderPlugin>();
    {
      MediaPackageElementBuilderPlugin plugin = null;
      for (Class<? extends MediaPackageElementBuilderPlugin> pluginClass : plugins) {
        plugin = createPlugin(pluginClass);
        if (plugin.accept(uri, type, flavor))
          candidates.add(plugin);
      }
    }

    // Check the plugins
    if (candidates.size() == 0) {
      throw new UnsupportedElementException("No suitable element builder plugin found for " + uri);
    } else if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (MediaPackageElementBuilderPlugin plugin : candidates) {
        if (buf.length() > 0)
          buf.append(", ");
        buf.append(plugin.toString());
      }
      logger.debug("More than one element builder plugin with the same priority claims responsibilty for " + uri + ": "
              + buf.toString());
    }

    // Create media package element depending on mime type flavor
    MediaPackageElementBuilderPlugin builderPlugin = candidates.get(0);
    MediaPackageElement element = builderPlugin.elementFromURI(uri);
    element.setFlavor(flavor);
    builderPlugin.destroy();
    return element;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElementBuilder#elementFromManifest(org.w3c.dom.Node,
   *      org.opencastproject.mediapackage.MediaPackageSerializer)
   */
  public MediaPackageElement elementFromManifest(Node node, MediaPackageSerializer serializer)
          throws UnsupportedElementException {
    List<MediaPackageElementBuilderPlugin> candidates = new ArrayList<MediaPackageElementBuilderPlugin>();
    for (Class<? extends MediaPackageElementBuilderPlugin> pluginClass : plugins) {
      MediaPackageElementBuilderPlugin plugin = createPlugin(pluginClass);
      if (plugin.accept(node)) {
        candidates.add(plugin);
      }
    }

    // Check the plugins
    if (candidates.size() == 0) {
      throw new UnsupportedElementException("No suitable element builder plugin found for node " + node.getNodeName());
    } else if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (MediaPackageElementBuilderPlugin plugin : candidates) {
        if (buf.length() > 0)
          buf.append(", ");
        buf.append(plugin.toString());
      }
      XPath xpath = XPathFactory.newInstance().newXPath();
      String name = node.getNodeName();
      String elementFlavor = null;
      try {
        elementFlavor = xpath.evaluate("@type", node);
      } catch (XPathExpressionException e) {
        elementFlavor = "(unknown)";
      }
      logger.debug("More than one element builder plugin claims responsability for " + name + " of flavor "
              + elementFlavor + ": " + buf.toString());
    }

    // Create a new media package element
    MediaPackageElementBuilderPlugin builderPlugin = candidates.get(0);
    MediaPackageElement element = builderPlugin.elementFromManifest(node, serializer);
    builderPlugin.destroy();
    return element;
  }

  /**
   * @see org.opencastproject.mediapackage.MediaPackageElementBuilder#newElement(org.opencastproject.mediapackage.MediaPackageElement.Type
   *      , org.opencastproject.mediapackage.MediaPackageElementFlavor)
   */
  public MediaPackageElement newElement(MediaPackageElement.Type type, MediaPackageElementFlavor flavor) {
    List<MediaPackageElementBuilderPlugin> candidates = new ArrayList<MediaPackageElementBuilderPlugin>();
    for (Class<? extends MediaPackageElementBuilderPlugin> pluginClass : plugins) {
      MediaPackageElementBuilderPlugin plugin = createPlugin(pluginClass);
      if (plugin.accept(type, flavor)) {
        candidates.add(plugin);
      }
    }

    // Check the plugins
    if (candidates.size() == 0)
      return null;
    else if (candidates.size() > 1) {
      StringBuffer buf = new StringBuffer();
      for (MediaPackageElementBuilderPlugin plugin : candidates) {
        if (buf.length() > 0)
          buf.append(", ");
        buf.append(plugin.toString());
      }
      logger.debug("More than one element builder plugin claims responsibilty for " + flavor + ": " + buf.toString());
    }

    // Create a new media package element
    MediaPackageElementBuilderPlugin builderPlugin = candidates.get(0);
    MediaPackageElement element = builderPlugin.newElement(type, flavor);
    builderPlugin.destroy();
    return element;
  }

  /**
   * Creates and initializes a new builder plugin.
   */
  private MediaPackageElementBuilderPlugin createPlugin(Class<? extends MediaPackageElementBuilderPlugin> clazz) {
    MediaPackageElementBuilderPlugin plugin = null;
    try {
      plugin = clazz.newInstance();
    } catch (InstantiationException e) {
      throw new RuntimeException("Cannot instantiate media package element builder plugin of type " + clazz.getName()
              + ". Did you provide a parameterless constructor?", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    try {
      plugin.init();
    } catch (Exception e) {
      throw new RuntimeException("An error occured while setting up media package element builder plugin " + plugin);
    }
    return plugin;
  }

}
