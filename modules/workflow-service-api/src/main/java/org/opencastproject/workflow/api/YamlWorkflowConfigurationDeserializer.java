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

package org.opencastproject.workflow.api;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;

public class YamlWorkflowConfigurationDeserializer extends StdDeserializer<JaxbWorkflowConfiguration> {

  public YamlWorkflowConfigurationDeserializer() {
    this(null);
  }

  protected YamlWorkflowConfigurationDeserializer(Class<?> vc) {
    super(vc);
  }

  @Override
  public JaxbWorkflowConfiguration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
          throws IOException, JacksonException {
    ObjectNode jn = jsonParser.getCodec().readTree(jsonParser);
    var fields = jn.fields();

    if (fields.hasNext()) {
      var entry = fields.next();
      // only parse first key value pair
      return new JaxbWorkflowConfiguration(entry.getKey(), entry.getValue().asText());
    } else {
      return null;
    }
  }

}
