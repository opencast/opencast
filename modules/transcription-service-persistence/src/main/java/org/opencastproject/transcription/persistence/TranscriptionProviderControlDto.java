/*
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

import static org.opencastproject.db.Queries.namedQuery;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Entity object for the providers.
 */
@Entity(name = "TranscriptionProvider")
@Table(name = "oc_transcription_service_provider")
@NamedQueries({
    @NamedQuery(
        name = "TranscriptionProvider.findProviderById",
        query = "SELECT c FROM TranscriptionProvider c WHERE c.id= :id"
    ),
    @NamedQuery(
        name = "TranscriptionProvider.findIdByProvider",
        query = "SELECT c FROM TranscriptionProvider c WHERE c.provider= :provider"
    ),
})
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
  public static Function<EntityManager, TranscriptionProviderControlDto> storeProviderQuery(String provider) {
    return em -> {
      TranscriptionProviderControlDto dto = new TranscriptionProviderControlDto(provider);
      em.persist(dto);
      return dto;
    };
  }

  /**
   * Find a transcription provider by its id.
   */
  public static Function<EntityManager, Optional<TranscriptionProviderControlDto>> findProviderByIdQuery(long id) {
    return namedQuery.findOpt(
        "TranscriptionProvider.findProviderById",
        TranscriptionProviderControlDto.class,
        Pair.of("id", id)
    );
  }

  /**
   * Find a transcription provider id by provider name.
   */
  public static Function<EntityManager, Optional<TranscriptionProviderControlDto>> findIdByProviderQuery(
      String provider) {
    return namedQuery.findOpt(
        "TranscriptionProvider.findIdByProvider",
        TranscriptionProviderControlDto.class,
        Pair.of("provider", provider)
    );
  }
}
