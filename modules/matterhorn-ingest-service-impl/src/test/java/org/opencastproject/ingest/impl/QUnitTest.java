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

package org.opencastproject.ingest.impl;

import static org.junit.Assert.assertTrue;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

@RunWith(Parameterized.class)
public class QUnitTest {
  private static final Logger logger = LoggerFactory.getLogger(QUnitTest.class);

  private File testFile;

  public QUnitTest(File testFile) {
    super();
    this.testFile = testFile;
  }

  @Parameters
  public static Collection<Object[]> testFiles() {
    ArrayList<Object[]> files = new ArrayList<Object[]>();
    File qunitDir = new File("src/test/qunit");
    for (File file : qunitDir.listFiles()) {
      if (file.getName().endsWith(".html")) {
        files.add(new Object[] { file });
      }
    }
    return files;
  }

  @Test
  public void runQUnitTests() throws Exception {
    WebClient client = new WebClient();
    client.setJavaScriptEnabled(true);

    HtmlPage page = client.getPage("file:///" + testFile.getAbsolutePath());

    // try 20 times to wait .5 second each for filling the page.
    HtmlElement element = null;
    for (int i = 0; i < 20; ++i) {
      try {
        element = page.getHtmlElementById("qunit-testresult");
        break;
      } catch (ElementNotFoundException e) {
        synchronized (page) {
          logger.info("Waiting for JavaScript tests...");
          page.wait(500);
        }
      }
    }
    logger.info(element.getTextContent());
    assertTrue(element.getTextContent().contains(", 0 failed."));
  }
}
