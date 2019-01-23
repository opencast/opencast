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
package org.opencastproject.assetmanager.api;

import org.opencastproject.assetmanager.api.query.ADeleteQuery;

/**
 * Call {@link ADeleteQuery#run(DeleteSnapshotHandler)} with a deletion handler to get notified about deletions.
 */
public interface DeleteSnapshotHandler {

  // TODO This should take a `Version`
  void notifyDeleteSnapshot(String mpId, long version);

  void notifyDeleteEpisode(String mpId);

  DeleteSnapshotHandler NOP_DELETE_SNAPSHOT_HANDLER = new DeleteSnapshotHandler() {
    @Override public void notifyDeleteSnapshot(String mpId, long version) {
    }

    @Override public void notifyDeleteEpisode(String mpId) {
    }
  };

  static DeleteSnapshotHandler compose(DeleteSnapshotHandler first, DeleteSnapshotHandler second) {
    return new DeleteSnapshotHandler() {
      @Override
      public void notifyDeleteSnapshot(String mpId, long version) {
        first.notifyDeleteSnapshot(mpId, version);
        second.notifyDeleteSnapshot(mpId, version);
      }

      @Override
      public void notifyDeleteEpisode(String mpId) {
        first.notifyDeleteEpisode(mpId);
        second.notifyDeleteEpisode(mpId);
      }
    };
  }
}
