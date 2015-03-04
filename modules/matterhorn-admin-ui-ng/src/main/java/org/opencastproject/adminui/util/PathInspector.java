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
package org.opencastproject.adminui.util;

import org.opencastproject.adminui.exception.IllegalPathException;

import java.util.List;

/**
 * A simple folder inspection utility.
 * 
 * @author ademasi
 * 
 */
public interface PathInspector {

  /**
   * Lists all files in a directory (path), if it exists and is readable.
   * 
   * @param languageFileFolder
   * @return A list of all filenames in a directory.
   * @throws IllegalPathException
   *           If the path does not exist or is not readable.
   */
  List<String> listFiles(String languageFileFolder) throws IllegalPathException;

}
