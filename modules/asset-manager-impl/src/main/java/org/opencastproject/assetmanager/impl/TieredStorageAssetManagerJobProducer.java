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
package org.opencastproject.assetmanager.impl;

import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.job.api.AbstractJobProducer;
import org.opencastproject.job.api.Job;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.serviceregistry.api.ServiceRegistryException;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.RequireUtil;

import com.entwinemedia.fn.data.Opt;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class TieredStorageAssetManagerJobProducer extends AbstractJobProducer {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(TieredStorageAssetManagerJobProducer.class);

  public static final String JOB_TYPE = "org.opencastproject.assetmanager";
  public static final Float JOB_LOAD = 1.0f;
  public static final Float NONTERMINAL_JOB_LOAD = 0.1f;

  public enum Operation {
    MoveById, MoveByIdAndVersion, MoveByIdAndDate, MoveByDate
  }

  private static final String OK = "OK";

  private TieredStorageAssetManager tsam = null;
  private ServiceRegistry serviceRegistry = null;
  private SecurityService securityService = null;
  private UserDirectoryService userDirectoryService = null;
  private OrganizationDirectoryService organizationDirectoryService = null;

  public TieredStorageAssetManagerJobProducer() {
    super(JOB_TYPE);
  }

  /**
   * OSGi callback on component activation.
   *
   * @param cc
   *          the component context
   */
  @Override
  public void activate(ComponentContext cc) {
    logger.info("Activating tiered storage assetmanager job service");
    super.activate(cc);
  }

  public boolean datastoreExists(String storeId) {
    Opt<AssetStore> store = tsam.getAssetStore(storeId);
    return store.isSome();
  }

  @Override
  protected String process(Job job) throws ServiceRegistryException {
    Operation op = null;
    String operation = job.getOperation();
    List<String> arguments = job.getArguments();
    String id;
    String targetStore = arguments.get(0);
    VersionImpl version;
    Date start;
    Date end;
    boolean bypass;
    Set<Job> subjobs;
    try {
      op = Operation.valueOf(operation);
      switch (op) {
        case MoveById:
          id = arguments.get(1);
          return internalMoveById(id, targetStore);
        case MoveByIdAndVersion:
          id = arguments.get(1);
          version = VersionImpl.mk(Long.parseLong(arguments.get(2)));
          return internalMoveByIdAndVersion(version, id, targetStore);
        case MoveByDate:
          start = new Date(Long.parseLong(arguments.get(1)));
          end = new Date(Long.parseLong(arguments.get(2)));
          return internalMoveByDate(start, end, targetStore);
        case MoveByIdAndDate:
          id = arguments.get(1);
          start = new Date(Long.parseLong(arguments.get(2)));
          end = new Date(Long.parseLong(arguments.get(3)));
          return internalMoveByIdAndDate(id, start, end, targetStore);
        default:
          throw new IllegalArgumentException("Unknown operation '" + operation + "'");
      }
    } catch (NotFoundException e) {
      throw new ServiceRegistryException("Error running job", e);
    } catch (Exception e) {
      throw new ServiceRegistryException("Error handling operation '" + op + "'", e);
    }
  }

  /**
   * Spawns a job to move a single snapshot from its current storage to a new target storage location
   *
   * @param version
   *  The {@link Version} to move
   * @param mpId
   *  The mediapackage ID of the snapshot to move
   * @param targetStorage
   *  The {@link org.opencastproject.assetmanager.impl.storage.RemoteAssetStore} ID where the snapshot should be moved
   * @return
   */
  public Job moveByIdAndVersion(final Version version, final String mpId, final String targetStorage) {
    RequireUtil.notNull(version, "version");
    RequireUtil.notEmpty(mpId, "mpId");
    RequireUtil.notEmpty(targetStorage, "targetStorage");
    List<String> args = new LinkedList<String>();
    args.add(targetStorage);
    args.add(mpId);
    args.add(version.toString());

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.MoveByIdAndVersion.toString(), args, null, true, JOB_LOAD);
    } catch (ServiceRegistryException e) {
      throw new AssetManagerException("Unable to create a job", e);
    }
  }

  /**
   * Triggers the move operation inside the {@link TieredStorageAssetManager}
   *
   * @param version
   *  The {@link Version} to move
   * @param mpId
   *  The mediapackage ID of the snapshot to move
   * @param targetStorage
   *  The {@link org.opencastproject.assetmanager.impl.storage.RemoteAssetStore} ID where the snapshot should be moved
   * @return
   *  The string "OK"
   * @throws NotFoundException
   */
  protected String internalMoveByIdAndVersion(final VersionImpl version, final String mpId, final String targetStorage) throws
          NotFoundException {
    tsam.moveSnapshotToStore(version, mpId, targetStorage);
    return OK;
  }

  /**
   * Spawns a job to move a all snapshots of a mediapackage from their current storage to a new target storage location
   *
   * @param mpId
   *  The mediapackage ID of the snapshot to move
   * @param targetStorage
   *  The {@link org.opencastproject.assetmanager.impl.storage.RemoteAssetStore} ID where the snapshot should be moved
   * @return
   *  The {@link Job}
   */
  public Job moveById(final String mpId, final String targetStorage) {
    RequireUtil.notEmpty(mpId, "mpId");
    RequireUtil.notEmpty(targetStorage, "targetStorage");
    List<String> args = new LinkedList<String>();
    args.add(targetStorage);
    args.add(mpId);

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.MoveById.toString(), args, null, true, NONTERMINAL_JOB_LOAD);
    } catch (ServiceRegistryException e) {
      throw new AssetManagerException("Unable to create a job", e);
    }
  }

  /**
   * Spawns subjobs on a per-snapshot level to move the appropriate snapshots to their new home
   *
   * @param mpId
   *  The mediapackage ID of the snapshot to move
   * @param targetStorage
   *  The {@link org.opencastproject.assetmanager.impl.storage.RemoteAssetStore} ID where the snapshot should be moved
   * @return
   *  The number of subjobs spawned
   */
  protected String internalMoveById(final String mpId, final String targetStorage) {
    RichAResult results = tsam.getSnapshotsById(mpId);
    List<Job> subjobs = spawnSubjobs(results, targetStorage);
    return Integer.toString(subjobs.size());
  }


  /**
   * Spawns a job to move a all snapshots taken between two points from their current storage to a new target storage location
   *
   * @param start
   *  The start {@link Date}
   * @param end
   *  The end {@link Date}
   * @param targetStorage
   *  The {@link org.opencastproject.assetmanager.impl.storage.RemoteAssetStore} ID where the snapshot should be moved
   * @return
   *  The {@link Job}
   */
  public Job moveByDate(final Date start, final Date end, final String targetStorage) {
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    RequireUtil.notNull(targetStorage, "targetStorage");
    List<String> args = new LinkedList<String>();
    args.add(targetStorage);
    args.add(Long.toString(start.getTime()));
    args.add(Long.toString(end.getTime()));

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.MoveByDate.toString(), args, null, true, NONTERMINAL_JOB_LOAD);
    } catch (ServiceRegistryException e) {
      throw new AssetManagerException("Unable to create a job", e);
    }
  }

  /**
   * Spawns subjobs on a per-snapshot level to move the appropriate snapshots to their new home
   *
   * @param start
   *  The start {@link Date}
   * @param end
   *  The end {@link Date}
   * @param targetStorage
   *  The {@link org.opencastproject.assetmanager.impl.storage.RemoteAssetStore} ID where the snapshot should be moved
   * @return
   *  The number of subjobs spawned
   */
  protected String internalMoveByDate(final Date start, final Date end, final String targetStorage) {
    RichAResult results = tsam.getSnapshotsByDate(start, end);
    List<Job> subjobs = spawnSubjobs(results, targetStorage);
    return Integer.toString(subjobs.size());
  }

  /**
   * Spawns a job to move a all snapshots of a given mediapackage taken between two points from their current storage to a new target storage location
   *
   * @param mpId
   *  The mediapackage ID of the snapshot to move
   * @param start
   *  The start {@link Date}
   * @param end
   *  The end {@link Date}
   * @param targetStorage
   *  The {@link org.opencastproject.assetmanager.impl.storage.RemoteAssetStore} ID where the snapshot should be moved
   * @return
   *  The {@link Job}
   */
  public Job moveByIdAndDate(final String mpId, final Date start, final Date end, final String targetStorage) {
    RequireUtil.notNull(mpId, "mpId");
    RequireUtil.notNull(start, "start");
    RequireUtil.notNull(end, "end");
    RequireUtil.notNull(targetStorage, "targetStorage");
    List<String> args = new LinkedList<String>();
    args.add(targetStorage);
    args.add(mpId);
    args.add(Long.toString(start.getTime()));
    args.add(Long.toString(end.getTime()));

    try {
      return serviceRegistry.createJob(JOB_TYPE, Operation.MoveByIdAndDate.toString(), args, null, true, NONTERMINAL_JOB_LOAD);
    } catch (ServiceRegistryException e) {
      throw new AssetManagerException("Unable to create a job", e);
    }
  }

  /**
   * Spawns subjobs on a per-snapshot level to move the appropriate snapshots to their new home
   *
   * @param mpId
   *  The mediapackage ID of the snapshot to move
   * @param start
   *  The start {@link Date}
   * @param end
   *  The end {@link Date}
   * @param targetStorage
   *  The {@link org.opencastproject.assetmanager.impl.storage.RemoteAssetStore} ID where the snapshot should be moved
   * @return
   *  The number of subjobs spawned
   */
  protected String internalMoveByIdAndDate(final String mpId, final Date start, final Date end, final String targetStorage) {
    RichAResult results = tsam.getSnapshotsByIdAndDate(mpId, start, end);
    List<Job> subjobs = spawnSubjobs(results, targetStorage);
    return Integer.toString(subjobs.size());
  }

  /**
   * Spawns the subjobs based on the stream of records
   *
   * @param records
   *  The stream of records containing the snapshots to move to the new target storage
   * @param targetStorage
   *  The {@link org.opencastproject.assetmanager.impl.storage.RemoteAssetStore} ID where the snapshot should be moved
   * @return
   *  The set of subjobs
   */
  private List<Job> spawnSubjobs(final RichAResult records, final String targetStorage) {
    List<Job> jobs = new LinkedList<>();
    records.forEach(new Consumer<ARecord>() {
      @Override
      public void accept(ARecord record) {
        Snapshot snap = record.getSnapshot().get();
        jobs.add(moveByIdAndVersion(snap.getVersion(), snap.getMediaPackage().getIdentifier().toString(), targetStorage));
      }
    });
    return jobs;
  }

  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  protected ServiceRegistry getServiceRegistry() {
    return this.serviceRegistry;
  }

  protected void setAssetManager(TieredStorageAssetManager assetManager) {
    this.tsam = assetManager;
  }

  protected void setSecurityService(SecurityService securityService) {
    this.securityService = securityService;
  }

  @Override
  protected SecurityService getSecurityService() {
    return this.securityService;
  }

  protected void setUserDirectoryService(UserDirectoryService uds) {
    this.userDirectoryService = uds;
  }

  @Override
  protected UserDirectoryService getUserDirectoryService() {
    return this.userDirectoryService;
  }


  protected void setOrganizationDirectoryService(OrganizationDirectoryService os) {
    this.organizationDirectoryService = os;
  }

  @Override
  protected OrganizationDirectoryService getOrganizationDirectoryService() {
    return this.organizationDirectoryService;
  }
}
