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
package org.opencastproject.dictionary.hunspell;

import static org.opencastproject.util.ReadinessIndicator.ARTIFACT;
import org.opencastproject.util.ReadinessIndicator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.io.UnsupportedEncodingException;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import org.apache.commons.lang.StringUtils;

import org.opencastproject.dictionary.api.DictionaryService;
import org.opencastproject.metadata.mpeg7.Textual;
import org.opencastproject.metadata.mpeg7.TextualImpl;

/**
 * This dictionary implementation is a dummy implementation which which will
 * just let the whole text pass through without any kind of filtering.
 */
public class DictionaryServiceImpl implements DictionaryService {

  /** The logging facility */
  private static final Logger logger =
    LoggerFactory.getLogger(DictionaryServiceImpl.class);

  public static final String HUNSPELL_BINARY_CONFIG_KEY =
    "org.opencastproject.dictionary.hunspell.binary";

  public static final String HUNSPELL_COMMAND_CONFIG_KEY =
    "org.opencastproject.dictionary.hunspell.command";

  /* The hunspell binary to execute */
  private String binary = "hunspell";

  /* The regular command line options for filtering */
  private String command = "-d de_DE,en_GB,en_US -G";

  public void setBinary(String b) {
    binary = b;
  }

  public String getBinary() {
    return binary;
  }

  public void setCommand(String c) {
    command = c;
  }

  public String getCommand() {
    return command;
  }

  /**
   * OSGi callback on component activation.
   *
   * @param  ctx  the bundle context
   */
  void activate(BundleContext ctx) throws UnsupportedEncodingException {
    Dictionary<String, String> properties = new Hashtable<String, String>();
    properties.put(ARTIFACT, "dictionary");
    ctx.registerService(ReadinessIndicator.class.getName(),
        new ReadinessIndicator(), properties);

    /* Get hunspell binary from config file */
    String binary = (String) ctx.getProperty(HUNSPELL_BINARY_CONFIG_KEY);
    if (binary != null) {
      /* Fix special characters */
      binary = new String(binary.getBytes("ISO-8859-1"), "UTF-8");
      logger.info("Setting hunspell binary to '{}'", binary);
      this.binary = binary;
    }

    /* Get hunspell command line options from config file */
    String command = (String) ctx.getProperty(HUNSPELL_COMMAND_CONFIG_KEY);
    if (command != null) {
      /* Fix special characters */
      command = new String(command.getBytes("ISO-8859-1"), "UTF-8");
      logger.info("Setting hunspell command line options to '{}'", command);
      this.command = command;
    }
  }


  /**
   * Run hunspell with text as input.
   **/
  public LinkedList<String> runHunspell(String text) throws Throwable {

    // create a new list of arguments for our process
    String commandLine = binary + ' ' + command;
    String[] commandList = commandLine.split("\\s+");


    InputStream  stdout = null;
    InputStream  stderr = null;
    OutputStream stdin  = null;
    Process p = null;
    BufferedReader bufr = null;
    LinkedList<String> words = new LinkedList<String>();

    logger.info("Executing hunspell command '{}'", StringUtils.join(commandList, " "));
    p = new ProcessBuilder(commandList).start();
    stderr = p.getErrorStream();
    stdout = p.getInputStream();
    stdin  = p.getOutputStream();

    /* Pipe text through hunspell for filtering */
    stdin.write(text.getBytes("UTF-8"));
    stdin.flush();
    stdin.close();

    /* Get output of hunspell */
    String line;
    bufr = new BufferedReader(new InputStreamReader(stdout, "UTF-8"));
    while ((line = bufr.readLine()) != null) {
      words.add(line);
    }
    bufr.close();

    /* Get error messages */
    bufr = new BufferedReader(new InputStreamReader(stderr));
    while ((line = bufr.readLine()) != null) {
      logger.warn(line);
    }
    bufr.close();

    if (p.waitFor() != 0) {
      logger.error("Hunspell reported an error (Missing dictionaries?)");
      throw new IllegalStateException("Hunspell returned error code");
    }

    return words;
  }


  /**
   * Filter the text according to the rules defined by the dictionary
   * implementation used. This implementation will just let the whole text pass
   * through.
   *
   * @return filtered text
   **/
  @Override
  public Textual cleanUpText(String text) {

    LinkedList<String> words = null;

    try {
      words = runHunspell(text);
    } catch (Throwable t) {
      logger.error("Error executing hunspell");
      logger.error(t.getMessage(), t);
      return null;
    }


    String result = StringUtils.join(words, " ");
    if ("".equals(result)) {
      return null;
    }
    return new TextualImpl(result);
  }

}
