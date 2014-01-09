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
package org.opencastproject.authorization.xacml.manager.endpoint;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.opencastproject.authorization.xacml.manager.endpoint.JsonConv.digestManagedAcl;
import static org.opencastproject.authorization.xacml.manager.endpoint.JsonConv.fullAccessControlList;
import static org.opencastproject.authorization.xacml.manager.endpoint.JsonConv.nest;
import static org.opencastproject.authorization.xacml.manager.impl.Util.getManagedAcl;
import static org.opencastproject.security.api.AccessControlUtil.acl;
import static org.opencastproject.util.Jsons.arr;
import static org.opencastproject.util.Jsons.obj;
import static org.opencastproject.util.Jsons.p;
import static org.opencastproject.util.RestUtil.splitCommaSeparatedParam;
import static org.opencastproject.util.RestUtil.R.badRequest;
import static org.opencastproject.util.RestUtil.R.conflict;
import static org.opencastproject.util.RestUtil.R.noContent;
import static org.opencastproject.util.RestUtil.R.notFound;
import static org.opencastproject.util.RestUtil.R.ok;
import static org.opencastproject.util.RestUtil.R.serverError;
import static org.opencastproject.util.data.Either.left;
import static org.opencastproject.util.data.Either.right;
import static org.opencastproject.util.data.Monadics.mlist;
import static org.opencastproject.util.data.Option.option;
import static org.opencastproject.util.data.Prelude.unexhaustiveMatch;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.functions.Misc.chuck;
import static org.opencastproject.util.data.functions.Strings.trimToNone;
import static org.opencastproject.util.doc.rest.RestParameter.Type.BOOLEAN;
import static org.opencastproject.util.doc.rest.RestParameter.Type.INTEGER;
import static org.opencastproject.util.doc.rest.RestParameter.Type.STRING;

