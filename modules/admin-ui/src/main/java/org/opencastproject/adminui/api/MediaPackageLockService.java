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
package org.opencastproject.adminui.api;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.security.api.User;

/**
 *
 * @author Tobias M Schiebeck
 */
public interface MediaPackageLockService {
  /**
   * Minimum duration the media package is locked for.
   */
  long MIN_LOCK_DURATION = 1800000;

  /**
   * Try to acquire a lock for the event's media package, if already held
   * by the user refresh the lock.
   *
   * @param event
   *        event that contains the media package we want to acquire a lock for
   * @param sessionId
   *        sessionId of the browser to lock
   * @param minLockDuration
   *        minimum duration for the lock, the lock will expire after the duration of the video
   *        or this whichever is the longer
   * @param user
   *        user information of the user that acquires the lock so we can provide this information to others
   * @return
   *        the LockInfo of the Lock that is currently held for this media package.
   *        This LockInfo contains a duration (the time until the lock is released)
   *        and the user info who holds the lock
   *        if the Duration is negative the time of the lock is newly created or just replaced an expired lock
   */
  LockInfo getMediaPackageLock(Event event, String sessionId, int minLockDuration, User user);

  /**
   * Try to release a lock for the event's media package.
   *
   * @param event
   *        event that contains the media package we want to release a lock for
   * @param sessionId
   *        sessionId of the browser to release
   */
  void releaseMediaPackageLock(Event event, String sessionId);

  /**
   * Remove expired locks.
   */
  void cleanUp();

  interface LockInfo {
    /**
     * get time remaining
     * @return time remaining until the lock is released
     */
    long getDuration();

    /**
     * get user info
     * @return user info who holds the lock
     */
    User getUser();
  }
}
