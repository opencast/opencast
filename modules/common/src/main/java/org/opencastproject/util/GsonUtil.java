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

package org.opencastproject.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.ToNumberPolicy;

/**
 * Provides the {@link Gson} instance Opencast uses for reading and writing JSON.
 * <p>
 * Gson's defaults are unsuitable for Opencast in four ways, which is why this shared instance exists instead of
 * every call site building its own:
 * <ul>
 * <li>By default Gson escapes <code>&lt;</code>, <code>&gt;</code>, <code>&amp;</code>, <code>=</code> and
 * <code>'</code> as <code>\\uXXXX</code>. Opencast serializes URLs almost everywhere, so this would litter the
 * output with escapes.</li>
 * <li>By default Gson omits object members whose value is <code>null</code>. The External API explicitly requires
 * empty fields to be present, so they have to be serialized.</li>
 * <li>By default Gson reads every JSON number into a <code>Double</code> when the target type is unknown, which
 * silently loses precision for large integers. <code>LONG_OR_DOUBLE</code> keeps integral values as
 * <code>Long</code>.</li>
 * <li>By default Gson parses leniently and accepts input the JSON specification does not allow, such as unquoted
 * keys. Requests carrying such input used to be rejected, so parsing is set to strict.</li>
 * </ul>
 * <p>
 * {@link Gson} is thread safe, so the single instance can be shared freely.
 */
public final class GsonUtil {

  private static final Gson GSON = new GsonBuilder()
      .disableHtmlEscaping()
      .serializeNulls()
      .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
      .setStrictness(Strictness.STRICT)
      .create();

  private GsonUtil() {
  }

  /**
   * Returns the shared, pre-configured {@link Gson} instance.
   *
   * @return the Gson instance to use for all JSON reading and writing
   */
  public static Gson gson() {
    return GSON;
  }
}
