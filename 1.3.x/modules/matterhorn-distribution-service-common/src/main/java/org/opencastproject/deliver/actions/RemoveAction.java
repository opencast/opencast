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

/**
 * Base class for Action implementations for remove media from a publication
 * channel.
 *
 * @author Jonathan A. Smith
 */

public abstract class RemoveAction extends Action {

    /** The name of the task that published the video clip. */
    private String publish_task;

    /**
     * Constructs a RemoveAction.
     */

    public RemoveAction() {
        super();
    }

    /**
     * Returns the name of the task that published the clip to be removed.
     *
     * @return task name
     */

    public String getPublishTask() {
        return publish_task;
    }

    /**
     * Sets the name of the task that published the clip to be removed.
     *
     * @param publish_task Task name
     */

    public void setPublishTask(String publish_task) {
        this.publish_task = publish_task;
    }

    /**
     * Checks the members of the action.
     */

    public void validate() throws InvalidException {
        if (publish_task == null || publish_task.equals(""))
            throw new InvalidException("Missing publish task");
    }

}
