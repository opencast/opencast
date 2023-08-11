/*
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

package org.opencastproject.serviceregistry.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.opencastproject.db.Queries.namedQuery;
import static org.opencastproject.security.api.SecurityConstants.ORGANIZATION_HEADER;
import static org.opencastproject.security.api.SecurityConstants.USER_HEADER;

import org.opencastproject.db.DBSession;
import org.opencastproject.db.DBSessionFactory;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.jpa.JpaJob;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.security.api.TrustedHttpClientException;
import org.opencastproject.security.api.User;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.HostRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.serviceregistry.api.SystemLoad;
import org.opencastproject.serviceregistry.impl.jpa.ServiceRegistrationJpaImpl;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * This dispatcher implementation will check for jobs in the QUEUED {@link Job.Status}. If
 * new jobs are found, the dispatcher will attempt to dispatch each job to the least loaded service.
 */
@Component(
    property = {
        "service.description=Job Dispatcher"
    },
    immediate = true,
    service = { JobDispatcher.class }
)
public class JobDispatcher {

  /** JPA persistence unit name */
  public static final String PERSISTENCE_UNIT = "org.opencastproject.common";

  /** Configuration key for the dispatch interval, in seconds */
  protected static final String OPT_DISPATCHINTERVAL = "dispatch.interval";

  /** Minimum delay between job dispatching attempts, in seconds */
  static final long MIN_DISPATCH_INTERVAL = 1;

  /** Default delay between job dispatching attempts, in seconds */
  static final long DEFAULT_DISPATCH_INTERVAL = 0;

  private static final Logger logger = LoggerFactory.getLogger(JobDispatcher.class);

  private ServiceRegistryJpaImpl serviceRegistry;

  private OrganizationDirectoryService organizationDirectoryService;
  private UserDirectoryService userDirectoryService;
  private SecurityService securityService;
  private TrustedHttpClient client;

  /** The thread pool to use for dispatching. */
  protected ScheduledThreadPoolExecutor scheduledExecutor = null;

  /** The factory used to generate the entity manager */
  private EntityManagerFactory emf = null;

  protected DBSessionFactory dbSessionFactory;

  protected DBSession db;

  private ScheduledFuture jdfuture = null;

  /**
   * A list with job types that cannot be dispatched in each interation
   */
  private List<String> undispatchableJobTypes = null;

  /** The dispatcher priority list */
  protected final Map<Long, String> dispatchPriorityList = new HashMap<>();

  /** OSGi DI */
  @Reference(target = "(osgi.unit.name=org.opencastproject.common)")
  void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  /** OSGi DI */
  @Reference
  public void setDBSessionFactory(DBSessionFactory dbSessionFactory) {
    this.dbSessionFactory = dbSessionFactory;
  }

  /** OSGi DI */
  @Reference()
  void setServiceRegistry(ServiceRegistryJpaImpl sr) {
    this.serviceRegistry = sr;
  }

  @Reference()
  void setOrganizationDirectoryService(OrganizationDirectoryService organizationDirectoryService) {
    this.organizationDirectoryService = organizationDirectoryService;
  }

  @Reference
  void setUserDirectoryService(UserDirectoryService svc) {
    this.userDirectoryService = svc;
  }

  @Reference
  void setSecurityService(SecurityService sec) {
    this.securityService = sec;
  }

  @Reference
  void setTrustedHttpClient(TrustedHttpClient client) {
    this.client = client;
  }

  @Activate
  public void activate(ComponentContext cc) throws ConfigurationException  {
    logger.info("Activate job dispatcher");
    db = dbSessionFactory.createSession(emf);
    scheduledExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    scheduledExecutor.setRemoveOnCancelPolicy(true);
    logger.info("Activated");
    updated(cc.getProperties());
  }


  @Modified
  public void modified(ComponentContext cc) throws ConfigurationException {
    logger.debug("Modified in job dispatcher");
    updated(cc.getProperties());
  }

