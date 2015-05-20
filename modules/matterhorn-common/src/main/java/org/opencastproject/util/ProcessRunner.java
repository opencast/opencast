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

import static com.entwinemedia.fn.Stream.$;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.Prelude;
import com.entwinemedia.fn.data.ListBuilder;
import com.entwinemedia.fn.data.ListBuilders;
import com.entwinemedia.fn.fns.Booleans;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class to run an external process on the host system and to read its STDOUT and STDERR streams.
 */
public final class ProcessRunner {
  public static final Pred<String> IGNORE = Booleans.yes();

  public static final Pred<String> TO_CONSOLE = new Pred<String>() {
    @Override public Boolean ap(String s) {
      System.out.println(s);
      return true;
    }
  };

  private ProcessRunner() {
  }

  public static int run(ProcessInfo info, Fn<String, Boolean> stdout, Fn<String, Boolean> stderr) throws IOException {
    final ProcessBuilder pb = new ProcessBuilder(info.getCommandLine()).redirectErrorStream(info.isRedirectErrorStream());
    pb.environment().putAll(info.getEnvironment());
    // create stream consumer runnables
    final StreamConsumer consumeOut = new StreamConsumer(stdout);
    final StreamConsumer consumeError = new StreamConsumer(stderr);
    // create threads and run them
    final Thread consumeOutThread = new Thread(consumeOut);
    final Thread consumeErrorThread = new Thread(consumeError);
    consumeOutThread.start();
    consumeErrorThread.start();
    // Wait for the consumer threads to run to be able to immediately consume output.
    // This is important to avoid deadlocks or blocks of the process.
    // From the java doc for {@link Process}:
    //   "Because some native platforms only provide limited buffer size for standard input and output streams,
    //    failure to promptly write the input stream or read the output stream of the subprocess may cause the
    //    subprocess to block, and even deadlock."
    consumeOut.waitUntilRunning();
    consumeError.waitUntilRunning();
    final Process p = pb.start();
    consumeOut.consume(p.getInputStream());
    consumeError.consume(p.getErrorStream());
    // wait until the streams have been fully consumed
    consumeOut.waitUntilFinished();
    consumeError.waitUntilFinished();
    // wait and exit
    try {
      return p.waitFor();
    } catch (InterruptedException e) {
      return Prelude.<Integer>chuck(e);
    }
  }

  private static final ListBuilder l = ListBuilders.looseImmutableArray;
  private static final Map<String, String> NO_ENV = new HashMap<String, String>();

  public static ProcessInfo mk(String commandLine) {
    return new ProcessInfo(mkCommandLine(commandLine), NO_ENV, false);
  }

  public static ProcessInfo mk(String commandLine, Map<String, String> environment, boolean redirectErrorStream) {
    return new ProcessInfo(mkCommandLine(commandLine), environment, redirectErrorStream);
  }

  public static ProcessInfo mk(String command, String options) {
    return new ProcessInfo(mkCommandLine(command, options), NO_ENV, false);
  }

  public static ProcessInfo mk(String command, String[] options) {
    return new ProcessInfo($(command).append($(options)).toList(), NO_ENV, false);
  }

  public static ProcessInfo mk(String[] commandLine) {
    return new ProcessInfo(l.mk(commandLine), NO_ENV, false);
  }

  public static ProcessInfo mk(String commandLine, boolean redirectErrorStream) {
    return new ProcessInfo(mkCommandLine(commandLine), NO_ENV, redirectErrorStream);
  }

  public static ProcessInfo mk(String[] commandLine, boolean redirectErrorStream) {
    return new ProcessInfo(l.mk(commandLine), NO_ENV, redirectErrorStream);
  }

  public static ProcessInfo mk(String command, String options, boolean redirectErrorStream) {
    return new ProcessInfo(mkCommandLine(command, options), NO_ENV, redirectErrorStream);
  }

  private static List<String> mkCommandLine(String command) {
    return l.mk(command.split("\\s+"));
  }

  private static List<String> mkCommandLine(String command, String options) {
    return $(command).append(mkCommandLine(options)).toList();
  }

  public static final class ProcessInfo {
    private final boolean redirectErrorStream;
    private final List<String> commandLine;
    private final Map<String, String> environment;

    public ProcessInfo(List<String> commandLine,
                       Map<String, String> environment,
                       boolean redirectErrorStream) {
      this.redirectErrorStream = redirectErrorStream;
      this.commandLine = commandLine;
      this.environment = environment;
    }

    public boolean isRedirectErrorStream() {
      return redirectErrorStream;
    }

    public List<String> getCommandLine() {
      return commandLine;
    }

    public Map<String, String> getEnvironment() {
      return environment;
    }
  }
}
