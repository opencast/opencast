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
package org.opencastproject.coverimage.impl.xsl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for class {@link XsltHelper}
 */
public class XsltHelperTest {

  private static final String SHORT_TEXT = "A short title";
  private static final String TEXT1 = "Multiscale Climatic, Topographic, and Biotic Controls of Tree Invasion in a "
          + "Sub-Alpine Parkland Landscape, Jefferson Park, Oregon Cascades, USA";
  private static final String TEXT2 = "The Ancient Ancestors of these Indians here: Colonial European Perceptions of "
          + "the Greatness of Classical Mesoamerican Cultures and the Apparent Degeneration of Conquest Era and "
          + "Colonial Indigenous Cultures in Mesoamerica, 1521-1821";
  private static final String TEXT3 = "Dies ist ein \"Klassiker\" zur Gesprächsführung: Eine der ersten "
          + "kommerziell erstellten Therapieaufnahmen, bei der die echte Patientin \"Gloria\" mit Rogers, Perls "
          + "(Gestalttherapie) und Ellis (Rational-emotive Therapie) spricht. Ich stelle Ihnen hier nur den "
          + "Ausschnitt mit Rogers (Copyright!) zur Verfügung.";
  private static final String TEXT4 = "Angehrn Tobias, Biedermann Anja, Böni Nicole, Ehrensperger Markus, Merk "
          + "Cornelia, Rohner Aline, Wattinger Stéphanie, Häni Yannick, Hilfiker Florian, Kratochwill Andreas, "
          + "Schlösser Nathalie, Schwarzer Andreas";

  @Test
  public void testSplit() {

    assertEquals(null, XsltHelper.split(null, 50, 1, false));
    assertEquals(TEXT1, XsltHelper.split(TEXT1, 0, 1, false));
    assertEquals(null, XsltHelper.split(TEXT1, 50, 0, false));

    assertEquals(SHORT_TEXT, XsltHelper.split(SHORT_TEXT, 50, 1, false));
    assertEquals(null, XsltHelper.split(SHORT_TEXT, 50, 2, false));

    assertEquals("Multiscale Climatic, Topographic, and Biotic", XsltHelper.split(TEXT1, 50, 1, false));
    assertEquals("Controls of Tree Invasion in a Sub-Alpine Parkland", XsltHelper.split(TEXT1, 50, 2, false));
    assertEquals("Landscape, Jefferson Park, Oregon Cascades, USA", XsltHelper.split(TEXT1, 50, 3, false));

    assertEquals("The Ancient Ancestors of these Indians here:", XsltHelper.split(TEXT2, 44, 1, false));
    assertEquals("The Ancient Ancestors of these Indians", XsltHelper.split(TEXT2, 43, 1, false));

    assertEquals("Dies ist ein", XsltHelper.split(TEXT3, 15, 1, false));
    assertEquals("\"Klassiker\" zur", XsltHelper.split(TEXT3, 15, 2, false));
    assertEquals("Dies ist ein", XsltHelper.split(TEXT3, 23, 1, false));
    assertEquals("\"Klassiker\" zur", XsltHelper.split(TEXT3, 23, 2, false));
    assertEquals("Dies ist ein \"Klassiker\"", XsltHelper.split(TEXT3, 24, 1, false));
    assertEquals("zur Gesprächsführung:", XsltHelper.split(TEXT3, 24, 2, false));

    assertEquals("Angehrn Tobias, Biedermann", XsltHelper.split(TEXT4, 31, 1, false));
    assertEquals("Angehrn Tobias, Biedermann Anja,", XsltHelper.split(TEXT4, 32, 1, false));
  }

  @Test
  public void testSplitAbbreviate() {

    assertEquals(SHORT_TEXT, XsltHelper.split(SHORT_TEXT, 13, 1, true));
    assertEquals("A short...", XsltHelper.split(SHORT_TEXT, 12, 1, true));
    assertEquals("A short...", XsltHelper.split(SHORT_TEXT, 11, 1, true));
    assertEquals("A short...", XsltHelper.split(SHORT_TEXT, 10, 1, true));
    assertEquals("A shor...", XsltHelper.split(SHORT_TEXT, 9, 1, true));
    assertEquals("A sho...", XsltHelper.split(SHORT_TEXT, 8, 1, true));
    assertEquals("A sh...", XsltHelper.split(SHORT_TEXT, 7, 1, true));
    assertEquals("A...", XsltHelper.split(SHORT_TEXT, 6, 1, true));
    assertEquals("A...", XsltHelper.split(SHORT_TEXT, 5, 1, true));
    assertEquals("A...", XsltHelper.split(SHORT_TEXT, 4, 1, true));
    assertEquals("A", XsltHelper.split(SHORT_TEXT, 3, 1, true));
    assertEquals("A", XsltHelper.split(SHORT_TEXT, 2, 1, true));
    assertEquals("A", XsltHelper.split(SHORT_TEXT, 1, 1, true));
    assertEquals(SHORT_TEXT, XsltHelper.split(SHORT_TEXT, 0, 1, true));
  }
}
