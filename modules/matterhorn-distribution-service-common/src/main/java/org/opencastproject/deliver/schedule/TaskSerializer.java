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
package org.opencastproject.deliver.schedule;

import org.opencastproject.deliver.store.MarkerLocator;
import org.opencastproject.deliver.store.Serializer;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;

/**
 * Serializer for Task objects. This supports JSON representation of a Task
 * with its associated Action object.
 *
 * @author Jonathan A. Smith
 */

public class TaskSerializer implements Serializer<Task> {

    /**
     * Constructs a new Task object from a JSON string representation of
     * that Task.
     *
     * @param json_string String representation of the Task
     * @return Task instance
     */

    public Task fromString(String json_string) {
        JSONDeserializer<Task> deserializer = new JSONDeserializer<Task>();
        deserializer.use("action", new MarkerLocator(Action.class));
        return deserializer.deserialize(json_string);
    }

    /**
     * Converts a Task object to a string representation of that task
     * (including its Action object). Note that this synchronizes on the Task
     * so as to prevent changes while serializing.
     *
     * @param value task to be serialized
     * @return JSON string representation of the Task
     */

    public String toString(Task value) {
        synchronized (value) {
            return new JSONSerializer().serialize(value);
        }
    }

}
