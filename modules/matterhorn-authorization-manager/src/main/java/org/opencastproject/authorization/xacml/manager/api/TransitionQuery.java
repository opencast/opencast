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
package org.opencastproject.authorization.xacml.manager.api;

import org.opencastproject.security.api.AclScope;
import org.opencastproject.util.data.Option;

import java.util.Date;

import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

/**
 * Represents a query to find transitions. Please note that predicates are joined to an "and" expression.
 */
public final class TransitionQuery {
  private Option<AclScope> scope = none();
  private Option<String> id = none();
  private Option<Date> after = none();
  private Option<Date> before = none();
  private Option<Long> transitionId = none();
  private Option<Boolean> done = none();
  private Option<Long> aclId = none();

  // TODO
  private String action;
  private String roleSet;

  private TransitionQuery() {
  }

  public static TransitionQuery query() {
    return new TransitionQuery();
  }

  public TransitionQuery withScope(AclScope scope) {
    this.scope = some(scope);
    return this;
  }

  public TransitionQuery withId(String id) {
    this.id = some(id);
    return this;
  }

  /** Find transitions that are applied in the future of <code>date</code>. */
  public TransitionQuery after(Date date) {
    this.after = some(date);
    return this;
  }

  /** Find transitions that have been applied before <code>date</code>. */
  public TransitionQuery before(Date date) {
    this.before = some(date);
    return this;
  }

  public TransitionQuery withTransitionId(long transitionId) {
    this.transitionId = some(transitionId);
    return this;
  }
  
  public TransitionQuery withAclId(Long aclId) {
    this.aclId = some(aclId);
    return this;
  }

  /**
   * Find transitions that are done or not.
   * 
   * @param done
   *          true to find done transitions
   */
  public TransitionQuery withDone(boolean done) {
    this.done = some(done);
    return this;
  }

  public Option<AclScope> getScope() {
    return scope;
  }

  public Option<String> getId() {
    return id;
  }
  
  public Option<Long> getAclId() {
    return aclId;
  }

  public Option<Date> getAfter() {
    return after;
  }

  public Option<Date> getBefore() {
    return before;
  }

  public Option<Long> getTransitionId() {
    return transitionId;
  }

  public Option<Boolean> getDone() {
    return done;
  }
}
