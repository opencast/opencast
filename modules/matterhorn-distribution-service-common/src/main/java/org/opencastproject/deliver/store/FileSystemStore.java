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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Store implementation that saves data as JSON files in a disk directory.
 *
 * @author Jonathan A. Smith
 */

public class FileSystemStore<ValueClass> implements Store<ValueClass> {

    /** Serializer to serialize values. */
    private Serializer<ValueClass> serializer;

    /** Root directory where data is stored. */
    private File directory;

    /** Extension used for data files. */
    private String extension;

    public FileSystemStore(File directory, Serializer<ValueClass> serializer,
                           boolean create_if_needed) {
        this.directory = directory;
        if (create_if_needed && !directory.exists())
            directory.mkdirs();

        this.serializer = serializer;
        this.extension = ".json";
    }

    public FileSystemStore(File directory, Serializer<ValueClass> serializer) {
        this(directory, serializer, false);
    }

    public void put(String key, ValueClass value) throws InvalidKeyException {
        File data_file = dataFile(key);
        String representation = serializer.toString(value);
        try {
            FileWriter out = new FileWriter(data_file);
            out.write(representation);
            out.close();
        } catch (IOException except) {
            Logger.getLogger(FileSystemStore.class.getName()
                    ).log(Level.SEVERE, null, except);
        }
    }

    public ValueClass get(String key) throws InvalidKeyException {
        File data_file = dataFile(key);
        String text = readFile(data_file);
        if (text == null)
            return null;
        else
            return serializer.fromString(text);
    }

    /**
     * Reads a file into a String.
     *
     * @param data_file
     * @return String containing text from file
     */

    private String readFile(File data_file) {
        StringBuilder out = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(data_file));
            boolean is_first = true;
            while (in.ready()) {
                if (!is_first) {
                    out.append("\n");
                    is_first = false;
                }
                out.append(in.readLine());
            }
            return out.toString();

        }
        catch (FileNotFoundException except) {
            return null;
        }
        catch (IOException except) {
            Logger.getLogger(FileSystemStore.class.getName()).log(
                    Level.SEVERE, null, except);
        }
        return null;
    }

    public ValueClass remove(String key) throws InvalidKeyException {
        File data_file = dataFile(key);
        if (!data_file.exists())
            return null;
        ValueClass value = get(key);
        data_file.delete();
        return value;
    }

    public boolean containsKey(String key) throws InvalidKeyException {
        File data_file = dataFile(key);
        return data_file.exists();
    }

    public long modified(String key) throws InvalidKeyException {
        File data_file = dataFile(key);
        if (!data_file.exists())
            return -1L;
        else
            return data_file.lastModified();
    }

    public Set<String> keySet() {
        HashSet<String> keys = new HashSet<String>();
        if (!directory.exists() || !directory.isDirectory())
            return keys;

        for (String name : directory.list()) {
            if (!name.endsWith(extension))
                continue;
            keys.add(name.substring(0, name.length() - extension.length()));
        }
        return keys;
    }

    /**
     * Returns the data file used to store a serialized data item.
     *
     * @param key key string
     * @return File
     * @throws InvalidKeyException 
     */

    private File dataFile(String key) throws InvalidKeyException {
        if (!key.matches("^[\\w\\-]+$"))
            throw new InvalidKeyException(key);
        return new File(directory, key + extension);
    }

}
