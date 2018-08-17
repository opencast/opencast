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

package org.opencastproject.annotation.api;

import java.util.Date;


/**
 * A class that represents an annotation
 *
 */
public interface Annotation {

  Long getAnnotationId();

  void setAnnotationId(Long annotationId);

  String getMediapackageId();

  void setMediapackageId(String mediapackageId);

  String getUserId();

  void setUserId(String userId);

  String getSessionId();

  void setSessionId(String sessionId);

  int getInpoint();

  void setInpoint(int inpoint);

  int getOutpoint();

  void setOutpoint(int outpoint);

  int getLength();

  String getType();

  void setType(String type);

  String getValue();

  void setValue(String value);

  Date getCreated();

  void setCreated(Date created);

  Boolean getPrivate();

  void setPrivate(Boolean isPrivate);

}
