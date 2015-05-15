/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.kernel.bundleinfo;

import static org.opencastproject.kernel.bundleinfo.BundleInfoImpl.bundleInfo;
import static org.opencastproject.util.data.Collections.toArray;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.persistence.Queries.named;

import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Function;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

@Entity(name = "BundleInfo")
@Table(name = "mh_bundleinfo", uniqueConstraints = { @UniqueConstraint(columnNames = { "host", "bundle_name",
        "bundle_version" }) })
@NamedQueries({
        @NamedQuery(name = "BundleInfo.findAll", query = "select a from BundleInfo a order by a.host, a.bundleSymbolicName"),
        @NamedQuery(name = "BundleInfo.findAllMh", query = "select a from BundleInfo a where a.bundleSymbolicName like 'matterhorn-%' order by a.host, a.bundleSymbolicName"),
        @NamedQuery(name = "BundleInfo.deleteAll", query = "delete from BundleInfo"),
        @NamedQuery(name = "BundleInfo.deleteByHost", query = "delete from BundleInfo where host = :host"),
        @NamedQuery(name = "BundleInfo.delete", query = "delete from BundleInfo where host = :host and bundleId = :bundleId") })
public class BundleInfoJpa {
  @Id
  @GeneratedValue
  protected long id;

  @Column(name = "host", length = 128, nullable = false)
  protected String host;

  @Column(name = "bundle_name", length = 128, nullable = false)
  protected String bundleSymbolicName;

  @Column(name = "bundle_id", length = 128, nullable = false)
  protected long bundleId;

  @Column(name = "bundle_version", length = 128, nullable = false)
  protected String bundleVersion;

  @Column(name = "build_number", length = 128, nullable = true)
  protected String buildNumber;

  @Column(name = "db_schema_version", length = 128, nullable = true)
  protected String dbSchemaVersion;

  public static BundleInfoJpa create(BundleInfo a) {
    final BundleInfoJpa dto = new BundleInfoJpa();
    dto.host = a.getHost();
    dto.bundleSymbolicName = a.getBundleSymbolicName();
    dto.bundleId = a.getBundleId();
    dto.bundleVersion = a.getBundleVersion();
    for (String x : a.getBuildNumber())
      dto.buildNumber = x;
    return dto;
  }

  public BundleInfo toBundleInfo() {
    return bundleInfo(host, bundleSymbolicName, bundleId, bundleVersion, option(buildNumber), option(dbSchemaVersion));
  }

  /** {@link #toBundleInfo()} as a function. */
  public static final Function<BundleInfoJpa, BundleInfo> toBundleInfo = new Function<BundleInfoJpa, BundleInfo>() {
    @Override
    public BundleInfo apply(BundleInfoJpa dto) {
      return dto.toBundleInfo();
    }
  };

  /** Find all in database. */
  public static final Function<EntityManager, List<BundleInfoJpa>> findAll = named.findAll("BundleInfo.findAll");

  /** Find all bundles whose symbolic names start with one of the given prefixes. */
  public static Function<EntityManager, List<BundleInfoJpa>> findAll(final String... prefixes) {
    return new Function<EntityManager, List<BundleInfoJpa>>() {
      @Override
      public List<BundleInfoJpa> apply(EntityManager em) {
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<BundleInfoJpa> q = cb.createQuery(BundleInfoJpa.class);
        final Root<BundleInfoJpa> r = q.from(BundleInfoJpa.class);
        q.select(r);
        final Expression<String> symbolicNamePath = r.get("bundleSymbolicName");
        final Predicate[] likes = toArray(Predicate.class, mlist(prefixes).map(new Function<String, Predicate>() {
          @Override
          public Predicate apply(String prefix) {
            return cb.like(symbolicNamePath, prefix + "%");
          }
        }).value());
        q.where(cb.or(likes));
        q.orderBy(cb.asc(r.get("host")), cb.asc(symbolicNamePath));
        return em.createQuery(q).getResultList();
      }
    };
  }

  /** Delete all entities of BundleInfo from the database. */
  public static final Effect<EntityManager> deleteAll = named.update("BundleInfo.deleteAll").toEffect();

  /** Delete all entities of BundleInfo of a certain host from the database. */
  public static Effect<EntityManager> deleteByHost(String host) {
    return named.update("BundleInfo.deleteByHost", tuple("host", host)).toEffect();
  }

  /** Delete a bundle info. */
  public static Effect<EntityManager> delete(final String host, final long bundleId) {
    return named.update("BundleInfo.delete", tuple("host", host), tuple("bundleId", bundleId)).toEffect();
  }
}
