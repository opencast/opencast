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

import flexjson.ClassLocator;
import flexjson.Path;

import java.util.Map;

/**
 * ClassLocator implementation to support deserialization of any subclass of a
 * specified marker class from a flexjson JSON representation.
 *
 * @author Jonathan A. Smith
 */

public class MarkerLocator implements ClassLocator {

    /** Class or interface. */
    @SuppressWarnings("unchecked")
    private Class marker_class;

    @SuppressWarnings("unchecked")
    public MarkerLocator(Class marker_class) {
        this.marker_class = marker_class;
    }


    /**
     * Returns a subclass of action needed to deserialize a JSON representation
     * of an instance of the class.
     *
     * @param map containing deserialized JSON data
     * @param path property name path to field being deserialized
     * @return Subclass of Action
     *
     * @throws ClassNotFoundException thrown if the named class is not found
     *                                or is not an action subclass.
     */

    @SuppressWarnings("unchecked")
    public Class locate(Map map, Path path)
            throws ClassNotFoundException {
        String class_name = (String)map.get("class");
        System.out.println(class_name);
        Class found_class = Class.forName(class_name);
        if (found_class != null && marker_class.isAssignableFrom(found_class))
            return found_class;
        throw new ClassNotFoundException();
    }

}
