/**
 *  Copyright 2009, 2010 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
package org.opencastproject.util;

import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.opencastproject.util.data.Tuple.tuple;

/** {@link Cache} factory. */
public final class Caches {
  private static final Logger logger = LoggerFactory.getLogger(Caches.class);

  private Caches() {
  }

  /**
   * A synchronized LRU cache with a maximum size and a time to live in ms per key.
   * This cache does not support null values.
   * Wrap them in an {@link org.opencastproject.util.data.Option#option(Object) Option}.
   */
  public static <K, V> Cache<K, V> lru(final int maxSize, final long ttl) {
    final Map<K, Tuple<Long, V>> cache = new LinkedHashMap<K, Tuple<Long, V>>(maxSize / 5, 0.75F, true) {
      @Override protected boolean removeEldestEntry(Map.Entry<K, Tuple<Long, V>> eldest) {
        final boolean full = size() > maxSize;
        if (full && logger.isTraceEnabled()) logger.trace("Cache full. Removing eldest " + eldest.getKey());
        return full;
      }
    };
    return new Cache<K, V>() {
      @Override public V get(K key, Function<K, V> loader) {
        synchronized (cache) {
          final long now = System.currentTimeMillis();
          final Tuple<Long, V> value = cache.get(key);
          if (value != null && now - value.getA() < ttl) {
            // value is still valid
            if (logger.isTraceEnabled()) logger.trace("Cache hit " + key);
            return value.getB();
          } else {
            // no value or expired
            if (logger.isTraceEnabled()) {
              if (value == null) logger.trace("Cache miss " + key);
              else logger.trace("Cache expired " + key);
            }
            V tmp = loader.apply(key);
            if (tmp == null) return null;
            final Tuple<Long, V> newValue = tuple(now, tmp); 
            // Since the LinkedHashMap is access ordered there is no need to remove the
            // key prior to putting. For an insertion ordered map the
            // key has to be removed first though otherwise the order does not update.
            cache.put(key, newValue);
            return newValue.getB();
          }
        }
      }
    };
  }
}
