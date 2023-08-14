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
package org.opencastproject.email.template.api;

import org.opencastproject.workflow.api.WorkflowInstance;

public interface EmailTemplateService {
  /**
   * Apply the template to the workflow instance.
   * 
   * @param templateName
   *          template name
   * @param templateContent
   *          template content
   * @param workflowInstance
   *          workflow
   * @return text with applied template
   */
  String applyTemplate(String templateName, String templateContent, WorkflowInstance workflowInstance);

  /**
   * Apply the template to the workflow instance.
   *
   * @param templateName
   *          template name
   * @param templateContent
   *          template content
   * @param workflowInstance
   *          workflow
   * @param multiValueDelimiter
   *          How to separate multi-value metadata fields
   * @return text with applied template
   */
  String applyTemplate(String templateName, String templateContent, WorkflowInstance workflowInstance,
      String multiValueDelimiter);

}
