/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.smil.entity.media.param.api;

import org.opencastproject.smil.entity.api.SmilObject;

/**
 * Represent SMIL param element (inside media elements).
 */
public interface SmilMediaParam extends SmilObject {

	String PARAM_NAME_TRACK_ID = "track-id";
	String PARAM_NAME_TRACK_FLAVOR = "track-flavor";
	String PARAM_NAME_TRACK_SRC = "track-src";
	String PARAM_NAME_TRACK = "tarck";

	/**
	 * Returns param name.
	 * @return the name
	 */
	String getName();

	/**
	 * Returns param value.
	 * @return the value
	 */
	String getValue();
	
}
