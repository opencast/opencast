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

import java.text.SimpleDateFormat;
import java.util.Locale;

public class LocaleFormattingStringProvider {

  private Locale locale;

  public LocaleFormattingStringProvider(Locale locale) {
    this.locale = locale;
  }

  public String getDateTimeFormat(int formatStyle) {
    SimpleDateFormat dateTime = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(formatStyle, formatStyle,
            locale);
    return dateTime.toPattern();
  }

  public String getDateFormat(int formatStyle) {
    SimpleDateFormat date = (SimpleDateFormat) SimpleDateFormat.getDateInstance(formatStyle, locale);
    return date.toPattern();
  }

  public String getTimeFormat(int formatStyle) {
    SimpleDateFormat time = (SimpleDateFormat) SimpleDateFormat.getTimeInstance(formatStyle, locale);
    return time.toPattern();
  }
}
