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
package org.opencastproject.assetmanager.impl.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.ok;

import org.opencastproject.assetmanager.impl.AssetManagerJobProducer;
import org.opencastproject.assetmanager.impl.VersionImpl;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobProducer;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;

import org.apache.commons.lang3.StringUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public abstract class AbstractTieredStorageAssetManagerRestEndpoint extends AbstractAssetManagerRestEndpoint {
  public static final String SDF_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

  protected AssetManagerJobProducer tsamjp = null;
  protected ServiceRegistry serviceRegistry = null;

  public void setJobProducer(AssetManagerJobProducer producer) {
    tsamjp = producer;
  }

  @Override
  public JobProducer getService() {
    return this.tsamjp;
  }

  /**
   * Callback from the OSGi declarative services to set the service registry.
   *
   * @param serviceRegistry
   *          the service registry
   */
  protected void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public ServiceRegistry getServiceRegistry() {
    return this.serviceRegistry;
  }

  @POST
  @Path("moveById")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "moveById", description = "Move a mediapackage based on its ID.",
      restParameters = {
          @RestParameter(
              name = "id",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The mediapackage ID to move."),
          @RestParameter(
              name = "target",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The target storage to move the mediapackage to.")},
      responses = {
          @RestResponse(
              description = "The job created to move the snapshot.",
              responseCode = SC_OK),
          @RestResponse(
              description = "Invalid parameters, and the job was not created",
              responseCode = SC_BAD_REQUEST),
          @RestResponse(
              description = "There has been an internal error, and the job was not created",
              responseCode = SC_INTERNAL_SERVER_ERROR)},
      returnDescription = "The Job created")
  public Response moveById(@FormParam("id") final String id, @FormParam("target") final String target) {
    String mpid = StringUtils.trimToNull(id);
    if (null == mpid) {
      return badRequest("Invalid mediapackage ID: " + mpid);
    }

    final String trimmedTarget = StringUtils.trimToNull(target);
    if (null == trimmedTarget) {
      return badRequest("Invalid target store ID: " + trimmedTarget);
    } else if (!tsamjp.datastoreExists(trimmedTarget)) {
      return badRequest("Target store " + trimmedTarget + " not found");
    }

    try {
      Job j = tsamjp.moveById(mpid, trimmedTarget);
      return Response.ok(new JaxbJob(j)).build();
    } catch (Exception e) {
      return handleException(e);
    }
  }

  @POST
  @Path("moveByIdAndVersion")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "moveByIdAndVersion", description = "Move a mediapackage based on its ID and version.",
      restParameters = {
          @RestParameter(
              name = "id",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The mediapackage ID to move."),
          @RestParameter(
              name = "version",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The version to move."),
          @RestParameter(
              name = "target",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The target storage to move the mediapackage to.")},
      responses = {
          @RestResponse(
              description = "The job created to move the snapshot.",
              responseCode = SC_OK),
          @RestResponse(
              description = "Invalid parameters, and the job was not created",
              responseCode = SC_BAD_REQUEST),
          @RestResponse(
              description = "There has been an internal error, and the job was not created",
              responseCode = SC_INTERNAL_SERVER_ERROR)},
      returnDescription = "The Job created")
  public Response moveByIdAndVersion(
      @FormParam("id") final String id,
      @FormParam("version") final String version,
      @FormParam("target") final String target
  ) {
    final String mpid = StringUtils.trimToNull(id);
    if (null == mpid) {
      return badRequest("Invalid mediapackage ID: " + mpid);
    }

    final VersionImpl v;
    try {
      Long versionNumber = Long.parseLong(version);
      v = new VersionImpl(versionNumber);
    } catch (NumberFormatException e) {
      return badRequest("Invalid version number format");
    }

    final String trimmedTarget = StringUtils.trimToNull(target);
    if (null == trimmedTarget) {
      return badRequest("Invalid target store ID: " + trimmedTarget);
    } else if (!tsamjp.datastoreExists(trimmedTarget)) {
      return badRequest("Target store " + trimmedTarget + " not found");
    }

    try {
      Job j = tsamjp.moveByIdAndVersion(v, mpid, trimmedTarget);
      return ok(new JaxbJob(j));
    } catch (Exception e) {
      return handleException(e);
    }
  }

  @POST
  @Path("moveByDate")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(name = "moveByDate", description = "Move all snapshots between two dates.",
      restParameters = {
          @RestParameter(
              name = "start",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The start date, in the format yyyy-MM-dd'T'HH:mm:ss'Z'."),
          @RestParameter(
              name = "end",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The end date, in the format yyyy-MM-dd'T'HH:mm:ss'Z'."),
          @RestParameter(
              name = "target",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The target storage to move the mediapackage to.")},
      responses = {
          @RestResponse(
              description = "The job created to move the snapshots.",
              responseCode = SC_OK),
          @RestResponse(
              description = "Invalid parameters, and the job was not created",
              responseCode = SC_BAD_REQUEST),
          @RestResponse(
              description = "There has been an internal error, and the job was not created",
              responseCode = SC_INTERNAL_SERVER_ERROR)},
      returnDescription = "The Job created")
  public Response moveByDate(
      @FormParam("start") final String startString,
      @FormParam("end") final String endString,
      @FormParam("target") final String target
  ) {
    DateFormat sdf = new SimpleDateFormat(SDF_FORMAT);
    Date start;
    Date end;
    try {
      start = sdf.parse(startString);
      end = sdf.parse(endString);
    } catch (ParseException e) {
      return badRequest("Invalid start or end date format");
    }

    if (end.before(start)) {
      return badRequest("Start date " + start + " must be before end date " + end);
    }

    final String trimmedTarget = StringUtils.trimToNull(target);
    if (null == trimmedTarget) {
      return badRequest("Invalid target store ID: " + trimmedTarget);
    } else if (!tsamjp.datastoreExists(trimmedTarget)) {
      return badRequest("Target store " + trimmedTarget + " not found");
    }

    try {
      Job j = tsamjp.moveByDate(start, end, trimmedTarget);
      return ok(new JaxbJob(j));
    } catch (Exception e) {
      return handleException(e);
    }
  }

  @POST
  @Path("moveByIdAndDate")
  @Produces(MediaType.TEXT_XML)
  @RestQuery(
      name = "moveByIdAndDate",
      description = "Move all snapshots for a given mediapackage taken between two dates.",
      restParameters = {
          @RestParameter(
              name = "id",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The mediapackage ID to move."),
          @RestParameter(
              name = "start",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The start date, in the format yyyy-MM-dd'T'HH:mm:ss'Z'."),
          @RestParameter(
              name = "end",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The end date, in the format yyyy-MM-dd'T'HH:mm:ss'Z'."),
          @RestParameter(
              name = "target",
              isRequired = true,
              type = RestParameter.Type.STRING,
              defaultValue = "",
              description = "The target storage to move the mediapackage to.")},
      responses = {
          @RestResponse(
              description = "The job created to move the snapshots.",
              responseCode = SC_OK),
          @RestResponse(
              description = "Invalid parameters, and the job was not created",
              responseCode = SC_BAD_REQUEST),
          @RestResponse(
              description = "There has been an internal error, and the job was not created",
              responseCode = SC_INTERNAL_SERVER_ERROR)},
      returnDescription = "The Job created")
  public Response moveByIdAndDate(
      @FormParam("id") final String id,
      @FormParam("start") final String startString,
      @FormParam("end") final String endString,
      @FormParam("target") final String target
  ) {
    DateFormat sdf = new SimpleDateFormat(SDF_FORMAT);
    final String mpid = StringUtils.trimToNull(id);
    if (null == mpid) {
      return badRequest("Invalid mediapackage ID: " + mpid);
    }

    Date start;
    Date end;
    try {
      start = sdf.parse(startString);
      end = sdf.parse(endString);
    } catch (ParseException e) {
      return badRequest("Invalid start or end date format");
    }

    if (end.before(start)) {
      return badRequest("Start date " + start + " must be before end date " + end);
    }

    final String trimmedTarget = StringUtils.trimToNull(target);
    if (null == trimmedTarget) {
      return badRequest("Invalid target store ID: " + trimmedTarget);
    } else if (!tsamjp.datastoreExists(trimmedTarget)) {
      return badRequest("Target store " + trimmedTarget + " not found");
    }

    try {
      Job j = tsamjp.moveByIdAndDate(mpid, start, end, trimmedTarget);
      return ok(new JaxbJob(j));
    } catch (Exception e) {
      return handleException(e);
    }
  }
}
