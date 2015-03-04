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

import org.junit.Ignore;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Ignore
public final class TestClassPathInspector extends ClassPathInspector {

    @Override
    public List<String> listFiles(String targetFolder) throws IllegalPathException {
        URL dirURL = getClass().getClassLoader().getResource(targetFolder);
        if (dirURL != null && "file".equals(dirURL.getProtocol())) {
            try {
                return Arrays.asList(new File(dirURL.toURI()).list());
            } catch (URISyntaxException e) {
                throw new IllegalPathException("", e);
            }
        }
        throw new IllegalPathException("");
    }
}
