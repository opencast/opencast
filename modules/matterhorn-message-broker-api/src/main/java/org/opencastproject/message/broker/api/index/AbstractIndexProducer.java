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

package org.opencastproject.message.broker.api.index;

import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;

import org.opencastproject.index.IndexProducer;
import org.opencastproject.message.broker.api.BaseMessage;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.util.data.Effect0;

import com.entwinemedia.fn.P1;
import com.entwinemedia.fn.Products;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Booleans;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

/**
 * This service produces messages for an elastic search index
 */
public abstract class AbstractIndexProducer implements IndexProducer {
  public static final P1<Serializable> IDENTITY_MSG = Products.E.<Serializable>p1(new Serializable() { });

  public abstract String getClassName();

  public abstract MessageReceiver getMessageReceiver();

  public abstract MessageSender getMessageSender();

  public abstract SecurityService getSecurityService();

  public abstract IndexRecreateObject.Service getService();

  public abstract String getSystemUserName();

  /** The message watcher */
  private MessageWatcher messageWatcher;

  /** Single thread executor */
  private final ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();

  /**
   * Initialize the index producer.
   */
  public void activate() {
    messageWatcher = new MessageWatcher();
    singleThreadExecutor.execute(messageWatcher);
  }

  /**
   * Clean-up resources at shutdown.
   */
  public void deactivate() {
    if (messageWatcher != null) {
      messageWatcher.stopListening();
    }
    singleThreadExecutor.shutdown();
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /**
   * Create a new batch.
   *
   * @param indexName
   *         the name of the index to recreate
   * @param queuePrefix
   *         the message queue prefix where messages are sent to
   * @param updatesTotal
   *         the number of updates that will be sent, i.e. how many times will the
   *         {@link org.opencastproject.message.broker.api.index.AbstractIndexProducer.IndexRecreationBatch#update(Organization, P1[])}
   *         method be called
   * @param endMessageOrg
   *         the organization under which the batch's end message should be sent;
   *         if none use the organization of the last update message
   */
  public IndexRecreationBatch mkRecreationBatch(String indexName, String queuePrefix, int updatesTotal,
                                                Opt<Organization> endMessageOrg) {
    return new IndexRecreationBatch(indexName, queuePrefix, updatesTotal, endMessageOrg);
  }

  /**
   * Create a new batch. The organization under which the final end message is sent is set to {@link DefaultOrganization}.
   *
   * @param indexName
   *         the name of the index to recreate
   * @param queuePrefix
   *         the message queue prefix where messages are sent to
   * @param updatesTotal
   *         the number of updates that will be sent, i.e. how many times will the
   *         {@link org.opencastproject.message.broker.api.index.AbstractIndexProducer.IndexRecreationBatch#update(Organization, P1[])}
   *         method be called
   * @see #mkRecreationBatch(String, String, int, Opt)
   */
  public IndexRecreationBatch mkRecreationBatch(String indexName, String queuePrefix, int updatesTotal) {
    return new IndexRecreationBatch(indexName, queuePrefix, updatesTotal, Opt.<Organization>some(new DefaultOrganization()));
  }

  /**
   * State management for a batch of recreate index update messages.
   * Messages are always sent under the identity of the system user.
   */
  public final class IndexRecreationBatch {
    private final Logger logger = LoggerFactory.getLogger(IndexRecreationBatch.class);

    private final String indexName;
    private final String destinationId;
    private final int updatesTotal;
    private final int responseInterval;
    private final Opt<Organization> endMessageOrg;

    private int updatesCurrent;

    /**
     * Create a new batch.
     *
     * @param indexName
     *         the name of the index to recreate
     * @param queuePrefix
     *         the message queue prefix where messages are sent to
     * @param updatesTotal
     *         the number of updates that will be sent, i.e. how many times will the {@link #update(Organization, P1[])}
     *         method be called
     * @param endMessageOrg
     *         the organization under which the batch's end message should be sent;
     *         if none use the organization of the last update message
     */
    private IndexRecreationBatch(String indexName, String queuePrefix, int updatesTotal,
                                 Opt<Organization> endMessageOrg) {
      this.indexName = indexName;
      this.destinationId = queuePrefix + WordUtils.capitalize(indexName);
      this.updatesTotal = RequireUtil.min(updatesTotal, 0);
      this.endMessageOrg = endMessageOrg;
      this.updatesCurrent = 0;
      this.responseInterval = (updatesTotal < 100) ? 1 : (updatesTotal / 100);
    }

    public int getUpdatesTotal() {
      return updatesTotal;
    }

    /**
     * Send one update to recreate the index. An update may consist of multiple messages.
     * Updates are sent under the identity of the system user of the given organization.
     * <p>
     * {@link #IDENTITY_MSG} is the identity element of messages, i.e. identity message will be filtered out
     */
    public void update(final Organization org, final Iterable<P1<? extends Serializable>> messages) {
      if (updatesCurrent < updatesTotal) {
        final User user = SecurityUtil.createSystemUser(getSystemUserName(), org);
        SecurityUtil.runAs(getSecurityService(), org, user, new Effect0() {
          @Override protected void run() {
            for (final P1<? extends Serializable> m : $(messages).filter(Booleans.<P1<? extends Serializable>>ne(IDENTITY_MSG))) {
              getMessageSender().sendObjectMessage(destinationId, MessageSender.DestinationType.Queue, m.get1());
            }
            updatesCurrent = updatesCurrent + 1;
            if (((updatesCurrent % responseInterval) == 0) || (updatesCurrent == updatesTotal)) {
              getMessageSender().sendObjectMessage(
                    IndexProducer.RESPONSE_QUEUE,
                    MessageSender.DestinationType.Queue,
                    IndexRecreateObject.update(
                            indexName,
                            getService(),
                            updatesTotal,
                            updatesCurrent));
            }
            if (updatesCurrent >= updatesTotal) {
              // send end-of-batch message
              final Organization emo = endMessageOrg.getOr(org);
              final User emu = SecurityUtil.createSystemUser(getSystemUserName(), emo);
              SecurityUtil.runAs(getSecurityService(), emo, emu, new Effect0() {
                @Override protected void run() {
                  getMessageSender().sendObjectMessage(
                          destinationId,
                          MessageSender.DestinationType.Queue,
                          IndexRecreateObject.end(indexName, getService()));
                }
              });
            }
          }
        });
      } else {
        throw new IllegalStateException(format("The number of allowed update messages (%d) has already been sent", updatesTotal));
      }
    }

    /**
     * @see #update(Organization, Iterable)
     */
    public void update(final Organization org, final P1<? extends Serializable>... messages) {
      update(org, $(messages));
    }

    @Override protected void finalize() throws Throwable {
      super.finalize();
      if (updatesCurrent < updatesTotal) {
        logger.warn(format("Only %d messages have been sent even though the batch has been initialized with %d", updatesCurrent, updatesTotal));
      }
    }
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  private class MessageWatcher implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(MessageWatcher.class);
    private volatile boolean listening = true;
    private volatile FutureTask<Serializable> future;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void stopListening() {
      this.listening = false;
      if (future != null) {
        future.cancel(true);
      }
      executor.shutdown();
    }

    @Override
    public void run() {
      if (getMessageReceiver() == null) {
        logger.warn("The message receiver for " + getClassName()
                + " was null so unable to listen for repopulate index messages. Ignore this warning if this is a test.");
        listening = false;
        return;
      }
      logger.info("Starting to listen for {} Messages", getClassName());
      while (listening) {
        try {
          future = getMessageReceiver().receiveSerializable(IndexProducer.RECEIVER_QUEUE + "." + getService(),
                  MessageSender.DestinationType.Queue);
          executor.execute(future);
          BaseMessage message = (BaseMessage) future.get();
          if (message == null || !(message.getObject() instanceof IndexRecreateObject))
            continue;

          IndexRecreateObject indexObject = (IndexRecreateObject) message.getObject();
          if (!indexObject.getService().equals(getService())
                  || !indexObject.getStatus().equals(IndexRecreateObject.Status.Start))
            continue;
          logger.info("Index '{}' has received a start repopulating command for service '{}'.",
                  indexObject.getIndexName(), getService());
          repopulate(indexObject.getIndexName());
          logger.info("Index '{}' has finished repopulating service '{}'.", indexObject.getIndexName(), getService());
        } catch (InterruptedException e) {
          logger.error("Problem while getting {} message events {}", getClassName(), ExceptionUtils.getStackTrace(e));
        } catch (ExecutionException e) {
          logger.error("Problem while getting {} message events {}", getClassName(), ExceptionUtils.getStackTrace(e));
        } catch (CancellationException e) {
          logger.trace("Listening for messages {} has been cancelled.", getClassName());
        } catch (Throwable t) {
          logger.error("Problem while getting {} message events {}", getClassName(), ExceptionUtils.getStackTrace(t));
        }
      }
      logger.info("Stopping listening for {} Messages", getClassName());
    }
  }
}
