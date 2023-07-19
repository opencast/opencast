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

package org.opencastproject.authorization.xacml.manager.impl.persistence;

import static org.opencastproject.db.Queries.namedQuery;
import static org.opencastproject.security.api.AccessControlParser.parseAclSilent;
import static org.opencastproject.security.api.AccessControlParser.toJsonSilent;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlList;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

@Entity(name = "ManagedAcl")
@Table(name = "oc_acl_managed_acl",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "organization_id"}))
@NamedQueries({
        @NamedQuery(name = "ManagedAcl.findByIdAndOrg",
                    query = "SELECT e FROM ManagedAcl e WHERE e.id = :id AND e.organizationId = :organization"),
        @NamedQuery(name = "ManagedAcl.findAllByOrg",
                    query = "SELECT e FROM ManagedAcl e WHERE e.organizationId = :organization"),
        @NamedQuery(name = "ManagedAcl.deleteByIdAndOrg",
                    query = "DELETE FROM ManagedAcl e WHERE e.id = :id AND e.organizationId = :organization") })
/** JPA link of {@link ManagedAcl}. */
public class ManagedAclEntity implements ManagedAcl {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "pk")
  private Long id;

  @Column(name = "name", nullable = false, length = 128)
  private String name;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "acl", nullable = false)
  private String acl;

  @Transient
  private AccessControlList parsedAcl;

  @Column(name = "organization_id", nullable = false, length = 128)
  private String organizationId;

  /** JPA constructor */
  public ManagedAclEntity() {
  }

  ManagedAclEntity update(String name, AccessControlList acl, String orgId) {
    // Update the ACL first, since it's fetching the entity and overriding the previous set values
    this.acl = toJsonSilent(acl);
    this.name = name;
    this.organizationId = orgId;
    return this;
  }

  @Override public Long getId() {
    return id;
  }

  @Override public String getName() {
    return name;
  }

  @Override public AccessControlList getAcl() {
    if (parsedAcl == null) {
      parsedAcl = parseAclSilent(acl);
    }
    return parsedAcl;
  }

  @Override public String getOrganizationId() {
    return organizationId;
  }

  /** Find a managed ACL by id. */
  public static Function<EntityManager, Optional<ManagedAclEntity>> findByIdAndOrgQuery(final String orgId,
      final Long id) {
    return namedQuery.findOpt(
        "ManagedAcl.findByIdAndOrg",
        ManagedAclEntity.class,
        Pair.of("id", id),
        Pair.of("organization", orgId)
    );
  }

  /** Find a managed ACL by id. */
  public static Function<EntityManager, Optional<ManagedAclEntity>> findByIdQuery(final Long id) {
    return namedQuery.findByIdOpt(ManagedAclEntity.class, id);
  }

  /** Find all ACLs of an organization. */
  public static Function<EntityManager, List<ManagedAclEntity>> findByOrgQuery(final String orgId) {
    return namedQuery.findAll(
        "ManagedAcl.findAllByOrg",
        ManagedAclEntity.class,
        Pair.of("organization", orgId)
    );
  }

  /** Delete an ACL by id. */
  public static Function<EntityManager, Integer> deleteByIdAndOrgQuery(final String orgId, final Long id) {
    return namedQuery.delete(
        "ManagedAcl.deleteByIdAndOrg",
        Pair.of("id", id),
        Pair.of("organization", orgId)
    );
  }
}
