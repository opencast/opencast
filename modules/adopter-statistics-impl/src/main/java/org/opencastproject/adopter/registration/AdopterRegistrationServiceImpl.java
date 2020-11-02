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

import org.opencastproject.security.api.SecurityService;

/**
 * This service is used for registration and retrieving form data for
 * the logged in user in the context of adopter statistics.
 */
public class AdopterRegistrationServiceImpl implements Service {

  //================================================================================
  // OSGi properties
  //================================================================================

  /** Security service for getting user information. */
  private SecurityService securityService;

  /** The database repository for the registration forms. */
  private FormRepository formRepository;


  @Override
  public void saveFormData(IForm form) {
    formRepository.save(form);
  }


  //================================================================================
  // Methods
  //================================================================================

  @Override
  public Form retrieveFormData() {
    Form registrationForm = (Form) formRepository.getForm();
    if (registrationForm == null) {
      return new Form();
    }
    return registrationForm;
  }

  @Override
  public void deleteFormData() {
    formRepository.delete();
  }


  //================================================================================
  // OSGI setter
  //================================================================================

  /** OSGi setter for the security service. */
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /** OSGi setter for the form repository. */
  protected void setFormRepository(FormRepository formRepository) {
    this.formRepository = formRepository;
  }

}
