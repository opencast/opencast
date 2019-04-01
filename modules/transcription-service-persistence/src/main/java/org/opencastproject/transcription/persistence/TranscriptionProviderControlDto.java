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
package org.opencastproject.transcription.persistence;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;

/**
 * Entity object for the providers.
 */
@Entity(name = "TranscriptionProvider")
@Table(name = "oc_transcription_service_provider")
@NamedQueries({
  @NamedQuery(name = "TranscriptionProvider.findProviderById", query = "SELECT c FROM TranscriptionProvider c WHERE c.id= :id"),
  @NamedQuery(name = "TranscriptionProvider.findIdByProvider", query = "SELECT c FROM TranscriptionProvider c WHERE c.provider= :provider")})
public class TranscriptionProviderControlDto implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", length = 128)
  private long id;

  @Column(name = "provider", nullable = false)
  private String provider;

  /**
   * Default constructor
   */
  public TranscriptionProviderControlDto() {
  }

  /**
   * Creates a provider
   *
   * @param provider the provider
   */
  public TranscriptionProviderControlDto(String provider) {
    super();
    this.provider = provider;
  }

  /**
   * Convert into business object.
   */
  public TranscriptionProviderControl toTranscriptionProviderControl() {
    return new TranscriptionProviderControl(id, provider);
  }

  /**
   * Store new provider
   */
  public static TranscriptionProviderControlDto storeProvider(EntityManager em, String provider) throws TranscriptionDatabaseException {
    TranscriptionProviderControlDto dto = new TranscriptionProviderControlDto(provider);
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();
      em.persist(dto);
      tx.commit();
      return dto;
    } catch (Exception e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw new TranscriptionDatabaseException(e);
    } finally {
      em.close();
    }
  }

  /**
   * Find a transcription provider by its id.
   */
  public static TranscriptionProviderControlDto findProviderById(EntityManager em, long id)
          throws TranscriptionDatabaseException {
    Query query = null;
    try {
      query = em.createNamedQuery("TranscriptionProvider.findProviderById");
      query.setParameter("id", id);
      return (TranscriptionProviderControlDto) query.getSingleResult();
    } catch (NoResultException e) {
      return null; // Not found
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

  /**
   * Find a transcription provider id by provider name.
   */
  public static TranscriptionProviderControlDto findIdByProvider(EntityManager em, String provider)
          throws TranscriptionDatabaseException {
    Query query = null;
    try {
      query = em.createNamedQuery("TranscriptionProvider.findIdByProvider");
      query.setParameter("provider", provider);
      return (TranscriptionProviderControlDto) query.getSingleResult();
    } catch (NoResultException e) {
      return null; // Not found
    } catch (Exception e) {
      throw new TranscriptionDatabaseException(e);
    }
  }

}
