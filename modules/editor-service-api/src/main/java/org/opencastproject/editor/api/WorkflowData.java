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
package org.opencastproject.editor.api;

import static com.entwinemedia.fn.data.json.Jsons.f;
import static com.entwinemedia.fn.data.json.Jsons.obj;
import static com.entwinemedia.fn.data.json.Jsons.v;

import com.entwinemedia.fn.data.json.JObject;
import com.entwinemedia.fn.data.json.Jsons;

import org.json.simple.JSONObject;

public final class WorkflowData {
  public static final String ID = "id";
  public static final String NAME = "name";
  public static final String DISPLAY_ORDER = "displayOrder";
  public static final String DESCRIPTION = "description";
  private final String id;
  private final String name;
  private final Integer displayOrder;
  private final String description;

  public WorkflowData(String id) {
    this(id, null, null, null);
  }

  public WorkflowData(String id, String name, Integer displayOrder, String description) {
    this.id = id;
    this.name = name;
    this.displayOrder = displayOrder;
    this.description = description;
  }

  protected static WorkflowData parse(JSONObject object) {
    if (object == null) {
      return null;
    }
    Integer displayOrder;
    Object jsonDisplayOrder = object.get(DISPLAY_ORDER);
    if (jsonDisplayOrder == null) {
      displayOrder = null;
    } else if (jsonDisplayOrder instanceof Long) {
      displayOrder = ((Long)jsonDisplayOrder).intValue();
    } else if (jsonDisplayOrder instanceof String) {
      displayOrder = Integer.decode((String) jsonDisplayOrder);
    } else if (jsonDisplayOrder instanceof Integer) {
      displayOrder = (Integer) jsonDisplayOrder;
    } else {
      throw new IllegalArgumentException("Unable to decode" + DISPLAY_ORDER);
    }

    return new WorkflowData((String) object.get(ID), (String) object.get(NAME),
            displayOrder, (String) object.get(DESCRIPTION));
  }

  protected JObject toJson() {
    return obj(f(ID, v(id)),
            f(NAME, v(name, Jsons.NULL)),
            f(DISPLAY_ORDER, v(displayOrder, Jsons.NULL)),
            f(DESCRIPTION, v(description, Jsons.NULL)));
  }

  public String getId() {
    return this.id;
  }
}