  /**
   * {@inheritDoc}
   *
   * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
   */
  @SuppressWarnings("rawtypes")
  public void updated(Dictionary properties) {

    logger.info("Updating job dipatcher properties");

    long dispatchInterval = DEFAULT_DISPATCH_INTERVAL;
    String dispatchIntervalString = StringUtils.trimToNull((String) properties.get(OPT_DISPATCHINTERVAL));
    if (StringUtils.isNotBlank(dispatchIntervalString)) {
      try {
        dispatchInterval = Long.parseLong(dispatchIntervalString);
      } catch (Exception e) {
        logger.warn("Dispatch interval '{}' is malformed, setting to {}", dispatchIntervalString, MIN_DISPATCH_INTERVAL);
        dispatchInterval = MIN_DISPATCH_INTERVAL;
      }
      if (dispatchInterval == 0) {
        logger.info("Dispatching disabled");
      } else if (dispatchInterval < MIN_DISPATCH_INTERVAL) {
        logger.warn("Dispatch interval {} ms too low, adjusting to {}", dispatchInterval, MIN_DISPATCH_INTERVAL);
        dispatchInterval = MIN_DISPATCH_INTERVAL;
      } else {
        logger.info("Dispatch interval set to {} seconds", dispatchInterval);
      }
    }

    long dispatchDelay = dispatchInterval;

    // Stop the current dispatch thread so we can configure a new one
    if (jdfuture != null) {
      jdfuture.cancel(true);
    }

    // Schedule the job dispatching.
    if (dispatchInterval > 0) {
      logger.info("Job dispatching is enabled");
      logger.debug("Starting job dispatching at a custom interval of {}s", dispatchInterval);
      jdfuture = scheduledExecutor.scheduleWithFixedDelay(getJobDispatcherRunnable(), dispatchDelay, dispatchInterval,
          TimeUnit.SECONDS);
    } else {
      logger.info("Job dispatching is disabled");
    }
  }

  Runnable getJobDispatcherRunnable() {
    return new JobDispatcherRunner();
  }

  public class JobDispatcherRunner implements Runnable {

