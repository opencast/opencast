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
package org.opencastproject.assetmanager.impl.persistence;

import org.opencastproject.assetmanager.api.Availability;

import com.entwinemedia.fn.Fn;
import com.mysema.query.Tuple;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.Expression;

import javax.persistence.EntityManager;

/**
 * Collection of non-JPA DTOs to tuple up the {@link SnapshotDto} JPA entity with some additional joined data.
 */
public final class SnapshotDtos {
  private SnapshotDtos() {
  }

  /**
   * Create base query for a {@link SnapshotDto} query.
   */
  public static JPAQuery baseQuery(EntityManager em) {
    final QSnapshotDto snapshotDto = QSnapshotDto.snapshotDto;
    return new JPAQuery(em, Database.TEMPLATES)
            .from(snapshotDto);
  }

  public static class Medium {
    private final SnapshotDto snapshotDto;
    private final String availability;
    private final String storageId;
    private final String organizationId;
    private final String owner;

    public Medium(SnapshotDto snapshotDto, Availability availability, String storageId, String organizationId, String owner) {
      this.snapshotDto = snapshotDto;
      this.availability = availability.name();
      this.storageId = storageId;
      this.organizationId = organizationId;
      this.owner = owner;
    }

    public SnapshotDto getSnapshotDto() {
      return snapshotDto;
    }

    public Availability getAvailability() {
      return Availability.valueOf(availability);
    }

    public String getStorageId() {
      return storageId;
    }

    public String getOrganizationId() {
      return organizationId;
    }

    public String getOwner() {
      return owner;
    }



    public static final Fn<Tuple, Medium> fromTuple = new Fn<Tuple, Medium>() {
      @Override public Medium apply(Tuple result) {
        return new Medium(
                result.get(QSnapshotDto.snapshotDto),
                Availability.valueOf(result
                        .get(QSnapshotDto.snapshotDto.availability)),
                result.get(QSnapshotDto.snapshotDto.storageId),
                result.get(QSnapshotDto.snapshotDto.organizationId),
                result.get(QSnapshotDto.snapshotDto.owner));
      }
    };

    /**
     * Parameter for query execution methods like
     * {@link com.mysema.query.jpa.impl.JPAQuery#singleResult(com.mysema.query.types.Expression[])} or
     * {@link com.mysema.query.jpa.impl.JPAQuery#list(Expression[])}.
     */
    public static final Expression<?>[] select =
            new Expression[]{QSnapshotDto.snapshotDto, QSnapshotDto.snapshotDto.storageId, QSnapshotDto.snapshotDto.availability, QSnapshotDto.snapshotDto.organizationId};
  }
}
