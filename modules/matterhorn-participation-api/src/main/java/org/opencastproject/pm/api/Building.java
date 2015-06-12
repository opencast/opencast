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

import java.util.ArrayList;
import java.util.List;

/**
 * Business object for a building.
 */
public class Building {

  /** The building id */
  private Long id;

  /** The building name */
  private String name;

  /** The list of rooms */
  private List<Room> rooms = new ArrayList<Room>();

  /**
   * Creates a building
   * 
   * @param name
   *          the name
   * @param rooms
   *          the list of rooms
   */
  public Building(String name, List<Room> rooms) {
    this.name = notEmpty(name, "name");
    this.rooms = notNull(rooms, "rooms");
  }

  /**
   * Creates a new building with an empty list of rooms
   * 
   * @param name
   *          the name
   */
  public Building(String name) {
    this.name = notEmpty(name, "name");
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the building id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the building id
   * 
   * @return the id
   */
  public Long getId() {
    return this.id;
  }

  /**
   * Sets the name
   * 
   * @param name
   *          the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns the name
   * 
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the room list
   * 
   * @param rooms
   *          the room list
   */
  public void setRooms(List<Room> rooms) {
    this.rooms = notNull(rooms, "rooms");
  }

  /**
   * Returns the room list
   * 
   * @return the room list
   */
  public List<Room> getRooms() {
    return rooms;
  }

  /**
   * Add a room to the building
   * 
   * @param room
   *          the room to add to this building
   * @return true if this collection changed as a result of the call
   */
  public boolean addRoom(Room room) {
    return rooms.add(notNull(room, "room"));
  }

  /**
   * Remove a room from the building
   * 
   * @param room
   *          the room to remove from this building
   * @return true if this collection changed as a result of the call
   */
  public boolean removeRoom(Room room) {
    return rooms.remove(notNull(room, "room"));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Building building = (Building) o;
    return name.equals(building.getName()) && rooms.equals(building.getRooms());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, name, rooms);
  }

  @Override
  public String toString() {
    return "Building:" + name;
  }

}
