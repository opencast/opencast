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

import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.fn.Snapshots;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.RemoteAssetStore;
import org.opencastproject.elasticsearch.index.AbstractSearchIndex;
import org.opencastproject.index.rebuild.AbstractIndexProducer;
import org.opencastproject.index.rebuild.IndexProducer;
import org.opencastproject.index.rebuild.IndexRebuildException;
import org.opencastproject.index.rebuild.IndexRebuildService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

/**
 * Ties the asset manager to the OSGi environment.
 * <p>
 * Composes the core asset manager with the {@link AssetManagerWithMessaging} and {@link AssetManagerWithSecurity}
 * implementations.
 */
@Component(
    property = {
        "service.description=Opencast Asset Manager"
    },
    immediate = true,
    service = { AssetManager.class, TieredStorageAssetManager.class, IndexProducer.class }
)
public class OsgiAssetManager extends AbstractIndexProducer implements AssetManager, TieredStorageAssetManager {
  /**
   * Log facility
   */
  private static final Logger logger = LoggerFactory.getLogger(OsgiAssetManager.class);

  private SecurityService secSvc;
  private AuthorizationService authSvc;
  private OrganizationDirectoryService orgDir;
  private Workspace workspace;
  private AssetStore assetStore;
  private HttpAssetProvider httpAssetProvider;
  private MessageSender messageSender;
  private MessageReceiver messageReceiver;
  private EntityManagerFactory emf;
  private List<RemoteAssetStore> remotes = new LinkedList<>();
  private String systemUserName;
  private AssetManagerWithMessaging withMessaging;

  // collect all objects that need to be closed on service deactivation
  private AutoCloseable toClose;

  private TieredStorageAssetManager delegate;

  /**
   * OSGi callback.
   */
  @Activate
  public synchronized void activate(ComponentContext cc) {
    logger.info("Activating AssetManager");
    final Database db = new Database(emf);
    systemUserName = SecurityUtil.getSystemUserName(cc);
    // create the core asset manager
    final AbstractAssetManagerWithTieredStorage core = new AbstractAssetManagerWithTieredStorage() {
      private HashMap<String, RemoteAssetStore> remoteStores = new LinkedHashMap<>();

      @Override
      public Database getDb() {
        return db;
      }

      @Override
      public HttpAssetProvider getHttpAssetProvider() {
        return httpAssetProvider;
      }

      @Override
      public AssetStore getLocalAssetStore() {
        return assetStore;
      }

      @Override
      public Set<String> getRemoteAssetStoreIds() {
        return remoteStores.keySet();
      }

      @Override
      public Opt<AssetStore> getRemoteAssetStore(String id) {
        if (remoteStores.containsKey(id)) {
          return Opt.some(remoteStores.get(id));
        } else {
          return Opt.none();
        }
      }

      @Override
      public void addRemoteAssetStore(RemoteAssetStore store) {
        remoteStores.put(store.getStoreType(), store);
      }

      @Override
      public void removeRemoteAssetStore(RemoteAssetStore store) {
        remoteStores.remove(store.getStoreType());
      }

      @Override
      protected Workspace getWorkspace() {
        return workspace;
      }

      @Override
      protected String getCurrentOrgId() {
        return secSvc.getOrganization().getId();
      }
    };
    // compose with ActiveMQ messaging
    withMessaging = new AssetManagerWithMessaging(core, messageSender, authSvc, workspace);
    // compose with security
    boolean includeAPIRoles = BooleanUtils.toBoolean(Objects.toString(cc.getProperties().get("includeAPIRoles"), null));
    boolean includeCARoles = BooleanUtils.toBoolean(Objects.toString(cc.getProperties().get("includeCARoles"), null));
    boolean includeUIRoles = BooleanUtils.toBoolean(Objects.toString(cc.getProperties().get("includeUIRoles"), null));
    delegate = new AssetManagerWithSecurity(
        withMessaging, authSvc, secSvc, includeAPIRoles, includeCARoles, includeUIRoles);
    for (RemoteAssetStore ras : remotes) {
      delegate.addRemoteAssetStore(ras);
    }
    remotes.clear();
    // collect all objects that need to be closed
    toClose = new AutoCloseable() {
      @Override
      public void close() throws Exception {
        withMessaging.close();
      }
    };
  }

  /**
   * OSGi callback. Close the database.
   */
  @Deactivate
  public void deactivate(ComponentContext cc) throws Exception {
    toClose.close();
  }

  //
  // AssetManager impl
  //

