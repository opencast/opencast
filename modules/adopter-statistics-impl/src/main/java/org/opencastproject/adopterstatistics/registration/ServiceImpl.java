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

package org.opencastproject.adopterstatistics.registration;

import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;

/**
 * This service is used for registration and retrieving form data for
 * the logged in user in the context of adopter statistics.
 */
public class ServiceImpl implements Service {

  /**
   * Security service for getting user information.
   */
  private SecurityService securityService;

  /**
   * The database repository for the registration forms.
   */
  private FormRepository formRepository;


  /**
   * @param securityService Instance of this class.
   */
  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * @param formRepository Instance of this class.
   */
  protected void setFormRepository(FormRepository formRepository) {
    this.formRepository = formRepository;
  }

  @Override
  public void saveFormData(IForm form) throws Exception {
    User user = securityService.getUser();
    if (user == null)
      throw new NotFoundException("User from Security Service was null.");

    ((Form) form).setUsername(user.getUsername());
    formRepository.save(form);
  }

  @Override
  public Form retrieveFormData() throws Exception {

    User user = securityService.getUser();
    if (user == null)
      throw new NotFoundException("User from Security Service was null.");

    Form registrationForm = (Form) formRepository.findByUsername(user.getUsername());

    if (registrationForm == null) {
      return new Form();
    }

    return registrationForm;
  }

}
