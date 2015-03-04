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
package org.opencastproject.pm.api;

import static org.opencastproject.util.RequireUtil.notEmpty;

import org.opencastproject.util.EqualsUtil;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.Jsons.Obj;

/**
 * Business object for a room.
 */
public class Room implements Blacklistable {

  public static final String TYPE = "room";

  /** The room identifier */
  private Long id;

  /** The room name */
  private String name;

  /**
   * Creates a room
   * 
   * @param name
   *          the name
   */
  public Room(String name) {
    this.name = notEmpty(name, "name");
  }

  /**
   * Sets the id
   * 
   * @param id
   *          the room id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * Returns the room id
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
  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Room room = (Room) o;
    return name.equals(room.getName());
  }

  @Override
  public int hashCode() {
    return EqualsUtil.hash(id, name);
  }

  @Override
  public String toString() {
    return "Room:" + name;
  }

  public Obj toJson() {
    return Jsons.obj(Jsons.p("id", id), Jsons.p("name", name));
  }

}
