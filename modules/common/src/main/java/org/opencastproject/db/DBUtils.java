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

import org.eclipse.persistence.exceptions.DatabaseException;

import java.sql.SQLException;

import javax.persistence.RollbackException;

public final class DBUtils {
  private DBUtils() {
  }

  public static boolean isTransactionException(Throwable t) {
    if (t instanceof RollbackException) {
      return true;
    }

    if (t instanceof DatabaseException) {
      var dbEx = (DatabaseException) t;
      if (dbEx.getInternalException() instanceof SQLException) {
        var sqlEx = (SQLException) dbEx.getInternalException();
        switch (sqlEx.getSQLState()) {
          case SqlState.TRANSACTION_ROLLBACK_NO_SUBCLASS:
          case SqlState.TRANSACTION_ROLLBACK_SERIALIZATION_FAILURE:
          case SqlState.TRANSACTION_ROLLBACK_INTEGRITY_CONSTRAINT_VIOLATION:
          case SqlState.TRANSACTION_ROLLBACK_STATEMENT_COMPLETION_UNKNOWN:
          case SqlState.TRANSACTION_ROLLBACK_DEADLOCK_DETECTED:
            return true;
          default:
            return false;
        }
      }
    }

    return false;
  }
}
