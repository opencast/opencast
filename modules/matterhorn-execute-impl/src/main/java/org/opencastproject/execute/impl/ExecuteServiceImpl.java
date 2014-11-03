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

package org.opencastproject.execute.impl;

import org.opencastproject.execute.api.ExecuteException;
import org.opencastproject.execute.api.ExecuteService;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElement.Type;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.UnsupportedElementException;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.ConfigurationException;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang.StringUtils;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implements a service that runs CLI commands with MediaPackage elements as arguments
 */
public class ExecuteServiceImpl extends AbstractJobProducer implements ExecuteService {

  public enum Operation {
    Execute_Element, Execute_Mediapackage
  }

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(ExecuteServiceImpl.class);

  /** Reference to the receipt service */
  private ServiceRegistry serviceRegistry = null;

  /** The security service */
  protected SecurityService securityService = null;

  /** The user directory service */
  protected UserDirectoryService userDirectoryService = null;

  /** The organization directory service */
  protected OrganizationDirectoryService organizationDirectoryService = null;

  /** The workspace service */
  protected Workspace workspace;

  /**
   * List of allowed commands that can be run with an executor. By convention, an empty set means any command can be run
   */
  protected final Set<String> allowedCommands = new HashSet<String>();

  /** Bundle property specifying which commands can be run with this executor */
  public static final String COMMANDS_ALLOWED_PROPERTY = "commands.allowed";

  /** The collection for the executor files */
  public static final String COLLECTION = "executor";

  /** To allow command-line parameter substitutions configured globally i.e. in config.properties */
  private BundleContext bundleContext;

  /** To allow command-line parameter substitutions configured at the service level */
  @SuppressWarnings("rawtypes")
  private Dictionary properties = null;

  /**
   * Creates a new instance of the execute service.
   */
  public ExecuteServiceImpl() {
    super(JOB_TYPE);
  }

