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

import static org.apache.http.HttpStatus.SC_CREATED;
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * A proxy to a remote series service.
 */
public class SchedulerServiceRemoteImpl extends RemoteBase implements SchedulerService {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceRemoteImpl.class);

  public SchedulerServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public Long addEvent(DublinCoreCatalog eventCatalog, Map<String, String> wfProperties) throws SchedulerException,
          UnauthorizedException {
    HttpPost post = new HttpPost("/");
    logger.debug("Start adding a new event {} through remote Schedule Service",
            eventCatalog.get(DublinCoreCatalog.PROPERTY_IDENTIFIER));

    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("dublincore", eventCatalog.toXmlString()));

      String wfPropertiesString = "";
      for (Map.Entry<String, String> entry : wfProperties.entrySet())
        wfPropertiesString += entry.getKey() + "=" + entry.getValue() + "\n";

      // Add an empty agentparameters as it is required by the Rest Endpoint
      params.add(new BasicNameValuePair("agentparameters", "remote.scheduler.empty.parameter"));
      params.add(new BasicNameValuePair("wfproperties", wfPropertiesString));

      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new SchedulerException("Unable to assemble a remote scheduler request for adding an event " + eventCatalog,
              e);
    }

    HttpResponse response = getResponse(post, SC_CREATED);
    try {
      if (response != null && SC_CREATED == response.getStatusLine().getStatusCode()) {
        Header header = response.getFirstHeader("Location");
        if (header != null) {
          String idString = FilenameUtils.getBaseName(header.getValue());
          if (StringUtils.isNotEmpty(idString)) {
            Long id = Long.parseLong(idString);
            logger.info("Successfully added event {} to the scheduler service with id {}", eventCatalog, id);
            return id;
          }
        }
        throw new SchedulerException("Event " + eventCatalog
                + " added to the scheduler service but got not event id in response.");
      } else {
        throw new SchedulerException("Unable to add event " + eventCatalog + " to the scheduler service");
      }
    } catch (Exception e) {
      throw new SchedulerException("Unable to add event " + eventCatalog + " to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
  }

  @Override
  public Long[] addReccuringEvent(DublinCoreCatalog eventCatalog, Map<String, String> wfProperties)
          throws SchedulerException, UnauthorizedException {
    HttpPost post = new HttpPost("/");
    logger.debug("Start adding new events through remote Schedule Service");

    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("dublincore", eventCatalog.toXmlString()));

      String wfPropertiesString = "";
      for (Map.Entry<String, String> entry : wfProperties.entrySet())
        wfPropertiesString += entry.getKey() + "=" + entry.getValue() + "\n";

      // Add an empty agentparameters as it is required by the Rest Endpoint
      params.add(new BasicNameValuePair("agentparameters", "remote.scheduler.empty.parameter"));
      params.add(new BasicNameValuePair("wfproperties", wfPropertiesString));

      post.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new SchedulerException("Unable to assemble a remote scheduler request for adding events " + eventCatalog, e);
    }

    HttpResponse response = getResponse(post, SC_CREATED);
    try {
      if (response != null && SC_CREATED == response.getStatusLine().getStatusCode()) {

        try {
          String idsStr = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
          List<Long> ids = new ArrayList<Long>();

          for (String id : StringUtils.split(idsStr, ",")) {
            ids.add(Long.parseLong(id));
          }

          return ids.toArray(new Long[ids.size()]);
        } catch (IOException e) {
          throw new SchedulerException("Unable to parse the events ids from the reponse creation.");
        }
      } else {
        throw new SchedulerException("Unable to add event " + eventCatalog + " to the scheduler service");
      }
    } catch (Exception e) {
      throw new SchedulerException("Unable to add event " + eventCatalog + " to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }

  }

  @Override
  public void updateCaptureAgentMetadata(Properties configuration, Tuple<Long, DublinCoreCatalog>... events)
          throws NotFoundException, SchedulerException {
    logger.debug("Start updating {} events with following capture agent metadata: {}", events.length, configuration);
    for (Tuple<Long, DublinCoreCatalog> event : events) {
      final long eventId = event.getA();
      final DublinCoreCatalog eventCatalog = event.getB();

      logger.debug("Start updating event {} with capture agent metadata", eventId);

      String propertiesString = "";
      for (Entry<Object, Object> entry : configuration.entrySet())
        propertiesString += (String) entry.getKey() + "=" + (String) entry.getValue() + "\n";

      HttpPut put = new HttpPut("/" + eventId);

      try {
        List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
        params.add(new BasicNameValuePair("dublincore", eventCatalog.toXmlString()));
        params.add(new BasicNameValuePair("agentproperties", propertiesString));

        put.setEntity(new UrlEncodedFormEntity(params));
      } catch (Exception e) {
        throw new SchedulerException("Unable to assemble a remote scheduler request for adding an event "
                + eventCatalog, e);
      }

      HttpResponse response = getResponse(put, SC_OK, SC_NOT_FOUND);
      try {
        if (response != null) {
          if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
            logger.warn("Event {} was not found by the scheduler service", eventId);
          } else if (SC_OK == response.getStatusLine().getStatusCode()) {
            logger.info("Event {} successfully updated with capture agent metadata.", eventId);
          } else {
            throw new SchedulerException("Unexpected status code " + response.getStatusLine());
          }
          return;
        }
      } catch (Exception e) {
        throw new SchedulerException("Unable to update event " + eventId + " to the scheduler service: " + e);
      } finally {
        closeConnection(response);
      }
      throw new SchedulerException("Unable to update  event " + eventId);
    }
  }

  @Override
  public void updateEvent(long eventId, DublinCoreCatalog eventCatalog, Map<String, String> wfProperties)
          throws NotFoundException, SchedulerException, UnauthorizedException {
    logger.debug("Start updating event {}.", eventId);
    HttpPut put = new HttpPut("/" + eventId);

    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();

      if (eventCatalog != null)
        params.add(new BasicNameValuePair("dublincore", eventCatalog.toXmlString()));

      String wfPropertiesString = "";
      for (Map.Entry<String, String> entry : wfProperties.entrySet())
        wfPropertiesString += entry.getKey() + "=" + entry.getValue() + "\n";

      params.add(new BasicNameValuePair("wfproperties", wfPropertiesString));

      put.setEntity(new UrlEncodedFormEntity(params));
    } catch (Exception e) {
      throw new SchedulerException("Unable to assemble a remote scheduler request for updating event " + eventCatalog,
              e);
    }

    HttpResponse response = getResponse(put, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          logger.warn("Event {} was not found by the scheduler service", eventId);
        } else if (SC_OK == response.getStatusLine().getStatusCode()) {
          logger.info("Event {} successfully updated with capture agent metadata.", eventId);
        } else {
          throw new SchedulerException("Unexpected status code " + response.getStatusLine());
        }
        return;
      }
    } catch (Exception e) {
      throw new SchedulerException("Unable to update event " + eventId + " to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to update  event " + eventId);
  }

  @Override
  public void removeEvent(long eventId) throws SchedulerException, NotFoundException, UnauthorizedException {
    logger.debug("Start removing event {} from scheduling service.", eventId);
    HttpDelete delete = new HttpDelete("/" + eventId);

    HttpResponse response = getResponse(delete, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null && SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
        logger.warn("Event {} was not found by the scheduler service", eventId);
        return;
      } else if (response != null && SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
        logger.info("Event {} removed from scheduling service.", eventId);
        return;
      }
    } catch (Exception e) {
      throw new SchedulerException("Unable to remove event " + eventId + " from the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to remove  event " + eventId);
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