  @Override
  public Snapshot takeSnapshot(String owner, MediaPackage mp) {
    return delegate.takeSnapshot(owner, mp);
  }

  @Override
  public Snapshot takeSnapshot(MediaPackage mp) {
    return delegate.takeSnapshot(mp);
  }

  @Override
  public Opt<Asset> getAsset(Version version, String mpId, String mpeId) {
    return delegate.getAsset(version, mpId, mpeId);
  }

  @Override
  public Opt<MediaPackage> getMediaPackage(String mediaPackageId) {
    return delegate.getMediaPackage(mediaPackageId);
  }

  @Override
  public void setAvailability(Version version, String mpId, Availability availability) {
    delegate.setAvailability(version, mpId, availability);
  }

  @Override
  public boolean setProperty(Property property) {
    return delegate.setProperty(property);
  }

  @Override
  public int deleteProperties(final String mediaPackageId) {
    return delegate.deleteProperties(mediaPackageId);
  }

  @Override
  public int deleteProperties(final String mediaPackageId, final String namespace) {
    return delegate.deleteProperties(mediaPackageId, namespace);
  }

  @Override
  public long countEvents(String organization) {
    return delegate.countEvents(organization);
  }

  @Override
  public boolean snapshotExists(final String mediaPackageId) {
    return delegate.snapshotExists(mediaPackageId);
  }

  @Override
  public boolean snapshotExists(final String mediaPackageId, final String organization) {
    return delegate.snapshotExists(mediaPackageId, organization);
  }

  @Override
  public List<Property> selectProperties(final String mediaPackageId, final String namespace) {
    return delegate.selectProperties(mediaPackageId, namespace);
  }

  @Override
  public AQueryBuilder createQuery() {
    return delegate.createQuery();
  }

  @Override
  public Opt<Version> toVersion(String version) {
    return delegate.toVersion(version);
  }

  //
  // OSGi depedency injection
  //

  @Reference(name = "entityManagerFactory", target = "(osgi.unit.name=org.opencastproject.assetmanager.impl)")
  public void setEntityManagerFactory(EntityManagerFactory emf) {
    this.emf = emf;
  }

  @Reference(name = "securityService")
  public void setSecurityService(SecurityService securityService) {
    this.secSvc = securityService;
  }

  @Reference(name = "authSvc")
  public void setAuthSvc(AuthorizationService authSvc) {
    this.authSvc = authSvc;
  }

  @Reference(name = "orgDir")
  public void setOrgDir(OrganizationDirectoryService orgDir) {
    this.orgDir = orgDir;
  }

  @Reference(name = "workspace")
  public void setWorkspace(Workspace workspace) {
    this.workspace = workspace;
  }

  @Reference(name = "assetStore")
  public void setAssetStore(AssetStore assetStore) {
    this.assetStore = assetStore;
  }

  @Reference(
      name = "remoteAssetStores",
      cardinality = ReferenceCardinality.MULTIPLE,
      policy = ReferencePolicy.DYNAMIC,
      unbind = "removeRemoteAssetStore"
  )
  public synchronized void addRemoteAssetStore(RemoteAssetStore assetStore) {
    if (null == delegate) {
      remotes.add(assetStore);
    } else {
      delegate.addRemoteAssetStore(assetStore);
    }
  }

  public synchronized void removeRemoteAssetStore(RemoteAssetStore assetStore) {
    if (null != delegate) {
      delegate.removeRemoteAssetStore(assetStore);
    } else {
      logger.warn("Unable to remove remote store of type {} because delegate is null!", assetStore.getStoreType());
    }
  }

  @Reference(name = "httpAssetProvider")
  public void setHttpAssetProvider(HttpAssetProvider httpAssetProvider) {
    this.httpAssetProvider = httpAssetProvider;
  }

  @Reference(name = "messageSender")
  public void setMessageSender(MessageSender messageSender) {
    this.messageSender = messageSender;
  }

  @Reference(name = "messageReceiver")
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  @Override
  public Set<String> getRemoteAssetStoreIds() {
    return delegate.getRemoteAssetStoreIds();
  }

  @Override
  public Opt<AssetStore> getRemoteAssetStore(String id) {
    return delegate.getRemoteAssetStore(id);
  }

  @Override
  public Opt<AssetStore> getAssetStore(String storeId) {
    return delegate.getAssetStore(storeId);
  }

  @Override
  public void moveSnapshotToStore(Version version, String mpId, String storeId) throws NotFoundException {
    delegate.moveSnapshotToStore(version, mpId, storeId);
  }

  @Override
  public RichAResult getSnapshotsById(String mpId) {
    return delegate.getSnapshotsById(mpId);
  }

