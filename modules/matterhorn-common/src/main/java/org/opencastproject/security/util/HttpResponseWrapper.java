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

package org.opencastproject.security.util;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpParams;

import java.util.Locale;
import java.util.UUID;

/**
 * A wrapper for {@link org.apache.http.HttpResponse} objects that implements
 * {@link #hashCode()} and {@link #equals(Object)} to allow for usage in hash based data structures.
 *
 * todo document motivation of this class
 */
public final class HttpResponseWrapper implements HttpResponse {
  private final HttpResponse response;
  private final String id;

  public HttpResponseWrapper(HttpResponse response) {
    this.response = response;
    this.id = UUID.randomUUID().toString();
  }

  @Override public int hashCode() {
    return id.hashCode();
  }

  @Override public boolean equals(Object o) {
    if (o instanceof HttpResponseWrapper) {
      return id.equals(((HttpResponseWrapper) o).id);
    } else {
      return false;
    }
  }

  @Override public StatusLine getStatusLine() {
    return response.getStatusLine();
  }

  @Override public void setStatusLine(StatusLine statusLine) {
    response.setStatusLine(statusLine);
  }

  @Override public void setStatusLine(ProtocolVersion protocolVersion, int i) {
    response.setStatusLine(protocolVersion, i);
  }

  @Override public void setStatusLine(ProtocolVersion protocolVersion, int i, String s) {
    response.setStatusLine(protocolVersion, i, s);
  }

  @Override public void setStatusCode(int i) throws IllegalStateException {
    response.setStatusCode(i);
  }

  @Override public void setReasonPhrase(String s) throws IllegalStateException {
    response.setReasonPhrase(s);
  }

  @Override public HttpEntity getEntity() {
    return response.getEntity();
  }

  @Override public void setEntity(HttpEntity httpEntity) {
    response.setEntity(httpEntity);
  }

  @Override public Locale getLocale() {
    return response.getLocale();
  }

  @Override public void setLocale(Locale locale) {
    response.setLocale(locale);
  }

  @Override public ProtocolVersion getProtocolVersion() {
    return response.getProtocolVersion();
  }

  @Override public boolean containsHeader(String s) {
    return response.containsHeader(s);
  }

  @Override public Header[] getHeaders(String s) {
    return response.getHeaders(s);
  }

  @Override public Header getFirstHeader(String s) {
    return response.getFirstHeader(s);
  }

  @Override public Header getLastHeader(String s) {
    return response.getLastHeader(s);
  }

  @Override public Header[] getAllHeaders() {
    return response.getAllHeaders();
  }

  @Override public void addHeader(Header header) {
    response.addHeader(header);
  }

  @Override public void addHeader(String s, String s2) {
    response.addHeader(s, s2);
  }

  @Override public void setHeader(Header header) {
    response.setHeader(header);
  }

  @Override public void setHeader(String s, String s2) {
    response.setHeader(s, s2);
  }

  @Override public void setHeaders(Header[] headers) {
    response.setHeaders(headers);
  }

  @Override public void removeHeader(Header header) {
    response.removeHeader(header);
  }

  @Override public void removeHeaders(String s) {
    response.removeHeaders(s);
  }

  @Override public HeaderIterator headerIterator() {
    return response.headerIterator();
  }

  @Override public HeaderIterator headerIterator(String s) {
    return response.headerIterator(s);
  }

  @Override public HttpParams getParams() {
    return response.getParams();
  }

  @Override public void setParams(HttpParams httpParams) {
    response.setParams(httpParams);
  }
}
