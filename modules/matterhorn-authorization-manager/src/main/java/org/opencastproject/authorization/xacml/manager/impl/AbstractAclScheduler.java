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
package org.opencastproject.authorization.xacml.manager.impl;

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.AclTransition;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.authorization.xacml.manager.api.TransitionResult;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.util.SecurityContext;
import org.opencastproject.util.NeedleEye;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Effect0;
import org.opencastproject.util.data.Function0;
import org.opencastproject.util.data.Option;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.quartz.TriggerUtils;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Prelude.unexhaustiveMatch;

/** Time control for the {@link AclServiceImpl}. */
public abstract class AbstractAclScheduler {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractAclScheduler.class);

  private static final String JOB_NAME = "mh-acl-job";
  private static final String JOB_GROUP = "mh-acl-job-group";
  private static final String TRIGGER_NAME = "mh-acl-trigger";
  private static final String TRIGGER_GROUP = "mh-acl-trigger-group";
  private static final String JOB_PARAM_PARENT = "parent";

  private final org.quartz.Scheduler quartz;

  public abstract AclServiceFactory getAclServiceFactory();

  public abstract OrganizationDirectoryService getOrganizationDirectoryService();

  /** Get a system administrator context for the given organization id. */
  public abstract SecurityContext getAdminContextFor(String orgId);

  protected AbstractAclScheduler() {
    try {
      quartz = new StdSchedulerFactory().getScheduler();
      quartz.start();
      // create and set the job. To actually run it call schedule(..)
      final JobDetail job = new JobDetail(JOB_NAME, JOB_GROUP, Runner.class);
      job.setDurability(false);
      job.setVolatility(true);
      job.getJobDataMap().put(JOB_PARAM_PARENT, this);
      quartz.addJob(job, true);
    } catch (org.quartz.SchedulerException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set the schedule and start or restart the scheduler.
   */
  public void schedule() {
    logger.info("Run AclScheduler every minute");
    try {
      final Trigger trigger = TriggerUtils.makeMinutelyTrigger();
      trigger.setStartTime(new Date());
      trigger.setName(TRIGGER_NAME);
      trigger.setGroup(TRIGGER_GROUP);
      trigger.setJobName(JOB_NAME);
      trigger.setJobGroup(JOB_GROUP);
      if (quartz.getTriggersOfJob(JOB_NAME, JOB_GROUP).length == 0) {
        quartz.scheduleJob(trigger);
      } else {
        quartz.rescheduleJob(TRIGGER_NAME, TRIGGER_GROUP, trigger);
      }
    } catch (Exception e) {
      logger.error("Error scheduling Quartz job", e);
    }
  }

  /** Shutdown the scheduler. */
  public void shutdown() {
    try {
      quartz.shutdown();
    } catch (org.quartz.SchedulerException ignore) {
    }
  }

  /** Trigger the scheduler once independent of it's actual schedule. */
  public void trigger() {
    try {
      quartz.triggerJobWithVolatileTrigger(JOB_NAME, JOB_GROUP);
    } catch (Exception e) {
      logger.error("Error triggering Quartz job", e);
    }
  }

  // just to make sure Quartz is being shut down...
  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    shutdown();
  }

  // --

  /** Quartz work horse. */
  public static class Runner extends TypedQuartzJob<AbstractAclScheduler> {
    private static final NeedleEye eye = new NeedleEye();

    public Runner() {
      super(some(eye));
    }

    @Override
    protected void execute(final AbstractAclScheduler parameters, JobExecutionContext ctx) {
      logger.debug("Running ACL scheduler");
      // iterate all organizations
      for (final Organization org : parameters.getOrganizationDirectoryService().getOrganizations()) {
        // set the org on the current thread... this approach should be deprecated
        // todo check if after the heavy refactoring this is still necessary.
        parameters.getAdminContextFor(org.getId()).runInContext(new Effect0() {
          @Override
          protected void run() {
            // create an ACL service for the org. This approach should be used in favour of thread bound orgs.
            final AclService aclMan = parameters.getAclServiceFactory().serviceFor(org);
            final Date now = new Date();
            final TransitionQuery q = TransitionQuery.query().before(now).withDone(false);
            try {
              final TransitionResult r = aclMan.getTransitions(q);
              final List<AclTransition> transitions = Collections.<AclTransition>concat(r.getEpisodeTransistions(), r.getSeriesTransistions());
              logger.debug("Found {} transition/s for organization {}", transitions.size(), org.getId());
              for (final AclTransition t : transitions) {
                if (t instanceof EpisodeACLTransition) {
                  final EpisodeACLTransition et = (EpisodeACLTransition) t;
                  logger.info("Apply transition to episode {}", et.getEpisodeId());
                  aclMan.applyEpisodeAclTransition(et);
                } else if (t instanceof SeriesACLTransition) {
                  final SeriesACLTransition st = (SeriesACLTransition) t;
                  logger.info("Apply transition to series {}", st.getSeriesId());
                  aclMan.applySeriesAclTransition(st);
                } else {
                  unexhaustiveMatch();
                }
              }
            } catch (AclServiceException e) {
              logger.error("Error executing runner", e);
            }
          }
        });
      }
      logger.debug("Finished ACL scheduling");
    }
  }

  /**
   * Please remember that derived classes need a no-arg constructor in order to work with Quartz. Sample usage:
   * 
   * <pre>
   * public class Runner extends TypedQuartzJob&lt;Scheduler&gt; {
   *   protected abstract void execute(Scheduler parameters, JobExecutionContext ctx) {
   *     // type safe parameter access
   *     parameters.getConfig();
   *   }
   * }
   * 
   * public class Scheduler {
   *   ...
   *   // create the job
   *   final JobDetail job = new JobDetail(JOB_NAME, JOB_GROUP, Runner.class);
   *   // set the scheduler as parameter source
   *   job.getJobDataMap().put(JOB_PARAM_PARENT, this);
   *   // add to Quartz scheduler
   *   quartz.addJob(job, true);
   *   ...
   *   // provide a config string
   *   public String getConfig() {...}
   * }
   * </pre>
   */
  public abstract static class TypedQuartzJob<A> implements Job {
    private final Option<NeedleEye> allowParallel;

    /**
     * @param allowParallel
     *          Pass a needle eye if only one job may be run at a time. Make the needle eye static to the inheriting
     *          class.
     */
    protected TypedQuartzJob(Option<NeedleEye> allowParallel) {
      this.allowParallel = allowParallel;
    }

    @Override
    public final void execute(final JobExecutionContext ctx) throws JobExecutionException {
      for (NeedleEye eye : allowParallel) {
        eye.apply(executeF(ctx));
        return;
      }
      executeF(ctx).apply();
    }

    /** Typesafe replacement for {@link #execute(org.quartz.JobExecutionContext)}. */
    protected abstract void execute(A parameters, JobExecutionContext ctx);

    private Function0<Integer> executeF(final JobExecutionContext ctx) {
      return new Function0.X<Integer>() {
        @Override
        public Integer xapply() throws Exception {
          try {
            execute((A) ctx.getJobDetail().getJobDataMap().get(JOB_PARAM_PARENT), ctx);
            return 0;
          } catch (Exception e) {
            logger.error("An error occurred while harvesting schedule", e);
            throw new JobExecutionException("An error occurred while harvesting schedule", e);
          }
        }
      };
    }
  }
}
