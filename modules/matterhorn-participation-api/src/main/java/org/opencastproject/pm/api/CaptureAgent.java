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

package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notEmpty;
import static org.opencastproject.util.RequireUtil.notNull;

import org.opencastproject.util.EqualsUtil;

import org.apache.commons.lang.StringUtils;

/**
 * Business object for an capture agent
 */
public class CaptureAgent {

  /** The capture agent id */
  private Long id;

  /** The room related to the capture agent */
  private Room room;

  /** The MH agent identifier */
  private String mhAgent;

  /**
   * Creates an capture agent
   * 
   * @param id
   *          the id
   * @param agentId
   *          the agent id
   */
  public CaptureAgent(Room room, String agentId) {
    this.setRoom(notNull(room, "room"));
    this.mhAgent = notEmpty(agentId, "mhAgent");
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the capture agent id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the capture agent id
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the MH agent identifier
   * 
   * @param mhAgent
   *          the agent id
   */
  public void setMhAgent(String mhAgent) {
    this.mhAgent = mhAgent;
  }

  /**
   * Returns the MH agent identifier
   * 
   * @return the agent id
   */
  public String getMhAgent() {
    return mhAgent;
  }

  /**
   * Gets the room containing the capture agent
   * 
   * @return the room
   */
  public Room getRoom() {
    return room;
  }

  /**
   * Sets the room containing the capture agent
   * 
   * @param room
   *          the room containing the agent
   */
  public void setRoom(Room room) {
    this.room = room;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CaptureAgent ca = (CaptureAgent) o;
    return getRoom().equals(ca.getRoom()) && mhAgent.equals(ca.getMhAgent());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, room, mhAgent);
  }

  @Override
  public String toString() {
    return "CaptureAgent:" + mhAgent;
  }

  /**
   * Get the Matterhorn capture agent id based on the room name
   * 
   * @param room
   *          the room containing the capture agent
   * @return the Matterhorn capture agent id
   */
  public static String getMhAgentIdFromRoom(Room room) {
    return StringUtils.trim(room.getName()).replaceAll("\\s+|\\W+|_", "-").toLowerCase();
  }
}
