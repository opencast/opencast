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
package org.opencastproject.ingest.api;

/**
 * Exception throws due to a problem ingesting media or metadata files.
 */
public class IngestException extends Exception {

  /** Serial version UID */
  private static final long serialVersionUID = -321218799805646569L;

  /**
   * Constructs an ingest exception
   * 
   * @param message
   *          the failure message
   */
  public IngestException(String message) {
    super(message);
  }

  /**
   * Constructs an ingest exception
   * 
   * @param cause
   *          the original cause
   */
  public IngestException(Throwable cause) {
    super(cause);
  }

  /**
   * Constructs an ingest exception
   * 
   * @param message
   *          the failure message
   * @param cause
   *          the original cause
   */
  public IngestException(String message, Throwable t) {
    super(message, t);
  }

}
