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
package org.opencastproject.deliver.itunesu;

import org.opencastproject.deliver.actions.DeliveryAction;
import org.opencastproject.deliver.schedule.FailedException;
import org.opencastproject.deliver.schedule.RetryException;
import org.opencastproject.deliver.store.InvalidKeyException;

/**
 * DeliveryAction to upload iTunes media.
 */
public class ITunesDeliveryAction extends DeliveryAction {

  private static final long serialVersionUID = 751731610211924552L;

  /** URL for accessing uploaded track. */
  private String trackURL;

  /**
   * Constructs a ITunesDeliveryAction.
   */
  public ITunesDeliveryAction() {
    super();
  }

  /**
   * Returns the iTunes track URL or null if not yet set.
   * 
   * @return track URL
   */
  public String getTrackURL() {
    return trackURL;
  }

  /**
   * Sets the iTunes track URL.
   * 
   * @param trackURL
   *          URL for uploaded track
   */
  public void setTrackURL(String trackURL) {
    this.trackURL = trackURL;
  }

  /**
   * Delivers a media file to iTunesU.
   * @throws FailedException 
   * @throws RetryException 
   * @throws InvalidKeyException 
   */
  @Override
  protected void execute() throws RetryException, FailedException, InvalidKeyException {
    // update status
    status("Upload in progress");

    /** Web service API instance */
    ITunesWSAPI api = new ITunesWSAPI(getDestination());

    String url = "";

    url = api.uploadFile(getMediaPath());

    // add metadata
    String title = getTitle();
    String creator = getCreator();
    String comments = getAbstract();
    String[] tags = getTags();

    // attach tags to abstract
    comments += "\n\n";
    for (String tag : tags) {
      comments += tag + ' ';
    }

    String trackHandle = url.substring(url.lastIndexOf('.') + 1);

    String xmlResponse = api.mergeTrack(trackHandle, title, creator, comments);
    if (xmlResponse.indexOf("error") > 0) {
      status("Error in media metadata");
      api.deleteTrack(trackHandle);
    } else {
      // skip "xxx.edu."
      int index = api.getSiteURL().length() + 1;
      setTrackURL(url.substring(index + 1));
      status("Media uploaded");
      succeed("Media delivered: " + getMediaPath() + " - " + url);
    }
  }
}
