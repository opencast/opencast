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
package org.opencastproject.editor;

import org.opencastproject.editor.api.EditorServiceException;
import org.opencastproject.editor.api.ErrorStatus;
import org.opencastproject.editor.api.LockData;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public class EditorLock {

  private static LoadingCache <String,LockData> lockedPackages;

  public EditorLock(int timeout) {
    lockedPackages = CacheBuilder.newBuilder().expireAfterWrite(timeout, TimeUnit.SECONDS).build(
      new CacheLoader<String, LockData>() {
        @Override
        public LockData load(String json) throws Exception {
          return LockData.parse(json);
        }
      });
  }

  public void lock(String mediaPackage, LockData lData) throws EditorServiceException {

    LockData lockData = lockedPackages.getIfPresent(mediaPackage);
    if (null != lockData) {
      String lockMessage = "MediaPackage " + mediaPackage + lockData.toString();
      throw new EditorServiceException(lockMessage,ErrorStatus.MEDIAPACKAGE_LOCKED_BY_USER);
    }
    lockedPackages.put(mediaPackage,lData);
  }

  public void unlock(String mediaPackage) {
    lockedPackages.invalidate(mediaPackage);
  }

  public boolean isLocked(String mediaPackage) {
    return (null != getLockData(mediaPackage));
  }

  public LockData getLockData(String mediaPackage) {
    return lockedPackages.getIfPresent(mediaPackage);
  }

  public void refresh(String mediaPackage) {
    lockedPackages.refresh(mediaPackage);
  }

}
