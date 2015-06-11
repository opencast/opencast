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

package org.opencastproject.util.jaxb;

import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * JAXB adapter that formats dates in UTC format YYYY-MM-DD'T'hh:mm:ss.MMM'Z' up to millisecond, e.g.
 * 1970-01-01T00:00:00.000Z
 */
public final class UtcTimestampAdapter extends XmlAdapter<String, Date> {
  @Override
  public String marshal(Date date) throws Exception {
    return ISODateTimeFormat.dateTime().withZoneUTC().print(new DateTime(date.getTime()));
  }

  @Override
  public Date unmarshal(String date) throws Exception {
    return ISODateTimeFormat.dateTimeParser().parseDateTime(date).toDate();
  }
}
