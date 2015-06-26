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


package org.opencastproject.oaipmh.server;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.List;

/**
 * XML generator for a regular (non-error) OAI response.
 */
public abstract class OaiVerbXmlGen extends OaiXmlGen {

  protected final String verb;

  public OaiVerbXmlGen(OaiPmhRepository repository, String verb) {
    super(repository);
    this.verb = verb;
  }

  /**
   * Create the request tag with verb attribute and base URL set.

   * @param attrs
   *        further attributes
   */
  Element request(Node... attrs) {
    return $e("request", merge(attrs, $a("verb", verb), $txt(repository.getBaseUrl())));
  }

  /**
   * Create the verb tag.
   */
  Element verb(Node... nodes) {
    return $e(verb, nodes);
  }

  /**
   * Create the verb tag.
   */
  Element verb(List<Node> nodes) {
    return $e(verb, nodes);
  }

}
