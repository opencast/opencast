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
package org.opencastproject.loadtest.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/** A class to execute command line operations such as zip. **/
public final class Execute {
  private static final Logger logger = LoggerFactory.getLogger(Execute.class);

  private Execute() {
    
  }

  /**
   * Launches a command line execution e.g. cp source destination
   * 
   * @param command
   *          The command line utility to execute.
   * **/
  public static void launch(String command) {
    InputStream stderr = null;
    InputStream stdout = null;
    BufferedReader bufferedReader = null;
    try {
      String line;
      
      // launch EXE and grab stdin/stdout and stderr
      Process process = Runtime.getRuntime().exec(command);

      stderr = process.getErrorStream();
      stdout = process.getInputStream();

      // Printout any stdout messages
      bufferedReader = new BufferedReader(new InputStreamReader(stdout));
      while ((line = bufferedReader.readLine()) != null) {
        logger.info(line);
      }
      bufferedReader.close();

      // Printout any stderr messages
      bufferedReader = new BufferedReader(new InputStreamReader(stderr));
      while ((line = bufferedReader.readLine()) != null) {
        logger.warn(line);
      }
      bufferedReader.close();
      // Printout the error code for the process
      process.waitFor();
      logger.info("Finished Executing:\"" + command + "\" with exit value " + process.exitValue());
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      // Close the streams regardless of what may have gone wrong.
      try {
        bufferedReader.close();
      } catch (IOException e) {
        logger.error("While trying to close buffered reader " + e.getMessage());
      }
      try {
        stderr.close();
      } catch (IOException e) {
        logger.error("While trying to close stderr " + e.getMessage());
      }
      try {
        stdout.close();
      } catch (IOException e) {
        logger.error("While trying to close stdout " + e.getMessage());
      }
    }
  }

}
