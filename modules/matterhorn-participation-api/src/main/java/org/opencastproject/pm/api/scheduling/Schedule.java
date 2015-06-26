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

package org.opencastproject.pm.api.scheduling;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.util.data.Tuple;

import java.util.List;

/** Describes a complete schedule. */
public interface Schedule {

  List<Tuple<Recording, DublinCoreCatalog>> getEpisodes();
}