    /**
     * {@inheritDoc}
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
      logger.debug("Starting job dispatch");

      undispatchableJobTypes = new ArrayList<>();
      try {
        //GDLGDL: move collectJobStats to the JD config, then this is reasonable
        // FIXME: the stats are not currently used and the queries are very expensive in database time.
        if (serviceRegistry.collectJobstats) {
          serviceRegistry.updateStatisticsJobData();
        }

        if (!dispatchPriorityList.isEmpty()) {
          logger.trace("Checking for outdated jobs in dispatchPriorityList's '{}' jobs", dispatchPriorityList.size());
          // Remove outdated jobs from priority list
          List<Long> jobIds = db.exec(getDispatchableJobsWithIdFilterQuery(dispatchPriorityList.keySet()));
          for (Long jobId : new HashSet<>(dispatchPriorityList.keySet())) {
            if (!jobIds.contains(jobId)) {
              logger.debug("Removing outdated dispatchPriorityList job '{}'", jobId);
              dispatchPriorityList.remove(jobId);
            }
          }
        }

        int jobsOffset = 0;
        List<JpaJob> dispatchableJobs;
        List<JpaJob> workflowJobs = new ArrayList<>();
        boolean jobsFound;
        do {
          // dispatch all dispatchable jobs with status restarted
          dispatchableJobs = db.exec(serviceRegistry.getDispatchableJobsWithStatusQuery(
              jobsOffset, ServiceRegistryJpaImpl.DEFAULT_DISPATCH_JOBS_LIMIT, Job.Status.RESTART
          ));
          jobsOffset += ServiceRegistryJpaImpl.DEFAULT_DISPATCH_JOBS_LIMIT;
          jobsFound = !dispatchableJobs.isEmpty();

          // skip all jobs of type workflow, we will handle them next
          for (JpaJob job : dispatchableJobs) {
            if (ServiceRegistryJpaImpl.TYPE_WORKFLOW.equals(job.getJobType())) {
              workflowJobs.add(job);
            }
          }
          if (dispatchableJobs.removeAll(workflowJobs) && dispatchableJobs.isEmpty()) {
            continue;
          }

          dispatchDispatchableJobs(dispatchableJobs);
        } while (jobsFound);

        jobsOffset = 0;
        do {
          // dispatch all dispatchable jobs with status queued
          dispatchableJobs = db.exec(serviceRegistry.getDispatchableJobsWithStatusQuery(
              jobsOffset, ServiceRegistryJpaImpl.DEFAULT_DISPATCH_JOBS_LIMIT, Job.Status.QUEUED
          ));
          jobsOffset += ServiceRegistryJpaImpl.DEFAULT_DISPATCH_JOBS_LIMIT;
          jobsFound = !dispatchableJobs.isEmpty();

          // skip all jobs of type workflow, we will handle them next
          for (JpaJob job : dispatchableJobs) {
            if (ServiceRegistryJpaImpl.TYPE_WORKFLOW.equals(job.getJobType())) {
              workflowJobs.add(job);
            }
          }
          if (dispatchableJobs.removeAll(workflowJobs) && dispatchableJobs.isEmpty()) {
            continue;
          }

          dispatchDispatchableJobs(dispatchableJobs);
        } while (jobsFound);

        if (!workflowJobs.isEmpty()) {
          dispatchDispatchableJobs(workflowJobs);
        }
      } catch (Throwable t) {
        logger.warn("Error dispatching jobs", t);
      } finally {
        undispatchableJobTypes = null;
      }

      logger.debug("Finished job dispatch");
    }

    /**
     * Dispatch the given jobs.
     *
     * @param jobsToDispatch list with dispatchable jobs to dispatch
     */
    private void dispatchDispatchableJobs(List<JpaJob> jobsToDispatch) {
      // Get the current system load
      SystemLoad systemLoad = db.exec(serviceRegistry.getHostLoadsQuery());

      for (JpaJob job : jobsToDispatch) {
        // Remember the job type
        String jobType = job.getJobType();

        // Skip jobs that we already know can't be dispatched except of jobs in the priority list
        String jobSignature = jobType + '@' + job.getOperation();
        if (undispatchableJobTypes.contains(jobSignature) && !dispatchPriorityList.containsKey(job.getId())) {
          logger.trace("Skipping dispatching of {} with type '{}' for this round of dispatching", job, jobType);
          continue;
        }

        // Set the job's user and organization prior to dispatching
        String creator = job.getCreator();
        String creatorOrganization = job.getOrganization();

        // Try to load the organization.
        Organization organization;
        try {
          organization = organizationDirectoryService.getOrganization(creatorOrganization);
          securityService.setOrganization(organization);
        } catch (NotFoundException e) {
          logger.debug("Skipping dispatching of job for non-existing organization '{}'", creatorOrganization);
          continue;
        }

        // Try to load the user
        User user = userDirectoryService.loadUser(creator);
        if (user == null) {
          logger.warn("Unable to dispatch {}: creator '{}' is not available", job, creator);
          continue;
        }
        securityService.setUser(user);

        // Start dispatching
        try {
          List<ServiceRegistration> services = db.exec(serviceRegistry.getServiceRegistrationsQuery());
          List<HostRegistration> hosts = db.exec(serviceRegistry.getHostRegistrationsQuery()).stream()
                                           .filter(host -> !dispatchPriorityList.containsValue(host.getBaseUrl())
                                               || host.getBaseUrl().equals(dispatchPriorityList.get(job.getId())))
                                           .collect(Collectors.toList());
          List<ServiceRegistration> candidateServices;

          // Depending on whether this running job is trying to reach out to other services or whether this is an
          // attempt to execute the next operation in a workflow, choose either from a limited or from the full list
          // of services
          Job parentJob = null;
          try {
            if (job.getParentJob() != null) {
              parentJob = serviceRegistry.getJob(job.getParentJob().getId());
            }
          } catch (NotFoundException e) {
            // That's ok
          }

          // When a job A starts a series of child jobs, then those child jobs should only be dispatched at the
          // same time if there is processing capacity available.
          boolean parentHasRunningChildren = false;
          if (parentJob != null) {
            for (Job child : serviceRegistry.getChildJobs(parentJob.getId())) {
              if (Job.Status.RUNNING.equals(child.getStatus())) {
                parentHasRunningChildren = true;
                break;
              }
            }
          }

          // If this is a root job (a new workflow or a new workflow operation), then only dispatch if there is
          // capacity, i. e. the workflow service is ok dispatching the next workflow or the next workflow operation.
          if (parentJob == null || ServiceRegistryJpaImpl.TYPE_WORKFLOW.equals(jobType) || parentHasRunningChildren) {
            logger.trace("Using available capacity only for dispatching of {} to a service of type '{}'", job, jobType);
            candidateServices = serviceRegistry.getServiceRegistrationsWithCapacity(jobType, services, hosts, systemLoad);
          } else {
            logger.trace("Using full list of services for dispatching of {} to a service of type '{}'", job, jobType);
            candidateServices = serviceRegistry.getServiceRegistrationsByLoad(jobType, services, hosts, systemLoad);
          }

          // Try to dispatch the job
          String hostAcceptingJob;
          try {
            hostAcceptingJob = dispatchJob(job, candidateServices);
            try {
              systemLoad.updateNodeLoad(hostAcceptingJob, job.getJobLoad());
            } catch (NotFoundException e) {
              logger.info("Host {} not found in load list, cannot dispatch {} to it", hostAcceptingJob, job);
            }

            dispatchPriorityList.remove(job.getId());
          } catch (ServiceUnavailableException e) {
            logger.debug("Jobs of type {} currently cannot be dispatched", job.getOperation());
            // Don't mark workflow jobs as undispatchable to not impact worklfow operations
            if (!ServiceRegistryJpaImpl.TYPE_WORKFLOW.equals(jobType)) {
              undispatchableJobTypes.add(jobSignature);
            }
            continue;
          } catch (UndispatchableJobException e) {
            logger.debug("{} currently cannot be dispatched", job);
            continue;
          }

          logger.debug("{} dispatched to {}", job, hostAcceptingJob);
        } catch (ServiceRegistryException e) {
          Throwable cause = (e.getCause() != null) ? e.getCause() : e;
          logger.error("Error dispatching {}: {}", job, cause);
        } finally {
          securityService.setUser(null);
          securityService.setOrganization(null);
        }
      }
    }

