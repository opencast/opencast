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
package org.opencastproject.smil.api;

import javax.xml.bind.JAXBException;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.api.SmilObject;

/**
 * {@link SmilService} response container for {@link Smil} and {@link SmilObject}s as entities.
 */
public interface SmilResponse {

	/**
	 * Returns {@link Smil} object from response.
	 * @return {@link Smil} object
	 */
	Smil getSmil();

	/**
	 * Returns number of entities defined with this response.
	 * @return number of entities
	 */
	int getEntitiesCount();

	/**
	 * Returns {@link SmilObject} if only one entity stored.
	 * Throws {@link SmilException} otherwise.
	 * @return {@link SmilObject}
	 * @throws SmilException if entities count not one
	 */
	SmilObject getEntity() throws SmilException;

	/**
	 * Returns {@link SmilObject}s if there are any or throws {@link SmilException}.
	 * @return {@link SmilObject}s as array
	 * @throws SmilException if there are no entities defined by response.
	 */
	SmilObject[] getEntities() throws SmilException;

	/**
	 * Return XML serialized instance of this {@link SmilResponse}.
	 * @return {@link SmilResponse} as XML
	 * @throws JAXBException if serialization failed
	 */
	String toXml() throws JAXBException;
}
