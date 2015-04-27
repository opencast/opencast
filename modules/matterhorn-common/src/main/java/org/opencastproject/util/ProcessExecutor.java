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

package org.opencastproject.util;

import static org.opencastproject.util.data.Collections.map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Helper class to execute processes on the host system and outside of the java vm. Since there are problems with
 * reading stdin, stdout and stderr that need to be taken into account when running on various platforms, this helper
 * class is used to deal with those.
 *
 * A generic Exception should be used to indicate what types of checked exceptions might be thrown from this process.
 *
 * @deprecated use {@link ProcessRunner} instead
 */
public class ProcessExecutor<T extends Exception> {
  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ProcessExecutor.class);

  private final boolean redirectErrorStream;
  private final String[] commandLine;
  private final Map<String, String> environment;

  protected ProcessExecutor(String commandLine, Map<String, String> environment, boolean redirectErrorStream) {
    this.commandLine = commandLine.split("\\s+");
    this.environment = environment;
    this.redirectErrorStream = redirectErrorStream;
  }

  protected ProcessExecutor(String command, String options) {
    this.commandLine = mkCommandLine(command, options);
    this.environment = map();
    this.redirectErrorStream = false;
  }

  protected ProcessExecutor(String command, String[] options) {
    List<String> commandLineList = new ArrayList<String>();
    commandLineList.add(command);
    commandLineList.addAll(Arrays.asList(options));
    this.commandLine = commandLineList.toArray(new String[commandLineList.size()]);
    this.environment = map();
    this.redirectErrorStream = false;
  }

  protected ProcessExecutor(String[] commandLine) {
    this.commandLine = commandLine;
    this.environment = map();
    this.redirectErrorStream = false;
  }

  protected ProcessExecutor(String commandLine, boolean redirectErrorStream) {
    this.commandLine = commandLine.split("\\s+");
    this.redirectErrorStream = redirectErrorStream;
    this.environment = map();
  }

  protected ProcessExecutor(String[] commandLine, boolean redirectErrorStream) {
    this.commandLine = commandLine;
    this.redirectErrorStream = redirectErrorStream;
    this.environment = map();
  }

  protected ProcessExecutor(String command, String options, boolean redirectErrorStream) {
    this.commandLine = mkCommandLine(command, options);
    this.redirectErrorStream = redirectErrorStream;
    this.environment = map();
  }

  private static String[] mkCommandLine(String command, String options) {
    final List<String> commandLineList = new ArrayList<String>();
    commandLineList.add(command);
    Collections.addAll(commandLineList, options.split("\\s+"));
    return commandLineList.toArray(new String[commandLineList.size()]);
  }

  // --

  public final void execute() throws ProcessExcecutorException {
    Process process = null;
    StreamHelper errorStreamHelper = null;
    StreamHelper inputStreamHelper = null;
    try {
      // no special working directory is set which means the working directory of the
      // current java process is used.
      final ProcessBuilder pbuilder = new ProcessBuilder(commandLine);
      pbuilder.redirectErrorStream(redirectErrorStream);
      pbuilder.environment().putAll(environment);
      process = pbuilder.start();
      // consume stdin (the process's stdout)
      // Quoting the java doc for {@link Process}:
      //   "Because some native platforms only provide limited buffer size for standard input and output streams,
      //    failure to promptly write the input stream or read the output stream of the subprocess may cause the
      //    subprocess to block, and even deadlock."
      // Since it's more likely that a process writes massive amounts to stdout it should be made
      // sure to read from it as fast as possible.
      // todo use a thread pool to avoid the delay of starting a new thread. Reading late from a
      //   process's stream is a serious issue which can cause the application to crash or hang.
      inputStreamHelper = new StreamHelper(process.getInputStream()) {
        @Override protected void append(String output) {
          onStdout(output);
        }
      };
      // consume stderr if it is not redirected and merged into stdin
      if (!redirectErrorStream) {
        errorStreamHelper = new StreamHelper(process.getErrorStream()) {
          @Override protected void append(String output) {
            onStderr(output);
          }
        };
      }
      // wait until streams have been emptied otherwise relevant information may get lost
      inputStreamHelper.join();
      if (errorStreamHelper != null) {
        errorStreamHelper.join();
      }
      // wait for the process to exit
      process.waitFor();
      int exitCode = process.exitValue();
      // allow subclasses to react to the process result
      onProcessFinished(exitCode);
    } catch (Throwable t) {
      String msg = null;
      if (errorStreamHelper != null && errorStreamHelper.contentBuffer != null) {
        msg = errorStreamHelper.contentBuffer.toString();
      }
      // TODO: What if the error stream has been redirected? Can we still get the error messgae?
      throw new ProcessExcecutorException(msg, t);
    } finally {
      IoSupport.closeQuietly(process);
    }
  }

  /**
   * A line of output has been read from the processe's stderr. Subclasses should override this method in order to deal
   * with process output.
   *
   * @param line
   *         the line from <code>stderr</code>
   */
  protected void onStderr(String line) {
    logger.warn(line);
  }

  /**
   * A line of output has been read from the processe's stdout. Subclasses should override this method in order to deal
   * with process output.
   *
   * @param line
   *         the line from <code>stdout</code>
   */
  protected void onStdout(String line) {
    logger.debug(line);
  }

  protected void onProcessFinished(int exitCode) throws T {
  }
}
