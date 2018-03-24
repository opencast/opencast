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

import org.json.simple.JSONObject;
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

  public long getMediaPackageLock(Event event, String sessionId) {
    EditorLock releaseLock = null;
    EditorLock lock = new EditorLock(event.getIdentifier(), sessionId);
    logger.debug("lock mp {} for session {}",event.getIdentifier(), sessionId);
    synchronized (mpLock) {
      releaseLock = mpLock.get(lock.getMediaPackageId());
      if (releaseLock == null) {
        Long lockDur = event.getDuration();
        if (lockDur == null) {
          lockDur = 0L;
          for (Publication pub : event.getPublications()) {
            for (Track track : pub.getTracks()) {
              lockDur = max(lockDur, (long) track.getDuration());
            }
          }
        }
        lock.setDuration(lockDur);
        mpLock.put(lock.getMediaPackageId(), lock);
        return -1;
      }
      return releaseLock.checkLock(lock);
    }
  }

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
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  public void cleanUp() {
    synchronized (mpLock) {
      for (EditorLock el : mpLock.values()) {
        if (el.removable("")) {
          mpLock.remove(el.getMediaPackageId());
        }
      }
    }
  }

  final class EditorLock {

    private final String mediaPackageId;
    private final String sessionId;
    private long duration;
    private final Date date;

    private EditorLock(String mediaPackageId, String sessionId) {
      this.mediaPackageId = mediaPackageId;
      this.sessionId = sessionId;
      this.duration = MIN_LOCK_DURATION;
      this.date = new Date();
    }

    EditorLock(JSONObject obj) {
      this((String) obj.get(MEDIAPACKAGEID_KEY), (String) obj.get(SESSION_KEY));
    }

    void setDuration(long duration) {
      this.duration = max(MIN_LOCK_DURATION, duration);
    }

    long checkLock(EditorLock other) {
      if (other == null) {
        return -1;
      }
      if (this.sessionId.equals(other.sessionId)) {
        return -1;
      }
      long ret = duration - other.date.getTime() + date.getTime();
      return duration - other.date.getTime() + date.getTime();
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
