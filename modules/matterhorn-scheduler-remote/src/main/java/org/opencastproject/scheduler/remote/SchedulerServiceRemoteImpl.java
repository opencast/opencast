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

package org.opencastproject.scheduler.remote;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.scheduler.api.Recording;
import org.opencastproject.scheduler.api.RecordingImpl;
import org.opencastproject.scheduler.api.SchedulerConflictException;
import org.opencastproject.scheduler.api.SchedulerException;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.scheduler.api.SchedulerTransactionLockException;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.scheduler.api.TechnicalMetadataImpl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.UnauthorizedException;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;

import com.entwinemedia.fn.data.Opt;

import net.fortuna.ical4j.model.property.RRule;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;

/**
 * A proxy to a remote series service.
 */
public class SchedulerServiceRemoteImpl extends RemoteBase implements SchedulerService {

  private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceRemoteImpl.class);

  /** A parser for handling JSON documents inside the body of a request. **/
  private final JSONParser parser = new JSONParser();

  public SchedulerServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  public SchedulerTransaction getTransaction(String id) throws NotFoundException, SchedulerException {
    HttpGet get = new HttpGet("transaction/".concat(id));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Scheduler transaction '" + id + "' not found on remote scheduler service!");
        } else {
          String transactionJson = EntityUtils.toString(response.getEntity(), UTF_8);
          JSONObject json = (JSONObject) parser.parse(transactionJson);
          String transactionId = (String) json.get("id");
          String schedulingSource = (String) json.get("source");
          logger.info("Successfully get scheduler transaction {} from the remote scheduler service", id);
          return new SchedulerTransactionRemoteImpl(transactionId, schedulingSource);
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get scheduler transaction from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get scheduler transaction from remote scheduler service");
  }

  @Override
  public SchedulerTransaction getTransactionBySource(String source) throws NotFoundException, SchedulerException {
    HttpGet get = new HttpGet("transaction/source/".concat(source));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException(
                  "Scheduler transaction with source '" + source + "' not found on remote scheduler service!");
        } else {
          String transactionJson = EntityUtils.toString(response.getEntity(), UTF_8);
          JSONObject json = (JSONObject) parser.parse(transactionJson);
          String transactionId = (String) json.get("id");
          String schedulingSource = (String) json.get("source");
          logger.info("Successfully get scheduler transaction with source {} from the remote scheduler service",
                  source);
          return new SchedulerTransactionRemoteImpl(transactionId, schedulingSource);
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get scheduler transaction from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get scheduler transaction from remote scheduler service");
  }

  @Override
  public boolean hasActiveTransaction(String mediaPackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet("transaction/event/".concat(mediaPackageId));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException(
                  "Event with mediapackage id '" + mediaPackageId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get active transaction status of event {}.", mediaPackageId);
          throw new UnauthorizedException(
                  "Unauthorized to get active transaction status of the event " + mediaPackageId);
        } else {
          String hasActiveTransactionString = EntityUtils.toString(response.getEntity(), UTF_8);
          Boolean booleanObject = BooleanUtils.toBooleanObject(hasActiveTransactionString);
          if (booleanObject == null)
            throw new SchedulerException("Could not parse active transaction status from the remote scheduler service: "
                    + hasActiveTransactionString);

          logger.info(
                  "Successfully get active transaction status of event with mediapackage id {} from the remote scheduler service",
                  mediaPackageId);
          return booleanObject.booleanValue();
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException(
              "Unable to get active transaction status for event from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException(
            "Unable to get active transaction status for event from from remote scheduler service");
  }

  @Override
  public SchedulerTransaction createTransaction(final String schedulingSource)
          throws UnauthorizedException, SchedulerConflictException, SchedulerException {
    HttpPost post = new HttpPost("/transaction");
    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("source", schedulingSource));
    post.setEntity(new UrlEncodedFormEntity(params, UTF_8));

    HttpResponse response = getResponse(post, SC_OK, SC_UNAUTHORIZED, SC_CONFLICT);
    try {
      if (response != null && SC_OK == response.getStatusLine().getStatusCode()) {
        String transactionJson = EntityUtils.toString(response.getEntity(), UTF_8);
        JSONObject json = (JSONObject) parser.parse(transactionJson);
        String transactionId = (String) json.get("id");
        String source = (String) json.get("source");
        logger.info("Successfully created scheduler transaction '{}' with id '{}' to the scheduler service", source,
                transactionId);
        return new SchedulerTransactionRemoteImpl(transactionId, source);
      } else if (response != null && SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
        logger.info("Unauthorized to create scheduler transaction");
        throw new UnauthorizedException("Unauthorized to create scheduler transaction");
      } else if (response != null && SC_CONFLICT == response.getStatusLine().getStatusCode()) {
        logger.info("Transaction with source {} already exists!", schedulingSource);
        throw new SchedulerConflictException("Transaction already exists with source " + schedulingSource);
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (SchedulerConflictException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException(
              "Unable to create scheduler transaction '" + schedulingSource + "' to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException(
            "Unable to create scheduler transaction '" + schedulingSource + "' to the scheduler service");
  }

  @Override
  public void cleanupTransactions() throws UnauthorizedException, SchedulerException {
    HttpPost post = new HttpPost("/transaction/cleanup");
    HttpResponse response = getResponse(post, SC_OK, SC_UNAUTHORIZED);
    try {
      if (response != null && SC_OK == response.getStatusLine().getStatusCode()) {
        logger.info("Successfully cleaned up scheduler transactions to the scheduler service");
        return;
      } else if (response != null && SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
        logger.info("Unauthorized to cleanup scheduler transactions");
        throw new UnauthorizedException("Unauthorized to cleanup scheduler transactions");
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to cleanup scheduler transactions to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to cleanup scheduler transactions to the scheduler service");
  }

  @Override
  public void addEvent(Date startDateTime, Date endDateTime, String captureAgentId, Set<String> userIds,
          MediaPackage mediaPackage, Map<String, String> wfProperties, Map<String, String> caMetadata,
          Opt<Boolean> optOut, Opt<String> schedulingSource, String origin) throws UnauthorizedException,
                  SchedulerTransactionLockException, SchedulerConflictException, SchedulerException {
    HttpPost post = new HttpPost("/");
    String eventId = mediaPackage.getIdentifier().compact();
    logger.debug("Start adding a new event {} through remote Schedule Service", eventId);

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("start", Long.toString(startDateTime.getTime())));
    params.add(new BasicNameValuePair("end", Long.toString(endDateTime.getTime())));
    params.add(new BasicNameValuePair("agent", captureAgentId));
    params.add(new BasicNameValuePair("users", StringUtils.join(userIds, ",")));
    params.add(new BasicNameValuePair("mediaPackage", MediaPackageParser.getAsXml(mediaPackage)));
    params.add(new BasicNameValuePair("wfproperties", toPropertyString(wfProperties)));
    params.add(new BasicNameValuePair("agentparameters", toPropertyString(caMetadata)));
    if (optOut.isSome())
      params.add(new BasicNameValuePair("optOut", Boolean.toString(optOut.get())));
    if (schedulingSource.isSome())
      params.add(new BasicNameValuePair("source", schedulingSource.get()));
    params.add(new BasicNameValuePair("origin", origin));
    post.setEntity(new UrlEncodedFormEntity(params, UTF_8));

    HttpResponse response = getResponse(post, SC_CREATED, SC_UNAUTHORIZED, SC_CONFLICT);
    try {
      if (response != null && SC_CREATED == response.getStatusLine().getStatusCode()) {
        logger.info("Successfully added event {} to the scheduler service", eventId);
        return;
      } else if (response != null && SC_CONFLICT == response.getStatusLine().getStatusCode()) {
        String errorJson = EntityUtils.toString(response.getEntity(), UTF_8);
        JSONObject json = (JSONObject) parser.parse(errorJson);
        JSONObject error = (JSONObject) json.get("error");
        String errorCode = (String) error.get("code");
        if (SchedulerTransactionLockException.ERROR_CODE.equals(errorCode)) {
          logger.info("Event is locked by a transaction, unable to add event {}", eventId);
          throw new SchedulerTransactionLockException(
                  "Event is locked by a transaction, unable to add event " + eventId);
        } else if (SchedulerConflictException.ERROR_CODE.equals(errorCode)) {
          logger.info("Conflicting events found when adding event {}", eventId);
          throw new SchedulerConflictException("Conflicting events found when adding event " + eventId);
        } else {
          throw new SchedulerException("Unexpected error code " + errorCode);
        }
      } else if (response != null && SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
        logger.info("Unauthorized to create the event");
        throw new UnauthorizedException("Unauthorized to create the event");
      } else {
        throw new SchedulerException("Unable to add event " + eventId + " to the scheduler service");
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (SchedulerTransactionLockException e) {
      throw e;
    } catch (SchedulerConflictException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to add event " + eventId + " to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
  }

  @Override
  public void updateEvent(String eventId, Opt<Date> startDateTime, Opt<Date> endDateTime, Opt<String> captureAgentId,
          Opt<Set<String>> userIds, Opt<MediaPackage> mediaPackage, Opt<Map<String, String>> wfProperties,
          Opt<Map<String, String>> caMetadata, Opt<Opt<Boolean>> optOut, String origin)
                  throws NotFoundException, UnauthorizedException, SchedulerTransactionLockException,
                  SchedulerConflictException, SchedulerException {
    logger.debug("Start updating event {}.", eventId);
    HttpPut put = new HttpPut("/" + eventId);

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    if (startDateTime.isSome())
      params.add(new BasicNameValuePair("start", Long.toString(startDateTime.get().getTime())));
    if (endDateTime.isSome())
      params.add(new BasicNameValuePair("end", Long.toString(endDateTime.get().getTime())));
    if (captureAgentId.isSome())
      params.add(new BasicNameValuePair("agent", captureAgentId.get()));
    if (userIds.isSome())
      params.add(new BasicNameValuePair("users", StringUtils.join(userIds.get(), ",")));
    if (mediaPackage.isSome())
      params.add(new BasicNameValuePair("mediaPackage", MediaPackageParser.getAsXml(mediaPackage.get())));
    if (wfProperties.isSome())
      params.add(new BasicNameValuePair("wfproperties", toPropertyString(wfProperties.get())));
    if (caMetadata.isSome())
      params.add(new BasicNameValuePair("agentparameters", toPropertyString(caMetadata.get())));
    if (optOut.isSome()) {
      params.add(new BasicNameValuePair("updateOptOut", Boolean.toString(true)));
      if (optOut.get().isSome())
        params.add(new BasicNameValuePair("optOut", Boolean.toString(optOut.get().get())));
    } else {
      params.add(new BasicNameValuePair("updateOptOut", Boolean.toString(false)));
    }
    params.add(new BasicNameValuePair("origin", origin));
    put.setEntity(new UrlEncodedFormEntity(params, UTF_8));

    HttpResponse response = getResponse(put, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED, SC_FORBIDDEN, SC_CONFLICT);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          logger.info("Event {} was not found by the scheduler service", eventId);
          throw new NotFoundException("Event '" + eventId + "' not found on remote scheduler service!");
        } else if (SC_OK == response.getStatusLine().getStatusCode()) {
          logger.info("Event {} successfully updated with capture agent metadata.", eventId);
          return;
        } else if (response != null && SC_CONFLICT == response.getStatusLine().getStatusCode()) {
          String errorJson = EntityUtils.toString(response.getEntity(), UTF_8);
          JSONObject json = (JSONObject) parser.parse(errorJson);
          JSONObject error = (JSONObject) json.get("error");
          String errorCode = (String) error.get("code");
          if (SchedulerTransactionLockException.ERROR_CODE.equals(errorCode)) {
            logger.info("Event is locked by a transaction, unable to update event {}", eventId);
            throw new SchedulerTransactionLockException(
                    "Event is locked by a transaction, unable to update event " + eventId);
          } else if (SchedulerConflictException.ERROR_CODE.equals(errorCode)) {
            logger.info("Conflicting events found when updating event {}", eventId);
            throw new SchedulerConflictException("Conflicting events found when updating event " + eventId);
          } else {
            throw new SchedulerException("Unexpected error code " + errorCode);
          }
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to update the event {}.", eventId);
          throw new UnauthorizedException("Unauthorized to update the event " + eventId);
        } else if (SC_FORBIDDEN == response.getStatusLine().getStatusCode()) {
          logger.info("Forbidden to update the event {}.", eventId);
          throw new SchedulerException("Event with specified ID cannot be updated");
        } else {
          throw new SchedulerException("Unexpected status code " + response.getStatusLine());
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (SchedulerTransactionLockException e) {
      throw e;
    } catch (SchedulerConflictException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to update event " + eventId + " to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to update  event " + eventId);
  }

  @Override
  public void removeEvent(String eventId)
          throws NotFoundException, UnauthorizedException, SchedulerTransactionLockException, SchedulerException {
    logger.debug("Start removing event {} from scheduling service.", eventId);
    HttpDelete delete = new HttpDelete("/" + eventId);

    HttpResponse response = getResponse(delete, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED, SC_CONFLICT);
    try {
      if (response != null && SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
        logger.info("Event {} was not found by the scheduler service", eventId);
        throw new NotFoundException("Event '" + eventId + "' not found on remote scheduler service!");
      } else if (response != null && SC_OK == response.getStatusLine().getStatusCode()) {
        logger.info("Event {} removed from scheduling service.", eventId);
        return;
      } else if (response != null && SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
        logger.info("Unauthorized to remove the event {}.", eventId);
        throw new UnauthorizedException("Unauthorized to remove the event " + eventId);
      } else if (response != null && SC_CONFLICT == response.getStatusLine().getStatusCode()) {
        logger.info("Event is locked by a transaction, unable to delete event {}", eventId);
        throw new SchedulerTransactionLockException(
                "Event is locked by a transaction, unable to delete event " + eventId);
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (NotFoundException e) {
      throw e;
    } catch (SchedulerTransactionLockException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to remove event " + eventId + " from the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to remove  event " + eventId);
  }

  @Override
  public MediaPackage getMediaPackage(String eventId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet(eventId.concat("/mediapackage.xml"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Event mediapackage '" + eventId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get mediapacakge of the event {}.", eventId);
          throw new UnauthorizedException("Unauthorized to get mediapackage of the event " + eventId);
        } else {
          MediaPackage mp = MediaPackageParser.getFromXml(EntityUtils.toString(response.getEntity(), UTF_8));
          logger.info("Successfully get event mediapackage {} from the remote scheduler service", eventId);
          return mp;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to parse event media package from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event media package from remote scheduler service");
  }

  @Override
  public DublinCoreCatalog getDublinCore(String eventId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet(eventId.concat("/dublincore.xml"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Event catalog '" + eventId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get dublincore of the event {}.", eventId);
          throw new UnauthorizedException("Unauthorized to get dublincore of the event " + eventId);
        } else {
          DublinCoreCatalog dublinCoreCatalog = DublinCores.read(response.getEntity().getContent());
          logger.info("Successfully get event dublincore {} from the remote scheduler service", eventId);
          return dublinCoreCatalog;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to parse event dublincore from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event dublincore from remote scheduler service");
  }

  @Override
  public TechnicalMetadata getTechnicalMetadata(String eventId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet(eventId.concat("/technical.json"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Event with id '" + eventId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get the technical metadata of the event {}.", eventId);
          throw new UnauthorizedException("Unauthorized to get the technical metadata of the event " + eventId);
        } else {
          String technicalMetadataJson = EntityUtils.toString(response.getEntity(), UTF_8);
          JSONObject json = (JSONObject) parser.parse(technicalMetadataJson);
          final String recordingId = (String) json.get("id");
          final Date start = new Date(DateTimeSupport.fromUTC((String) json.get("start")));
          final Date end = new Date(DateTimeSupport.fromUTC((String) json.get("end")));
          final boolean optOut = (Boolean) json.get("optOut");
          final String location = (String) json.get("location");

          final Set<String> presenters = new HashSet<>();
          JSONArray presentersArr = (JSONArray) json.get("presenters");
          for (int i = 0; i < presentersArr.size(); i++) {
            presenters.add((String) presentersArr.get(i));
          }

          final Map<String, String> wfProperties = new HashMap<>();
          JSONObject wfPropertiesObj = (JSONObject) json.get("wfProperties");
          Set<Entry<String, String>> entrySet = wfPropertiesObj.entrySet();
          for (Entry<String, String> entry : entrySet) {
            wfProperties.put(entry.getKey(), entry.getValue());
          }

          final Map<String, String> agentConfig = new HashMap<>();
          JSONObject agentConfigObj = (JSONObject) json.get("agentConfig");
          entrySet = agentConfigObj.entrySet();
          for (Entry<String, String> entry : entrySet) {
            agentConfig.put(entry.getKey(), entry.getValue());
          }

          String status = (String) json.get("state");
          String lastHeard = (String) json.get("lastHeardFrom");
          Recording recording = null;
          if (StringUtils.isNotBlank(status) && StringUtils.isNotBlank(lastHeard)) {
            recording = new RecordingImpl(recordingId, status, DateTimeSupport.fromUTC(lastHeard));
          }
          final Opt<Recording> recordingOpt = Opt.nul(recording);
          logger.info("Successfully get the technical metadata of event '{}' from the remote scheduler service",
                  eventId);
          return new TechnicalMetadataImpl(recordingId, location, start, end, optOut, presenters, wfProperties,
                  agentConfig, recordingOpt);
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to parse the technical metadata from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get the technical metadata from remote scheduler service");
  }

  @Override
  public AccessControlList getAccessControlList(String eventId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet(eventId.concat("/acl"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Event '" + eventId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get acl of the event {}.", eventId);
          throw new UnauthorizedException("Unauthorized to get acl of the event " + eventId);
        } else {
          String aclString = EntityUtils.toString(response.getEntity(), UTF_8);
          AccessControlList accessControlList = AccessControlParser.parseAcl(aclString);
          logger.info("Successfully get event {} access control list from the remote scheduler service", eventId);
          return accessControlList;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get event access control list from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event access control list from remote scheduler service");
  }

  @Override
  public Map<String, String> getWorkflowConfig(String eventId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet(eventId.concat("/workflow.properties"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException(
                  "Event workflow configuration '" + eventId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get workflow config of the event {}.", eventId);
          throw new UnauthorizedException("Unauthorized to get workflow config of the event " + eventId);
        } else {
          Properties properties = new Properties();
          properties.load(response.getEntity().getContent());
          logger.info("Successfully get event workflow configuration {} from the remote scheduler service", eventId);
          return new HashMap<String, String>((Map) properties);
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to parse event workflow configuration from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event workflow configuration from remote scheduler service");
  }

  @Override
  public Map<String, String> getCaptureAgentConfiguration(String eventId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet(eventId.concat("/agent.properties"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException(
                  "Event capture agent configuration '" + eventId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get capture agent config of the event {}.", eventId);
          throw new UnauthorizedException("Unauthorized to get capture agent config of the event " + eventId);
        } else {
          Properties properties = new Properties();
          properties.load(response.getEntity().getContent());
          logger.info("Successfully get event capture agent configuration {} from the remote scheduler service",
                  eventId);
          return new HashMap<String, String>((Map) properties);
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException(
              "Unable to parse event capture agent configuration from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event capture agent configuration from remote scheduler service");
  }

  @Override
  public boolean isOptOut(String mediapackageId) throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet(UrlSupport.concat(mediapackageId, "optOut"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException(
                  "Event with mediapackage id '" + mediapackageId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get opt out status of the event {}.", mediapackageId);
          throw new UnauthorizedException("Unauthorized to get opt out status of the event " + mediapackageId);
        } else {
          String optOutString = EntityUtils.toString(response.getEntity(), UTF_8);
          Boolean booleanObject = BooleanUtils.toBooleanObject(optOutString);
          if (booleanObject == null)
            throw new SchedulerException(
                    "Could not parse opt out status from the remote scheduler service: " + optOutString);

          logger.info(
                  "Successfully get opt out status of event with mediapackage id {} from the remote scheduler service",
                  mediapackageId);
          return booleanObject.booleanValue();
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get event opt out status from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event opt out status from remote scheduler service");
  }

  @Override
  public void updateReviewStatus(String mediapackageId, ReviewStatus reviewStatus)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpPut put = new HttpPut(UrlSupport.concat(mediapackageId, "reviewStatus"));

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("reviewStatus", reviewStatus.toString()));
    put.setEntity(new UrlEncodedFormEntity(params, UTF_8));

    HttpResponse response = getResponse(put, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          logger.warn("Event with mediapackage id {} was not found by the scheduler service", mediapackageId);
          throw new NotFoundException(
                  "Event with mediapackage id '" + mediapackageId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to update review status of the event {}.", mediapackageId);
          throw new UnauthorizedException("Unauthorized to update review status of the event " + mediapackageId);
        } else if (SC_OK == response.getStatusLine().getStatusCode()) {
          logger.info("Event with mediapackage id {} successfully updated with review status.", mediapackageId);
          return;
        } else {
          throw new SchedulerException("Unexpected status code " + response.getStatusLine());
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException(
              "Unable to update event with mediapackage id " + mediapackageId + " to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to update  event with mediapackage id " + mediapackageId);
  }

  @Override
  public ReviewStatus getReviewStatus(String mediapackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet(UrlSupport.concat(mediapackageId, "reviewStatus"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException(
                  "Event with mediapackage id '" + mediapackageId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get review status of the event {}.", mediapackageId);
          throw new UnauthorizedException("Unauthorized to get review status of the event " + mediapackageId);
        } else {
          String reviewStatusString = EntityUtils.toString(response.getEntity(), UTF_8);

          ReviewStatus reviewStatus;
          try {
            reviewStatus = ReviewStatus.valueOf(reviewStatusString);
          } catch (Exception e) {
            throw new SchedulerException(
                    "Could not parse review status from the remote scheduler service: " + reviewStatusString);
          }
          logger.info(
                  "Successfully get review status of event with mediapackage id {} from the remote scheduler service",
                  mediapackageId);
          return reviewStatus;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get event review status from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event review status from remote scheduler service");
  }

  @Override
  public boolean isBlacklisted(String mediapackageId)
          throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpGet get = new HttpGet(UrlSupport.concat(mediapackageId, "blacklisted"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException(
                  "Event with mediapackage id '" + mediapackageId + "' not found on remote scheduler service!");
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to get blacklist status of the event {}.", mediapackageId);
          throw new UnauthorizedException("Unauthorized to get blacklist status of the event " + mediapackageId);
        } else {
          String blacklistString = EntityUtils.toString(response.getEntity(), UTF_8);

          Boolean booleanObject = BooleanUtils.toBooleanObject(blacklistString);
          if (booleanObject == null)
            throw new SchedulerException(
                    "Could not parse blacklist status from the remote scheduler service: " + blacklistString);

          logger.info(
                  "Successfully get blacklist status of event with mediapackage id {} from the remote scheduler service",
                  mediapackageId);
          return booleanObject.booleanValue();
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get event blacklist status from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get event blacklist status from remote scheduler service");
  }

  @Override
  public String getScheduleLastModified(String agentId) throws SchedulerException {
    HttpGet get = new HttpGet(UrlSupport.concat(agentId, "lastmodified"));
    HttpResponse response = getResponse(get, SC_OK);
    try {
      if (response != null) {
        if (SC_OK == response.getStatusLine().getStatusCode()) {
          String agentHash = EntityUtils.toString(response.getEntity(), UTF_8);
          logger.info("Successfully get agent last modified hash of agent with id {} from the remote scheduler service",
                  agentId);
          return agentHash;
        }
      }
    } catch (Exception e) {
      throw new SchedulerException("Unable to get agent last modified hash from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get agent last modified hash from remote scheduler service");
  }

  @Override
  public List<MediaPackage> search(Opt<String> captureAgentId, Opt<Date> startsFrom, Opt<Date> startsTo,
          Opt<Date> endFrom, Opt<Date> endTo) throws UnauthorizedException, SchedulerException {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    for (String s : captureAgentId) {
      queryStringParams.add(new BasicNameValuePair("agent", s));
    }
    for (Date d : startsFrom) {
      queryStringParams.add(new BasicNameValuePair("startsfrom", Long.toString(d.getTime())));
    }
    for (Date d : startsTo) {
      queryStringParams.add(new BasicNameValuePair("startsto", Long.toString(d.getTime())));
    }
    for (Date d : endFrom) {
      queryStringParams.add(new BasicNameValuePair("endsfrom", Long.toString(d.getTime())));
    }
    for (Date d : endTo) {
      queryStringParams.add(new BasicNameValuePair("endsto", Long.toString(d.getTime())));
    }
    HttpGet get = new HttpGet("recordings.xml?".concat(URLEncodedUtils.format(queryStringParams, UTF_8)));
    HttpResponse response = getResponse(get, SC_OK, SC_UNAUTHORIZED);
    try {
      if (response != null) {
        if (SC_OK == response.getStatusLine().getStatusCode()) {
          String mediaPackageXml = EntityUtils.toString(response.getEntity(), UTF_8);
          List<MediaPackage> events = MediaPackageParser.getArrayFromXml(mediaPackageXml);
          logger.info("Successfully get recordings from the remote scheduler service");
          return events;
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to search for events");
          throw new UnauthorizedException("Unauthorized to search for events");
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get recordings from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get recordings from remote scheduler service");
  }

  @Override
  public List<MediaPackage> findConflictingEvents(String captureDeviceID, Date startDate, Date endDate)
          throws UnauthorizedException, SchedulerException {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("agent", captureDeviceID));
    queryStringParams.add(new BasicNameValuePair("start", Long.toString(startDate.getTime())));
    queryStringParams.add(new BasicNameValuePair("end", Long.toString(endDate.getTime())));
    HttpGet get = new HttpGet("conflicts.xml?".concat(URLEncodedUtils.format(queryStringParams, UTF_8)));
    HttpResponse response = getResponse(get, SC_OK, SC_NO_CONTENT);
    try {
      if (response != null) {
        if (SC_OK == response.getStatusLine().getStatusCode()) {
          String mediaPackageXml = EntityUtils.toString(response.getEntity(), UTF_8);
          List<MediaPackage> events = MediaPackageParser.getArrayFromXml(mediaPackageXml);
          logger.info("Successfully get conflicts from the remote scheduler service");
          return events;
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to search for conflicting events");
          throw new UnauthorizedException("Unauthorized to search for conflicting events");
        } else if (SC_NO_CONTENT == response.getStatusLine().getStatusCode()) {
          return Collections.<MediaPackage> emptyList();
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get conflicts from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get conflicts from remote scheduler service");
  }

  @Override
  public List<MediaPackage> findConflictingEvents(String captureAgentId, RRule rrule, Date startDate, Date endDate,
          long duration, TimeZone timezone) throws UnauthorizedException, SchedulerException {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    queryStringParams.add(new BasicNameValuePair("agent", captureAgentId));
    queryStringParams.add(new BasicNameValuePair("rrule", rrule.getRecur().toString()));
    queryStringParams.add(new BasicNameValuePair("start", Long.toString(startDate.getTime())));
    queryStringParams.add(new BasicNameValuePair("end", Long.toString(endDate.getTime())));
    queryStringParams.add(new BasicNameValuePair("duration", Long.toString(duration)));
    queryStringParams.add(new BasicNameValuePair("timezone", timezone.getID()));
    HttpGet get = new HttpGet("conflicts.xml?".concat(URLEncodedUtils.format(queryStringParams, UTF_8)));
    HttpResponse response = getResponse(get, SC_OK, SC_NO_CONTENT);
    try {
      if (response != null) {
        if (SC_OK == response.getStatusLine().getStatusCode()) {
          String mediaPackageXml = EntityUtils.toString(response.getEntity(), UTF_8);
          List<MediaPackage> events = MediaPackageParser.getArrayFromXml(mediaPackageXml);
          logger.info("Successfully get conflicts from the remote scheduler service");
          return events;
        } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
          logger.info("Unauthorized to search for conflicting events");
          throw new UnauthorizedException("Unauthorized to search for conflicting events");
        } else if (SC_NO_CONTENT == response.getStatusLine().getStatusCode()) {
          return Collections.<MediaPackage> emptyList();
        }
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get conflicts from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get conflicts from remote scheduler service");
  }

  @Override
  public String getCalendar(Opt<String> captureAgentId, Opt<String> seriesId, Opt<Date> cutoff)
          throws SchedulerException {
    List<NameValuePair> queryStringParams = new ArrayList<NameValuePair>();
    for (String s : captureAgentId) {
      queryStringParams.add(new BasicNameValuePair("agentid", s));
    }
    for (String s : seriesId) {
      queryStringParams.add(new BasicNameValuePair("seriesid", s));
    }
    for (Date d : cutoff) {
      queryStringParams.add(new BasicNameValuePair("cutoff", Long.toString(d.getTime())));
    }
    HttpGet get = new HttpGet("calendars?".concat(URLEncodedUtils.format(queryStringParams, UTF_8)));
    HttpResponse response = getResponse(get, SC_OK);
    try {
      if (response != null) {
        if (SC_OK == response.getStatusLine().getStatusCode()) {
          String calendar = EntityUtils.toString(response.getEntity(), UTF_8);
          logger.info("Successfully get calendar of agent with id {} from the remote scheduler service",
                  captureAgentId);
          return calendar;
        }
      }
    } catch (Exception e) {
      throw new SchedulerException("Unable to get calendar from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get calendar from remote scheduler service");
  }

  @Override
  public void removeScheduledRecordingsBeforeBuffer(long buffer) throws UnauthorizedException, SchedulerException {
    HttpPost post = new HttpPost("/removeOldScheduledRecordings");
    logger.debug("Start removing old schedules before buffer {} through remote Schedule Service", buffer);

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("buffer", Long.toString(buffer)));
    post.setEntity(new UrlEncodedFormEntity(params, UTF_8));

    HttpResponse response = getResponse(post, SC_OK, SC_UNAUTHORIZED);
    try {
      if (response != null && SC_OK == response.getStatusLine().getStatusCode()) {
        logger.info("Successfully started removing old schedules before butter {} to the scheduler service", buffer);
        return;
      } else if (SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
        logger.info("Unauthorized to remove old schedules before buffer {}.", buffer);
        throw new UnauthorizedException("Unauthorized to remove old schedules");
      }
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to remove old schedules from the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to remove old schedules from the scheduler service");
  }

  @Override
  public boolean updateRecordingState(String mediapackageId, String state)
          throws NotFoundException, SchedulerException {
    HttpPut put = new HttpPut(UrlSupport.concat(mediapackageId, "recordingStatus"));

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("state", state));
    put.setEntity(new UrlEncodedFormEntity(params, UTF_8));

    HttpResponse response = getResponse(put, SC_OK, SC_NOT_FOUND, SC_BAD_REQUEST);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          logger.warn("Event with mediapackage id {} was not found by the scheduler service", mediapackageId);
          throw new NotFoundException(
                  "Event with mediapackage id '" + mediapackageId + "' not found on remote scheduler service!");
        } else if (SC_BAD_REQUEST == response.getStatusLine().getStatusCode()) {
          logger.info("Unable to update event with mediapackage id {}, invalid recording state: {}.", mediapackageId,
                  state);
          return false;
        } else if (SC_OK == response.getStatusLine().getStatusCode()) {
          logger.info("Event with mediapackage id {} successfully updated with recording status.", mediapackageId);
          return true;
        } else {
          throw new SchedulerException("Unexpected status code " + response.getStatusLine());
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to update recording state of event with mediapackage id " + mediapackageId
              + " to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to update recording state of event with mediapackage id " + mediapackageId);
  }

  @Override
  public Recording getRecordingState(String id) throws NotFoundException, SchedulerException {
    HttpGet get = new HttpGet(UrlSupport.concat(id, "recordingStatus"));
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_OK == response.getStatusLine().getStatusCode()) {
          String recordingStateJson = EntityUtils.toString(response.getEntity(), UTF_8);
          JSONObject json = (JSONObject) parser.parse(recordingStateJson);
          String recordingId = (String) json.get("id");
          String status = (String) json.get("state");
          Long lastHeard = (Long) json.get("lastHeardFrom");
          logger.info("Successfully get calendar of agent with id {} from the remote scheduler service", id);
          return new RecordingImpl(recordingId, status, lastHeard);
        } else if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          logger.warn("Event with mediapackage id {} was not found by the scheduler service", id);
          throw new NotFoundException("Event with mediapackage id '" + id + "' not found on remote scheduler service!");
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException("Unable to get calendar from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get calendar from remote scheduler service");
  }

  @Override
  public void removeRecording(String eventId) throws NotFoundException, SchedulerException {
    HttpDelete delete = new HttpDelete(UrlSupport.concat(eventId, "recordingStatus"));

    HttpResponse response = getResponse(delete, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null && SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
        logger.info("Event {} was not found by the scheduler service", eventId);
        throw new NotFoundException("Event '" + eventId + "' not found on remote scheduler service!");
      } else if (response != null && SC_OK == response.getStatusLine().getStatusCode()) {
        logger.info("Recording status of event {} removed from scheduling service.", eventId);
        return;
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException(
              "Unable to remove recording status of event " + eventId + " from the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to remove  recording status of event " + eventId);
  }

  @Override
  public Map<String, Recording> getKnownRecordings() throws SchedulerException {
    HttpGet get = new HttpGet("recordingStatus");
    HttpResponse response = getResponse(get, SC_OK);
    try {
      if (response != null) {
        if (SC_OK == response.getStatusLine().getStatusCode()) {
          String recordingStates = EntityUtils.toString(response.getEntity(), UTF_8);
          JSONArray recordings = (JSONArray) parser.parse(recordingStates);
          Map<String, Recording> recordingsMap = new HashMap<String, Recording>();
          for (int i = 0; i < recordings.size(); i++) {
            JSONObject recording = (JSONObject) recordings.get(i);
            String recordingId = (String) recording.get("id");
            String status = (String) recording.get("state");
            Long lastHeard = (Long) recording.get("lastHeardFrom");
            recordingsMap.put(recordingId, new RecordingImpl(recordingId, status, lastHeard));
          }
          logger.info("Successfully get recording states from the remote scheduler service");
          return recordingsMap;
        }
      }
    } catch (Exception e) {
      throw new SchedulerException("Unable to get recording states from remote scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to get recording states from remote scheduler service");
  }

  private void addTransactionEvent(String id, Date startDateTime, Date endDateTime, String captureAgentId,
          Set<String> userIds, MediaPackage mediaPackage, Map<String, String> wfProperties,
          Map<String, String> caMetadata, Opt<Boolean> optOut)
                  throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpPut put = new HttpPut(UrlSupport.concat("transaction", id, "add"));

    List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
    params.add(new BasicNameValuePair("start", Long.toString(startDateTime.getTime())));
    params.add(new BasicNameValuePair("end", Long.toString(endDateTime.getTime())));
    params.add(new BasicNameValuePair("agent", captureAgentId));
    params.add(new BasicNameValuePair("users", StringUtils.join(userIds, ",")));
    params.add(new BasicNameValuePair("mediaPackage", MediaPackageParser.getAsXml(mediaPackage)));
    params.add(new BasicNameValuePair("wfproperties", toPropertyString(wfProperties)));
    params.add(new BasicNameValuePair("agentparameters", toPropertyString(caMetadata)));
    if (optOut.isSome())
      params.add(new BasicNameValuePair("optOut", Boolean.toString(optOut.get())));
    put.setEntity(new UrlEncodedFormEntity(params, UTF_8));

    final String eventId = mediaPackage.getIdentifier().compact();
    HttpResponse response = getResponse(put, SC_OK, SC_NOT_FOUND, SC_CONFLICT, SC_UNAUTHORIZED);
    try {
      if (response != null && SC_OK == response.getStatusLine().getStatusCode()) {
        logger.info("Successfully added event '{}' to scheduler transaction '{}' on the scheduler service", eventId,
                id);
        return;
      } else if (response != null && SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
        logger.info("Unauthorized to add event '{}' to scheduler transaction '{}'", eventId, id);
        throw new UnauthorizedException("Unauthorized to add event to scheduler transaction");
      } else if (response != null && SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
        logger.info("Scheduler transaction '{}' not found", id);
        throw new NotFoundException("Not found scheduler transaction " + id);
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException(
              "Unable to add event to scheduler transaction '" + id + "' on the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to add event to scheduler transaction '" + id + "' on the scheduler service");
  }

  private void commitTransaction(String id) throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpPost post = new HttpPost(UrlSupport.concat("transaction", id, "commit"));

    HttpResponse response = getResponse(post, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null && SC_OK == response.getStatusLine().getStatusCode()) {
        logger.info("Successfully committed scheduler transaction '{}' to the scheduler service", id);
        return;
      } else if (response != null && SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
        logger.info("Unauthorized to commit scheduler transaction '{}'", id);
        throw new UnauthorizedException("Unauthorized to commit scheduler transaction");
      } else if (response != null && SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
        logger.info("Scheduler transaction '{}' not found", id);
        throw new NotFoundException("Not found scheduler transaction " + id);
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException(
              "Unable to commit scheduler transaction '" + id + "' to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to commit scheduler transaction '" + id + "' to the scheduler service");
  }

  private void rollbackTransaction(String id) throws NotFoundException, UnauthorizedException, SchedulerException {
    HttpPost post = new HttpPost(UrlSupport.concat("transaction", id, "rollback"));

    HttpResponse response = getResponse(post, SC_OK, SC_NOT_FOUND, SC_UNAUTHORIZED);
    try {
      if (response != null && SC_OK == response.getStatusLine().getStatusCode()) {
        logger.info("Successfully rolled back scheduler transaction '{}' to the scheduler service", id);
        return;
      } else if (response != null && SC_UNAUTHORIZED == response.getStatusLine().getStatusCode()) {
        logger.info("Unauthorized to rollback scheduler transaction '{}'", id);
        throw new UnauthorizedException("Unauthorized to rollback scheduler transaction");
      } else if (response != null && SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
        logger.info("Scheduler transaction '{}' not found", id);
        throw new NotFoundException("Not found scheduler transaction " + id);
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (UnauthorizedException e) {
      throw e;
    } catch (Exception e) {
      throw new SchedulerException(
              "Unable to rollback scheduler transaction '" + id + "' to the scheduler service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new SchedulerException("Unable to rollback scheduler transaction '" + id + "' to the scheduler service");
  }

  private final class SchedulerTransactionRemoteImpl implements SchedulerTransaction {

    private final String id;
    private final String schedulingSource;

    SchedulerTransactionRemoteImpl(String id, String schedulingSource) {
      this.id = id;
      this.schedulingSource = schedulingSource;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public String getSource() {
      return schedulingSource;
    }

    @Override
    public void addEvent(Date startDateTime, Date endDateTime, String captureAgentId, Set<String> userIds,
            MediaPackage mediaPackage, Map<String, String> wfProperties, Map<String, String> caMetadata,
            Opt<Boolean> optOut) throws NotFoundException, UnauthorizedException, SchedulerException {
      addTransactionEvent(id, startDateTime, endDateTime, captureAgentId, userIds, mediaPackage, wfProperties,
              caMetadata, optOut);
    }

    @Override
    public void commit() throws NotFoundException, UnauthorizedException, SchedulerException {
      commitTransaction(id);
    }

    @Override
    public void rollback() throws NotFoundException, UnauthorizedException, SchedulerException {
      rollbackTransaction(id);
    }

    @Override
    public int hashCode() {
      return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SchedulerTransaction) {
        return getId().equals(((SchedulerTransaction) obj).getId());
      }
      return false;
    }

  }

  private String toPropertyString(Map<String, String> properties) {
    StringBuilder wfPropertiesString = new StringBuilder();
    for (Map.Entry<String, String> entry : properties.entrySet())
      wfPropertiesString.append(entry.getKey() + "=" + entry.getValue() + "\n");
    return wfPropertiesString.toString();
  }

}
