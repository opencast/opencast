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
package org.opencastproject.serviceregistry.api;

/** Locale dependent information for a {@link org.opencastproject.job.api.Incident}. */
public interface IncidentL10n {
  /** Return the effective locale of the localized texts */
//  Locale getLocale();

  /** Get the fully processed, localized title. */
  String getTitle();

  /** Get the fully processed, localized description. */
  String getDescription();
}
