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

import org.opencastproject.adminui.exception.IllegalPathException;
import org.opencastproject.adminui.impl.LanguageServiceImpl;

import org.osgi.framework.Bundle;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Uses the bundle for finding files in the bundle's classpath.
 * 
 * @author ademasi
 * 
 */
public class ClassPathInspector implements PathInspector {
    private Bundle bundle;

    public ClassPathInspector() {
    };

    public ClassPathInspector(Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public List<String> listFiles(String path) throws IllegalPathException {
        Enumeration<?> entries = bundle.findEntries(path, "*", false);
        return toList(entries);
    }

    private List<String> toList(Enumeration<?> entries) {
        List<String> result = new ArrayList<String>();
        while (entries.hasMoreElements()) {
            URL file = (URL) entries.nextElement();
            result.add(file.getPath().replaceAll(LanguageServiceImpl.TRANSLATION_FILES_PATH, "").replaceAll("/", ""));
        }
        return result;
    }
}
