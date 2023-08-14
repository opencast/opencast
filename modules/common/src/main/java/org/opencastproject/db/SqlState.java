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

package org.opencastproject.db;

public final class SqlState {
  // SQL State Class 40 - Transaction rollback
  public static final String TRANSACTION_ROLLBACK_NO_SUBCLASS = "40000";
  public static final String TRANSACTION_ROLLBACK_SERIALIZATION_FAILURE = "40001";
  public static final String TRANSACTION_ROLLBACK_INTEGRITY_CONSTRAINT_VIOLATION = "40002";
  public static final String TRANSACTION_ROLLBACK_STATEMENT_COMPLETION_UNKNOWN = "40003";
  public static final String TRANSACTION_ROLLBACK_DEADLOCK_DETECTED = "40P01";

  private SqlState() {
  }
}
