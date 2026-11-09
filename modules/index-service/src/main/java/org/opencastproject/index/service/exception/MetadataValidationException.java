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

package org.opencastproject.index.service.exception;

/**
 * An exception indicating that a metadata field did not pass validation while storing a catalog.
 * <p>
 * In contrast to a plain {@link IllegalArgumentException}, this exception names the field which failed validation. That
 * allows callers to distinguish a value sent by a client from a field which was already invalid in the stored catalog,
 * and to pick an appropriate response for either case.
 * <p>
 * This extends {@link IllegalArgumentException} so that existing handlers keep working unchanged.
 */
public class MetadataValidationException extends IllegalArgumentException {

  private static final long serialVersionUID = 6320734174319294928L;

  private final String fieldId;

  /**
   * Create a new exception for the field which failed validation.
   *
   * @param fieldId
   *          The input identifier of the metadata field which failed validation.
   * @param message
   *          The message describing the validation failure.
   */
  public MetadataValidationException(String fieldId, String message) {
    super(message);
    this.fieldId = fieldId;
  }

  /**
   * Returns the input identifier of the metadata field which failed validation.
   *
   * @return The field identifier.
   */
  public String getFieldId() {
    return fieldId;
  }

}
