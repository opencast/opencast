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
package org.opencastproject.rest;

/**
 * Constant definition for the shared <code>HTTP</code> context.
 */
public interface SharedHttpContext {

  /**
   * The shared context key as used throughout OSGi.
   */
  String CONTEXT_ID = "httpContext.id";

  /**
   * The context key for marking shared contexts.
   */
  String SHARED = "httpContext.shared";

  /**
   * The key for the servlet alias.
   */
  String ALIAS = "alias";

  /**
   * Key for the servlet name.
   */
  String SERVLET_NAME = "servlet-name";

  /**
   * The key for defining a pattern for request filters.
   */
  String PATTERN = "urlPatterns";

  /**
   * Prefix for servlet init keys.
   */
  String INIT_PREFIX = "init.";

  /**
   * Property to define the ranking of a service in the filter chain
   */
  String SERVICE_RANKING = "service.ranking";

}
