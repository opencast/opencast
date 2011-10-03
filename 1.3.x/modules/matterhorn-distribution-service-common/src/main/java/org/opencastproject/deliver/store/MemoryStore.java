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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Temporary memory-based store.
 *
 * @author Jonathan A. Smith
 */

public class MemoryStore<ValueClass> implements Store<ValueClass> {

    /**
     * Immutable entry objects for storing a key and its associated value.
     */

    private class Entry {
        /** Key string. */
        private String key;

        /** JSON serialized value. */
        private String json_string;

        /** Time when entry was modified. */
        private long modified;

        /**
         * Constructs an Entry.
         *
         * @param key key value
         * @param modified time when modified
         * @param json stored value json
         */

        Entry(String key, ValueClass value) {
            this.key = key;
            this.modified = System.currentTimeMillis();
            this.json_string = serializer.toString(value);
        }
        
        String getKey() {
            return key;
        }

        ValueClass getValue() {
            return serializer.fromString(json_string);
        }

        long getModified() {
            return modified;
        }
    }

    /** Serializer implementation used to serialize and deserialize values. */
    private Serializer<ValueClass> serializer;

    /** Maps from key string to JSON-serialized value. */
    private HashMap<String, Entry> contents_map;

    /**
     * Constructs a MemoryStore that uses a specified Serialiser to convert
     * values to and from String represenations.
     *
     * @param serializer Serializer implementation
     */

    @SuppressWarnings("unchecked")
    public MemoryStore(Serializer serializer) {
        this.serializer = serializer;
        contents_map = new HashMap<String, Entry>();
    }

    /**
     * Adds a value to the store, replacing any prior value associated
     * with the specified key.
     *
     * @param key key value
     * @param value value to be associated with the key
     */

    public void put(String key, ValueClass value) {
        contents_map.put(key, new Entry(key, value));
    }

    /**
     * Gets a value from the Store. Returns null if not found.
     *
     * @param key
     * @return value from the Store
     */

    public ValueClass get(String key) {
        Entry entry = contents_map.get(key);
        if (entry == null)
            return null;
        else
            return entry.getValue();
    }

    /**
     * Removes a value from the store. Returns the removed value or null
     * if not found.
     * @param key String
     * @return value associated with key or null if key is not found
     */

    public ValueClass remove(String key) {
        Entry entry = contents_map.remove(key);
        if (entry == null)
            return null;
        else
            return entry.getValue();
    }

    /**
     * Returns true only if an entry with the specified key is found in
     * the Store. Returns false otherwise.
     * 
     * @param key String
     * @return true only if Store contains the specified key
     */

    public boolean containsKey(String key) {
        return contents_map.containsKey(key);
    }

    /**
     * Returns the time (in system milliseconds) when the entry was
     * last modified. Returns -1L if not found.
     *
     * @param key String
     * @return modification time in milliseconds or -1L
     */

    public long modified(String key) {
        Entry entry = contents_map.get(key);
        if (entry == null)
            return -1L;
        else
            return entry.getModified();
    }

    /**
     * Returns the set of keys for all items in the store.
     *
     * @return key set
     */

    public Set<String> keySet() {
        return new HashSet<String>(contents_map.keySet());
    }

}
