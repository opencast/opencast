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

import static org.opencastproject.authorization.xacml.manager.impl.persistence.ManagedAclEntity.findByIdAndOrg;
import static org.opencastproject.util.data.functions.Misc.chuck;
import static org.opencastproject.util.persistence.PersistenceEnvs.persistenceEnvironment;
import static org.opencastproject.util.persistence.PersistenceUtil.equip2;
import static org.opencastproject.util.persistence.PersistenceUtil.persist;

import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.impl.AclDb;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.Organization;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.functions.Misc;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceEnv2;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.util.List;

import javax.persistence.EntityManager;
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
  private PersistenceEnv penv;
  private PersistenceEnv2<Void> penvf;

  @Deactivate
  public synchronized void deactivate() {
    penv.close();
  }

  @Reference(target = "(osgi.unit.name=org.opencastproject.authorization.xacml.manager)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.penv = persistenceEnvironment(emf);
    this.penvf = equip2(penv, uniqueConstraintViolationHandler);
  }

  @Override public List<ManagedAcl> getAcls(Organization org) {
    return Misc.widen(Monadics.mlist(penv.tx(ManagedAclEntity.findByOrg(org.getId()))).value());
  }

  @Override public Option<ManagedAcl> getAcl(Organization org, long id) {
    return Misc.widen(penv.tx(findByIdAndOrg(org.getId(), id)));
  }

  @Override public boolean updateAcl(final ManagedAcl acl) {
    return penv.tx(new Function<EntityManager, Boolean>() {
      @Override public Boolean apply(final EntityManager em) {
        for (ManagedAclEntity e : ManagedAclEntity.findByIdAndOrg(acl.getOrganizationId(), acl.getId()).apply(em)) {
          final ManagedAclEntity updated = e.update(acl.getName(), acl.getAcl(), acl.getOrganizationId());
          em.merge(updated);
          return true;
        }
        return false;
      }
    });
  }

  @Override public Option<ManagedAcl> createAcl(Organization org, AccessControlList acl, String name) {
    final ManagedAcl e = new ManagedAclEntity().update(name, acl, org.getId());
    return penvf.tx(persist(e)).right().toOption();
  }

  @Override public boolean deleteAcl(Organization org, long id) {
    return penv.tx(ManagedAclEntity.deleteByIdAndOrg(org.getId(), id));
  }

  /** DB exception handler that takes care of unique constraint violation and rethrows any other exception. */
  public static final Function<Exception, Void> uniqueConstraintViolationHandler = new Function<Exception, Void>() {
    @Override
    public Void apply(Exception e) {
      if (e instanceof RollbackException) {
        final Throwable cause = e.getCause();
        String message = cause.getMessage().toLowerCase();
        if (message.contains("unique") || message.contains("duplicate")) {
          return null;
        }
      }
      return chuck(e);
    }
  };
}
