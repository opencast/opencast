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

import static java.lang.String.format;
import static org.opencastproject.assetmanager.api.fn.Enrichments.enrich;
import static org.opencastproject.util.RequireUtil.notEmpty;

import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.fn.Snapshots;
import org.opencastproject.assetmanager.api.query.ADeleteQuery;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.RichAResult;
import org.opencastproject.assetmanager.api.query.Target;
import org.opencastproject.assetmanager.impl.query.AbstractADeleteQuery.DeleteSnapshotHandler;
import org.opencastproject.index.IndexProducer;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.MessageSender.DestinationType;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.TakeSnapshot;
import org.opencastproject.message.broker.api.index.AbstractIndexProducer;
import org.opencastproject.message.broker.api.index.IndexRecreateObject;
import org.opencastproject.message.broker.api.index.IndexRecreateObject.Service;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Bind an asset manager to ActiveMQ messaging.
 * <p>
 * Please make sure to {@link #close()} the AssetManager.
 */
public class AssetManagerWithMessaging extends AssetManagerDecorator<TieredStorageAssetManager>
        implements DeleteSnapshotHandler, IndexProducer, AutoCloseable {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AssetManagerWithMessaging.class);

  private final MessageSender messageSender;
  private final MessageReceiver messageReceiver;
  private final AuthorizationService authSvc;
  private final OrganizationDirectoryService orgDir;
  private final SecurityService secSvc;
  private final Workspace workspace;

  private final AbstractIndexProducer indexProducerMsgReceiver;

  public AssetManagerWithMessaging(final TieredStorageAssetManager delegate, final MessageSender messageSender,
          MessageReceiver messageReceiver, AuthorizationService authSvc, OrganizationDirectoryService orgDir,
          SecurityService secSvc, Workspace workspace, final String systemUserName) {
    super(delegate);
    this.messageSender = messageSender;
    this.messageReceiver = messageReceiver;
    this.authSvc = authSvc;
    this.orgDir = orgDir;
    this.secSvc = secSvc;
    this.workspace = workspace;
    this.indexProducerMsgReceiver = new AbstractIndexProducer() {
      @Override
      public String getClassName() {
        return AssetManagerWithMessaging.class.getName();
      }

      @Override
      public MessageReceiver getMessageReceiver() {
        return AssetManagerWithMessaging.this.messageReceiver;
      }

      @Override
      public Service getService() {
        return Service.AssetManager;
      }

      @Override
      public MessageSender getMessageSender() {
        return AssetManagerWithMessaging.this.messageSender;
      }

      @Override
      public SecurityService getSecurityService() {
        return AssetManagerWithMessaging.this.secSvc;
      }

      @Override
      public String getSystemUserName() {
        return systemUserName;
      }

      @Override
      public void repopulate(final String indexName) {
        notEmpty(indexName, "indexName");
        final Organization org = getSecurityService().getOrganization();
        final User user = (org != null ? getSecurityService().getUser() : null);
        try {
          final Organization defaultOrg = new DefaultOrganization();
          final User systemUser = SecurityUtil.createSystemUser(getSystemUserName(), defaultOrg);
          getSecurityService().setOrganization(defaultOrg);
          getSecurityService().setUser(systemUser);

          final AQueryBuilder q = delegate.createQuery();
          final RichAResult r = enrich(q.select(q.snapshot()).where(q.version().isLatest()).run());
          final int total = r.countSnapshots();
          logger.info(format("Populating index '%s' with %d snapshots | start", indexName, total));
          final int responseInterval = (total < 100) ? 1 : (total / 100);
          int current = 0;

          final Map<String, List<Snapshot>> byOrg = r.getSnapshots().groupMulti(Snapshots.getOrganizationId);
          for (String orgId : byOrg.keySet()) {
            final Organization snapshotOrg;
            try {
              snapshotOrg = AssetManagerWithMessaging.this.orgDir.getOrganization(orgId);
              getSecurityService().setOrganization(snapshotOrg);
              getSecurityService().setUser(SecurityUtil.createSystemUser(systemUserName, snapshotOrg));

              for (Snapshot snapshot : byOrg.get(orgId)) {
                current += 1;
                try {
                  TakeSnapshot takeSnapshot = mkTakeSnapshotMessage(snapshot, null);
                  getMessageSender().sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE_PREFIX + WordUtils.capitalize(indexName),
                          MessageSender.DestinationType.Queue, takeSnapshot);
                } catch (Throwable t) {
                  logger.error("Unable to recreate event {} from organization {}",
                          snapshot.getMediaPackage().getIdentifier().compact(), orgId, t);
                }
                if (((current % responseInterval) == 0) || (current == total)) {
                  getMessageSender().sendObjectMessage(IndexProducer.RESPONSE_QUEUE,
                          MessageSender.DestinationType.Queue, IndexRecreateObject.update(indexName, getService(),
                                  total, current));
                }
              }
            } catch (Throwable t) {
              logger.error("Unable to recreate event index for organization {}", orgId, t);
            } finally {
              getSecurityService().setOrganization(defaultOrg);
              getSecurityService().setUser(systemUser);
            }
          }
          getMessageSender().sendObjectMessage(IndexProducer.RESPONSE_QUEUE, MessageSender.DestinationType.Queue,
                  IndexRecreateObject.end(indexName, getService()));

        } finally {
          getSecurityService().setOrganization(org);
          getSecurityService().setUser(user);
        }
      }
    };
    this.indexProducerMsgReceiver.activate();
  }

  @Override
  public void close() throws Exception {
    indexProducerMsgReceiver.deactivate();
  }

  @Override
  public Snapshot takeSnapshot(String owner, MediaPackage mp) {
    final Snapshot snapshot = super.takeSnapshot(owner, mp);
    // We pass the original media package here, instead of using snapshot.getMediaPackage(), for security reasons.
    // The original media package has elements with URLs of type http://.../files/... in it. These URLs will be pulled
    // from the Workspace cache without a HTTP call.
    // Were we to use snapshot.getMediaPackage(), we'd have a HTTP call on our hands that's secured via the asset
    // manager security model. But the snapshot taken here doesn't have the necessary security properties installed (yet).
    // This happens in AssetManagerWithSecurity, some layers higher up. So there's a weird loop in here.
    notifyTakeSnapshot(snapshot, mp);
    return snapshot;
  }

  @Override
  public AQueryBuilder createQuery() {
    return new AQueryBuilderDecorator(super.createQuery()) {
      @Override
      public ADeleteQuery delete(String owner, Target target) {
        return new ADeleteQueryWithMessaging(super.delete(owner, target));
      }
    };
  }

  public void notifyTakeSnapshot(Snapshot snapshot, MediaPackage mp) {
    logger.info(format("Send update message for snapshot %s, %s to ActiveMQ",
            snapshot.getMediaPackage().getIdentifier().toString(), snapshot.getVersion()));
    messageSender.sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE, DestinationType.Queue,
            mkTakeSnapshotMessage(snapshot, mp));
  }

  @Override
  public void notifyDeleteSnapshot(String mpId, VersionImpl version) {
    logger.info(format("Send delete message for snapshot %s, %s to ActiveMQ", mpId, version));
    messageSender.sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE, DestinationType.Queue,
            AssetManagerItem.deleteSnapshot(mpId, version.value(), new Date()));
  }

  @Override
  public void notifyDeleteEpisode(String mpId) {
    logger.info(format("Send delete message for episode %s to ActiveMQ", mpId));
    messageSender.sendObjectMessage(AssetManagerItem.ASSETMANAGER_QUEUE, DestinationType.Queue,
            AssetManagerItem.deleteEpisode(mpId, new Date()));
  }

  /**
   * Create a {@link TakeSnapshot} message.
   * <p>
   * Do not call outside of a security context.
   */
  private TakeSnapshot mkTakeSnapshotMessage(Snapshot snapshot, MediaPackage mp) {
    final MediaPackage chosenMp;
    if (mp != null) {
      chosenMp = mp;
    } else {
      chosenMp = snapshot.getMediaPackage();
    }
    return AssetManagerItem.add(workspace, chosenMp, authSvc.getActiveAcl(chosenMp).getA(), getVersionLong(snapshot),
            snapshot.getArchivalDate());
  }

  private long getVersionLong(Snapshot snapshot) {
    try {
      return Long.parseLong(snapshot.getVersion().toString());
    } catch (NumberFormatException e) {
      // The index requires a version to be a long value.
      // Since the asset manager default implementation uses long values that should be not a problem.
      // However, a decent exception message is helpful if a different implementation of the asset manager
      // is used.
      throw new RuntimeException("The current implementation of the index requires versions being of type 'long'.");
    }
  }

  /*
   * ------------------------------------------------------------------------------------------------------------------
   */

  @Override
  public void repopulate(String indexName) throws Exception {
    indexProducerMsgReceiver.repopulate(indexName);
  }

  /*
   * ------------------------------------------------------------------------------------------------------------------
   */

  /**
   * Call {@link org.opencastproject.assetmanager.impl.query.AbstractADeleteQuery#run(DeleteSnapshotHandler)} with a
   * delete handler that sends messages to ActiveMQ. Also make sure to propagate the behaviour to subsequent instances.
   */
  private final class ADeleteQueryWithMessaging extends ADeleteQueryDecorator {
    ADeleteQueryWithMessaging(ADeleteQuery delegate) {
      super(delegate);
    }

    @Override
    public long run() {
      return RuntimeTypes.convert(delegate).run(AssetManagerWithMessaging.this);
    }

    @Override
    protected ADeleteQueryDecorator mkDecorator(ADeleteQuery delegate) {
      return new ADeleteQueryWithMessaging(delegate);
    }
  }
}
