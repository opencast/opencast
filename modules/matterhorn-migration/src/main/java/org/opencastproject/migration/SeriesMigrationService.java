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
package org.opencastproject.migration;

import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlParsingException;
import org.opencastproject.series.impl.SeriesServiceDatabase;
import org.opencastproject.series.impl.SeriesServiceDatabaseException;
import org.opencastproject.series.impl.persistence.SeriesEntity;
import org.opencastproject.util.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

/**
 * This class provides series ACL migration to Matterhorn.
 */
public class SeriesMigrationService {

  private static final String NEW_NAMESPACE = "http://org.opencastproject.security";

  private static final Logger logger = LoggerFactory.getLogger(SeriesMigrationService.class);

  /** Series service set by OSGi */
  private SeriesServiceDatabase seriesService = null;

  /** Persistence provider set by OSGi */
  protected PersistenceProvider persistenceProvider;

  /** Persistence properties used to create {@link EntityManagerFactory} */
  protected Map<String, Object> persistenceProperties;

  /** Factory used to create {@link EntityManager}s for transactions */
  protected EntityManagerFactory emf;

  /**
   * Callback for setting the series service.
   * 
   * @param seriesService
   *          the series service to set
   */
  public void setSeriesService(SeriesServiceDatabase seriesService) {
    this.seriesService = seriesService;
  }

  /**
   * OSGi callback to set persistence properties.
   * 
   * @param persistenceProperties
   *          persistence properties
   */
  public void setPersistenceProperties(Map<String, Object> persistenceProperties) {
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * OSGi callback to set persistence provider.
   * 
   * @param persistenceProvider
   *          {@link PersistenceProvider} object
   */
  public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
    this.persistenceProvider = persistenceProvider;
  }

  public void activate(ComponentContext cc) {
    emf = persistenceProvider.createEntityManagerFactory("org.opencastproject.series.impl.persistence",
            persistenceProperties);

    try {
      migrateSeriesAcl();
    } catch (SeriesServiceDatabaseException e) {
      logger.error("Coud not terminate the migration correctly: {}", e);
    } catch (IOException e) {
      logger.error("Coud not terminate the migration correctly: {}", e);
    }
  }

  @SuppressWarnings("unchecked")
  public void migrateSeriesAcl() throws SeriesServiceDatabaseException, IOException {

    logger.info("Start series ACL migration from 1.3 to 1.4");

    EntityManager em = emf.createEntityManager();
    Query query = em.createNamedQuery("Series.findAll");
    List<SeriesEntity> seriesEntities = null;
    try {
      seriesEntities = (List<SeriesEntity>) query.getResultList();
    } catch (Exception e) {
      logger.error("Could not retrieve all series: {}", e.getMessage());
      throw new SeriesServiceDatabaseException(e);
    } finally {
      em.close();
    }

    logger.info("{} series ACL to migrate", seriesEntities.size());
    int index = 0;

    for (SeriesEntity entity : seriesEntities) {

      logger.info(
              "migration of ACL from series {}: {}/{}",
              new String[] { entity.getSeriesId(), Integer.toString(++index, 10),
                      Integer.toString(seriesEntities.size()) });

      // Check if the series ACL has already been migrated
      if (entity.getAccessControl().contains(NEW_NAMESPACE)) {
        logger.info("Skip series {} has already been migrated", entity.getSeriesId());
        continue;
      }
      try {
        seriesService.storeSeriesAccessControl(entity.getSeriesId(), parse13Acl(entity.getAccessControl()));
      } catch (NotFoundException e) {
        logger.error("Could not retrieve series {}: {}", entity.getSeriesId(), e.getMessage());
        throw new SeriesServiceDatabaseException(e);
      } catch (AccessControlParsingException e) {
        logger.error("Could not parse the ACL from series {}: {}", entity.getSeriesId(), e.getMessage());
      }
    }

    logger.info("Series ACL migration ended correctly");
  }

  /**
   * Parse the series ACL from Matterhorn 1.3 and returns a valid ACL for 1.4
   * 
   * @param serializedForm
   *          The serialized version of the 1.3 ACL
   * @return A valid version of the given ACL for 1.4
   * @throws IOException
   * @throws AccessControlParsingException
   */
  public static AccessControlList parse13Acl(String serializedForm) throws IOException, AccessControlParsingException {
    ByteArrayOutputStream baos = null;
    ByteArrayInputStream bais = null;
    InputStream finalAcl = null;
    try {
      finalAcl = IOUtils.toInputStream(serializedForm, "UTF-8");
      Document domMP = new SAXBuilder().build(finalAcl);

      Namespace newNS = Namespace.getNamespace(NEW_NAMESPACE);

      Iterator<Element> it = domMP.getDescendants(new ElementFilter());
      while (it.hasNext()) {
        Element elem = it.next();
        elem.setNamespace(newNS);
      }

      baos = new ByteArrayOutputStream();
      new XMLOutputter().output(domMP, baos);
      bais = new ByteArrayInputStream(baos.toByteArray());
      return AccessControlParser.parseAcl(IOUtils.toString(bais, "UTF-8"));
    } catch (JDOMException e) {
      throw new AccessControlParsingException(e);
    } finally {
      IOUtils.closeQuietly(bais);
      IOUtils.closeQuietly(baos);
      IOUtils.closeQuietly(finalAcl);
    }
  }
}
