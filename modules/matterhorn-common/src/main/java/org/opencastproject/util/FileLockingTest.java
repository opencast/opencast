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

package org.opencastproject.util;

import org.opencastproject.util.data.Effect;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.UUID;

import static org.opencastproject.util.IoSupport.locked;
import static org.opencastproject.util.IoSupport.withResource;

/**
 * Run this program in two separate JVMs.
 * <h3>Running the program</h3>
 * <pre>
 * $ cd modules/matterhorn-common
 * $ mvn install
 * </pre>
 * Now open a second terminal and get the classpath in each of them
 * <pre>
 * $ cp=$(mvn dependency:build-classpath|grep -A 1 'Dependencies classpath'|tail -1):target/classes
 * </pre>
 * Run the program in both terminals
 * <pre>
 * $ java -cp $cp org.opencastproject.util.FileLockingTest
 * </pre>
 * The passes if not errors are thrown.
 */
public final class FileLockingTest {
  private FileLockingTest() {
  }

  public static void main(String[] args) throws Exception {
    final File file = new File("FILE_LOCKING_TEST_9139134.txt");
    final String myId = UUID.randomUUID().toString();
    System.out.println(myId + ": Starting ");
    try {
    locked(file, new Effect.X<File>() {
      @Override public void xrun(File file) throws Exception {
        withResource(new PrintWriter(new FileOutputStream(file)), new Effect.X<PrintWriter>() {
          @Override public void xrun(PrintWriter out) throws Exception {
            System.out.println(myId + ": Writing to file");
            out.println(myId);
            System.out.println(myId + ": Sleeping 7 sec...");
            Thread.sleep(7000);
            System.out.println(myId + ": Writing to file");
            out.println(myId);
          }
        });
        withResource(new BufferedReader(new FileReader(file)), new Effect.X<BufferedReader>() {
          @Override public void xrun(BufferedReader in) throws Exception {
            if (!myId.equals(in.readLine()))
              throw new Error("File not locked");
            if (!myId.equals(in.readLine()))
              throw new Error("File not locked");
            if (null != in.readLine())
              throw new Error("File not locked");
          }
        });
      }
    });
    } finally {
      System.out.println(myId + ": Stopping " + myId);
    }
  }
}
