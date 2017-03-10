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
package org.opencastproject.assetmanager.impl.query;

import com.entwinemedia.fn.Fn;
import com.mysema.query.types.EntityPath;
import com.mysema.query.types.expr.BooleanExpression;

final class Join {
  // CHECKSTYLE:OFF
  final EntityPath<?> from;
  final EntityPath<?> join;
  final BooleanExpression on;
  // CHECKSTYLE:ON

  Join(EntityPath<?> from, EntityPath<?> join, BooleanExpression on) {
    this.from = from;
    this.join = join;
    this.on = on;
  }

  static final Fn<Join, EntityPath<?>> getFrom = new Fn<Join, EntityPath<?>>() {
    @Override public EntityPath<?> apply(Join a) {
      return a.from;
    }
  };

  static final Fn<Join, EntityPath<?>> getJoin = new Fn<Join, EntityPath<?>>() {
    @Override public EntityPath<?> apply(Join a) {
      return a.join;
    }
  };

  static final Fn<Join, BooleanExpression> getOn = new Fn<Join, BooleanExpression>() {
    @Override public BooleanExpression apply(Join a) {
      return a.on;
    }
  };
}
