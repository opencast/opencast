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

package org.opencastproject.message.broker.api.group;

import org.opencastproject.security.api.Group;
import org.opencastproject.security.api.GroupParser;
import org.opencastproject.security.api.JaxbGroup;

import java.io.IOException;
import java.io.Serializable;

/**
 * {@link Serializable} class that represents all of the possible messages sent through an Acl queue.
 */
public class GroupItem implements Serializable {

  private static final long serialVersionUID = 6332696075634123068L;

  public static final String GROUP_QUEUE_PREFIX = "GROUP.";

  public static final String GROUP_QUEUE = GROUP_QUEUE_PREFIX + "QUEUE";

  private final String groupId;
  private final String group;
  private final Type type;

  public enum Type {
    Update, Delete
  };

  /**
   * Builds a {@link GroupItem} for creating or updating a Group.
   *
   * @param group
   *          The group
   * @return A new {@link GroupItem} with the correct information to update or create it.
   */
  public static GroupItem update(JaxbGroup group) {
    return new GroupItem(group, Type.Update);
  }

  /**
   * @return Builds {@link GroupItem} for deleting a group.
   */
  public static GroupItem delete(String groupId) {
    return new GroupItem(groupId, Type.Delete);
  }

  /**
   * Constructor to build a Create or Update {@link GroupItem}
   */
  public GroupItem(JaxbGroup group, Type type) {
    this.groupId = group.getGroupId();
    try {
      this.group = GroupParser.I.toXml(group);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    this.type = Type.Update;
  }

  /**
   * Constructor to build a delete {@link GroupItem}.
   */
  public GroupItem(String groupId, Type type) {
    this.groupId = groupId;
    this.group = null;
    this.type = type;
  }

  public String getGroupId() {
    return groupId;
  }

  public Group getGroup() {
    try {
      return group == null ? null : GroupParser.I.parseGroupFromXml(group);
    } catch (Exception e) {
      throw new IllegalStateException();
    }
  }

  public Type getType() {
    return type;
  }

}
