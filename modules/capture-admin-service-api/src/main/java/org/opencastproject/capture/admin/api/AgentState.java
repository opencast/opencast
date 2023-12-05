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

package org.opencastproject.capture.admin.api;

import java.util.Arrays;
import java.util.List;

/**
 * A representation of the capture client's current state (MH-730). This is not an enum because we wish to preserve
 * inter-version compatibility (eg, a version 2 agent talking to a version 1 core)
 */
public interface AgentState {

  /** Constant <code>IDLE="idle"</code> */
  String IDLE = "idle";

  /** Constant <code>SHUTTING_DOWN="shutting_down"</code> */
  String SHUTTING_DOWN = "shutting_down";

  /** Constant <code>CAPTURING="capturing"</code> */
  String CAPTURING = "capturing";

  /** Constant <code>UPLOADING="uploading"</code> */
  String UPLOADING = "uploading";

  /** Constant <code>UNKNOWN="unknown"</code> */
  String UNKNOWN = "unknown";

  /** Constant <code>OFFLINE="offline"</code> */
  String OFFLINE = "offline";

  /** Constant <code>ERROR="error"</code> */
  String ERROR = "error";

  /** The collection of all known states. TODO: Remove this when the states are replaced with enums */
  List<String> KNOWN_STATES = Arrays.asList(IDLE, SHUTTING_DOWN, CAPTURING, UPLOADING, ERROR, UNKNOWN, OFFLINE);

  String TRANSLATION_PREFIX = "AGENTS.STATUS.";

}
