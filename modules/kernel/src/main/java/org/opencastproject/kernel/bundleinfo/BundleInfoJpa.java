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

package org.opencastproject.kernel.bundleinfo;

import static org.opencastproject.db.Queries.namedQuery;
import static org.opencastproject.kernel.bundleinfo.BundleInfoImpl.bundleInfo;
import static org.opencastproject.util.data.Option.option;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

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
@Table(name = "oc_bundleinfo", uniqueConstraints = { @UniqueConstraint(columnNames = { "host", "bundle_name",
        "bundle_version" }) })
@NamedQueries({
        @NamedQuery(name = "BundleInfo.findAll", query = "select a from BundleInfo a order by a.host, a.bundleSymbolicName"),
        @NamedQuery(name = "BundleInfo.deleteAll", query = "delete from BundleInfo"),
        @NamedQuery(name = "BundleInfo.deleteByHost", query = "delete from BundleInfo where host = :host"),
        @NamedQuery(name = "BundleInfo.delete", query = "delete from BundleInfo where host = :host and bundleId = :bundleId") })
public class BundleInfoJpa {
  @Id
  @Column(name = "id")
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

  /** Find all in database. */
  public static final Function<EntityManager, List<BundleInfoJpa>> findAll = namedQuery
      .findAll("BundleInfo.findAll", BundleInfoJpa.class);

  /** Find all bundles whose symbolic names start with one of the given prefixes. */
  public static Function<EntityManager, List<BundleInfoJpa>> findAll(final String... prefixes) {
    return em -> {
      final CriteriaBuilder cb = em.getCriteriaBuilder();
      final CriteriaQuery<BundleInfoJpa> q = cb.createQuery(BundleInfoJpa.class);
      final Root<BundleInfoJpa> r = q.from(BundleInfoJpa.class);
      q.select(r);
      final Expression<String> symbolicNamePath = r.get("bundleSymbolicName");
      final Predicate[] likes = Stream.of(prefixes)
          .map(prefix -> cb.like(symbolicNamePath, prefix + "%"))
          .toArray(Predicate[]::new);
      q.where(cb.or(likes));
      q.orderBy(cb.asc(r.get("host")), cb.asc(symbolicNamePath));
      return em.createQuery(q).getResultList();
    };
  }

  /** Delete all entities of BundleInfo from the database. */
  public static final Function<EntityManager, Integer> deleteAll = namedQuery.update("BundleInfo.deleteAll");

  /** Delete all entities of BundleInfo of a certain host from the database. */
  public static Function<EntityManager, Integer> deleteByHost(String host) {
    return namedQuery.update("BundleInfo.deleteByHost", Pair.of("host", host));
  }

  /** Delete a bundle info. */
  public static Function<EntityManager, Integer> delete(final String host, final long bundleId) {
    return namedQuery.update("BundleInfo.delete", Pair.of("host", host), Pair.of("bundleId", bundleId));
  }
}