import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.AclServiceException;
import org.opencastproject.authorization.xacml.manager.api.AclServiceFactory;
import org.opencastproject.authorization.xacml.manager.api.AclServiceNoReferenceException;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.authorization.xacml.manager.api.TransitionResult;
import org.opencastproject.authorization.xacml.manager.impl.AclTransitionDbDuplicatedException;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.authorization.xacml.manager.impl.Util;
import org.opencastproject.episode.api.EpisodeQuery;
import org.opencastproject.episode.api.EpisodeService;
import org.opencastproject.episode.api.HttpMediaPackageElementProvider;
import org.opencastproject.episode.api.SearchResultItem;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.security.api.AccessControlUtil;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.series.api.SeriesException;
import org.opencastproject.series.api.SeriesService;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.Jsons;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Monadics;
import org.opencastproject.util.data.MultiMap;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Predicate;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.functions.Functions;
import org.opencastproject.util.data.functions.Options;
import org.opencastproject.util.doc.rest.RestParameter;
import org.opencastproject.util.doc.rest.RestQuery;
import org.opencastproject.util.doc.rest.RestResponse;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public abstract class AbstractAclServiceRestEndpoint {
  private static final Logger logger = LoggerFactory.getLogger(AbstractAclServiceRestEndpoint.class);

  private static final AccessControlList EMPTY_ACL = acl();

  protected abstract AclServiceFactory getAclServiceFactory();

  protected abstract String getEndpointBaseUrl();

  protected abstract SecurityService getSecurityService();

  protected abstract AuthorizationService getAuthorizationService();

  protected abstract EpisodeService getEpisodeService();

  protected abstract SeriesService getSeriesService();

  protected abstract HttpMediaPackageElementProvider getHttpMediaPackageElementProvider();

  @PUT
  @Path("/series/{transitionId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updateseriestransition", description = "Update an existing series transition", returnDescription = "Update an existing series transition", pathParameters = { @RestParameter(name = "transitionId", isRequired = true, description = "The transition id", type = STRING) }, restParameters = {
          @RestParameter(name = "applicationDate", isRequired = true, description = "The date to applicate", type = STRING),
          @RestParameter(name = "managedAclId", isRequired = true, description = "The managed access control list id", type = INTEGER),
          @RestParameter(name = "workflowDefinitionId", isRequired = false, description = "The workflow definition identifier", type = STRING),
          @RestParameter(name = "workflowParams", isRequired = false, description = "The workflow parameters as JSON", type = STRING),
          @RestParameter(name = "override", isRequired = false, description = "If to override the episode ACL's", type = STRING, defaultValue = "false") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The series transition has successfully been updated"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The given managed acl id could not be found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during updating a series transition") })
  public String updateSeriesTransition(@PathParam("transitionId") long transitionId,
          @FormParam("applicationDate") String applicationDate, @FormParam("managedAclId") long managedAclId,
          @FormParam("workflowDefinitionId") String workflowDefinitionId,
          @FormParam("workflowParams") String workflowParams, @FormParam("override") boolean override)
          throws NotFoundException {
    try {
      final Date at = new Date(DateTimeSupport.fromUTC(applicationDate));
      final Option<ConfiguredWorkflowRef> workflow = createConfiguredWorkflowRef(workflowDefinitionId, workflowParams);
      final SeriesACLTransition t = aclService().updateSeriesTransition(transitionId, managedAclId, at, workflow,
              override);
      return JsonConv.full(t).toJson();
    } catch (AclServiceNoReferenceException e) {
      logger.info("Managed acl with id '{}' could not be found", managedAclId);
      throw new WebApplicationException(Status.BAD_REQUEST);
    } catch (AclServiceException e) {
      logger.warn("Error updating series transition: {}", e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Unable to parse the application date");
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @PUT
  @Path("/episode/{transitionId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updateepisodetransition", description = "Update an existing episode transition", returnDescription = "Update an existing episode transition", pathParameters = { @RestParameter(name = "transitionId", isRequired = true, description = "The transition id", type = STRING) }, restParameters = {
          @RestParameter(name = "applicationDate", isRequired = true, description = "The date to applicate", type = STRING),
          @RestParameter(name = "managedAclId", isRequired = false, description = "The managed access control list id", type = INTEGER),
          @RestParameter(name = "workflowDefinitionId", isRequired = false, description = "The workflow definition identifier", type = STRING),
          @RestParameter(name = "workflowParams", isRequired = false, description = "The workflow parameters as JSON", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The episode transition has successfully been updated"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during updating an episode transition") })
  public String updateEpisodeTransition(@PathParam("transitionId") long transitionId,
          @FormParam("applicationDate") String applicationDate, @FormParam("managedAclId") Long managedAclId,
          @FormParam("workflowDefinitionId") String workflowDefinitionId,
          @FormParam("workflowParams") String workflowParams) throws NotFoundException {
    try {
      final Date at = new Date(DateTimeSupport.fromUTC(applicationDate));
      final Option<ConfiguredWorkflowRef> workflow = createConfiguredWorkflowRef(workflowDefinitionId, workflowParams);
      final EpisodeACLTransition t = aclService().updateEpisodeTransition(transitionId, option(managedAclId), at,
              workflow);
      return JsonConv.full(t).toJson();
    } catch (AclServiceException e) {
      logger.warn("Error updating episode transition: {}", e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (NotFoundException e) {
      throw e;
    } catch (Exception e) {
      logger.warn("Unable to parse the application date");
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/series/{seriesId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "addseriestransition", description = "Add a series transition", returnDescription = "Add a series transition", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series id", type = STRING) }, restParameters = {
          @RestParameter(name = "applicationDate", isRequired = true, description = "The date to applicate", type = STRING),
          @RestParameter(name = "managedAclId", isRequired = true, description = "The managed access control list id", type = INTEGER),
          @RestParameter(name = "workflowDefinitionId", isRequired = false, description = "The workflow definition identifier", type = STRING),
          @RestParameter(name = "workflowParams", isRequired = false, description = "The workflow parameters as JSON", type = STRING),
          @RestParameter(name = "override", isRequired = false, description = "If to override the episode ACL's", type = STRING, defaultValue = "false") }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The series transition has successfully been added"),
          @RestResponse(responseCode = SC_CONFLICT, description = "The series transition with the applicationDate already exists"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "The given managed acl id could not be found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during adding a series transition") })
  public String addSeriesTransition(@PathParam("seriesId") String seriesId,
          @FormParam("applicationDate") String applicationDate, @FormParam("managedAclId") long managedAclId,
          @FormParam("workflowDefinitionId") String workflowDefinitionId,
          @FormParam("workflowParams") String workflowParams, @FormParam("override") boolean override) {
    try {
      final Date at = new Date(DateTimeSupport.fromUTC(applicationDate));
      final Option<ConfiguredWorkflowRef> workflow = createConfiguredWorkflowRef(workflowDefinitionId, workflowParams);
      SeriesACLTransition seriesTransition = aclService().addSeriesTransition(seriesId, managedAclId, at, override,
              workflow);
      return JsonConv.full(seriesTransition).toJson();
    } catch (AclServiceNoReferenceException e) {
      logger.info("Managed acl with id '{}' coudl not be found", managedAclId);
      throw new WebApplicationException(Status.BAD_REQUEST);
    } catch (AclTransitionDbDuplicatedException e) {
      logger.info("Error adding series transition: transition with date {} already exists", applicationDate);
      throw new WebApplicationException(Status.CONFLICT);
    } catch (AclServiceException e) {
      logger.warn("Error adding series transition: {}", e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      logger.warn("Unable to parse the application date");
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @POST
  @Path("/episode/{episodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "addepisodetransition", description = "Add an episode transition", returnDescription = "Add an episode transition", pathParameters = { @RestParameter(name = "episodeId", isRequired = true, description = "The episode id", type = STRING) }, restParameters = {
          @RestParameter(name = "applicationDate", isRequired = true, description = "The date to applicate", type = STRING),
          @RestParameter(name = "managedAclId", isRequired = false, description = "The managed access control list id", type = INTEGER),
          @RestParameter(name = "workflowDefinitionId", isRequired = false, description = "The workflow definition identifier", type = STRING),
          @RestParameter(name = "workflowParams", isRequired = false, description = "The workflow parameters as JSON", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The episode transition has successfully been added"),
          @RestResponse(responseCode = SC_CONFLICT, description = "The episode transition with the applicationDate already exists"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during adding an episode transition") })
  public String addEpisodeTransition(@PathParam("episodeId") String episodeId,
          @FormParam("applicationDate") String applicationDate, @FormParam("managedAclId") Long managedAclId,
          @FormParam("workflowDefinitionId") String workflowDefinitionId,
          @FormParam("workflowParams") String workflowParams) {
    try {
      final Date at = new Date(DateTimeSupport.fromUTC(applicationDate));
      final Option<ConfiguredWorkflowRef> workflow = createConfiguredWorkflowRef(workflowDefinitionId, workflowParams);
      final EpisodeACLTransition transition = aclService().addEpisodeTransition(episodeId, option(managedAclId), at,
              workflow);
      return JsonConv.full(transition).toJson();
    } catch (AclTransitionDbDuplicatedException e) {
      logger.info("Error adding episode transition: transition with date {} already exists", applicationDate);
      throw new WebApplicationException(Status.CONFLICT);
    } catch (AclServiceException e) {
      logger.warn("Error adding episode transition: {}", e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      logger.warn("Unable to parse the application date");
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("/episode/{transitionId}")
  @RestQuery(name = "deleteepisodetransition", description = "Delets an episode transition", returnDescription = "Delets an episode transition", pathParameters = { @RestParameter(name = "transitionId", isRequired = true, description = "The transition id", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The episode transition has been deleted successfully"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The episode transition has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during deleting the episode transition") })
  public Response deleteEpisodeTransition(@PathParam("transitionId") long transitionId) throws NotFoundException {
    try {
      aclService().deleteEpisodeTransition(transitionId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (AclServiceException e) {
      logger.warn("Error deleting episode transition {}: {}", transitionId, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @DELETE
  @Path("/series/{transitionId}")
  @RestQuery(name = "deleteseriestransition", description = "Delets a series transition", returnDescription = "Delets a series transition", pathParameters = { @RestParameter(name = "transitionId", isRequired = true, description = "The transition id", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_NO_CONTENT, description = "The series transition has been deleted successfully"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The series transition has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during deleting the series transition") })
  public Response deleteSeriesTransition(@PathParam("transitionId") long transitionId) throws NotFoundException {
    try {
      aclService().deleteSeriesTransition(transitionId);
      return Response.noContent().build();
    } catch (NotFoundException e) {
      throw e;
    } catch (AclServiceException e) {
      logger.warn("Error deleting series transition {}: {}", transitionId, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
  }

  @GET
  @Path("/transitionsfor.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getTransitionsForAsJson", description = "Get the transitions for a list of episodes and/or series as json. At least one of the lists must not be empty.", returnDescription = "Get the transitions as json", restParameters = {
          @RestParameter(name = "episodeIds", isRequired = false, description = "A list of comma separated episode IDs", type = STRING),
          @RestParameter(name = "seriesIds", isRequired = false, description = "A list of comma separated series IDs", type = STRING),
          @RestParameter(name = "done", isRequired = false, description = "Indicates if already applied transitions should be included", type = BOOLEAN) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The request was processed succesfully"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Parameter error"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during processing the request") })
  public Response getTransitionsFor(@QueryParam("episodeIds") String episodeIds,
          @QueryParam("seriesIds") String seriesIds, @DefaultValue("false") @QueryParam("done") final boolean done) {
    final Monadics.ListMonadic<String> eIds = splitCommaSeparatedParam(option(episodeIds));
    final Monadics.ListMonadic<String> sIds = splitCommaSeparatedParam(option(seriesIds));
    if (eIds.value().isEmpty() && sIds.value().isEmpty()) {
      return badRequest();
    }
    final AclService aclService = aclService();
    try {
      // episodeId -> [transitions]
      final Map<String, List<EpisodeACLTransition>> eTs = eIds
              .foldl(MultiMap.<String, EpisodeACLTransition> multiHashMapWithArrayList(),
                      new Function2.X<MultiMap<String, EpisodeACLTransition>, String, MultiMap<String, EpisodeACLTransition>>() {
                        @Override
                        public MultiMap<String, EpisodeACLTransition> xapply(
                                MultiMap<String, EpisodeACLTransition> mmap, String id) throws Exception {
                          // todo it is quite expensive to query each episode separately
                          final TransitionQuery q = TransitionQuery.query().withId(id).withScope(AclScope.Episode)
                                  .withDone(done);
                          return mmap.putAll(id, aclService.getTransitions(q).getEpisodeTransistions());
                        }
                      }).value();
      // seriesId -> [transitions]
      final Map<String, List<SeriesACLTransition>> sTs = sIds.foldl(
              MultiMap.<String, SeriesACLTransition> multiHashMapWithArrayList(),
              new Function2.X<MultiMap<String, SeriesACLTransition>, String, MultiMap<String, SeriesACLTransition>>() {
                @Override
                public MultiMap<String, SeriesACLTransition> xapply(MultiMap<String, SeriesACLTransition> mmap,
                        String id) throws Exception {
                  // todo it is quite expensive to query each series separately
                  final TransitionQuery q = TransitionQuery.query().withId(id).withScope(AclScope.Series)
                          .withDone(done);
                  return mmap.putAll(id, aclService.getTransitions(q).getSeriesTransistions());
                }
              }).value();
      final Jsons.Obj episodesObj = buildEpisodesObj(eTs);
      final Jsons.Obj seriesObj = buildSeriesObj(sTs);
      return ok(obj(p("episodes", episodesObj), p("series", seriesObj)).toJson());
    } catch (Exception e) {
      logger.error("Error generating getTransitionsFor response", e);
      return serverError();
    }
  }

  @GET
  @Path("/transitions.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "gettransitionsasjson", description = "Get the transitions as json", returnDescription = "Get the transitions as json", restParameters = {
          @RestParameter(name = "after", isRequired = false, description = "All transitions with an application date after this one", type = STRING),
          @RestParameter(name = "before", isRequired = false, description = "All transitions with an application date before this one", type = STRING),
          @RestParameter(name = "scope", isRequired = false, description = "The transition scope", type = STRING),
          @RestParameter(name = "id", isRequired = false, description = "The series or episode identifier", type = STRING),
          @RestParameter(name = "transitionId", isRequired = false, description = "The transition identifier", type = STRING),
          @RestParameter(name = "managedAclId", isRequired = false, description = "The managed acl identifier", type = INTEGER),
          @RestParameter(name = "done", isRequired = false, description = "Indicates if already applied", type = BOOLEAN) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The request was processed succesfully"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Error parsing the given scope"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during processing the request") })
  public Response getTransitionsAsJson(@QueryParam("after") String afterStr, @QueryParam("before") String beforeStr,
          @QueryParam("scope") String scopeStr, @QueryParam("id") String id,
          @QueryParam("transitionId") Long transitionId, @QueryParam("managedAclId") Long managedAclId,
          @QueryParam("done") Boolean done) {
    try {
      final TransitionQuery query = TransitionQuery.query();

      if (StringUtils.isNotBlank(afterStr))
        query.after(new Date(DateTimeSupport.fromUTC(afterStr)));

      if (StringUtils.isNotBlank(beforeStr))
        query.before(new Date(DateTimeSupport.fromUTC(beforeStr)));

      if (StringUtils.isNotBlank(id))
        query.withId(id);

      if (StringUtils.isNotBlank(scopeStr)) {
        if ("episode".equalsIgnoreCase(scopeStr))
          query.withScope(AclScope.Episode);
        else if ("series".equalsIgnoreCase(scopeStr))
          query.withScope(AclScope.Series);
        else
          return badRequest();
      }

      if (transitionId != null)
        query.withTransitionId(transitionId);

      if (managedAclId != null)
        query.withAclId(managedAclId);

      if (done != null)
        query.withDone(done);

      final AclService aclService = aclService();
      // run query
      final TransitionResult r = aclService().getTransitions(query);
      // episodeId -> [transitions]
      final Map<String, List<EpisodeACLTransition>> episodeGroup = groupByEpisodeId(r.getEpisodeTransistions());
      // seriesId -> [transitions]
      final Map<String, List<SeriesACLTransition>> seriesGroup = groupBySeriesId(r.getSeriesTransistions());
      final Jsons.Obj episodesObj = buildEpisodesObj(episodeGroup);
      final Jsons.Obj seriesObj = buildSeriesObj(seriesGroup);
      // create final response
      return ok(obj(p("episodes", episodesObj), p("series", seriesObj)).toJson());
    } catch (Exception e) {
      logger.error("Error generating getTransitions response", e);
      return serverError();
    }
  }

  /** Build JSON object for all episodes containing all transitions and the active ACL. */
  private Jsons.Obj buildEpisodesObj(Map<String, List<EpisodeACLTransition>> episodeGroup) {
    final AclService aclService = aclService();
    return mlist(episodeGroup.entrySet().iterator()).foldl(obj(),
            new Function2<Jsons.Obj, Map.Entry<String, List<EpisodeACLTransition>>, Jsons.Obj>() {
              @Override
              public Jsons.Obj apply(Jsons.Obj obj, Map.Entry<String, List<EpisodeACLTransition>> ts) {
                final Jsons.Arr transitions = arr(mlist(ts.getValue()).map(JsonConv.digestEpisodeAclTransition));
                final Jsons.Obj activeAcl = buildAclObj(getActiveAclForEpisode(aclService, ts.getKey()), true);
                return obj.append(buildActiveAclAndTransitionsObj(ts.getKey(), activeAcl, transitions));
              }
            });
  }

  /** Build JSON object for all series containing all transitions and the active ACL. */
  private Jsons.Obj buildSeriesObj(Map<String, List<SeriesACLTransition>> seriesGroup) {
    final AclService aclService = aclService();
    return mlist(seriesGroup.entrySet().iterator()).foldl(obj(),
            new Function2<Jsons.Obj, Map.Entry<String, List<SeriesACLTransition>>, Jsons.Obj>() {
              @Override
              public Jsons.Obj apply(Jsons.Obj obj, Map.Entry<String, List<SeriesACLTransition>> ts) {
                final Jsons.Arr transitions = arr(mlist(ts.getValue()).map(JsonConv.digestSeriesAclTransition));
                final Jsons.Obj activeAcl = buildAclObj(getActiveAclForSeries(aclService, ts.getKey()), false);
                return obj.append(buildActiveAclAndTransitionsObj(ts.getKey(), activeAcl, transitions));
              }
            });
  }

  private static final Jsons.Obj fromSeries = obj(p("isFromSeries", true));

  private static final Jsons.Obj fromEpisode = obj(p("isFromSeries", false));

  /** Build the JSON obj for un/managed ACLs. */
  private Jsons.Obj buildAclObj(final Either<AccessControlList, Tuple<ManagedAcl, AclScope>> acl,
          final boolean withFlavor) {
    return acl.fold(nest("unmanagedAcl").o(fullAccessControlList),
    // managed acl
            new Function<Tuple<ManagedAcl, AclScope>, Jsons.Obj>() {
              @Override
              public Jsons.Obj apply(Tuple<ManagedAcl, AclScope> acl) {
                final Jsons.Obj digest = digestManagedAcl.apply(acl.getA());
                final Jsons.Obj enriched;
                if (withFlavor) {
                  switch (acl.getB()) {
                    case Episode:
                      enriched = digest.append(fromEpisode);
                      break;
                    case Series:
                      enriched = digest.append(fromSeries);
                      break;
                    default:
                      enriched = unexhaustiveMatch();
                  }
                } else {
                  enriched = digest;
                }
                return obj(p("managedAcl", enriched));
              }
            });
  }

  /**
   * Build the obj with active ACL and transitions.
   * 
   * @param id
   *          either the episode or the series id.
   */
  private Jsons.Obj buildActiveAclAndTransitionsObj(String id, Jsons.Obj activeAcl, Jsons.Arr transitions) {
    return obj(p(id, obj(p("activeAcl", activeAcl), p("transitions", transitions))));
  }

  /** Group all episode ACL transitions by episode ID. */
  private Map<String, List<EpisodeACLTransition>> groupByEpisodeId(List<EpisodeACLTransition> ts) {
    return mlist(ts)
            .foldl(MultiMap.<String, EpisodeACLTransition> multiHashMapWithArrayList(),
                    new Function2<MultiMap<String, EpisodeACLTransition>, EpisodeACLTransition, MultiMap<String, EpisodeACLTransition>>() {
                      @Override
                      public MultiMap<String, EpisodeACLTransition> apply(MultiMap<String, EpisodeACLTransition> mmap,
                              EpisodeACLTransition t) {
                        return mmap.put(t.getEpisodeId(), t);
                      }
                    }).value();
  }

  /** Group all series ACL transitions by series ID. */
  private Map<String, List<SeriesACLTransition>> groupBySeriesId(List<SeriesACLTransition> ts) {
    return mlist(ts)
            .foldl(MultiMap.<String, SeriesACLTransition> multiHashMapWithArrayList(),
                    new Function2<MultiMap<String, SeriesACLTransition>, SeriesACLTransition, MultiMap<String, SeriesACLTransition>>() {
                      @Override
                      public MultiMap<String, SeriesACLTransition> apply(MultiMap<String, SeriesACLTransition> mmap,
                              SeriesACLTransition t) {
                        return mmap.put(t.getSeriesId(), t);
                      }
                    }).value();
  }

  private Either<AccessControlList, Tuple<ManagedAcl, AclScope>> getActiveAclForEpisode(AclService aclService,
          String episodeId) {
    final EpisodeQuery queryForEpisode = EpisodeQuery.query(getSecurityService()).id(episodeId).onlyLastVersion();
    for (SearchResultItem sr : mlist(
            getEpisodeService().find(queryForEpisode, getHttpMediaPackageElementProvider().getUriRewriter()).getItems())
            .headOpt()) {
      // get active ACL of found media package
      final Tuple<AccessControlList, AclScope> activeAcl = getAuthorizationService().getActiveAcl(sr.getMediaPackage());
      // find corresponding managed ACL
      for (ManagedAcl macl : matchAcls(aclService, activeAcl.getA())) {
        return right(tuple(macl, activeAcl.getB()));
      }
      return left(activeAcl.getA());
    }
    // episode does not exist
    logger.warn("Episode {} cannot be found in EpisodeService", episodeId);
    return left(EMPTY_ACL);
  }

  private Either<AccessControlList, Tuple<ManagedAcl, AclScope>> getActiveAclForSeries(AclService aclService,
          String seriesId) {
    try {
      final AccessControlList activeAcl = getSeriesService().getSeriesAccessControl(seriesId);
      for (ManagedAcl macl : matchAcls(aclService, activeAcl)) {
        return right(tuple(macl, AclScope.Series));
      }
      return left(activeAcl);
    } catch (NotFoundException e) {
      // series does not exist
      logger.warn("Series {} cannot be found in SeriesService", seriesId);
    } catch (SeriesException e) {
      logger.error("Error accessing SeriesService", e);
      return chuck(e);
    }
    return left(EMPTY_ACL);
  }

  /** Matches the given ACL against all managed ACLs returning the first match. */
  private static Option<ManagedAcl> matchAcls(final AclService aclService, final AccessControlList acl) {
    return mlist(aclService.getAcls()).find(new Predicate<ManagedAcl>() {
      @Override
      public Boolean apply(ManagedAcl macl) {
        return AccessControlUtil.equals(acl, macl.getAcl());
      }
    });
  }

  @GET
  @Path("/acl/{aclId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getacl", description = "Return the ACL by the given id", returnDescription = "Return the ACL by the given id", pathParameters = { @RestParameter(name = "aclId", isRequired = true, description = "The ACL identifier", type = INTEGER) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been returned"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during returning the ACL") })
  public String getAcl(@PathParam("aclId") long aclId) throws NotFoundException {
    final Option<ManagedAcl> managedAcl = aclService().getAcl(aclId);
    if (managedAcl.isNone()) {
      logger.info("No ACL with id '{}' could be found", aclId);
      throw new NotFoundException();
    }
    return JsonConv.full(managedAcl.get()).toJson();
  }

  @GET
  @Path("/acl/acls.json")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "getacls", description = "Lists the ACL's as JSON", returnDescription = "The list of ACL's as JSON", reponses = {
          @RestResponse(responseCode = SC_OK, description = "The list of ACL's has successfully been returned"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during returning the list of ACL's") })
  public String getAcls() {
    return Jsons.arr(mlist(aclService().getAcls()).map(Functions.<ManagedAcl, Jsons.Val> co(JsonConv.fullManagedAcl)))
            .toJson();
  }

  @POST
  @Path("/acl")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "createacl", description = "Create an ACL", returnDescription = "Create an ACL", restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING),
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been added"),
          @RestResponse(responseCode = SC_CONFLICT, description = "An ACL with the same name already exists"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during adding the ACL") })
  public String createAcl(@FormParam("name") String name, @FormParam("acl") String accessControlList) {
    final AccessControlList acl = parseAcl.apply(accessControlList);
    final Option<ManagedAcl> managedAcl = aclService().createAcl(acl, name);
    if (managedAcl.isNone()) {
      logger.info("An ACL with the same name '{}' already exists", name);
      throw new WebApplicationException(Response.Status.CONFLICT);
    }
    return JsonConv.full(managedAcl.get()).toJson();
  }

  @PUT
  @Path("/acl/{aclId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RestQuery(name = "updateacl", description = "Update an ACL", returnDescription = "Update an ACL", pathParameters = { @RestParameter(name = "aclId", isRequired = true, description = "The ACL identifier", type = INTEGER) }, restParameters = {
          @RestParameter(name = "name", isRequired = true, description = "The ACL name", type = STRING),
          @RestParameter(name = "acl", isRequired = true, description = "The access control list", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been updated"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_BAD_REQUEST, description = "Unable to parse the ACL"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during updating the ACL") })
  public String updateAcl(@PathParam("aclId") long aclId, @FormParam("name") String name,
          @FormParam("acl") String accessControlList) throws NotFoundException {
    final Organization org = getSecurityService().getOrganization();
    final AccessControlList acl = parseAcl.apply(accessControlList);
    final ManagedAclImpl managedAcl = new ManagedAclImpl(aclId, name, org.getId(), acl);
    if (!aclService().updateAcl(managedAcl)) {
      logger.info("No ACL with id '{}' could be found under organization '{}'", aclId, org.getId());
      throw new NotFoundException();
    }
    return JsonConv.full(managedAcl).toJson();
  }

  @DELETE
  @Path("/acl/{aclId}")
  @RestQuery(name = "deleteacl", description = "Delete an ACL", returnDescription = "Delete an ACL", pathParameters = { @RestParameter(name = "aclId", isRequired = true, description = "The ACL identifier", type = INTEGER) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has successfully been deleted"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL has not been found"),
          @RestResponse(responseCode = SC_CONFLICT, description = "The ACL could not be deleted, there are still references on it"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Error during deleting the ACL") })
  public Response deleteAcl(@PathParam("aclId") long aclId) throws NotFoundException {
    try {
      if (!aclService().deleteAcl(aclId))
        return conflict();
    } catch (AclServiceException e) {
      logger.warn("Error deleting manged acl with id '{}': {}", aclId, e);
      throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
    }
    return noContent();
  }

  @POST
  @Path("/apply/episode/{episodeId}")
  @RestQuery(name = "applyAclToEpisode", description = "Immediate application of an ACL to an episode", returnDescription = "Status code", pathParameters = { @RestParameter(name = "episodeId", isRequired = true, description = "The episode ID", type = STRING) }, restParameters = {
          @RestParameter(name = "aclId", isRequired = false, description = "The ID of the ACL to apply. If missing the episode ACL will be deleted to fall back to the series ACL", type = INTEGER),
          @RestParameter(name = "workflowDefinitionId", isRequired = false, description = "The optional workflow to apply to the episode after", type = STRING),
          @RestParameter(name = "workflowParams", isRequired = false, description = "Parameters for the optional workflow", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has been successfully applied"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL or the episode has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Internal error") })
  public Response applyAclToEpisode(@PathParam("episodeId") String episodeId, @FormParam("aclId") Long aclId,
          @FormParam("workflowDefinitionId") String workflowDefinitionId,
          @FormParam("workflowParams") String workflowParams) {
    final AclService aclService = aclService();
    final Option<Option<ManagedAcl>> macl = option(aclId).map(getManagedAcl(aclService));
    if (macl.isSome() && macl.get().isNone())
      return notFound();
    final Option<ConfiguredWorkflowRef> workflow = createConfiguredWorkflowRef(workflowDefinitionId, workflowParams);
    try {
      if (aclService.applyAclToEpisode(episodeId, Options.join(macl), workflow))
        return ok();
      else
        return notFound();
    } catch (AclServiceException e) {
      logger.error("Error applying acl to episode {}", episodeId);
      return serverError();
    }
  }

  @POST
  @Path("/apply/series/{seriesId}")
  @RestQuery(name = "applyAclToSeries", description = "Immediate application of an ACL to a series", returnDescription = "Status code", pathParameters = { @RestParameter(name = "seriesId", isRequired = true, description = "The series ID", type = STRING) }, restParameters = {
          @RestParameter(name = "aclId", isRequired = true, description = "The ID of the ACL to apply", type = INTEGER),
          @RestParameter(name = "override", isRequired = false, defaultValue = "false", description = "If true the series ACL will take precedence over any existing episode ACL", type = STRING),
          @RestParameter(name = "workflowDefinitionId", isRequired = false, description = "The optional workflow to apply to the episode after", type = STRING),
          @RestParameter(name = "workflowParams", isRequired = false, description = "Parameters for the optional workflow", type = STRING) }, reponses = {
          @RestResponse(responseCode = SC_OK, description = "The ACL has been successfully applied"),
          @RestResponse(responseCode = SC_NOT_FOUND, description = "The ACL or the episode has not been found"),
          @RestResponse(responseCode = SC_INTERNAL_SERVER_ERROR, description = "Internal error") })
  public Response applyAclToSeries(@PathParam("seriesId") String seriesId, @FormParam("aclId") long aclId,
          @DefaultValue("false") @FormParam("override") boolean override,
          @FormParam("workflowDefinitionId") String workflowDefinitionId,
          @FormParam("workflowParams") String workflowParams) {
    final AclService aclService = aclService();
    for (ManagedAcl macl : aclService.getAcl(aclId)) {
      final Option<ConfiguredWorkflowRef> workflow = createConfiguredWorkflowRef(workflowDefinitionId, workflowParams);
      try {
        if (aclService.applyAclToSeries(seriesId, macl, override, workflow))
          return ok();
        else
          return notFound();
      } catch (AclServiceException e) {
        logger.error("Error applying acl to series {}", seriesId);
        return serverError();
      }
    }
    // acl not found
    return notFound();
  }

  /** Create a ConfiguredWorkflowRef from raw request strings that may be null. */
  private static Option<ConfiguredWorkflowRef> createConfiguredWorkflowRef(String workflowId, String workflowParamsJson) {
    return Util.createConfiguredWorkflowRef(option(workflowId).bind(trimToNone),
            option(workflowParamsJson).bind(trimToNone));
  }

  private static final Function<String, AccessControlList> parseAcl = new Function<String, AccessControlList>() {
    @Override
    public AccessControlList apply(String acl) {
      try {
        return AccessControlParser.parseAcl(acl);
      } catch (Exception e) {
        logger.warn("Unable to parse ACL");
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
      }
    }
  };

  private AclService aclService() {
    return getAclServiceFactory().serviceFor(getSecurityService().getOrganization());
  }
}
