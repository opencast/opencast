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
package org.opencastproject.adminui.impl;

import static java.lang.Math.max;

import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.User;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;

/**
 *
 * @author Tobias M Schiebeck
 */
public class MediaPackageLockService implements ManagedService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MediaPackageLockService.class);

  /**
   * minimum duration the editor is locked to avoid synchronous editing
   */
  private static final long MIN_LOCK_DURATION = 1800000;

  /** 
   * map to store the mediaPackageLocks
   */
  private static final HashMap<String, EditorLock> mpLock = new HashMap<String, EditorLock>();

  /**
   * The Json key for the MediaPackageId.
   */
  public static final String MEDIAPACKAGEID_KEY = "id";

  /**
   * The Json key for the SessionId.
   */
  public static final String SESSION_KEY = "session";

  public MediaPackageLockService() {
    super();
    logger.info("starting MP Lock service");
  }

  /** method to acquire a lock for a specific mediaPackage
   * 
   * @param event
   *        event that contains the mediapackage we want to acquire a lock for
   * @param sessionId
   *        sessionId of the browser to lock
   * @param minLockDuration
   *        minimum duration for the lock, the lock will expire after the duration of the video 
   *        or this whichever is the longer
   * @param user
   *        user information of the user that aquires the lock so we can provide this information to others 
   * @return
   *        the LockInfo of the Lock that is currently held for this mediapackage. 
   *        This LockInfo contains a duration (the time until the lock is released) 
   *        and the userinfo who holds the lock
   *        if the Duration is negative the time of the lock is newly created or just replaced an expired lock 
   */
  public LockInfo getMediaPackageLock(Event event, String sessionId, int minLockDuration, User user) {
    EditorLock releaseLock;
    EditorLock lock = new EditorLock(event.getIdentifier(), sessionId, user);
    logger.debug("lock mp {} for session {}", event.getIdentifier(), sessionId);
    synchronized (mpLock) {
      releaseLock = mpLock.get(lock.getMediaPackageId());
      if (releaseLock != null) {
        LockInfo out = releaseLock.checkLock(lock);
        if (out.getDuration() > 0) {
          return out;
        }
      }
      Long lockDur = event.getDuration();
      if (lockDur == null) {
        lockDur = 0L;
        for (Publication pub : event.getPublications()) {
          for (Track track : pub.getTracks()) {
            lockDur = max(lockDur, (long) track.getDuration());
          }
        }
      }
      lock.setDuration(max((long)minLockDuration * 60000, lockDur));
      mpLock.put(lock.getMediaPackageId(), lock);
      return new LockInfo(-1, user);
    }
  }

  /** method to release a lock for a specific mediaPackage
   * 
   * @param event
   *        event that contains the mediapackage we want to release a lock for
   * @param sessionId
   *        sessionId of the browser to release
   */
  public void releaseMediaPackageLock(final Event event, String sessionId) {
    logger.debug("unlock mp {}",event.getIdentifier());
    synchronized (mpLock) {
      EditorLock el = mpLock.get(event.getIdentifier());
      if (el == null) {
        for (String eventId : mpLock.keySet()) {
          if (mpLock.get(eventId).removable(sessionId)) {
            mpLock.remove(eventId);
          }
        }
      } else {
        if (el.removable(sessionId)) {
          mpLock.remove(event.getIdentifier());
        }
      }
    }
  }

  @Override
  public void updated(Dictionary<String, ?> dctnr) throws ConfigurationException {
    // do nothing
  }

  /**
   * method to clean up expired media package locks
   */
  public void cleanUp() {
    synchronized (mpLock) {
      for (EditorLock el : mpLock.values()) {
        if (el.removable("")) {
          mpLock.remove(el.getMediaPackageId());
        }
      }
    }
  }

  /**
   * Info class for the Media Package Lock
   */
  public final class LockInfo {

    /**
     * time remaining until the lock is released
     */
    private final long duration;

    /**
     * user that holds the lock
     */
    private final User user;

    private LockInfo(long duration, User user) {
      this.duration = duration;
      this.user = user;
    }

    /**
     * get time remaining
     * @return time remaining until the lock is released
     */
    public long getDuration() {
      return duration;
    }

    /**
     * get user info
     * @return user info who holds the lock
     */
    public User getUser() {
      return user;
    }
  }

  final class EditorLock {

    private final String mediaPackageId;
    private final String sessionId;
    private long duration;
    private final Date date;
    private final User user;

    private EditorLock(String mediaPackageId, String sessionId, User user) {
      this.mediaPackageId = mediaPackageId;
      this.sessionId = sessionId;
      this.duration = MIN_LOCK_DURATION;
      this.date = new Date();
      this.user = user;
    }

    void setDuration(long duration) {
      this.duration = duration;
    }

    LockInfo checkLock(EditorLock other) {
      if (other == null) {
        return new LockInfo(-1, user);
      }
      if (this.sessionId.equals(other.sessionId)) {
        return new LockInfo(-1, user);
      }
      return new LockInfo(duration - other.date.getTime() + date.getTime(), user);
    }

    boolean removable(String sessionId) {
      Date now = new Date();
      if (this.sessionId.equals(sessionId)) {
        return true;
      }
      if ((now.getTime() - date.getTime()) > duration) {
        return true;
      }
      return false;
    }

    String getMediaPackageId() {
      return this.mediaPackageId;
    }
  }

}
