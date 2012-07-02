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
package org.opencastproject.remotetest;

import org.opencastproject.remotetest.server.WorkingFileRepoRestEndpointTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.opencastproject.remotetest.server.CaptureAdminRestEndpointTest;
import org.opencastproject.remotetest.server.ComposerRestEndpointTest;
import org.opencastproject.remotetest.server.DistributionDownloadRestEndpointTest;
import org.opencastproject.remotetest.server.EngageModuleTest;
import org.opencastproject.remotetest.server.IngestRestEndpointTest;
import org.opencastproject.remotetest.server.IngestZipTest;
import org.opencastproject.remotetest.server.LtiAuthenticationTest;
import org.opencastproject.remotetest.server.MaintenanceModeTest;
import org.opencastproject.remotetest.server.MultiPartTest;
import org.opencastproject.remotetest.server.OaiPmhServerTest;
import org.opencastproject.remotetest.server.PreProcessingWorkflowTest;
import org.opencastproject.remotetest.server.ScheduledCaptureTest;
import org.opencastproject.remotetest.server.UnscheduledCaptureTest;
import org.opencastproject.remotetest.server.UploadTest;
import org.opencastproject.remotetest.server.WorkflowRestEndpointTest;

/**
 * Runs the server-side tests
 */
@RunWith(Suite.class)
@SuiteClasses({
  WorkflowRestEndpointTest.class,
  PreProcessingWorkflowTest.class,
  WorkingFileRepoRestEndpointTest.class,
  DistributionDownloadRestEndpointTest.class,
  IngestZipTest.class,
  IngestRestEndpointTest.class,
  ComposerRestEndpointTest.class,
  CaptureAdminRestEndpointTest.class,
  EngageModuleTest.class,
  UploadTest.class,
  MultiPartTest.class,
  ScheduledCaptureTest.class,
  UnscheduledCaptureTest.class,
  UploadTest.class,
  MaintenanceModeTest.class,
  OaiPmhServerTest.class,
  LtiAuthenticationTest.class
})

public class ServerTests {
}