  /**
   * Activates this component with its properties once all of the collaborating services have been set
   * 
   * @param cc
   *          The component's context, containing the properties used for configuration
   */
  public void activate(ComponentContext cc) {

    properties = cc.getProperties();

    if (properties != null) {
      String commandString = (String) properties.get(COMMANDS_ALLOWED_PROPERTY);
      if (commandString != null)
        for (String command : commandString.split("\\s+"))
          allowedCommands.add(command);
    }

    this.bundleContext = cc.getBundleContext();
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.textanalyzer.api.ExecuteService#execute(String, String)
   */
  @Override
  public Job execute(String exec, String params, MediaPackageElement inElement) throws ExecuteException {
    return execute(exec, params, inElement, null, null);
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.textanalyzer.api.ExecuteService#execute(String, String)
   * @throws IllegalArgumentException
   *           if the input arguments are incorrect
   * @throws ExecuteException
   *           if an internal error occurs
   */
  @Override
  public Job execute(String exec, String params, MediaPackageElement inElement, String outFileName, Type expectedType)
          throws ExecuteException, IllegalArgumentException {

    logger.debug("Creating Execute Job for command: {}", exec);

    if (StringUtils.isBlank(exec))
      throw new IllegalArgumentException("The command to execute cannot be null");

    if (StringUtils.isBlank(params))
      throw new IllegalArgumentException("The command arguments cannot be null");

    if (inElement == null)
      throw new IllegalArgumentException("The input MediaPackage element cannot be null");

    outFileName = StringUtils.trimToNull(outFileName);
    if ((outFileName == null) && (expectedType != null) || (outFileName != null) && (expectedType == null))
      throw new IllegalArgumentException("Expected element type and output filename cannot be null");

    try {
      List<String> paramList = new ArrayList<String>(5);
      paramList.add(exec);
      paramList.add(params);
      paramList.add(MediaPackageElementParser.getAsXml(inElement));
      paramList.add(outFileName);
      paramList.add((expectedType == null) ? null : expectedType.toString());

      return serviceRegistry.createJob(JOB_TYPE, Operation.Execute_Element.toString(), paramList);

    } catch (ServiceRegistryException e) {
      throw new ExecuteException(String.format("Unable to create a job of type '%s'", JOB_TYPE), e);
    } catch (MediaPackageException e) {
      throw new ExecuteException("Error serializing an element", e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.execute.api.ExecuteService#executeOnce(java.lang.String, java.lang.String,
   *      org.opencastproject.mediapackage.MediaPackage, java.lang.String,
   *      org.opencastproject.mediapackage.MediaPackageElement.Type,
   *      org.opencastproject.mediapackage.MediaPackageElementFlavor, java.lang.String[])
   */
  @Override
  public Job execute(String exec, String params, MediaPackage mp, String outFileName, Type expectedType)
          throws ExecuteException {

    if (StringUtils.isBlank(exec))
      throw new IllegalArgumentException("The command to execute cannot be null");

    if (StringUtils.isBlank(params))
      throw new IllegalArgumentException("The command arguments cannot be null");

    if (mp == null)
      throw new IllegalArgumentException("The input MediaPackage cannot be null");

    outFileName = StringUtils.trimToNull(outFileName);
    if ((outFileName == null) && (expectedType != null) || (outFileName != null) && (expectedType == null))
      throw new IllegalArgumentException("Expected element type and output filename cannot be null");

    try {
      List<String> paramList = new ArrayList<String>(5);
      paramList.add(exec);
      paramList.add(params);
      paramList.add(MediaPackageParser.getAsXml(mp));
      paramList.add(outFileName);
      paramList.add((expectedType == null) ? null : expectedType.toString());

      return serviceRegistry.createJob(JOB_TYPE, Operation.Execute_Mediapackage.toString(), paramList);
    } catch (ServiceRegistryException e) {
      throw new ExecuteException(String.format("Unable to create a job of type '%s'", JOB_TYPE), e);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @throws ExecuteException
   * @throws NotFoundException
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#process(org.opencastproject.job.api.Job)
   */
  @Override
  protected String process(Job job) throws ExecuteException {
    List<String> arguments = new ArrayList<String>(job.getArguments());

    // Check this operation is allowed
    if (!allowedCommands.isEmpty() && !allowedCommands.contains(arguments.get(0)))
      throw new ExecuteException("Command '" + arguments.get(0) + "' is not allowed");

    String outFileName = null;
    String strAux = null;
    MediaPackage mp = null;
    Type expectedType = null;
    MediaPackageElement element = null;
    Operation op = null;

    try {
      op = Operation.valueOf(job.getOperation());

      switch (arguments.size()) {
        case 5:
          strAux = arguments.remove(4);
          expectedType = (strAux == null) ? null : Type.valueOf(strAux);
          outFileName = StringUtils.trimToNull(arguments.remove(3));
          if (((outFileName != null) && (expectedType == null)) || ((outFileName == null) && (expectedType != null)))
            throw new ExecuteException("The output type and filename must be both specified");
          outFileName = (outFileName == null) ? null : job.getId() + "_" + outFileName;

        case 3:
          switch (op) {
            case Execute_Mediapackage:
              mp = MediaPackageParser.getFromXml(arguments.remove(2));
              return doProcess(arguments, mp, outFileName, expectedType);
            case Execute_Element:
              element = MediaPackageElementParser.getFromXml(arguments.remove(2));
              return doProcess(arguments, element, outFileName, expectedType);
            default:
              throw new IllegalStateException("Don't know how to handle operation '" + job.getOperation() + "'");
          }

        default:
          throw new IndexOutOfBoundsException("Incorrect number of parameters for operation execute_" + op + ": "
                  + arguments.size());
      }

    } catch (MediaPackageException e) {
      throw new ExecuteException("Error unmarshalling the input mediapackage/element", e);
    } catch (IllegalArgumentException e) {
      throw new ExecuteException("This service can't handle operations of type '" + op + "'", e);
    } catch (IndexOutOfBoundsException e) {
      throw new ExecuteException("The argument list for operation '" + op + "' does not meet expectations", e);
    }
  }

  public String doProcess(List<String> arguments, MediaPackage mp, String outFileName, Type expectedType)
          throws ExecuteException {

    String params = arguments.remove(1);

    File outFile = null;
    MediaPackageElement[] elementsByFlavor = null;

    try {
      if (outFileName != null) {
        // FIXME : Find a better way to place the output File
        File firstElement = workspace.get(mp.getElements()[0].getURI());
        outFile = new File(firstElement.getParentFile(), outFileName);
      }

      // Get the substitution pattern.
      // The following pattern matches, any construct with the form
      // #{name}
      // , where 'name' is the value of a certain property. It is stored in the backreference group 1.
      // Optionally, expressions can take a parameter, like
      // #{name(parameter)}
      // , where 'parameter' is the name of a certain parameter.
      // If specified, 'parameter' is stored in the group 2. Otherwise it's null.
      // Both name and parameter match any character sequence that does not contain {, }, ( or ) .
      Pattern pat = Pattern.compile("#\\{([^\\{\\}\\(\\)]+)(?:\\(([^\\{\\}\\(\\)]+)\\))?\\}");

      // Substitute the appearances of the patterns with the actual absolute paths
      Matcher matcher = pat.matcher(params);
      StringBuffer sb = new StringBuffer();
      while (matcher.find()) {
        // group(1) = property. group(2) = (optional) parameter
        if (matcher.group(1).equals("id")) {
          matcher.appendReplacement(sb, mp.getIdentifier().toString());
        } else if (matcher.group(1).equals("flavor")) {
          elementsByFlavor = mp.getElementsByFlavor(MediaPackageElementFlavor.parseFlavor(matcher.group(2)));
          if (elementsByFlavor.length == 0)
            throw new ExecuteException("No elements in the MediaPackage match the flavor '" + matcher.group(1) + "'.");

          if (elementsByFlavor.length > 1)
            logger.warn("Found more than one element with flavor '{}'. Using {} by default...", matcher.group(1),
                    elementsByFlavor[0].getIdentifier());

          File elementFile = workspace.get(elementsByFlavor[0].getURI());
          matcher.appendReplacement(sb, elementFile.getAbsolutePath());
        } else if (matcher.group(1).equals("out")) {
          matcher.appendReplacement(sb, outFile.getAbsolutePath());
        } else if (properties.get(matcher.group(1)) != null) {
          matcher.appendReplacement(sb, (String) properties.get(matcher.group(1)));
        } else if (bundleContext.getProperty(matcher.group(1)) != null) {
          matcher.appendReplacement(sb, bundleContext.getProperty(matcher.group(1)));
        }
      }
      matcher.appendTail(sb);
      params = sb.toString();
    } catch (IllegalArgumentException e) {
      throw new ExecuteException("Tag 'flavor' must specify a valid MediaPackage element flavor.", e);
    } catch (NotFoundException e) {
      throw new ExecuteException("The element '" + elementsByFlavor[0].getURI().toString()
              + "' does not exist in the workspace.", e);
    } catch (IOException e) {
      throw new ExecuteException("Error retrieving MediaPackage element from workspace: '"
              + elementsByFlavor[0].getURI().toString() + "'.", e);
    }

    arguments.addAll(splitParameters(params));

    return runCommand(arguments, outFile, expectedType);
  }

  /**
   * Does the actual processing
   * 
   * @param exec
   *          The command to run
   * @param params
   *          The CLI line including the command name
   * @param element
   *          A Matterhorn track
   * @return A {@code String} containing the command output
   * @throws ExecuteException
   *           if some internal error occurred
   * @throws NotFoundException
   *           if the mediapackage element could not be found in the workspace
   */
  public String doProcess(List<String> arguments, MediaPackageElement element, String outFileName, Type expectedType)
          throws ExecuteException {

    // arguments(1) contains a list of space-separated arguments for the command
    String params = arguments.remove(1);
    arguments.addAll(splitParameters(params));

    File outFile = null;

    try {
      // Get the track file from the workspace
      File trackFile = workspace.get(element.getURI());

      // Put the destination file in the same folder as the source file
      if (outFileName != null)
        outFile = new File(trackFile.getParentFile(), outFileName);

      // Substitute the appearances of the patterns with the actual absolute paths
      for (int i = 1; i < arguments.size(); i++) {
        if (arguments.get(i).contains(INPUT_FILE_PATTERN)) {
          arguments.set(i, arguments.get(i).replace(INPUT_FILE_PATTERN, trackFile.getAbsolutePath()));
          continue;
        }
        if (arguments.get(i).contains(OUTPUT_FILE_PATTERN)) {
          if (outFile != null) {
            arguments.set(i, arguments.get(i).replace(OUTPUT_FILE_PATTERN, outFile.getAbsolutePath()));
            continue;
          } else {
            logger.error("{} pattern found, but no valid output filename was specified", OUTPUT_FILE_PATTERN);
            throw new ExecuteException(OUTPUT_FILE_PATTERN
                    + " pattern found, but no valid output filename was specified");
          }
        }
      }

      return runCommand(arguments, outFile, expectedType);

    } catch (IOException e) {
      logger.error("Error retrieving file from workspace: {}", element.getURI());
      throw new ExecuteException("Error retrieving file from workspace: " + element.getURI(), e);
    } catch (NotFoundException e) {
      logger.error("Element '{}' cannot be found in the workspace.", element.getURI());
      throw new ExecuteException("Element " + element.getURI() + " cannot be found in the workspace");
    }
  }

  private String runCommand(List<String> arguments, File outFile, Type expectedType) throws ExecuteException {

    Process p = null;
    int result = 0;

    try {

      logger.info("Running command {}", arguments.get(0));

      if (logger.isDebugEnabled()) {
        logger.debug("Starting subprocess {} with arguments:", arguments.get(0));
        for (String arg : arguments.subList(1, arguments.size()))
          logger.debug(arg);
      }

      ProcessBuilder pb = new ProcessBuilder(arguments);
      pb.redirectErrorStream(true);

      p = pb.start();
      result = p.waitFor();

      logger.debug("Command {} finished with result {}", arguments.get(0), result);

      if (result == 0) {
        // Read the command output
        if (outFile != null) {
          if (outFile.isFile()) {
            URI newURI = workspace.putInCollection(COLLECTION, outFile.getName(), new FileInputStream(outFile));
            if (outFile.delete()) {
              logger.debug("Deleted the local copy of the encoded file at {}", outFile.getAbsolutePath());
            } else {
              logger.warn("Unable to delete the encoding output at {}", outFile.getAbsolutePath());
            }
            return MediaPackageElementParser.getAsXml(MediaPackageElementBuilderFactory.newInstance()
                    .newElementBuilder().elementFromURI(newURI, expectedType, null));
          }
        }
        return "";
      } else {
        // 'Scanner' reads tokens delimited by an specific character (set).
        // By telling a Scanner to use the 'beginning of the input boundary' character as delimiter, which of course
        // will never find, yields the whole String as the next token.
        String line;
        try {
          line = new Scanner(p.getInputStream()).useDelimiter("\\A").next();
        } catch (NoSuchElementException e) {
          line = "";
        }

        throw new ExecuteException(String.format("Process %s returned error code %d with this output:\n%s",
                arguments.get(0), result, line.trim()));
      }
    } catch (InterruptedException e) {
      throw new ExecuteException("The executor thread has been unexpectedly interrupted", e);
    } catch (IOException e) {
      // Only log the first argument, the executable, as other arguments may contain sensitive values
      // e.g. MySQL password/user, paths, etc. that should not be shown to caller
      logger.error("Could not start subprocess {}", arguments.get(0));
      throw new ExecuteException("Could not start subprocess: " + arguments.get(0), e);
    } catch (UnsupportedElementException e) {
      throw new ExecuteException("Couldn't create a new MediaPackage element of type " + expectedType.toString(), e);
    } catch (ConfigurationException e) {
      throw new ExecuteException("Couldn't instantiate a new MediaPackage element builder", e);
    } catch (MediaPackageException e) {
      throw new ExecuteException("Couldn't serialize a new Mediapackage element of type " + expectedType.toString(), e);
    } finally {
      IoSupport.closeQuietly(p);
    }
  }

  /**
   * Returns a list of strings broken on whitespace characters except where those whitespace characters are escaped or
   * quoted.
   * 
   * @return list of individual arguments
   */
  private List<String> splitParameters(String input) {

    // This delimiter matches any non-escaped quote
    final String QUOTE_DELIM = "(?<!\\\\)\"";

    // This delimiter matches any number of non-escaped spaces
    final String SPACE_DELIM = "((?<!\\\\)\\s)+";

    ArrayList<String> parsedInput = new ArrayList<String>();
    boolean quoted = false;

    for (String token1 : input.split(QUOTE_DELIM))
      if (quoted) {
        parsedInput.add(token1);
        quoted = false;
      } else {
        for (String token2 : token1.split(SPACE_DELIM))
          // This ignores empty tokens if quotes are at the beginning or the end of the string
          if (!token2.isEmpty())
            parsedInput.add(token2);
        quoted = true;
      }

    return parsedInput;
  }

  /**
   * Sets the receipt service
   * 
   * @param serviceRegistry
   *          the service registry
   */
  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getServiceRegistry()
   */
  @Override
  protected ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getSecurityService()
   */
  @Override
  protected SecurityService getSecurityService() {
    return securityService;
  }

  /**
   * Callback for setting the security service.
   * 
   * @param securityService
   *          the securityService to set
   */
  public void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  /**
   * Callback for setting the user directory service.
   * 
   * @param userDirectoryService
   *          the userDirectoryService to set
   */
  public void setUserDirectoryService(UserDirectoryService userDirectoryService) {
    this.userDirectoryService = userDirectoryService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getUserDirectoryService()
   */
  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return userDirectoryService;
  }

  /**
   * {@inheritDoc}
   * 
   * @see org.opencastproject.job.api.AbstractJobProducer#getOrganizationDirectoryService()
   */
  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return organizationDirectoryService;
  }

  /**
   * Sets a reference to the organization directory service.
   * 
   * @param organizationDirectory
   *          the organization directory
   */
  public void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectory) {
    this.organizationDirectoryService = organizationDirectory;
  }

  /**
   * Sets a reference to the workspace service.
   * 
   * @param workspace
   */
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

}
