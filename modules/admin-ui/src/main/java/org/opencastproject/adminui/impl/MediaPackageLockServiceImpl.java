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

import org.opencastproject.adminui.api.MediaPackageLockService;
import org.opencastproject.adminui.api.MediaPackageLockService.LockInfo;
import org.opencastproject.index.service.impl.index.event.Event;
import org.opencastproject.mediapackage.Publication;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.security.api.User;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author Tobias M Schiebeck
 */
public class MediaPackageLockServiceImpl implements MediaPackageLockService {

  /** The logging facility */
  private static final Logger logger = LoggerFactory.getLogger(MediaPackageLockServiceImpl.class);

  private static final HashMap<String, MediaPackageLock> mpLock = new HashMap<String, MediaPackageLock>();

  /** OSGi component activation callback */
  void activate(BundleContext bundleContext) {
    logger.info("Media Package Lock Service");
  }

  @Override
  public LockInfo getMediaPackageLock(Event event, String sessionId, int minLockDuration, User user) {
    logger.debug("lock MP {} for session {}", event.getIdentifier(), sessionId);

    synchronized (mpLock) {
      MediaPackageLock lock = mpLock.get(event.getIdentifier());

      if (lock == null || lock.timeRemaining() < 0) {
        MediaPackageLock newLock = new MediaPackageLock(event, sessionId, user);
        mpLock.put(newLock.getEventId(), newLock);
        return new LockInfoImpl(-1, user);
      } else if (lock.holder(sessionId)) {
        lock.refresh();
        return new LockInfoImpl(-1, lock.getUser());
      } else {
        return new LockInfoImpl(lock.timeRemaining(), lock.getUser());
      }
    }
  }

  @Override
  public void releaseMediaPackageLock(final Event event, String sessionId) {
    logger.debug("unlock MP {} for session {}", event.getIdentifier(), sessionId);

    synchronized (mpLock) {
      MediaPackageLock lock = mpLock.get(event.getIdentifier());

      if (lock == null) {
        for (String eventId : mpLock.keySet()) {
          if (mpLock.get(eventId).removable(sessionId)) {
            mpLock.remove(eventId);
          }
        }
      } else {
        if (lock.removable(sessionId)) {
          mpLock.remove(event.getIdentifier());
        }
      }
    }
  }

  @Override
  public void cleanUp() {
    synchronized (mpLock) {
      for (MediaPackageLock lock : mpLock.values()) {
        if (lock.removable("")) {
          mpLock.remove(lock.getEventId());
        }
      }
    }
  }

  /**
   * Info class for the Media Package Lock
   */
  public final class LockInfoImpl implements LockInfo {
    private final long duration;
    private final User user;

    LockInfoImpl(long duration, User user) {
      this.duration = duration;
      this.user = user;
    }

    @Override
    public long getDuration() {
      return duration;
    }

    @Override
    public User getUser() {
      return user;
    }
  }

  private final class MediaPackageLock {
    private final String eventId;
    private final String sessionId;
    private final User user;
    private Long duration;
    private Date dateStamp;

    private MediaPackageLock(Event event, String sessionId, User user) {
      this.eventId = event.getIdentifier();
      this.sessionId = sessionId;
      this.user = user;
      this.dateStamp = new Date();
      this.duration = event.getDuration();

      if (duration == null) {
        duration = MIN_LOCK_DURATION;
        for (Publication pub : event.getPublications()) {
          for (Track track : pub.getTracks()) {
            duration = max(duration, (long) track.getDuration());
          }
        }
      } else if (duration < MIN_LOCK_DURATION) {
        duration = MIN_LOCK_DURATION;
      }
    }

    boolean holder(String sessionId) {
      return this.sessionId.equals(sessionId);
    }

    long timeRemaining() {
      Date now = new Date();
      return duration - now.getTime() + dateStamp.getTime();
    }

    void refresh() {
      this.dateStamp = new Date();
    }

    boolean removable(String sessionId) {
      if (holder(sessionId)) {
        return true;
      }
      return timeRemaining() < 0;
    }

    String getEventId() {
      return eventId;
    }

    User getUser() {
      return user;
    }
  }

}
