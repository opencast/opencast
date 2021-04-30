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

package org.opencastproject.workflow.handler.incident;

import static org.opencastproject.util.EnumSupport.parseEnum;
import static org.opencastproject.util.data.Option.option;

import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.serviceregistry.api.NopService;
import org.opencastproject.util.Log;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IncidentCreatorWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Log log = Log.mk(IncidentCreatorWorkflowOperationHandler.class);

  private static final String OPT_CODE = "code";
  private static final String OPT_SEVERITY = "severity";
  private static final String OPT_DETAILS = "details";
  private static final String OPT_PARAMS = "params";

  private NopService nopService;

  @Override
  public WorkflowOperationResult start(WorkflowInstance wi, JobContext ctx) throws WorkflowOperationException {
    final WorkflowOperationInstance woi = wi.getCurrentOperation();
    final int code = option(woi.getConfiguration(OPT_CODE)).bind(Strings.toInt).getOrElse(1);
    final Severity severity = option(woi.getConfiguration(OPT_SEVERITY)).bind(parseEnum(Severity.FAILURE))
            .getOrElse(Severity.INFO);

    final List<Tuple<String, String>> details = Arrays.stream(ArrayUtils.nullToEmpty(
            StringUtils.split(woi.getConfiguration(OPT_DETAILS), ";")))
            .map((opt) -> opt.split("="))
            .filter((t) -> t.length == 2)
            .map((x) -> Tuple.tuple(x[0], x[1]))
            .collect(Collectors.toList());
    final Map<String, String> params = Arrays.stream(ArrayUtils.nullToEmpty(
            StringUtils.split(woi.getConfiguration(OPT_PARAMS), ";")))
            .map((opt) -> opt.split("="))
            .filter((t) -> t.length == 2)
            .collect(Collectors.toMap(x -> x[0], x -> x[1]));
    log.info("Create nop job");
    final Job job = nopService.nop();
    log.info("Log a dummy incident with code %d", code);
    serviceRegistry.incident().record(job, severity, code, params, details);
    if (!waitForStatus(job).isSuccess()) {
      throw new WorkflowOperationException("Job did not complete successfully");
    } else {
      return createResult(WorkflowOperationResult.Action.CONTINUE);
    }
  }

  /** OSGi DI. */
  public void setNopService(NopService nopService) {
    this.nopService = nopService;
  }
}
