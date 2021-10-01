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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.message.broker.api.MessageSender.DestinationType;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.DeleteEpisode;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.DeleteSnapshot;
import org.opencastproject.message.broker.api.assetmanager.AssetManagerItem.TakeSnapshot;

import com.entwinemedia.fn.Fx;
import com.entwinemedia.fn.data.Opt;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Test;

import java.io.Serializable;

/**
 * Test message sending to ActiveMQ.
 */
public class AssetManagerMessagingTest extends AssetManagerTestBase {
  private MessageSender ms;

  @Override
  public AssetManagerImpl makeAssetManager() throws Exception {
    ms = EasyMock.createMock(MessageSender.class);

    AssetManagerImpl am = super.makeAssetManager();
    am.setMessageSender(ms);
    return am;
  }

  /* -------------------------------------------------------------------------------------------------------------- */

  @Test
  public void test1() throws Exception {
    runTest(1, 1, 1, 1, new Fx<String[]>() {
      @Override public void apply(String[] mp) {
        q.delete(OWNER, q.snapshot()).run();
      }
    });
  }

  @Test
  public void test2() throws Exception {
    runTest(2, 1, 1, 1, new Fx<String[]>() {
      @Override public void apply(String[] mp) {
        q.delete(OWNER, q.snapshot()).where(q.mediaPackageId(mp[0])).run();
      }
    });
  }

  @Test
  public void test3() throws Exception {
    runTest(3, 2, 2, 1, new Fx<String[]>() {
      @Override public void apply(String[] mp) {
        q.delete(OWNER, q.snapshot()).where(q.mediaPackageId(mp[0])).run();
      }
    });
  }

  @Test
  public void test4() throws Exception {
    runTest(3, 2, 1, 0, new Fx<String[]>() {
      @Override public void apply(String[] mp) {
        q.delete(OWNER, q.snapshot()).where(q.mediaPackageId(mp[0]).and(q.version().isLatest())).run();
      }
    });
  }

  @Test
  public void test5() throws Exception {
    runTest(3, 2, 2, 0, new Fx<String[]>() {
      @Override public void apply(String[] mp) {
        q.delete(OWNER, q.snapshot())
            .where((q.mediaPackageId(mp[0]).or(q.mediaPackageId(mp[1])).and(q.version().isLatest())))
            .run();
      }
    });
  }

  @Test
  public void test6() throws Exception {
    runTest(3, 2, 3, 0, new Fx<String[]>() {
      @Override public void apply(String[] mp) {
        q.delete(OWNER, q.snapshot()).where(q.version().isLatest()).run();
      }
    });
  }

  @Test
  public void test7() throws Exception {
    runTest(3, 2, 6, 3, new Fx<String[]>() {
      @Override public void apply(String[] mp) {
        q.delete(OWNER, q.snapshot()).where(p.agent.eq("agent")).run();
      }
    });
  }

  @Test
  public void test8() throws Exception {
    runTest(2, 9, 18, 2, new Fx<String[]>() {
      @Override public void apply(String[] mp) {
        q.delete(OWNER, q.snapshot()).where(p.agent.eq("agent")).run();
      }
    });
  }

  /** Only one call per test case! */
  private void runTest(
          int mpCount,
          int versionCount,
          int deleteSnapshotMsgCount,
          int deleteEpisodeMsgCount,
          Fx<String[]> deleteQuery)
          throws Exception {
    q = am.createQuery();
    assertThat(q, instanceOf(AQueryBuilderDecorator.class));
    // expect add messages
    expectObjectMessage(ms, TakeSnapshot.class, mpCount * versionCount);
    // expect delete messages
    expectObjectMessage(ms, DeleteSnapshot.class, deleteSnapshotMsgCount);
    expectObjectMessage(ms, DeleteEpisode.class, deleteEpisodeMsgCount);
    EasyMock.replay(ms);
    //
    String[] mp = createAndAddMediaPackagesSimple(mpCount, versionCount, versionCount, Opt.<String>none());
    for (String id : mp) {
      am.setProperty(p.agent.mk(id, "agent"));
      am.setProperty(p.legacyId.mk(id, "id"));
    }
    // run deletion
    deleteQuery.apply(mp);
    // verify "delete" expectation
    EasyMock.verify(ms);
  }

  private void expectObjectMessage(
          final MessageSender ms,
          final Class<? extends Serializable> messageType,
          final int times) {
    if (times > 0) {
      ms.sendObjectMessage(
              EasyMock.eq(AssetManagerItem.ASSETMANAGER_QUEUE),
              EasyMock.eq(DestinationType.Queue),
              EasyMock.<Serializable>anyObject());
      EasyMock.expectLastCall().andAnswer(new IAnswer<Void>() {
        @Override public Void answer() throws Throwable {
          assertThat(EasyMock.getCurrentArguments()[2], instanceOf(messageType));
          return null;
        }
      }).times(times);
    }
  }
}
