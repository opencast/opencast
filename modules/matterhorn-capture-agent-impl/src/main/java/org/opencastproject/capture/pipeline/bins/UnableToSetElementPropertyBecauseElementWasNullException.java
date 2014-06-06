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
package org.opencastproject.capture.pipeline.bins;

import org.gstreamer.Element;

/**
 * When the properties of a gstreamer element fail to be set this exception is thrown.
 */
public class UnableToSetElementPropertyBecauseElementWasNullException extends Exception {
  private String message;

  private static final long serialVersionUID = -2613833830552791683L;

  public UnableToSetElementPropertyBecauseElementWasNullException(Element element, String property) {
    message = "Unable to set property " + property + " on element " + element.getName();
  }

  @Override
  public String getMessage() {
    return message;
  }
}
