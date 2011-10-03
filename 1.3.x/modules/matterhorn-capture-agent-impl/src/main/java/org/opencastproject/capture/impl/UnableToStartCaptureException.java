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
package org.opencastproject.capture.impl;

/** This class is used to throw exceptions while trying to start a capture. **/
public class UnableToStartCaptureException extends RuntimeException {
  private static final long serialVersionUID = 3683577250029869724L;
  private String message; 
  public UnableToStartCaptureException(String message) {
    this.message = message;    
  }
  
  @Override
  public String getMessage() {
    return message;
  }
}