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

import com.entwinemedia.fn.Fns;
import com.entwinemedia.fn.fns.Booleans;
import com.entwinemedia.fn.fns.Strings;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

public class ProcessRunnerTest {
  @Test
  @Ignore
  public void testRunner() throws IOException {
    ProcessRunner.run(ProcessRunner.mk("ls -Xal", true),
            Fns.<String, Boolean> tee(Booleans.not.o(Strings.matches("^ls.*")), ProcessRunner.TO_CONSOLE.toFx()),
            ProcessRunner.IGNORE);
  }
}
