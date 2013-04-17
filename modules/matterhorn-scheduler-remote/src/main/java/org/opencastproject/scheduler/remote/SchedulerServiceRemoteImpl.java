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
package org.opencastproject.scheduler.remote;

import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;

import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerQuery;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Tuple;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A proxy to a remote scheduler service.
 */
public class SchedulerServiceRemoteImpl extends RemoteBase implements SchedulerService {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceRemoteImpl.class);

  public SchedulerServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Long addEvent(DublinCoreCatalog eventCatalog, Map<String, String> wfProperties) throws SchedulerException,
          UnauthorizedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Long[] addReccuringEvent(DublinCoreCatalog eventCatalog, Map<String, String> wfProperties)
          throws SchedulerException, UnauthorizedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateCaptureAgentMetadata(Properties configuration, Tuple<Long, DublinCoreCatalog>... events)
          throws NotFoundException, SchedulerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateEvent(long eventId, DublinCoreCatalog eventCatalog, Map<String, String> wfProperties)
          throws NotFoundException, SchedulerException, UnauthorizedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeEvent(long eventId) throws SchedulerException, NotFoundException, UnauthorizedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public DublinCoreCatalog getEventDublinCore(long eventId) throws NotFoundException, SchedulerException {
    HttpGet get = new HttpGet(Long.toString(eventId).concat(".xml"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Event catalog '" + eventId + "' not found on remote scheduler service!");
        } else {
          DublinCoreCatalog dublinCoreCatalog = new DublinCoreCatalogImpl(response.getEntity().getContent());
          logger.info("Successfully get event dublincore {} from the remote scheduler service", eventId);
          return dublinCoreCatalog;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to parse event dublincore from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event dublincore from remote scheduler service");
  }

  @Override
  public Properties getEventCaptureAgentConfiguration(long eventId) throws NotFoundException, SchedulerException {
    HttpGet get = new HttpGet(Long.toString(eventId).concat("/agent.properties"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Event capture agent configuration '" + eventId
                  + "' not found on remote scheduler service!");
        } else {
          Properties properties = new Properties();
          properties.load(response.getEntity().getContent());
          logger.info("Successfully get event capture agent configuration {} from the remote scheduler service",
                  eventId);
          return properties;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to parse event capture agent configuration from remote scheduler service: "
              + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event capture agent configuration from remote scheduler service");
  }

  @Override
  public DublinCoreCatalogList search(SchedulerQuery query) throws SchedulerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public DublinCoreCatalogList findConflictingEvents(String captureDeviceID, Date startDate, Date endDate)
          throws SchedulerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public DublinCoreCatalogList findConflictingEvents(String captureDeviceID, String rrule, Date startDate,
          Date endDate, long duration, String timezone) throws SchedulerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getCalendar(SchedulerQuery filter) throws SchedulerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getScheduleLastModified(SchedulerQuery filter) throws SchedulerException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void updateEvents(List<Long> eventIds, DublinCoreCatalog eventCatalog) throws NotFoundException,
          SchedulerException, UnauthorizedException {
    throw new UnsupportedOperationException();
  }

}
