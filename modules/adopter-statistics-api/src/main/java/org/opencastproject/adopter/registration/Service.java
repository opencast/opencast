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

package org.opencastproject.adopter.registration;

/**
 * API for the Adopter Statistics Registrations.
 */
public interface Service {

  /**
   * Saves the submitted registration form.
   * @param form The adopter registration form.
   */
  void saveFormData(IForm form);

  /**
   * Loads the adopter registration form.
   * @return The adopter registration form.
   */
  IForm retrieveFormData();

  /** Deletes the registration form of the adopter. */
  void deleteFormData();

}