    /**
     * Dispatches the job to the least loaded service that will accept the job, or throws a
     * <code>ServiceUnavailableException</code> if there is no such service.
     *
     * @param job      the job to dispatch
     * @param services a list of service registrations
     * @return the host that accepted the dispatched job, or <code>null</code> if no services took the job.
     * @throws ServiceRegistryException    if the service registrations are unavailable
     * @throws ServiceUnavailableException if no service is available or if all available services refuse to take on more work
     * @throws UndispatchableJobException  if the current job cannot be processed
     */
    private String dispatchJob(JpaJob job, List<ServiceRegistration> services)
        throws ServiceRegistryException, ServiceUnavailableException, UndispatchableJobException {
      if (services.size() == 0) {
        logger.debug("No service is currently available to handle jobs of type '" + job.getJobType() + "'");
        throw new ServiceUnavailableException("No service of type " + job.getJobType() + " available");
      }

      // Try the service registrations, after the first one finished, we quit;
      job.setStatus(Job.Status.DISPATCHING);

      boolean triedDispatching = false;
      boolean jobLoadExceedsMaximumLoads = false;

      final Float highestMaxLoad = services.stream()
                                           .map(s -> ((ServiceRegistrationJpaImpl) s).getHostRegistration())
                                           .map(HostRegistration::getMaxLoad)
                                           .max(Comparator.naturalOrder())
                                           .get();

      if (job.getJobLoad() > highestMaxLoad) {
        // None of the available hosts is able to accept the job because the largest max load value is less than this job's load value
        jobLoadExceedsMaximumLoads = true;
      }

      for (ServiceRegistration registration : services) {
        job.setProcessorServiceRegistration((ServiceRegistrationJpaImpl) registration);

        // Skip registration of host with less max load than highest available max load
        // Note: This service registration may or may not live on a node which is set to accept jobs exceeding its max load
        if (jobLoadExceedsMaximumLoads && job.getProcessorServiceRegistration().getHostRegistration().getMaxLoad() != highestMaxLoad) {
          continue;
        }

        try {
          job = serviceRegistry.updateInternal(job); // will open a tx
        } catch (Exception e) {
          // In theory, we should catch javax.persistence.OptimisticLockException. Unfortunately, eclipselink throws
          // org.eclipse.persistence.exceptions.OptimisticLockException. In order to avoid importing the implementation
          // specific APIs, we just catch Exception.
          logger.debug("Unable to dispatch {}.  This is likely caused by another service registry dispatching the job",
              job);
          throw new UndispatchableJobException(job + " is already being dispatched");
        }

        triedDispatching = true;

        String serviceUrl = UrlSupport.concat(registration.getHost(), registration.getPath(), "dispatch");
        HttpPost post = new HttpPost(serviceUrl);

        // Add current organization and user so they can be used during execution at the remote end
        post.addHeader(ORGANIZATION_HEADER, securityService.getOrganization().getId());
        post.addHeader(USER_HEADER, securityService.getUser().getUsername());

        List<BasicNameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("id", Long.toString(job.getId())));
        params.add(new BasicNameValuePair("operation", job.getOperation()));
        post.setEntity(new UrlEncodedFormEntity(params, UTF_8));

        // Post the request
        HttpResponse response = null;
        int responseStatusCode;
        try {
          logger.debug("Trying to dispatch {} type '{}' load {} to {}", job, job.getJobType(), job.getJobLoad(),
              registration.getHost());
          if (!ServiceRegistryJpaImpl.START_WORKFLOW.equals(job.getOperation())) {
            serviceRegistry.setCurrentJob(job.toJob());
          }
          response = client.execute(post);
          responseStatusCode = response.getStatusLine().getStatusCode();
          if (responseStatusCode == HttpStatus.SC_NO_CONTENT) {
            return registration.getHost();
          } else if (responseStatusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
            logger.debug("Service {} is currently refusing to accept jobs of type {}", registration, job.getOperation());
            continue;
          } else if (responseStatusCode == HttpStatus.SC_PRECONDITION_FAILED) {
            job.setStatus(Job.Status.FAILED);
            job = serviceRegistry.updateJob(job); // will open a tx
            logger.debug("Service {} refused to accept {}", registration, job);
            throw new UndispatchableJobException(IOUtils.toString(response.getEntity().getContent()));
          } else if (responseStatusCode == HttpStatus.SC_METHOD_NOT_ALLOWED) {
            logger.debug("Service {} is not yet reachable", registration);
            continue;
          } else {
            logger.warn("Service {} failed ({}) accepting {}", registration, responseStatusCode, job);
            continue;
          }
        } catch (UndispatchableJobException e) {
          throw e;
        } catch (TrustedHttpClientException e) {
          // Will try another node. If no other node, it will be re-queued
          logger.warn("Unable to dispatch {}", job, e);
          continue;
        } catch (Exception e) {
          logger.warn("Unable to dispatch {}", job, e);
        } finally {
          try {
            client.close(response);
          } catch (IOException e) {
            // ignore
          }
          serviceRegistry.setCurrentJob(null);
        }
      }

