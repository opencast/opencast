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
package org.opencastproject.archive.opencast;

import org.opencastproject.archive.api.Query;
import org.opencastproject.archive.base.QueryBuilder;
import org.opencastproject.archive.base.QueryBuilderBase;
import org.opencastproject.util.data.Option;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/** Query builder for Opencast. */
public class OpencastQueryBuilder extends QueryBuilderBase<OpencastQueryBuilder> implements OpencastQuery {
  private Option<String> dcTitle = none();
  private Option<String> dcCreator = none();
  private Option<String> dcContributor = none();
  private Option<String> dcLanguage = none();
  private Option<String> dcLicense = none();
  private Option<String> seriesTitle = none();
  private Option<String> fullText = none();
  private Option<Order> order = none();
  private boolean orderAscending = false;

  protected OpencastQueryBuilder() {
  }

  /** Create a new query builder. */
  public static OpencastQueryBuilder query() {
    return new OpencastQueryBuilder();
  }

  /** Create a new query builder from a plain query object. */
  public static OpencastQueryBuilder query(Query q) {
    return QueryBuilder.query(q, new OpencastQueryBuilder());
  }

  @Override public Option<String> getDcTitle() {
    return dcTitle;
  }

  public OpencastQueryBuilder dcTitle(String a) {
    return dcTitle(some(a));
  }

  public OpencastQueryBuilder dcTitle(Option<String> a) {
    this.dcTitle = a;
    return self();
  }

  @Override public Option<String> getDcCreator() {
    return dcCreator;
  }

  public OpencastQueryBuilder dcCreator(String a) {
    return dcCreator(some(a));
  }

  public OpencastQueryBuilder dcCreator(Option<String> a) {
    this.dcCreator = a;
    return self();
  }

  @Override public Option<String> getDcContributor() {
    return dcContributor;
  }

  public OpencastQueryBuilder dcContributor(String a) {
    return dcContributor(some(a));
  }

  public OpencastQueryBuilder dcContributor(Option<String> a) {
    this.dcContributor = a;
    return self();
  }

  @Override public Option<String> getDcLanguage() {
    return dcLanguage;
  }

  public OpencastQueryBuilder dcLanguage(String a) {
    return dcLanguage(some(a));
  }

  public OpencastQueryBuilder dcLanguage(Option<String> a) {
    this.dcLanguage = a;
    return self();
  }

  @Override public Option<String> getDcLicense() {
    return dcLicense;
  }

  public OpencastQueryBuilder dcLicense(String a) {
    return dcLicense(some(a));
  }

  public OpencastQueryBuilder dcLicense(Option<String> a) {
    this.dcLicense = a;
    return self();
  }

  @Override public Option<String> getSeriesTitle() {
    return seriesTitle;
  }

  public OpencastQueryBuilder seriesTitle(String a) {
    return seriesTitle(some(a));
  }

  public OpencastQueryBuilder seriesTitle(Option<String> a) {
    this.seriesTitle = a;
    return self();
  }

  @Override public Option<String> getFullText() {
    return fullText;
  }

  public OpencastQueryBuilder fullText(String a) {
    return fullText(some(a));
  }

  public OpencastQueryBuilder fullText(Option<String> a) {
    this.fullText = a;
    return self();
  }

  @Override public Option<Order> getOrder() {
    return order;
  }

  public OpencastQueryBuilder order(Order a) {
    return order(some(a));
  }

  public OpencastQueryBuilder order(Option<Order> a) {
    this.order = a;
    return self();
  }

  @Override public boolean isOrderAscending() {
    return orderAscending;
  }

  public OpencastQueryBuilder orderAscending(boolean a) {
    this.orderAscending = a;
    return self();
  }
}
