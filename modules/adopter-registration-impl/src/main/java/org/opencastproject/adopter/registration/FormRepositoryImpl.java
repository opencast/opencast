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
 * Repository that handles registration forms for the adopter statistics.
 */
public class FormRepositoryImpl implements FormRepository {

  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(FormRepository.class);

  /** The factory for creating the entity manager. */
  protected EntityManagerFactory emf = null;

  /** OSGi setter for the entity manager factory. */
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }


  //================================================================================
  // Methods
  //================================================================================

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
        // Null means, that there is no entry in the DB yet, so we create UUIDs for the keys.
        form.setAdopterKey(UUID.randomUUID().toString());
        form.setStatisticKey(UUID.randomUUID().toString());
        form.setDateCreated(new Date());
        form.setDateModified(new Date());
        em.persist(form);
      } else {
        dbForm.merge(form);
        em.merge(dbForm);
      }
      tx.commit();
    } catch (Exception e) {
      logger.error("Couldn't update the adopter statistics registration form: {}", e.getMessage());
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new FormRepositoryException(e);
    } finally {
      if (em != null) {
        em.close();
      }
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
      logger.error("Error occurred while deleting the adopter registration table. {}", e.getMessage());
      throw new RuntimeException(e);
    } finally {
      if (em != null) {
        em.close();
      }
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
      if (em != null) {
        em.close();
      }
    }
  }

  /**
   * Return the adopter registration form from db.
   * @param em An open entity manager.
   * @return The registration form or <code>null</code> if not found
   * @throws FormRepositoryException If there is a problem communicating
   *                                 with the underlying data store.
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
