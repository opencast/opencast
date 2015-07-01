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

package org.opencastproject.archive.api;

/**
 * A HttpMediaPackageElementProvider is an instance that is able to deliver archived media package elements via HTTP and
 * therefore provides a means to rewrite element URIs to point to itself.
 * 
 * @see Archive#applyWorkflow(org.opencastproject.workflow.api.ConfiguredWorkflow, UriRewriter, Query)
 * @see Archive#applyWorkflow(org.opencastproject.workflow.api.ConfiguredWorkflow, UriRewriter, java.util.List)
 */
public interface HttpMediaPackageElementProvider {
  /**
   * Return a URI rewriter that rewrites media package element URIs so that they point to the
   * HttpMediaPackageElementProvider.
   */
  UriRewriter getUriRewriter();
}
