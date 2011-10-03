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
package org.opencastproject.deliver.store;

/**
 * An interface for value serializers. This allows plugabble value
 * serialization. Note that serializers are used in specific store
 * implementations, not in all Stores, so as to proivde for Store
 * implementations that use some other means to save value objects.
 *
 * @author Jonathan A. Smith
 */

public interface Serializer<ValueClass> {

    ValueClass fromString(String json_string);

    String toString(ValueClass value);
}
