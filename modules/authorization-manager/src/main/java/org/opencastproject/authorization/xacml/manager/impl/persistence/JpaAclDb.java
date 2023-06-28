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

import static org.opencastproject.authorization.xacml.manager.impl.persistence.ManagedAclEntity.findByIdAndOrgQuery;
import static org.opencastproject.db.Queries.namedQuery;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.impl.AclDb;
import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.data.functions.Misc;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManagerFactory;
import javax.persistence.RollbackException;

/** JPA based impl of an {@link org.opencastproject.authorization.xacml.manager.impl.AclDb}. */
@Component(
    property = {
        "service.description=JPA based ACL Provider"
    },
    immediate = true,
    service = { AclDb.class }
)
public final class JpaAclDb implements AclDb {
  private DBSessionFactory dbSessionFactory;
  private EntityManagerFactory emf;
  private DBSession db;

  @Activate
  public void activate() {
    db = dbSessionFactory.createSession(emf);
  }

  @Deactivate
  public synchronized void deactivate() {
    db.close();
  }

  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  @Reference(target = "(osgi.unit.name=org.opencastproject.authorization.xacml.manager)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Override
  public List<ManagedAcl> getAcls(Organization org) {
    return Misc.widen(db.execTx(ManagedAclEntity.findByOrgQuery(org.getId())));
  }

  @Override
  public Optional<ManagedAcl> getAcl(Organization org, long id) {
    return Misc.widen(db.execTx(ManagedAclEntity.findByIdAndOrgQuery(org.getId(), id)));
  }

  @Override
  public boolean updateAcl(final ManagedAcl acl) {
    return db.execTx(em -> {
      Optional<ManagedAclEntity> e = findByIdAndOrgQuery(acl.getOrganizationId(), acl.getId()).apply(em);
      if (e.isEmpty()) {
        return false;
      }
      final ManagedAclEntity updated = e.get().update(acl.getName(), acl.getAcl(), acl.getOrganizationId());
      em.merge(updated);
      return true;
    });
  }

  @Override
  public Optional<ManagedAcl> createAcl(Organization org, AccessControlList acl, String name) {
    try {
      final ManagedAcl e = new ManagedAclEntity().update(name, acl, org.getId());
      return db.execTx(namedQuery.persistOpt(e));
    } catch (RollbackException e) {
      // DB exception handler that takes care of unique constraint violation and rethrows any other exception.
      final Throwable cause = e.getCause();
      String message = cause.getMessage().toLowerCase();
      if (message.contains("unique") || message.contains("duplicate")) {
        return Optional.empty();
      }
      throw e;
    }
  }

  @Override
  public boolean deleteAcl(Organization org, long id) {
    return db.execTx(ManagedAclEntity.deleteByIdAndOrgQuery(org.getId(), id)) > 0;
  }
}
