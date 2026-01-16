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

package org.opencastproject.tobira.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Tobira API Configuration")
public @interface TobiraConfig {


  @AttributeDefinition(
      name = "Tobira auth header name",
      description = "HTTP Header name that will contain the username"
  )
  String headerName() default "X-WEBAUTH-USER";

  @AttributeDefinition(
      name = "Tobira Callback Token",
      description = "The token to authorize the callback request.",
      type = AttributeType.STRING
  )
  String callbackToken();

  @AttributeDefinition(
      name = "Allowed Roles Pattern",
      description = "The pattern to match the roles that are allowed for the user outcome.",
      type = AttributeType.STRING
  )
  String allowedRolesPattern() default "ROLE_TOBIRA_.*";
}
