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

package org.opencastproject.adopterstatistics.registration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

/**
 * Repository that handles registration forms for the adopter statistics
 */
public class FormRepositoryImpl implements FormRepository {

  /**
   * The factory used to generate the entity manager.
   */
  protected EntityManagerFactory emf = null;

  /**
   * Logging utilities
   */
  private static final Logger logger = LoggerFactory.getLogger(FormRepository.class);

  /**
   * The setter for OSGI.
   *
   * @param emf The entity manager factory.
   */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Override
  public void save(IForm f) throws FormRepositoryException {
    Form form = (Form) f;
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      Form dbForm = getForm(em);
      if (dbForm == null) {
        // There is no entry in the DB yet, so we create a UUID
        form.setAdopterKey(UUID.randomUUID().toString());
        form.setDateCreated(new Date());
        form.setDateModified(new Date());
        em.persist(form);
      } else {
        dbForm.merge(form);
        em.merge(dbForm);
      }
      tx.commit();
    } catch (Exception e) {
      logger.error("Could not update adopter statistics registration form: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new FormRepositoryException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public void delete() {
    EntityManager em = null;
    EntityTransaction tx;
    try {
      em = emf.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      em.createNamedQuery("Form.deleteAll", Form.class).executeUpdate();
      tx.commit();
    } catch (Exception e) {
      logger.error("Error occurred at deleting adopter registration table.");
      throw new RuntimeException(e);
    } finally {
      if (em != null)
        em.close();
    }
  }

  @Override
  public IForm getForm() throws FormRepositoryException {
    EntityManager em = null;
    try {
      em = emf.createEntityManager();
      return getForm(em);
    } catch (NoResultException e) {
      return null;
    } finally {
      if (em != null)
        em.close();
    }
  }

  /**
   * Return the adopter registration form from db.
   *
   * @param em       an open entity manager
   * @return the registration form or <code>null</code> if not found
   * @throws FormRepositoryException if there is a problem communicating
   *                                 with the underlying data store
   */
  private Form getForm(EntityManager em) throws FormRepositoryException {
    TypedQuery<Form> q = em.createNamedQuery("Form.findAll", Form.class);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    } catch (Exception e) {
      throw new FormRepositoryException(e);
    }
  }

}
