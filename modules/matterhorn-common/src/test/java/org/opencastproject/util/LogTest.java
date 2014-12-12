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

import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Not a real unit test yet. */
public class LogTest {
  @Test
  public void testLog() throws Exception {
    final Log log = new Log(LoggerFactory.getLogger(LogTest.class));
    log.info("hello");
    log.startUnitOfWork();
    log.info("hello");
    log.endUnitOfWork();
    log.info("hello");
    spawnThread(log.getContext());
    log.endUnitOfWork();
    log.endUnitOfWork();
    log.endUnitOfWork();
    log.info("hello");
    log.info("hello");
    log.info("hello");
  }

  private void spawnThread(List<String> unitOfWork) {
    final Log log = new Log(LoggerFactory.getLogger(LogTest.class));
    log.continueContext(unitOfWork);
    log.info("new thread");
  }
}
