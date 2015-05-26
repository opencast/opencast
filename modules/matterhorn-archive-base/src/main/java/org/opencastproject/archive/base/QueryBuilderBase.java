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

package org.opencastproject.archive.base;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.archive.api.Query;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.data.Option;

import java.util.Date;

/**
 * Mutable query builder.
 */
public abstract class QueryBuilderBase<A extends QueryBuilderBase<A>> implements Query {
  // definition of all fields and setters

  // default values
  private Option<String> mediaPackageId = none();
  private Option<String> seriesId = none();
  private Option<String> organizationId = none();
  private boolean onlyLastVersion = false;
  private Option<Integer> limit = none();
  private Option<Integer> offset = none();
  private Option<Date> archivedAfter = none();
  private Option<Date> archivedBefore = none();
  private boolean includeDeleted = false;
  private Option<Date> deletedAfter = none();
  private Option<Date> deletedBefore = none();

  protected QueryBuilderBase() {
  }

  @Override
  public Option<String> getMediaPackageId() {
    return mediaPackageId;
  }

  public A mediaPackageId(String a) {
    return mediaPackageId(some(a));
  }

  public A mediaPackageId(Option<String> a) {
    this.mediaPackageId = a;
    // this cast is safe since QueryBuilderBase can only be extended using a type parameter
    // of the same type or a subtype of the extending class. All other types
    // will lead to a type error when implementing copy().
    return self();
  }

  @Override
  public Option<String> getSeriesId() {
    return seriesId;
  }

  public A seriesId(String a) {
    return seriesId(some(a));
  }

  public A seriesId(Option<String> a) {
    this.seriesId = a;
    return self();
  }

  @Override
  public Option<String> getOrganizationId() {
    return organizationId;
  }

  public A organizationId(String a) {
    return organizationId(some(a));
  }

  public A organizationId(Option<String> a) {
    this.organizationId = a;
    return self();
  }

  public A currentOrganization(SecurityService a) {
    return organizationId(a.getOrganization().getId());
  }

  @Override
  public boolean isOnlyLastVersion() {
    return onlyLastVersion;
  }

  public A onlyLastVersion(boolean a) {
    this.onlyLastVersion = a;
    return self();
  }

  @Override
  public Option<Integer> getLimit() {
    return limit;
  }

  public A limit(int a) {
    return limit(some(a));
  }

  public A limit(Option<Integer> a) {
    this.limit = a;
    return self();
  }

  @Override
  public Option<Integer> getOffset() {
    return offset;
  }

  public A offset(int a) {
    return offset(some(a));
  }

  public A offset(Option<Integer> a) {
    this.offset = a;
    return self();
  }

  @Override
  public Option<Date> getArchivedAfter() {
    return archivedAfter;
  }

  public A archivedAfter(Date a) {
    return archivedAfter(some(a));
  }

  public A archivedAfter(Option<Date> a) {
    this.archivedAfter = a;
    return self();
  }

  @Override
  public Option<Date> getArchivedBefore() {
    return archivedBefore;
  }

  public A archivedBefore(Date a) {
    return archivedBefore(some(a));
  }

  public A archivedBefore(Option<Date> a) {
    this.archivedBefore = a;
    return self();
  }

  @Override
  public boolean isIncludeDeleted() {
    return includeDeleted;
  }

  public A includeDeleted(boolean a) {
    this.includeDeleted = a;
    return self();
  }

  @Override
  public Option<Date> getDeletedAfter() {
    return deletedAfter;
  }

  public A deletedAfter(Date a) {
    return deletedAfter(some(a));
  }

  public A deletedAfter(Option<Date> a) {
    this.deletedAfter = a;
    return self();
  }

  @Override
  public Option<Date> getDeletedBefore() {
    return deletedBefore;
  }

  public A deletedBefore(Date a) {
    return deletedBefore(some(a));
  }

  public A deletedBefore(Option<Date> a) {
    this.deletedBefore = a;
    return self();
  }

  protected A self() {
    // this cast is safe since QueryBuilderBase can only be extended using a type parameter
    // of the same type or a subtype of the extending class. All other types
    // will lead to a type error when implementing copy().
    return (A) this;
  }
}
