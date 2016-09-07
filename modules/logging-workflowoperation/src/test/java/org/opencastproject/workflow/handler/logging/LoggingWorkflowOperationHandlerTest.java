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

package org.opencastproject.workflow.handler.logging;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class LoggingWorkflowOperationHandlerTest {

  private LoggingWorkflowOperationHandler operation = new LoggingWorkflowOperationHandler();
  private WorkflowInstanceImpl workflow;
  private WorkflowOperationInstance instance;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    MediaPackageBuilder builder = new MediaPackageBuilderImpl();
    MediaPackage mediaPackage = builder.createNew();

    instance = EasyMock.createNiceMock(WorkflowOperationInstanceImpl.class);
    EasyMock.expect(instance.getId()).andReturn(2L).anyTimes();

    workflow = EasyMock.createNiceMock(WorkflowInstanceImpl.class);
    EasyMock.expect(workflow.getMediaPackage()).andReturn(mediaPackage).anyTimes();
    EasyMock.expect(workflow.getId()).andReturn(1L).anyTimes();
    EasyMock.expect(workflow.getCurrentOperation()).andReturn(instance).anyTimes();
  }

  @Test
  public void testSimpleLog() throws Exception {
    EasyMock.replay(workflow, instance);
    operation.start(workflow, null);
  }

  @Test
  public void testLogToFile() throws Exception {
    File tempFolder = testFolder.newFolder("logging.test");
    EasyMock.expect(instance.getConfiguration("directory")).andReturn(tempFolder.getAbsolutePath()).anyTimes();
    EasyMock.expect(instance.getConfiguration("mediapackage-xml")).andReturn("true").anyTimes();
    EasyMock.expect(instance.getConfiguration("workflowinstance-xml")).andReturn("true").anyTimes();
    EasyMock.replay(workflow, instance);
    operation.start(workflow, null);
    Assert.assertEquals(3, FileUtils.listFilesAndDirs(tempFolder, TrueFileFilter.TRUE, TrueFileFilter.TRUE).size());
  }

}
