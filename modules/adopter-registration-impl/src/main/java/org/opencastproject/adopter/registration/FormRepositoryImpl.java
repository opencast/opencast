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
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.adopter.registration;

import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Repository that handles registration forms for the adopter statistics.
 */
@Component(
    immediate = true,
    service = FormRepository.class,
    property = {
        "service.description=Repository for the statistics registration form"
    }
)
public class FormRepositoryImpl implements FormRepository {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(FormRepository.class);

  /** The factory for creating the entity manager. */
  protected EntityManagerFactory emf = null;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  /** OSGi setter for the entity manager factory. */
  @Reference(target = "(osgi.unit.name=org.opencastproject.adopter)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  @Activate
  public void activate() {
    db = dbSessionFactory.createSession(emf);
  }

  @Deactivate
  public void deactivate() {
    db.close();
  }

  //================================================================================
  // Methods
  //================================================================================

  @Override
  public void save(IForm f) throws FormRepositoryException {
    Form form = (Form) f;
    try {
      db.execTx(em -> {
        Optional<Form> dbForm = getFormQuery().apply(em);
        if (dbForm.isEmpty()) {
          // Null means, that there is no entry in the DB yet, so we create UUIDs for the keys.
          form.setAdopterKey(UUID.randomUUID().toString());
          form.setStatisticKey(UUID.randomUUID().toString());
          form.setDateCreated(new Date());
          form.setDateModified(new Date());
          em.persist(form);
        } else {
          dbForm.get().merge(form);
          em.merge(dbForm.get());
        }
      });
    } catch (Exception e) {
      logger.error("Couldn't update the adopter statistics registration form: {}", e.getMessage());
      throw new FormRepositoryException(e);
    }
  }

  @Override
  public void delete() {
    try {
      db.execTx(namedQuery.delete("Form.deleteAll"));
    } catch (Exception e) {
      logger.error("Error occurred while deleting the adopter registration table. {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @Override
  public IForm getForm() throws FormRepositoryException {
    return db.exec(getFormQuery()).orElse(null);
  }

  /**
   * Return the adopter registration form from db.
   * @return The registration form or <code>null</code> if not found
   * @throws FormRepositoryException If there is a problem communicating
   *                                 with the underlying data store.
   */
  private Function<EntityManager, Optional<Form>> getFormQuery() {
    return namedQuery.findOpt("Form.findAll", Form.class);
  }
}
