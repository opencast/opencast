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

package org.opencastproject.graphql.execution.context;

import org.osgi.framework.BundleContext;

import graphql.schema.DataFetchingEnvironment;

public final class OpencastContextManager {

  public static final String CONTEXT = "context";

  private static final InheritableThreadLocal<OpencastContext> contextHolder = new InheritableThreadLocal<>();

  private OpencastContextManager() {
  }

  public static OpencastContext initiateContext(BundleContext bundleContext) {

    var context = getCurrentContext();
    if (context == null) {
      context = OpencastContext.newContext(bundleContext);
    }

    contextHolder.set(context);
    return context;
  }

  public static OpencastContext restoreContext(final DataFetchingEnvironment environment) {
    OpencastContext context = environment.getGraphQlContext().get(CONTEXT);
    contextHolder.set(context);
    return context;
  }

  public static OpencastContext getCurrentContext() {
    return contextHolder.get();
  }

  public static OpencastContext enrichContext(final DataFetchingEnvironment environment) {
    OpencastContext context = getCurrentContext();
    if (context == null) {
      context = restoreContext(environment);
    }

    environment.getGraphQlContext().put(CONTEXT, context);
    contextHolder.set(context);

    return context;
  }

  public static void clearContext() {
    contextHolder.remove();
  }

}