  @Override
  public void moveSnapshotsById(String mpId, String targetStore) throws NotFoundException {
    delegate.moveSnapshotsById(mpId, targetStore);
  }

  @Override
  public RichAResult getSnapshotsByIdAndVersion(String mpId, Version version) {
    return delegate.getSnapshotsByIdAndVersion(mpId, version);
  }

  @Override
  public void moveSnapshotsByIdAndVersion(String mpId, Version version, String targetStore) throws NotFoundException {
    delegate.moveSnapshotsByIdAndVersion(mpId, version, targetStore);
  }

  @Override
  public RichAResult getSnapshotsByDate(Date start, Date end) {
    return delegate.getSnapshotsByDate(start, end);
  }

  @Override
  public void moveSnapshotsByDate(Date start, Date end, String targetStore) throws NotFoundException {
    delegate.moveSnapshotsByDate(start, end, targetStore);
  }

  @Override
  public RichAResult getSnapshotsByIdAndDate(String mpId, Date start, Date end) {
    return delegate.getSnapshotsByIdAndDate(mpId, start, end);
  }

  @Override
  public void moveSnapshotsByIdAndDate(String mpId, Date start, Date end, String targetStore) throws NotFoundException {
    delegate.moveSnapshotsByIdAndDate(mpId, start, end, targetStore);
  }

  @Override
  public Opt<String> getSnapshotStorageLocation(Version version, String mpId) throws NotFoundException {
    return delegate.getSnapshotStorageLocation(version, mpId);
  }

  @Override
  public Opt<String> getSnapshotStorageLocation(Snapshot snap) throws NotFoundException {
    return delegate.getSnapshotStorageLocation(snap);
  }

  @Override
  public Opt<String> getSnapshotRetrievalTime(Version version, String mpId) {
    return delegate.getSnapshotRetrievalTime(version, mpId);
  }

  @Override
  public Opt<String> getSnapshotRetrievalCost(Version version, String mpId) {
    return delegate.getSnapshotRetrievalCost(version, mpId);
  }

  /**
   * AbstractIndexProducer Implementation
   */

  @Override
  public IndexRebuildService.Service getService() {
    return IndexRebuildService.Service.AssetManager;
  }

  @Override
  public void repopulate(final AbstractSearchIndex index) throws IndexRebuildException {
    final Organization org = secSvc.getOrganization();
    final User user = (org != null ? secSvc.getUser() : null);
    try {
      final Organization defaultOrg = new DefaultOrganization();
      final User systemUser = SecurityUtil.createSystemUser(systemUserName, defaultOrg);
      secSvc.setOrganization(defaultOrg);
      secSvc.setUser(systemUser);

      final AQueryBuilder q = delegate.createQuery();
      final RichAResult r = enrich(q.select(q.snapshot()).where(q.version().isLatest()).run());
      final int total = r.countSnapshots();
      int current = 0;
      logIndexRebuildBegin(logger, index.getIndexName(), total, "snapshot(s)");

      final Map<String, List<Snapshot>> byOrg = r.getSnapshots().groupMulti(Snapshots.getOrganizationId);
      for (String orgId : byOrg.keySet()) {
        final Organization snapshotOrg;
        try {
          snapshotOrg = orgDir.getOrganization(orgId);
          secSvc.setOrganization(snapshotOrg);
          secSvc.setUser(SecurityUtil.createSystemUser(systemUserName, snapshotOrg));

          for (Snapshot snapshot : byOrg.get(orgId)) {
            current += 1;
            try {
              AssetManagerItem.TakeSnapshot takeSnapshot = withMessaging.mkTakeSnapshotMessage(snapshot, null);
              messageSender.sendObjectMessage(
                      AssetManagerItem.ASSETMANAGER_QUEUE_PREFIX + WordUtils.capitalize(index.getIndexName()),
                      MessageSender.DestinationType.Queue, takeSnapshot);
            } catch (Throwable t) {
              logSkippingElement(logger, "event", snapshot.getMediaPackage().getIdentifier().toString(), org, t);
            }
            logIndexRebuildProgress(logger, index.getIndexName(), total, current);
          }
        } catch (Throwable t) {
          logIndexRebuildError(logger, index.getIndexName(), t, org);
          throw new IndexRebuildException(index.getIndexName(), getService(), org, t);
        } finally {
          secSvc.setOrganization(defaultOrg);
          secSvc.setUser(systemUser);
        }
      }
    } finally {
      secSvc.setOrganization(org);
      secSvc.setUser(user);
    }
  }
}
