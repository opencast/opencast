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
package org.opencastproject.coverimage.impl;

import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.workspace.api.Workspace;

import org.apache.batik.ext.awt.image.GraphicsUtil;
import org.apache.batik.ext.awt.image.codec.jpeg.JPEGRegistryEntry;
import org.apache.batik.ext.awt.image.renderable.DeferRable;
import org.apache.batik.ext.awt.image.renderable.Filter;
import org.apache.batik.ext.awt.image.renderable.RedRable;
import org.apache.batik.ext.awt.image.rendered.Any2sRGBRed;
import org.apache.batik.ext.awt.image.rendered.CachableRed;
import org.apache.batik.ext.awt.image.rendered.FormatRed;
import org.apache.batik.ext.awt.image.spi.ImageTagRegistry;
import org.apache.batik.util.ParsedURL;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * Implementation of {@link AbstractCoverImageService} for use in OSGi environment
 */
public class CoverImageServiceOsgiImpl extends AbstractCoverImageService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(CoverImageServiceOsgiImpl.class);

  /**
   * OSGi activation callback
   *
   * @param cc
   *          the OSGi component context
   */
  protected void activate(ComponentContext cc) {
    // See
    // http://www.stichlberger.com/software/workaround-for-batiks-noclassdeffounderrorclassnotfoundexception-truncatedfileexception/
    // ---------------
    // add this code before you use batik (make sure is runs only once)
    // via the lower priority this subclass is registered before JPEGRegistryEntry
    // and prevents JPEGRegistryEntry.handleStream from breaking when used on a non Sun/Oracle JDK
    JPEGRegistryEntry entry = new JPEGRegistryEntry() {

      public float getPriority() {
        // higher than that of JPEGRegistryEntry (which is 1000)
        return 500;
      }

      /**
       * Decode the Stream into a RenderableImage
       *
       * @param inIS
       *          The input stream that contains the image.
       * @param origURL
       *          The original URL, if any, for documentation purposes only. This may be null.
       * @param needRawData
       *          If true the image returned should not have any default color correction the file may specify applied.
       */
      public Filter handleStream(InputStream inIS, ParsedURL origURL, boolean needRawData) {
        // Code from org.apache.batik.ext.awt.image.codec.jpeg.JPEGRegistryEntry#handleStream
        // Reading image with ImageIO to prevent NoClassDefFoundError on OpenJDK

        final DeferRable dr = new DeferRable();
        final InputStream is = inIS;
        final String errCode;
        final Object[] errParam;
        if (origURL != null) {
          errCode = ERR_URL_FORMAT_UNREADABLE;
          errParam = new Object[] { "JPEG", origURL };
        } else {
          errCode = ERR_STREAM_FORMAT_UNREADABLE;
          errParam = new Object[] { "JPEG" };
        }

        Thread t = new Thread() {
          public void run() {
            Filter filt;
            try {
              BufferedImage image;
              image = ImageIO.read(is);
              dr.setBounds(new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()));
              CachableRed cr;
              cr = GraphicsUtil.wrap(image);
              cr = new Any2sRGBRed(cr);
              cr = new FormatRed(cr, GraphicsUtil.sRGB_Unpre);
              WritableRaster wr = (WritableRaster) cr.getData();
              ColorModel cm = cr.getColorModel();
              image = new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
              cr = GraphicsUtil.wrap(image);
              filt = new RedRable(cr);
            } catch (IOException ioe) {
              // Something bad happened here...
              filt = ImageTagRegistry.getBrokenLinkImage(this, errCode, errParam);
            } catch (ThreadDeath td) {
              filt = ImageTagRegistry.getBrokenLinkImage(this, errCode, errParam);
              dr.setSource(filt);
              throw td;
            } catch (Throwable t) {
              filt = ImageTagRegistry.getBrokenLinkImage(this, errCode, errParam);
            }

            dr.setSource(filt);
          }
        };
        t.start();
        return dr;
      }
    };

    ImageTagRegistry.getRegistry().register(entry);

    logger.info("Cover image service activated");
  }

  /**
   * OSGi callback to set the workspace service.
   *
   * @param workspace
   *          the workspace service
   */
  protected void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  /**
   * OSGi callback to set the service registry service
   *
   * @param serviceRegistry
   *          the service registry service
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * OSGi callback to set the security service
   *
   * @param securityService
   *          the security service
   */
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * OSGi callback to set the user directory service
   *
   * @param userDirectoryService
   *          the user directory service
   */
  protected void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * OSGi callback to set the organization directory service
   *
   * @param organizationDirectoryService
   *          the organization directory service
   */
  protected void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

}
