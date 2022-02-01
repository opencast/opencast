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

package org.opencastproject.dictionary.none;

import static org.opencastproject.util.ReadinessIndicator.ARTIFACT;

import org.opencastproject.dictionary.api.DictionaryService;
import org.opencastproject.metadata.mpeg7.Textual;
import org.opencastproject.metadata.mpeg7.TextualImpl;
import org.opencastproject.util.ReadinessIndicator;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * This dictionary implementation is a dummy implementation which which will
 * just let the whole text pass through without any kind of filtering.
 */
@Component(
    immediate = true,
    service = DictionaryService.class,
    property = {
        "service.description=Dictionary Service"
    }
)
public class DictionaryServiceImpl implements DictionaryService {

  /**
   * OSGi callback on component activation.
   *
   * @param ctx
   *          the bundle context
   */
  @Activate
  void activate(BundleContext ctx) {
    Dictionary<String, String> properties = new Hashtable<String, String>();
    properties.put(ARTIFACT, "dictionary");
    ctx.registerService(ReadinessIndicator.class.getName(),
        new ReadinessIndicator(), properties);
  }

  /**
   * Filter the text according to the rules defined by the dictionary
   * implementation used. This implementation will just let the whole text pass
   * through.
   *
   * @return filtered text
   **/
  @Override
  public Textual cleanUpText(String text) {
    return new TextualImpl(text);
  }

}
