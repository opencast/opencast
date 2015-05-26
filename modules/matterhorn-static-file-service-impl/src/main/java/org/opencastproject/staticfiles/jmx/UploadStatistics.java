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

package org.opencastproject.staticfiles.jmx;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UploadStatistics implements UploadStatisticsMXBean {

  private long totalNumBytesRead = 0L;
  private int successful = 0;
  private int failed = 0;
  private Cache<Long, Long> bytesCounter = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();

  /**
   * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean#getSuccessfulUploadOperations()
   */
  @Override
  public int getSuccessfulUploadOperations() {
    return successful;
  }

  /**
   * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean#getFailedUploadOperations()
   */
  @Override
  public int getFailedUploadOperations() {
    return failed;
  }

  /**
   * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean#getTotalBytes()
   */
  @Override
  public long getTotalBytes() {
    return totalNumBytesRead;
  }

  /**
   * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean#getBytesInLastMinute()
   */
  @Override
  public long getBytesInLastMinute() {
    long key = getKeyByTime(new DateTime().minusMinutes(1).getMillis());
    return key != 0 ? totalNumBytesRead - bytesCounter.getIfPresent(key) : 0;
  }

  /**
   * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean#getBytesInLastFiveMinutes()
   */
  @Override
  public long getBytesInLastFiveMinutes() {
    long key = getKeyByTime(new DateTime().minusMinutes(5).getMillis());
    return key != 0 ? totalNumBytesRead - bytesCounter.getIfPresent(key) : 0;
  }

  /**
   * @see org.opencastproject.staticfiles.jmx.UploadStatisticsMXBean#getBytesInLastFifteenMinutes()
   */
  @Override
  public long getBytesInLastFifteenMinutes() {
    long key = getKeyByTime(new DateTime().minusMinutes(15).getMillis());
    return key != 0 ? totalNumBytesRead - bytesCounter.getIfPresent(key) : 0;
  }

  private long getKeyByTime(long timeBeforeFiveMinute) {
    long key = 0L;
    List<Long> bytes = new ArrayList<Long>(bytesCounter.asMap().keySet());
    Collections.sort(bytes);
    for (Long milis : bytes) {
      if (milis > timeBeforeFiveMinute) {
        key = milis;
        break;
      }
    }
    return key;
  }

  public void add(long bytes) {
    if (totalNumBytesRead == 0)
      bytesCounter.put(System.currentTimeMillis(), 0L);
    totalNumBytesRead += bytes;
    bytesCounter.put(System.currentTimeMillis(), totalNumBytesRead);
  }

  public void successful() {
    successful++;
  }

  public void failed() {
    failed++;
  }

}
