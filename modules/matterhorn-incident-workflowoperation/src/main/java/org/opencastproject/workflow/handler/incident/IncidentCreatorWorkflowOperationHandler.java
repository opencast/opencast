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
package org.opencastproject.workflow.handler.incident;

import static org.opencastproject.util.EnumSupport.parseEnum;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.fun.juc.Immutables;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobContext;
import org.opencastproject.serviceregistry.api.NopService;
import org.opencastproject.util.Log;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Strings;
import org.opencastproject.util.data.functions.Tuples;
import org.opencastproject.workflow.api.AbstractWorkflowOperationHandler;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationResult;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.regex.Pattern;

public class IncidentCreatorWorkflowOperationHandler extends AbstractWorkflowOperationHandler {
  private static final Log log = Log.mk(IncidentCreatorWorkflowOperationHandler.class);

  private static final String OPT_CODE = "code";
  private static final String OPT_SEVERITY = "severity";
  private static final String OPT_DETAILS = "details";
  private static final String OPT_PARAMS = "params";

  private static final SortedMap<String, String> CONFIG_OPTS = Immutables.sortedMap(
          tuple(OPT_CODE, "The code number of the incident to produce."),
          tuple(OPT_SEVERITY, "The severity"),
          tuple(OPT_DETAILS, "Some details: title=content;title=content;..."),
          tuple(OPT_PARAMS, "Some params: key=value;key=value;..."));

  private NopService nopService;

  @Override
  public SortedMap<String, String> getConfigurationOptions() {
    return CONFIG_OPTS;
  }

  @Override
  public WorkflowOperationResult start(WorkflowInstance wi, JobContext ctx) throws WorkflowOperationException {
    final WorkflowOperationInstance woi = wi.getCurrentOperation();
    final int code = option(woi.getConfiguration(OPT_CODE)).bind(Strings.toInt).getOrElse(1);
    final Severity severity = option(woi.getConfiguration(OPT_SEVERITY)).bind(parseEnum(Severity.FAILURE)).getOrElse(Severity.INFO);
    final List<Tuple<String, String>> details =  option(woi.getConfiguration(OPT_DETAILS)).mlist()
            .bind(splitBy(";")).map(splitBy("="))
            .filter(Tuples.<String>listHasSize(2)).map(Tuples.<String>fromList()).value();
    final Map<String, String> params = Immutables.map(
            option(woi.getConfiguration(OPT_PARAMS)).mlist()
                    .bind(splitBy(";")).map(splitBy("="))
                    .filter(Tuples.<String>listHasSize(2)).map(Tuples.<String>fromList()).value());
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

  private static Function<String, List<String>> splitBy(String p) {
    return Immutables.<String>listFromArrayFn().o(Strings.split(Pattern.compile(p)));
  }

  /** OSGi DI. */
  public void setNopService(NopService nopService) {
    this.nopService = nopService;
  }
}
