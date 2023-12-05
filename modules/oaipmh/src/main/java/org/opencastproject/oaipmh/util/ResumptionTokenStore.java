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
package org.opencastproject.oaipmh.util;

import static org.opencastproject.util.data.Option.option;

import org.opencastproject.oaipmh.server.ResumableQuery;
import org.opencastproject.util.data.Option;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.map.LRUMap;

import java.util.Collections;
import java.util.Map;

/**
 * Strategy to store resumption tokens. Uses a {@link org.apache.commons.collections4.map.LRUMap}.
 * <p>
 * Use in {@link org.opencastproject.oaipmh.server.OaiPmhRepository#saveQuery(org.opencastproject.oaipmh.server.ResumableQuery)}
 * and {@link org.opencastproject.oaipmh.server.OaiPmhRepository#getSavedQuery(String)}.
 */
public final class ResumptionTokenStore {
  private final Map<String, ResumableQuery> resumptionTokens;

  @SuppressWarnings("unchecked")
  private ResumptionTokenStore(int size) {
    resumptionTokens = Collections.synchronizedMap(new LRUMap(size));
  }

  public static ResumptionTokenStore create() {
    return new ResumptionTokenStore(100);
  }

  public static ResumptionTokenStore create(int size) {
    return new ResumptionTokenStore(size);
  }

  public String put(ResumableQuery query) {
    String token = DigestUtils.md5Hex(Double.toString(Math.random()));
    resumptionTokens.put(token, query);
    return token;
  }

  public Option<ResumableQuery> get(String resumptionToken) {
    return option(resumptionTokens.get(resumptionToken));
  }

}
