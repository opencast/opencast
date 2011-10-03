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
package org.opencastproject.deliver.actions;

import org.opencastproject.deliver.schedule.Action;
import org.opencastproject.deliver.schedule.InvalidException;

import java.io.File;

/**
 * Action to deliver a media item to a destination. Specific actions needed to accomplish delivery are implemented in
 * subclasses.
 * 
 * @author Jonathan A. Smith
 */

public abstract class DeliveryAction extends Action {

  private static final long serialVersionUID = -3883702651539494030L;
  private String destination;
  private int item_number;
  private String title;
  private String creator;
  private String description;
  private String date;
  private String media_path;
  private String[] tags;

  public DeliveryAction() {
    super();
    tags = new String[0];
  }

  // **** Access

  public String getCreator() {
    return creator;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getAbstract() {
    return description;
  }

  public void setAbstract(String description) {
    this.description = description;
  }

  public int getItemNumber() {
    return item_number;
  }

  public void setItemNumber(int item_number) {
    this.item_number = item_number;
  }

  public String getMediaPath() {
    return media_path;
  }

  public void setMediaPath(String media_path) {
    this.media_path = media_path;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDestination() {
    return destination;
  }

  public void setDestination(String destination) {
    this.destination = destination;
  }

  // **** Validation

  /**
   * Checks the members of the delivery action to insure that all required members are set and that all values are
   * valid.
   * 
   * @throws InvalidException
   */

  public void validate() throws InvalidException {
    if (destination == null || destination.equals(""))
      throw new InvalidException("Missing destination");
    if (title == null || title.equals(""))
      throw new InvalidException("Missing title");
    if (media_path == null || media_path.equals(""))
      throw new InvalidException("Missing media path");
    if (!new File(media_path).exists())
      throw new InvalidException("Missing media: " + media_path);
  }
}