      // We've tried dispatching to every online service that can handle this type of job, with no luck.
      if (triedDispatching) {
        // Workflow type jobs are not set to priority list, because they handle accepting jobs not based on the job load
        // If the system don't accepts jobs whose load exceeds the host's max load we can't make use of the priority
        // list
        if (serviceRegistry.acceptJobLoadsExeedingMaxLoad && !dispatchPriorityList.containsKey(job.getId()) && !ServiceRegistryJpaImpl.TYPE_WORKFLOW.equals(job.getJobType())
            && job.getProcessorServiceRegistration() != null) {
          String host = job.getProcessorServiceRegistration().getHost();
          logger.debug("About to add {} to dispatchPriorityList with processor host {}", job, host);
          dispatchPriorityList.put(job.getId(), host);
        }

        try {
          job.setStatus(Job.Status.QUEUED);
          job.setProcessorServiceRegistration(null);
          job = serviceRegistry.updateJob(job); // will open a tx
        } catch (Exception e) {
          logger.error("Unable to put {} back into queue", job, e);
        }
      }

      logger.debug("Unable to dispatch {}, no service is currently ready to accept the job", job);
      throw new UndispatchableJobException(job + " is currently undispatchable");
    }

    /**
     * Return dispatchable job ids, where the job status is RESTART or QUEUED and the job id is listed in the given set.
     *
     * @param jobIds set with job id's interested in
     * @return list with dispatchable job id's from the given set, with job status RESTART or QUEUED
     */
    protected Function<EntityManager, List<Long>> getDispatchableJobsWithIdFilterQuery(Set<Long> jobIds) {
      return em -> {
        if (jobIds == null || jobIds.isEmpty()) {
          return Collections.emptyList();
        }

        return namedQuery.findAll(
            "Job.dispatchable.status.idfilter",
            Long.class,
            Pair.of("jobids", dispatchPriorityList.keySet()),
            Pair.of("statuses", List.of(
                Job.Status.RESTART.ordinal(),
                Job.Status.QUEUED.ordinal()
            ))
        ).apply(em);
      };
    }

    private final Function<ServiceRegistration, HostRegistration> toHostRegistration = new Function<ServiceRegistration, HostRegistration>() {
      @Override
      public HostRegistration apply(ServiceRegistration s) {
        return ((ServiceRegistrationJpaImpl) s).getHostRegistration();
      }
    };

    private final Function<HostRegistration, Float> toMaxLoad = new Function<HostRegistration, Float>() {
      @Override
      public Float apply(HostRegistration h) {
        return h.getMaxLoad();
      }
    };

    private final Comparator<Float> sortFloatValuesDesc = new Comparator<Float>() {
      @Override
      public int compare(Float o1, Float o2) {
        return o2.compareTo(o1);
      }
    };
  }
}
