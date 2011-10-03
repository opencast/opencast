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
package org.opencastproject.capture.impl.jobs;

/**
 * Defines contants used in many of the jobs. If you have a constant you need to add, this is the class it should live
 * in.
 */
public interface JobParameters {
  /** Constant used to define the key for the pointer to the state service. */
  String STATE_SERVICE = "state_service";

  /** Constant used to define the key for the pointer to the configuration service. */
  String CONFIG_SERVICE = "config_service";

  /** Constant used to define the key for the properties object which is pulled out of the execution context. */
  String CAPTURE_PROPS = "capture_props";

  /** Constant used to define the key for the media package object which is pulled out of the execution context. */
  String MEDIA_PACKAGE = "media_package";

  /** A constant which defines the key to retrieve a pointer to this object in the Quartz job classes. */
  String SCHEDULER = "scheduler";

  /** Constant used to define the key for the CaptureAgentImpl object which is pulled out of the execution context. */
  String CAPTURE_AGENT = "capture_agent";

  /** Constant used to define the key for the SchedulerImpl object which is pulled out of the execution context. */
  String SCHEDULER_IMPL = "scheduler_impl";

  /** Constant used to define the key for the TrustedHttpClient object which is pulled out of the execution context. */
  String TRUSTED_CLIENT = "trusted_client";

  /** Constant used to define the key for the IngestJob object which is pulled out of the execution context. */
  String INGEST_JOB = "ingest_job";

  /** Constant used to define the key for the Parent Job's context which is pulled out of the execution context. */
  String PARENT_CONTEXT = "parent_context";

  /**
   * Constant used to define the scheduler which should be used to schedule post-capture jobs.
   */
  String JOB_SCHEDULER = "job_scheduler";

  /** Constant used to define the postfix applied to all job names. */
  String JOB_POSTFIX = "job_postfix";

  /** Constant defining the job type for capture jobs. */
  String CAPTURE_TYPE = "captures";

  /** Constant defining the job type for capture related jobs (stop, manifest, serialize, ingest) */
  String SUPPORT_TYPE = "capture_related";

  /** Constant defining the job type for recurring jobs. */
  String RECURRING_TYPE = "recurring";

  /** Constant defining the job type or jobs which do not fit in the above types. */
  String OTHER_TYPE = "other";
}
