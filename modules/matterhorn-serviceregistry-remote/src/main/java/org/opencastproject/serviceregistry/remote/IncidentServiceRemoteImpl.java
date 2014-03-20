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
package org.opencastproject.serviceregistry.remote;

import static org.apache.http.HttpStatus.SC_CONFLICT;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.opencastproject.util.IoSupport.closeAfterwards;

import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.job.api.IncidentParser;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobParser;
import org.opencastproject.serviceregistry.api.IncidentL10n;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.IncidentServiceException;
import org.opencastproject.serviceregistry.api.RemoteBase;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A proxy to a remote incident service.
 */
public class IncidentServiceRemoteImpl extends RemoteBase implements IncidentService {
  /** The logger */
  private static final Logger logger = LoggerFactory.getLogger(IncidentServiceRemoteImpl.class);

  private static final IncidentParser parser = IncidentParser.I;

  public IncidentServiceRemoteImpl() {
    super(JOB_TYPE);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Incident storeIncident(Job job, Date timestamp, String code, Severity severity,
          Map<String, String> descriptionParameters, List<Tuple<String, String>> details)
          throws IncidentServiceException, IllegalStateException {
    HttpPost post = new HttpPost();
    try {
      List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
      params.add(new BasicNameValuePair("job", JobParser.toXml(job)));
      params.add(new BasicNameValuePair("date", DateTimeSupport.toUTC(timestamp.getTime())));
      params.add(new BasicNameValuePair("code", code));
      params.add(new BasicNameValuePair("severity", severity.name()));
      if (descriptionParameters != null)
        params.add(new BasicNameValuePair("params", mapToString(descriptionParameters)));
      if (details != null) {
        JSONArray json = new JSONArray();
        for (Tuple<String, String> detail : details) {
          JSONObject jsTuple = new JSONObject();
          jsTuple.put("title", detail.getA());
          jsTuple.put("content", detail.getB());
          json.add(jsTuple);
        }
        params.add(new BasicNameValuePair("details", json.toJSONString()));
      }
      post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
    } catch (Exception e) {
      throw new IncidentServiceException("Unable to assemble a remote incident service request", e);
    }
    HttpResponse response = getResponse(post, SC_CREATED, SC_CONFLICT);
    try {
      if (response != null) {
        if (response.getStatusLine().getStatusCode() == SC_CONFLICT) {
          throw new IllegalStateException("No related job " + job.getId() + " of incident job found");
        } else if (response.getStatusLine().getStatusCode() == SC_CREATED) {
          Incident incident = parser.parseIncidentFromXml(response.getEntity().getContent()).toIncident();
          logger.info("Incident '{}' created", incident.getId());
          return incident;
        }
      }
    } catch (Exception e) {
      throw new IncidentServiceException(e);
    } finally {
      closeConnection(response);
    }
    throw new IncidentServiceException("Unable to store an incident of job " + job.getId());
  }

  @Override
  public Incident getIncident(long incidentId) throws IncidentServiceException, NotFoundException {
    final Function<InputStream, Incident> handler = new Function.X<InputStream, Incident>() {
      @Override public Incident xapply(InputStream in) throws IOException {
        return parser.parseIncidentFromXml(in).toIncident();
      }
    };
    return getIncidentXml(handler, incidentId);
  }

  public <A> A getIncidentXml(Function<InputStream, A> handler, long incidentId)
          throws IncidentServiceException, NotFoundException {
    final HttpGet get = new HttpGet(incidentId + ".xml");
    final HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Incident from incident id " + incidentId
                                              + " not found in remote incident service!");
        } else {
          final A result = closeAfterwards(handler).apply(response.getEntity().getContent());
          logger.debug("Successfully received incident from incident id {} from the remote incident service",
                       incidentId);
          return result;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      throw new IncidentServiceException("Unable to parse incident from remote incident service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new IncidentServiceException("Unable to get incident from remote incident service");
  }

  @Override
  public IncidentL10n getLocalization(long id, Locale locale) throws IncidentServiceException, NotFoundException {
    HttpGet get = new HttpGet(UrlSupport.concat("localization", Long.toString(id)) + "?locale=" + locale.toString());
    HttpResponse response = getResponse(get, SC_OK);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("No localization found for the job incident with id " + id);
        } else if (SC_OK == response.getStatusLine().getStatusCode()) {
          String json = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
          final JSONObject obj = (JSONObject) JSONValue.parse(json);
          logger.debug(
                  "Successfully received localization from job incident id {} with the locale {} from the remote incident service",
                  id, locale.toString());
          return new IncidentL10n() {
            @Override
            public String getTitle() {
              return (String) obj.get("title");
            }

            @Override
            public String getDescription() {
              return (String) obj.get("description");
            }
          };
        }
      }
    } catch (Exception e) {
      throw new IncidentServiceException("Unable to get localization of job incident from remote incident service: "
              + e);
    } finally {
      closeConnection(response);
    }
    throw new IncidentServiceException("Unable to get localization of job incident from remote incident service");
  }

  @Override
  public IncidentTree getIncidentsOfJob(long jobId, boolean cascade) throws NotFoundException,
          IncidentServiceException {
    final Function<InputStream, IncidentTree> handler = new Function.X<InputStream, IncidentTree>() {
      @Override public IncidentTree xapply(InputStream in) throws Exception {
        return parser.parseIncidentTreeFromXml(in).toIncidentTree();
      }
    };
    return getIncidentsOfJobXml(handler, jobId, cascade);
  }

  public <A> A getIncidentsOfJobXml(Function<InputStream, A> handler, long jobId, boolean cascade) throws NotFoundException,
          IncidentServiceException {
    HttpGet get = new HttpGet("job/" + jobId + ".xml?cascade=" + Boolean.toString(cascade) + "&format=sys");
    HttpResponse response = getResponse(get, SC_OK, SC_NOT_FOUND);
    try {
      if (response != null) {
        if (SC_NOT_FOUND == response.getStatusLine().getStatusCode()) {
          throw new NotFoundException("Incident from job id " + jobId + " not found in remote incident service!");
        } else {
          final A result = closeAfterwards(handler).apply(response.getEntity().getContent());
          logger.debug("Successfully received incident from job id {} from the remote incident service", jobId);
          return result;
        }
      }
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unable to parse incident form remote incident service", e);
      throw new IncidentServiceException("Unable to parse incident from remote incident service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new IncidentServiceException("Unable to get incident from remote incident service");
  }

  @Override
  public List<Incident> getIncidentsOfJob(List<Long> jobIds) throws IncidentServiceException {
    final Function<InputStream, List<Incident>> handler = new Function.X<InputStream, List<Incident>>() {
      @Override public List<Incident> xapply(InputStream in) throws Exception {
        return parser.parseIncidentsFromXml(in).toIncidents();
      }
    };
    return getIncidentsOfJobXml(handler, jobIds);
  }

  public <A> A getIncidentsOfJobXml(Function<InputStream, A> handler, List<Long> jobIds) throws IncidentServiceException {
    StringBuilder url = new StringBuilder("job/incidents.xml?format=sys");
    if (!jobIds.isEmpty()) {
      for (Long jobId : jobIds) {
        url.append("&id=").append(jobId);
      }
    }
    HttpGet get = new HttpGet(url.toString());
    HttpResponse response = getResponse(get);
    try {
      if (response != null && SC_OK == response.getStatusLine().getStatusCode()) {
        final A result = closeAfterwards(handler).apply(response.getEntity().getContent());
        logger.debug("Successfully received incident summary from job ids {} from the remote incident service", jobIds);
        return result;
      }
    } catch (Exception e) {
      throw new IncidentServiceException("Unable to parse incidents from remote incident service: " + e);
    } finally {
      closeConnection(response);
    }
    throw new IncidentServiceException("Unable to get incidents from remote incident service");
  }

  /**
   * Converts a Map<String, String> to s key=value\n string, suitable for the properties form parameter expected by the
   * workflow rest endpoint.
   * 
   * @param props
   *          The map of strings
   * @return the string representation
   */
  private String mapToString(Map<String, String> props) {
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : props.entrySet()) {
      sb.append(entry.getKey());
      sb.append("=");
      sb.append(entry.getValue());
      sb.append("\n");
    }
    return sb.toString();
  }
}
