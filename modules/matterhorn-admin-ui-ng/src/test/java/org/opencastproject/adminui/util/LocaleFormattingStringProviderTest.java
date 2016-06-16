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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class LocaleFormattingStringProviderTest {

  @Test
  public void testDateTimeFormat() {
    LocaleFormattingStringProvider formatter = new LocaleFormattingStringProvider(Locale.ENGLISH);
    assertEquals("M/d/yy h:mm a", formatter.getDateTimeFormat(SimpleDateFormat.SHORT));
    assertEquals("M/d/yy", formatter.getDateFormat(SimpleDateFormat.SHORT));
    assertEquals("h:mm a", formatter.getTimeFormat(SimpleDateFormat.SHORT));
  }

  @Test
  public void testDateFormat() {
    LocaleFormattingStringProvider formatter = new LocaleFormattingStringProvider(Locale.ENGLISH);
    assertEquals("MMM d, yyyy h:mm:ss a", formatter.getDateTimeFormat(SimpleDateFormat.MEDIUM));
    assertEquals("h:mm:ss a", formatter.getTimeFormat(SimpleDateFormat.MEDIUM));
    assertEquals("MMM d, yyyy", formatter.getDateFormat(SimpleDateFormat.MEDIUM));
  }

  @Test
  public void testTimeFormat() {
    LocaleFormattingStringProvider formatter = new LocaleFormattingStringProvider(Locale.ENGLISH);
    assertEquals("EEEE, MMMM d, yyyy h:mm:ss a z", formatter.getDateTimeFormat(SimpleDateFormat.FULL));
    assertEquals("h:mm:ss a z", formatter.getTimeFormat(SimpleDateFormat.FULL));
    assertEquals("EEEE, MMMM d, yyyy", formatter.getDateFormat(SimpleDateFormat.FULL));
  }
}
