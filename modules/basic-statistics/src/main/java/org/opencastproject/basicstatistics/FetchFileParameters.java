/*
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
package org.opencastproject.basicstatistics;

public class FetchFileParameters {
  private String elem; // (file) element ID, which is the path segment after the video ID
  private Long from; // start of byte range of what was downloaded. Non-range-request specify 0.
  private Long to; // end of byte range of what was downloaded. null if the request does not specify an end and the
          // file server has no way to check what bytes were actually sent to the client.

  public String getElem() {
    return elem;
  }

  public void setElem(String elem) {
    this.elem = elem;
  }

  public Long getFrom() {
    return from;
  }

  public void setFrom(Long from) {
    this.from = from;
  }

  public Long getTo() {
    return to;
  }

  public void setTo(Long to) {
    this.to = to;
  }
}
