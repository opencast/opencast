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
package org.opencastproject.editor.api;

public class CommentReplyData {
  private final long id;
  private final String creationDate;
  private final String author;
  private final String displayName;
  private final String text;

  public CommentReplyData(long id, String creationDate, String author, String displayName, String text) {
    this.id = id;
    this.creationDate = creationDate;
    this.author = author;
    this.displayName = displayName;
    this.text = text;
  }

  public long getId() {
    return id;
  }

  public String getCreationDate() {
    return creationDate;
  }

  public String getAuthor() {
    return author;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getText() {
    return text;
  }
}
