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

package org.opencastproject.authorization.xacml.manager.impl.persistence;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Lazy;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistence.PersistenceUtil;

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
import java.util.List;

import static org.opencastproject.security.api.AccessControlParser.parseAclSilent;
import static org.opencastproject.security.api.AccessControlParser.toJsonSilent;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.persistence.PersistenceUtil.findAll;
import static org.opencastproject.util.persistence.PersistenceUtil.runSingleResultQuery;
import static org.opencastproject.util.persistence.PersistenceUtil.runUpdate;

@Entity(name = "ManagedAcl")
@Table(name = "mh_acl_managed_acl",
       uniqueConstraints = @UniqueConstraint(columnNames = {"name", "organization_id"}))
@NamedQueries({
        @NamedQuery(name = "ManagedAcl.findByIdAndOrg",
                    query = "SELECT e FROM ManagedAcl e WHERE e.id = :id AND e.organizationId = :organization"),
        @NamedQuery(name = "ManagedAcl.findAllByOrg",
                    query = "SELECT e FROM ManagedAcl e WHERE e.organizationId = :organization"),
        @NamedQuery(name = "ManagedAcl.deleteByIdAndOrg",
                    query = "DELETE FROM ManagedAcl e WHERE e.id = :id AND e.organizationId = :organization") })
/** JPA link of {@link ManagedAcl}. */
public final class ManagedAclEntity implements ManagedAcl {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "pk")
  private Long id;

  @Column(name = "name", nullable = false)
  private String name;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "acl", nullable = false)
  private String acl;

  @Transient
  private Lazy<AccessControlList> parsedAcl = new Lazy<AccessControlList>(new Function0<AccessControlList>() {
    @Override public AccessControlList apply() {
      return parseAclSilent(acl);
    }
  });

  @Column(name = "organization_id", nullable = false)
  private String organizationId;


  /** JPA constructor */
  public ManagedAclEntity() {
  }

  ManagedAclEntity update(String name, AccessControlList acl, String orgId) {
    this.name = name;
    this.acl = toJsonSilent(acl);
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
    return parsedAcl.value();
  }

  @Override public String getOrganizationId() {
    return organizationId;
  }

  /** Find a managed ACL by id. */
  public static Function<EntityManager, Option<ManagedAclEntity>> findByIdAndOrg(final String orgId, final Long id) {
    return new Function<EntityManager, Option<ManagedAclEntity>>() {
      @Override public Option<ManagedAclEntity> apply(EntityManager em) {
        return runSingleResultQuery(em, "ManagedAcl.findByIdAndOrg", tuple("id", id), tuple("organization", orgId));
      }
    };
  }

  /** Find a managed ACL by id. */
  public static Function<EntityManager, Option<ManagedAclEntity>> findById(final Long id) {
    return PersistenceUtil.findById(ManagedAclEntity.class, id);
  }

  /** Find all ACLs of an organization. */
  public static Function<EntityManager, List<ManagedAclEntity>> findByOrg(final String orgId) {
    return new Function<EntityManager, List<ManagedAclEntity>>() {
      @Override public List<ManagedAclEntity> apply(EntityManager em) {
        return findAll(em, "ManagedAcl.findAllByOrg", tuple("organization", orgId));
      }
    };
  }

  /** Delete an ACL by id. */
  public static Function<EntityManager, Boolean> deleteByIdAndOrg(final String orgId, final Long id) {
    return new Function<EntityManager, Boolean>() {
      @Override public Boolean apply(EntityManager em) {
        return runUpdate(em, "ManagedAcl.deleteByIdAndOrg", tuple("id", id), tuple("organization", orgId));
      }
    };
  }
}
