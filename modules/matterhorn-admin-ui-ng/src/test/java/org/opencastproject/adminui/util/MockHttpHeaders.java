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
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public class MockHttpHeaders implements HttpHeaders {

  private Locale locale;

  public MockHttpHeaders() {
    this.locale = Locale.ENGLISH;
  }

  public MockHttpHeaders(Locale locale) {
    this.locale = locale;
  }

  @Override
  public List<Locale> getAcceptableLanguages() {
    return Arrays.asList(locale);
  }

  @Override
  public List<MediaType> getAcceptableMediaTypes() {
    // not needed
    return null;
  }

  @Override
  public Map<String, Cookie> getCookies() {
    // not needed
    return null;
  }

  @Override
  public Locale getLanguage() {
    // not needed
    return null;
  }

  @Override
  public MediaType getMediaType() {
    // not needed
    return null;
  }

  @Override
  public List<String> getRequestHeader(String arg0) {
    // not needed
    return null;
  }

  @Override
  public MultivaluedMap<String, String> getRequestHeaders() {
    // not needed
    return null;
  }

}
