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
package org.opencastproject.kernel.scanner;

import org.opencastproject.util.Log;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.quartz.CronExpression;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

public abstract class AbstractBufferScanner extends AbstractScanner {
  private static final Log logger = new Log(LoggerFactory.getLogger(AbstractBufferScanner.class));

  /** The key that will be used to find the amount of buffer time to search for scheduled recordings to remove before now. */
  protected static final String PARAM_KEY_BUFFER = "buffer";

  protected long buffer = -1;

  @SuppressWarnings("rawtypes")
  public void updated(Dictionary properties) throws ConfigurationException {
    String cronExpression;
    boolean enabled;

    unschedule();

    if (properties != null) {
      logger.debug("Updating configuration...");

      enabled = BooleanUtils.toBoolean((String) properties.get(PARAM_KEY_ENABLED));
      setEnabled(enabled);
      logger.debug("enabled = " + enabled);

      cronExpression = (String) properties.get(PARAM_KEY_CRON_EXPR);
      if (StringUtils.isBlank(cronExpression) || !CronExpression.isValidExpression(cronExpression))
        throw new ConfigurationException(PARAM_KEY_CRON_EXPR, "Cron expression must be valid");
      setCronExpression(cronExpression);
      logger.debug("cronExpression = '" + cronExpression + "'");

      try {
        buffer = Long.valueOf((String) properties.get(PARAM_KEY_BUFFER));
        if (buffer < 0) {
          throw new ConfigurationException(PARAM_KEY_BUFFER, "Buffer must be 0 or greater");
        }
      } catch (NumberFormatException e) {
        throw new ConfigurationException(PARAM_KEY_BUFFER, "Buffer must be a valid integer", e);
      }
      logger.debug("buffer = " + buffer);
    }

    schedule();
  }

  public long getBuffer() {
    return buffer;
  }

  public void setBuffer(long buffer) {
    this.buffer = buffer;
  }
}
