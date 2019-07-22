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

import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.impl.RuntimeTypes;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

@Entity(name = "Property")
@Table(name = "oc_assets_properties")
@NamedQueries({
    @NamedQuery(name = "Property.selectByMediaPackageAndNamespace", query = "select p from Property p where "
            + "p.mediaPackageId = :mediaPackageId and p.namespace = :namespace"),
    @NamedQuery(name = "Property.delete", query = "delete from Property p where p.mediaPackageId = :mediaPackageId"),
    @NamedQuery(name = "Property.deleteByNamespace", query = "delete from Property p "
            + "where p.mediaPackageId = :mediaPackageId and p.namespace = :namespace")})
public class PropertyDto {
  private static final Logger logger = LoggerFactory.getLogger(PropertyDto.class);

  /** Surrogate key. */
  @Id
  @GeneratedValue
  @Column(name = "id")
  private Long id;

  /** Part 1 of the business key. */
  @Column(name = "mediapackage_id", nullable = false, length = 128)
  private String mediaPackageId;

  /** Part 2 of the business key. */
  @Column(name = "namespace", nullable = false, length = 128)
  private String namespace;

  /** Part 3 of the business key. */
  @Column(name = "property_name", nullable = false, length = 128)
  private String propertyName;

  @Column(name = "val_string", nullable = true)
  private String stringValue;

  @Column(name = "val_date", nullable = true)
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateValue;

  @Column(name = "val_long", nullable = true)
  private Long longValue;

  @Column(name = "val_bool", nullable = true)
  private Boolean boolValue;

  public static PropertyDto mk(Property property) {
    final PropertyDto dto = new PropertyDto();
    dto.mediaPackageId = property.getId().getMediaPackageId();
    dto.namespace = property.getId().getNamespace();
    dto.propertyName = property.getId().getName();
    setValue(dto, property.getValue());
    return dto;
  }

  public Property toProperty() {
    final PropertyId id = new PropertyId(mediaPackageId, namespace, propertyName);
    if (stringValue != null) {
      return new Property(id, Value.mk(stringValue));
    } else if (dateValue != null) {
      return new Property(id, Value.mk(dateValue));
    } else if (longValue != null) {
      return new Property(id, Value.mk(longValue));
    } else if (boolValue != null) {
      return new Property(id, Value.mk(boolValue));
    } else {
      throw new RuntimeException("Bug. At least one of the value columns must be non null.");
    }
  }

  public PropertyDto update(Value value) {
    final PropertyDto dto = new PropertyDto();
    dto.id = id;
    dto.mediaPackageId = mediaPackageId;
    dto.namespace = namespace;
    dto.propertyName = propertyName;
    setValue(dto, value);
    return dto;
  }

  public static final Fn<PropertyDto, Property> toProperty = new Fn<PropertyDto, Property>() {
    @Override public Property apply(PropertyDto a) {
      return a.toProperty();
    }
  };

  private static void setValue(final PropertyDto dto, final Value value) {
    value.decompose(
            new Fx<String>() {
              @Override public void apply(String a) {
                dto.stringValue = a;
              }
            }.toFn(),
            new Fx<Date>() {
              @Override public void apply(Date a) {
                dto.dateValue = a;
              }
            }.toFn(),
            new Fx<Long>() {
              @Override public void apply(Long a) {
                dto.longValue = a;
              }
            }.toFn(),
            new Fx<Boolean>() {
              @Override public void apply(Boolean a) {
                dto.boolValue = a;
              }
            }.toFn(),
            new Fx<Version>() {
              @Override public void apply(Version a) {
                dto.longValue = RuntimeTypes.convert(a).value();
              }
            }.toFn());
  }

  public static int delete(EntityManager em, final String mediaPackageId) {
    return delete(em, mediaPackageId, null);
  }

  public static int delete(EntityManager em, final String mediaPackageId, final String namespace) {
    TypedQuery<PropertyDto> query;
    if (namespace == null) {
      query = em.createNamedQuery("Property.delete", PropertyDto.class)
              .setParameter("mediaPackageId", mediaPackageId);
    } else {
      query = em.createNamedQuery("Property.deleteByNamespace", PropertyDto.class)
              .setParameter("mediaPackageId", mediaPackageId)
              .setParameter("namespace", namespace);
    }
    logger.debug("Executing query {}", query);
    EntityTransaction tx = em.getTransaction();
    tx.begin();
    final int num = query.executeUpdate();
    tx.commit();
    return num;
  }

  public static List<Property> select(EntityManager em, final String mediaPackageId, final String namespace) {
    TypedQuery<PropertyDto> query = em.createNamedQuery("Property.selectByMediaPackageAndNamespace", PropertyDto.class)
              .setParameter("mediaPackageId", mediaPackageId)
              .setParameter("namespace", namespace);
    logger.debug("Executing query {}", query);
    return query.getResultList().parallelStream().map(PropertyDto::toProperty).collect(Collectors.toList());
  }
}
