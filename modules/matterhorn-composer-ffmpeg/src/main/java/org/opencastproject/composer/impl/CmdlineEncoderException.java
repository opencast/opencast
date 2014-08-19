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
package org.opencastproject.composer.impl;

import org.opencastproject.composer.api.EncoderEngine;
import org.opencastproject.composer.api.EncoderException;

/** Specialized exception for command line encoders. */
public class CmdlineEncoderException extends EncoderException {
  private final String commandLine;

  public CmdlineEncoderException(EncoderEngine engine, String message, String commandLine, Throwable cause) {
    super(engine, message, cause);
    this.commandLine = commandLine;
  }

  public CmdlineEncoderException(EncoderEngine engine, String message, String commandLine) {
    super(engine, message);
    this.commandLine = commandLine;
  }

  public String getCommandLine() {
    return commandLine;
  }
}
