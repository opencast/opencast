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

package org.opencastproject.adminui.util;

import java.util.Arrays;
import java.util.List;

/**
 * This Mock doesn't peek into the filesystem, it merely returns {@link #TESTFILES} and ignores the path argument in
 * listFiles.
 * 
 * @author ademasi
 * 
 */
public class MockPathInspector implements PathInspector {

  public static final List<String> TESTFILES = Arrays.asList("lang-de_DE.json", "lang-en_US.json");
  private List<String> serverAvailableLanguages;

  @Override
  public List<String> listFiles(String path) {
    if (serverAvailableLanguages != null) {
      return serverAvailableLanguages;
    } else {
      return TESTFILES;
    }
  }

  public void setServerAvailableLanguages(List<String> serverAvailableLanguages) {
    this.serverAvailableLanguages = serverAvailableLanguages;
  }

}
