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

package org.opencastproject.adminui.endpoint;

import static org.opencastproject.index.service.util.CatalogAdapterUtil.getCatalogProperties;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.adminui.endpoint.AbstractEventEndpointTest.TestEnv;
import org.opencastproject.adminui.impl.AdminUIConfiguration;
import org.opencastproject.adminui.impl.index.AdminUISearchIndex;
import org.opencastproject.archive.api.HttpMediaPackageElementProvider;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.StoragePath;
import org.opencastproject.archive.base.persistence.AbstractArchiveDb;
import org.opencastproject.archive.base.persistence.ArchiveDb;
import org.opencastproject.archive.base.storage.DeletionSelector;
import org.opencastproject.archive.base.storage.ElementStore;
import org.opencastproject.archive.opencast.OpencastArchive;
import org.opencastproject.archive.opencast.OpencastArchivePublisher;
import org.opencastproject.archive.opencast.OpencastQuery;
import org.opencastproject.archive.opencast.OpencastResultItem;
import org.opencastproject.archive.opencast.OpencastResultSet;
import org.opencastproject.archive.opencast.solr.SolrIndexManager;
import org.opencastproject.archive.opencast.solr.SolrRequester;
import org.opencastproject.authorization.xacml.manager.api.AclService;
import org.opencastproject.authorization.xacml.manager.api.EpisodeACLTransition;
import org.opencastproject.authorization.xacml.manager.api.ManagedAcl;
import org.opencastproject.authorization.xacml.manager.api.SeriesACLTransition;
import org.opencastproject.authorization.xacml.manager.api.TransitionQuery;
import org.opencastproject.authorization.xacml.manager.api.TransitionResult;
import org.opencastproject.authorization.xacml.manager.impl.ManagedAclImpl;
import org.opencastproject.authorization.xacml.manager.impl.TransitionResultImpl;
import org.opencastproject.capture.CaptureParameters;
import org.opencastproject.capture.admin.api.Agent;
import org.opencastproject.capture.admin.api.CaptureAgentStateService;
import org.opencastproject.event.comment.EventComment;
import org.opencastproject.event.comment.EventCommentReply;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.fun.juc.Immutables;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.impl.IndexServiceImpl;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.job.api.IncidentImpl;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.job.api.IncidentTreeImpl;
import org.opencastproject.job.api.Job;
import org.opencastproject.job.api.JobImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.metadata.dublincore.StaticMetadataServiceDublinCoreImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.scheduler.api.SchedulerService;
import org.opencastproject.schema.OcDublinCore;
import org.opencastproject.schema.OcDublinCoreUtil;
import org.opencastproject.security.api.AccessControlEntry;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AclScope;
import org.opencastproject.security.api.AuthorizationService;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.Permissions;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.security.urlsigning.service.UrlSigningService;
import org.opencastproject.series.impl.SeriesServiceDatabaseException;
import org.opencastproject.series.impl.SeriesServiceImpl;
import org.opencastproject.series.impl.solr.SeriesServiceSolrIndex;
import org.opencastproject.serviceregistry.api.IncidentService;
import org.opencastproject.serviceregistry.api.Incidents;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.DateTimeSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.data.VCell;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workflow.api.ConfiguredWorkflowRef;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowOperationDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowQuery;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workflow.api.WorkflowSetImpl;
import org.opencastproject.workspace.api.Workspace;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.Ignore;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestEventEndpoint extends AbstractEventEndpoint {
  private final TestEnv env;

  private final MediaPackageBuilderImpl mpBuilder = new MediaPackageBuilderImpl();

  /** The solr root directory */
  private final String solrRoot = "target" + File.separator + "opencast" + File.separator + "archiveindex";

  private final JaxbOrganization defaultOrganization = new DefaultOrganization();

  private final JaxbOrganization opencastOrganization = new JaxbOrganization("opencastproject.org");

  private static final String PREVIEW_SUBTYPE = "preview";

  /** A user with permissions. */
  private final User userWithPermissions = new JaxbUser("sample", null, "WithPermissions", "with@permissions.com",
          "test", defaultOrganization,
          new HashSet<JaxbRole>(Arrays.asList(new JaxbRole("ROLE_STUDENT", defaultOrganization),
                  new JaxbRole("ROLE_OTHERSTUDENT", defaultOrganization),
                  new JaxbRole(defaultOrganization.getAnonymousRole(), defaultOrganization))));

  /** A user without permissions. */
  private final User userWithoutPermissions = new JaxbUser("sample", null, "WithoutPermissions",
          "without@permissions.com", "test", opencastOrganization,
          new HashSet<JaxbRole>(Arrays.asList(new JaxbRole("ROLE_NOTHING", opencastOrganization))));

  private final User defaultUser = userWithPermissions;

  private final UriRewriter rewriter = new UriRewriter() {
    @Override
    public URI apply(Version version, MediaPackageElement mpe) {
      return uri("http://episodes", mpe.getMediaPackage().getIdentifier(), mpe.getIdentifier(), version,
              mpe.getElementType().toString().toLowerCase() + "."
                      + mpe.getMimeType().getSuffix().getOrElse(".unknown"));

    }
  };

  /** Answers with a constant response. */
  public static class Responder<A> implements IAnswer<A> {
    private A response;

    Responder(A response) {
      this.response = response;
    }

    public void setResponse(A response) {
      this.response = response;
    }

    @Override
    public A answer() throws Throwable {
      return response;
    }
  }

  public TestEventEndpoint() throws Exception {
    env = AbstractEventEndpointTest.testEnv();

    // security service
    final SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    Responder<User> userResponder = new Responder<User>(defaultUser);
    Responder<Organization> organizationResponder = new Responder<Organization>(defaultOrganization);
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andAnswer(organizationResponder).anyTimes();
    EasyMock.replay(securityService);
    env.setSecurityService(securityService);

    UrlSigningService urlSigningService = EasyMock.createNiceMock(UrlSigningService.class);
    EasyMock.expect(urlSigningService.accepts(EasyMock.anyString())).andReturn(false).anyTimes();
    EasyMock.replay(urlSigningService);
    env.setUrlSigningService(urlSigningService);

    // AdminUISearchIndex
    AdminUISearchIndex searchIndex = EasyMock.createNiceMock(AdminUISearchIndex.class);
    EasyMock.replay(searchIndex);
    env.setIndex(searchIndex);

    // Preview subtype
    AdminUIConfiguration adminUIConfiguration = new AdminUIConfiguration();
    Hashtable<String, String> dictionary = new Hashtable<String, String>();
    dictionary.put(AdminUIConfiguration.OPT_PREVIEW_SUBTYPE, PREVIEW_SUBTYPE);
    adminUIConfiguration.updated(dictionary);
    env.setAdminUIConfiguration(adminUIConfiguration);

    // acl
    final String anonymousRole = securityService.getOrganization().getAnonymousRole();
    final AccessControlList acl = new AccessControlList(
            new AccessControlEntry(anonymousRole, Permissions.Action.READ.toString(), true));
    /* The authorization service */
    final AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
            .andReturn(new Tuple<>(acl, AclScope.Episode)).anyTimes();
    EasyMock.expect(
            authorizationService.hasPermission((MediaPackage) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(true).anyTimes();
    EasyMock.replay(authorizationService);

    List<ManagedAcl> managedAcls = new ArrayList<ManagedAcl>();
    ManagedAcl managedAcl1 = new ManagedAclImpl(43L, "Public", defaultOrganization.getId(), acl);
    managedAcls.add(managedAcl1);
    managedAcls.add(new ManagedAclImpl(44L, "Private", defaultOrganization.getId(), acl));

    Date transitionDate = new Date(DateTimeSupport.fromUTC("2014-06-05T15:00:00Z"));
    TransitionResult transitionResult = getTransitionResult(managedAcl1, transitionDate);

    AclService aclService = EasyMock.createNiceMock(AclService.class);
    EasyMock.expect(aclService.getAcls()).andReturn(managedAcls).anyTimes();
    EasyMock.expect(aclService.getTransitions(EasyMock.anyObject(TransitionQuery.class))).andReturn(transitionResult)
            .anyTimes();
    EasyMock.replay(aclService);

    env.setAclService(aclService);

    // service registry
    final ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    JobImpl job = new JobImpl(12L);
    Date dateCreated = new Date(DateTimeSupport.fromUTC("2014-06-05T15:00:00Z"));
    Date dateCompleted = new Date(DateTimeSupport.fromUTC("2014-06-05T16:00:00Z"));
    job.setDateCreated(dateCreated);
    job.setDateCompleted(dateCompleted);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job).anyTimes();
    EasyMock.expect(serviceRegistry.createJob((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
            (List<String>) EasyMock.anyObject(), (String) EasyMock.anyObject(), EasyMock.anyBoolean()))
            .andReturn(new JobImpl()).anyTimes();
    EasyMock.expect(serviceRegistry.updateJob((Job) EasyMock.anyObject())).andReturn(new JobImpl()).anyTimes();
    EasyMock.expect(serviceRegistry.getJobs((String) EasyMock.anyObject(), (Job.Status) EasyMock.anyObject()))
            .andReturn(new ArrayList<Job>()).anyTimes();
    EasyMock.replay(serviceRegistry);

    // Org directory
    OrganizationDirectoryService orgDirectory = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectory.getOrganization((String) EasyMock.anyObject())).andReturn(defaultOrganization)
            .anyTimes();
    EasyMock.replay(orgDirectory);

    // workspace
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        final URI uri = (URI) EasyMock.getCurrentArguments()[0];
        if ("file".equals(uri.getScheme())) {
          return new File(uri);
        } else if (uri.toString().startsWith("http://episodes/10.0000/1/publish-catalog-1/1/")) {
          return new File(getClass().getResource("/dublincore.xml").toURI());
        } else {
          return new File(getClass().getResource("/" + uri.toString()).toURI());
        }
      }
    }).anyTimes();
    EasyMock.replay(workspace);

    // workflow service
    WorkflowSetImpl workflowSet = new WorkflowSetImpl();

    WorkflowDefinition wfD = new WorkflowDefinitionImpl();
    wfD.setTitle("Full");
    wfD.setId("full");
    WorkflowOperationDefinitionImpl wfDOp1 = new WorkflowOperationDefinitionImpl("ingest", "Ingest", "error", false);
    WorkflowOperationDefinitionImpl wfDOp2 = new WorkflowOperationDefinitionImpl("archive", "Archive", "error", true);
    wfD.add(wfDOp1);
    wfD.add(wfDOp2);

    WorkflowDefinitionImpl wfD2 = new WorkflowDefinitionImpl();
    wfD2.setTitle("Full HTML5");
    wfD2.setId("full-html5");
    wfD2.addTag("quick_actions");
    wfD2.addTag("test");
    wfD2.setDescription("Test description");
    wfD2.setConfigurationPanel("<h2>Test</h2>");

    final WorkflowInstanceImpl workflowInstanceImpl1 = new WorkflowInstanceImpl(wfD,
            loadMpFromResource("jobs_mediapackage1"), 2L, null, null, new HashMap<String, String>());
    final WorkflowInstanceImpl workflowInstanceImpl2 = new WorkflowInstanceImpl(wfD,
            loadMpFromResource("jobs_mediapackage2"), 2L, null, null, new HashMap<String, String>());
    final WorkflowInstanceImpl workflowInstanceImpl3 = new WorkflowInstanceImpl(wfD,
            loadMpFromResource("jobs_mediapackage3"), 2L, null, null, new HashMap<String, String>());

    workflowInstanceImpl1.setId(1);
    workflowInstanceImpl2.setId(2);
    workflowInstanceImpl3.setId(3);
    workflowInstanceImpl1.getOperations().get(0).setId(4L);
    workflowInstanceImpl1.getOperations().get(1).setId(5L);

    workflowSet.addItem(workflowInstanceImpl1);
    workflowSet.addItem(workflowInstanceImpl2);
    workflowSet.addItem(workflowInstanceImpl3);

    workflowSet.setTotalCount(3);

    WorkflowService workflowService = EasyMock.createNiceMock(WorkflowService.class);
    EasyMock.expect(workflowService.getWorkflowDefinitionById(EasyMock.anyString())).andReturn(wfD).anyTimes();
    EasyMock.expect(workflowService.getWorkflowById(EasyMock.anyLong())).andReturn(workflowInstanceImpl1).anyTimes();
    EasyMock.expect(workflowService.getWorkflowInstances(EasyMock.anyObject(WorkflowQuery.class)))
            .andReturn(workflowSet).anyTimes();
    EasyMock.expect(workflowService.listAvailableWorkflowDefinitions()).andReturn(Arrays.asList(wfD, wfD2));
    EasyMock.replay(workflowService);
    env.setWorkflowService(workflowService);

    // Persistence storage
    final PersistenceEnv penv = PersistenceUtil.newTestPersistenceEnv("org.opencastproject.archive.base.persistence");
    ArchiveDb episodeDatabase = new AbstractArchiveDb() {
      @Override
      protected PersistenceEnv getPenv() {
        return penv;
      }

      @Override
      protected SecurityService getSecurityService() {
        return securityService;
      }
    };

    // element store
    final ElementStore elementStore = EasyMock.createNiceMock(ElementStore.class);
    EasyMock.expect(elementStore.delete(EasyMock.<DeletionSelector> anyObject())).andReturn(true).once();
    EasyMock.expect(elementStore.copy(EasyMock.<StoragePath> anyObject(), EasyMock.<StoragePath> anyObject()))
            .andReturn(true).anyTimes();
    EasyMock.replay(elementStore);

    final SolrServer solrServer = OpencastArchivePublisher.setupSolr(new File(solrRoot));
    final SolrRequester solrRequester = new SolrRequester(solrServer) {
      @Override
      public OpencastResultSet find(OpencastQuery q) throws SolrServerException {
        if (q.getMediaPackageId().isSome()) {
          return new OpencastResultSet() {
            @Override
            public List<OpencastResultItem> getItems() {
              List<OpencastResultItem> items = new ArrayList<OpencastResultItem>();
              items.add(getOpencastResultItem(acl, workflowInstanceImpl3));
              return items;
            }

            @Override
            public String getQuery() {
              return "";
            }

            @Override
            public long getTotalSize() {
              return 1;
            }

            @Override
            public long getLimit() {
              return 0;
            }

            @Override
            public long getOffset() {
              return 0;
            }

            @Override
            public long getSearchTime() {
              return 0;
            }
          };
        } else {
          return new OpencastResultSet() {
            @Override
            public List<OpencastResultItem> getItems() {
              List<OpencastResultItem> items = new ArrayList<>();
              items.add(getOpencastResultItem(acl, workflowInstanceImpl1));
              items.add(getOpencastResultItem(acl, workflowInstanceImpl2));
              items.add(getOpencastResultItem(acl, workflowInstanceImpl3));
              return items;
            }

            @Override
            public String getQuery() {
              return "";
            }

            @Override
            public long getTotalSize() {
              return 3;
            }

            @Override
            public long getLimit() {
              return 0;
            }

            @Override
            public long getOffset() {
              return 0;
            }

            @Override
            public long getSearchTime() {
              return 0;
            }
          };
        }
      }
    };

    // mpeg7 service
    final Mpeg7CatalogService mpeg7CatalogService = new Mpeg7CatalogService();

    // series service
    final SeriesServiceImpl seriesService = new SeriesServiceImpl();
    final SeriesServiceSolrIndex seriesServiceSolrIndex = new SeriesServiceSolrIndex() {
      private final Map<String, String> idMap = map(tuple("series-a", "/series-dublincore-a.xml"),
              tuple("series-b", "/series-dublincore-b.xml"), tuple("series-c", "/series-dublincore-c.xml"),
              tuple("foobar-series", "/series-dublincore.xml"));

      @Override
      public DublinCoreCatalog getDublinCore(String seriesId) throws SeriesServiceDatabaseException, NotFoundException {
        String file = idMap.get(seriesId);
        if (file != null) {
          return withResource(getClass().getResourceAsStream(file), new Function<InputStream, DublinCoreCatalog>() {
            @Override
            public DublinCoreCatalog apply(InputStream is) {
              return DublinCores.read(is);
            }
          });
        }
        throw new Error("Mock error");
      }

      @Override
      public AccessControlList getAccessControl(String seriesID)
              throws NotFoundException, SeriesServiceDatabaseException {
        return acl;
      }

    };
    seriesService.setIndex(seriesServiceSolrIndex);

    StaticMetadataServiceDublinCoreImpl metadataSvcs = new StaticMetadataServiceDublinCoreImpl();
    metadataSvcs.setWorkspace(workspace);

    final SolrIndexManager solrIndex = new SolrIndexManager(solrServer, workspace,
            VCell.cell(Arrays.asList((StaticMetadataService) metadataSvcs)), seriesService, mpeg7CatalogService,
            securityService);

    // Org directory
    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    EasyMock.replay(messageSender);

    MessageReceiver messageReceiver = EasyMock.createNiceMock(MessageReceiver.class);
    EasyMock.replay(messageReceiver);

    OpencastArchive archive = new OpencastArchive(solrIndex, solrRequester, securityService, authorizationService,
            orgDirectory, serviceRegistry, workflowService, workspace, episodeDatabase, elementStore, "root",
            messageSender, messageReceiver);
    env.setArchive(archive);


    Date now = new Date(DateTimeSupport.fromUTC("2014-06-05T09:15:56Z"));
    EventComment comment = EventComment.create(Option.some(65L), "abc123", "mh_default_org", "Comment 1",
            userWithPermissions, "Sick", true, now, now);
    EventComment comment2 = EventComment.create(Option.some(65L), "abc123", "mh_default_org", "Comment 2",
            userWithPermissions, "Defect", false, now, now);
    EventCommentReply reply = EventCommentReply.create(Option.some(78L), "Cant reproduce", userWithoutPermissions, now,
            now);
    comment2.addReply(reply);

    EventCommentService eventCommentService = EasyMock.createNiceMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(EasyMock.anyString())).andReturn(Arrays.asList(comment, comment2))
            .anyTimes();
    EasyMock.expect(eventCommentService.getComment(EasyMock.anyLong())).andReturn(comment2);
    EasyMock.expect(eventCommentService.updateComment(EasyMock.anyObject(EventComment.class))).andReturn(comment2);
    EasyMock.replay(eventCommentService);
    env.setEventCommentService(eventCommentService);

    HttpMediaPackageElementProvider httpMediaPackageElementProvider = EasyMock
            .createNiceMock(HttpMediaPackageElementProvider.class);
    EasyMock.expect(httpMediaPackageElementProvider.getUriRewriter()).andReturn(rewriter).anyTimes();
    EasyMock.replay(httpMediaPackageElementProvider);

    env.setHttpMediaPackageElementProvider(httpMediaPackageElementProvider);

    Map<String, String> licences = new HashMap<>();
    licences.put("uuid-series1", "Series 1");
    licences.put("uuid-series2", "Series 2");

    ListProvidersService listProvidersService = EasyMock.createNiceMock(ListProvidersService.class);
    EasyMock.expect(listProvidersService.getList(EasyMock.anyString(), EasyMock.anyObject(ResourceListQuery.class),
            EasyMock.anyObject(Organization.class), false)).andReturn(licences).anyTimes();
    EasyMock.replay(listProvidersService);

    final IncidentTree r = new IncidentTreeImpl(
            Immutables
                    .list(mkIncident(Severity.INFO), mkIncident(Severity.INFO),
                            mkIncident(
                                    Severity.INFO)),
            Immutables
                    .<IncidentTree> list(
                            new IncidentTreeImpl(
                                    Immutables
                                            .list(mkIncident(Severity.INFO),
                                                    mkIncident(
                                                            Severity.WARNING)),
                                    Immutables.<IncidentTree> list(new IncidentTreeImpl(
                                            Immutables.list(mkIncident(Severity.WARNING), mkIncident(Severity.INFO)),
                                            Immutables.<IncidentTree> nil())))));

    IncidentService incidentService = EasyMock.createNiceMock(IncidentService.class);
    EasyMock.expect(incidentService.getIncident(EasyMock.anyLong())).andReturn(mkIncident(Severity.INFO)).anyTimes();
    EasyMock.expect(incidentService.getIncidentsOfJob(EasyMock.anyLong(), EasyMock.anyBoolean())).andReturn(r)
            .anyTimes();
    EasyMock.replay(incidentService);

    JobEndpoint endpoint = new JobEndpoint();
    endpoint.setServiceRegistry(serviceRegistry);
    endpoint.setWorkflowService(workflowService);
    endpoint.setIncidentService(incidentService);
    endpoint.activate(null);
    env.setJobService(endpoint);

    List<DublinCoreCatalog> catalogs = new ArrayList<>();
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    catalogs.add(DublinCores.read(TestEventEndpoint.class.getResourceAsStream("/dublincore2.xml")));
    DublinCoreCatalogList dublinCoreCatalogList = new DublinCoreCatalogList(catalogs, 1);
    SchedulerService schedulerService = EasyMock.createNiceMock(SchedulerService.class);
    EasyMock.expect(schedulerService.findConflictingEvents(EasyMock.anyString(), EasyMock.anyObject(Date.class),
            EasyMock.anyObject(Date.class))).andReturn(dublinCoreCatalogList).anyTimes();
    EasyMock.expect(schedulerService.findConflictingEvents(EasyMock.anyString(), EasyMock.anyString(),
            EasyMock.anyObject(Date.class), EasyMock.anyObject(Date.class), EasyMock.anyLong(), EasyMock.anyString()))
            .andReturn(dublinCoreCatalogList).anyTimes();
    EasyMock.replay(schedulerService);
    env.setSchedulerService(schedulerService);

    CaptureAgentStateService captureAgentStateService = EasyMock.createNiceMock(CaptureAgentStateService.class);
    EasyMock.expect(captureAgentStateService.getAgent(EasyMock.anyString())).andReturn(getAgent()).anyTimes();
    EasyMock.replay(captureAgentStateService);
    env.setCaptureAgentStateService(captureAgentStateService);

    EventCatalogUIAdapter catalogUIAdapter = EasyMock.createNiceMock(EventCatalogUIAdapter.class);
    EasyMock.replay(catalogUIAdapter);

    CommonEventCatalogUIAdapter episodeDublinCoreCatalogUIAdapter = new CommonEventCatalogUIAdapter();

    Properties episodeCatalogProperties = getCatalogProperties(getClass(), "/episode-catalog.properties");

    episodeDublinCoreCatalogUIAdapter.updated(episodeCatalogProperties);

    IndexServiceImpl indexService = new IndexServiceImpl();
    indexService.addCatalogUIAdapter(catalogUIAdapter);
    indexService.setCommonEventCatalogUIAdapter(episodeDublinCoreCatalogUIAdapter);
    indexService.setSecurityService(securityService);
    indexService.setSeriesService(seriesService);
    indexService.setWorkspace(workspace);
    env.setIndexService(indexService);

    // TODO authorization service
  }

  private Agent getAgent() {
    return new Agent() {
      @Override
      public void setUrl(String agentUrl) {
      }

      @Override
      public void setState(String newState) {
      }

      @Override
      public void setLastHeardFrom(Long time) {
      }

      @Override
      public void setConfiguration(Properties configuration) {
      }

      @Override
      public String getUrl() {
        return "10.234.12.323";
      }

      @Override
      public String getState() {
        return "idle";
      }

      @Override
      public String getName() {
        return "testagent";
      }

      @Override
      public Long getLastHeardFrom() {
        return 13345L;
      }

      @Override
      public Properties getConfiguration() {
        Properties properties = new Properties();
        properties.put(CaptureParameters.CAPTURE_DEVICE_NAMES, "input1,input2");
        properties.put(CaptureParameters.AGENT_NAME, "testagent");
        properties.put("capture.device.timezone.offset", "-360");
        properties.put("capture.device.timezone", "America/Los_Angeles");
        return properties;
      }

      @Override
      public Properties getCapabilities() {
        Properties properties = new Properties();
        properties.put(CaptureParameters.CAPTURE_DEVICE_NAMES, "input1,input2");
        return properties;
      }
    };
  }

  private TransitionResult getTransitionResult(final ManagedAcl macl, final Date now) {
    return new TransitionResultImpl(
            org.opencastproject.util.data.Collections.<EpisodeACLTransition> list(new EpisodeACLTransition() {
              @Override
              public String getEpisodeId() {
                return "episode";
              }

              @Override
              public Option<ManagedAcl> getAccessControlList() {
                return some(macl);
              }

              @Override
              public boolean isDelete() {
                return getAccessControlList().isNone();
              }

              @Override
              public long getTransitionId() {
                return 1L;
              }

              @Override
              public String getOrganizationId() {
                return "org";
              }

              @Override
              public Date getApplicationDate() {
                return now;
              }

              @Override
              public Option<ConfiguredWorkflowRef> getWorkflow() {
                return none();
              }

              @Override
              public boolean isDone() {
                return false;
              }
            }), org.opencastproject.util.data.Collections.<SeriesACLTransition> list(new SeriesACLTransition() {
              @Override
              public String getSeriesId() {
                return "series";
              }

              @Override
              public ManagedAcl getAccessControlList() {
                return macl;
              }

              @Override
              public boolean isOverride() {
                return true;
              }

              @Override
              public long getTransitionId() {
                return 2L;
              }

              @Override
              public String getOrganizationId() {
                return "org";
              }

              @Override
              public Date getApplicationDate() {
                return now;
              }

              @Override
              public Option<ConfiguredWorkflowRef> getWorkflow() {
                return none();
              }

              @Override
              public boolean isDone() {
                return false;
              }
            }));
  }

  private OpencastResultItem getOpencastResultItem(final AccessControlList acl,
          final WorkflowInstanceImpl workflowInstance) {
    return new OpencastResultItem() {
      @Override
      public String getMediaPackageId() {
        return workflowInstance.getMediaPackage().getIdentifier().compact();
      }

      @Override
      public Option<OcDublinCore> getSeriesDublinCore() {
        return Option.some(OcDublinCoreUtil
                .create(DublinCores.read(TestEventEndpoint.class.getResourceAsStream("/dublincore2.xml")))
                .getDublinCore());
      }

      @Override
      public MediaPackage getMediaPackage() {
        return workflowInstance.getMediaPackage();
      }

      @Override
      public Option<String> getSeriesId() {
        return Option.option(workflowInstance.getMediaPackage().getSeries());
      }

      @Override
      public AccessControlList getAcl() {
        return acl;
      }

      @Override
      public Version getVersion() {
        return Version.version(1);
      }

      @Override
      public String getOrganizationId() {
        return defaultOrganization.getId();
      }

      @Override
      public OcDublinCore getDublinCore() {
        return OcDublinCoreUtil
                .create(DublinCores.read(TestEventEndpoint.class.getResourceAsStream("/dublincore3.xml")))
                .getDublinCore();
      }

      @Override
      public boolean isLatestVersion() {
        return true;
      }

    };
  }

  private MediaPackage loadMpFromResource(String name) throws Exception {
    URI publishedMediaPackageURI = getClass().getResource("/" + name + ".xml").toURI();
    return mpBuilder.loadFromXml(publishedMediaPackageURI.toURL().openStream());
  }

  private Incident mkIncident(Severity s) throws Exception {
    Date date = new Date(DateTimeSupport.fromUTC("2014-06-05T09:15:56Z"));
    return new IncidentImpl(0, 0, "servicetype", "host", date, s, "code", Incidents.NO_DETAILS, Incidents.NO_PARAMS);
  }

  @Override
  public WorkflowService getWorkflowService() {
    return env.getWorkflowService();
  }

  @Override
  public OpencastArchive getArchive() {
    return env.getArchive();
  }

  @Override
  public HttpMediaPackageElementProvider getHttpMediaPackageElementProvider() {
    return env.getHttpMediaPackageElementProvider();
  }

  @Override
  public JobEndpoint getJobService() {
    return env.getJobService();
  }

  @Override
  public AclService getAclService() {
    return env.getAclService();
  }

  @Override
  public EventCommentService getEventCommentService() {
    return env.getEventCommentService();
  }

  @Override
  public SecurityService getSecurityService() {
    return env.getSecurityService();
  }

  @Override
  public IndexService getIndexService() {
    return env.getIndexService();
  }

  @Override
  public AuthorizationService getAuthorizationService() {
    return env.getAuthorizationService();
  }

  @Override
  public SchedulerService getSchedulerService() {
    return env.getSchedulerService();
  }

  @Override
  public CaptureAgentStateService getCaptureAgentStateService() {
    return env.getCaptureAgentStateService();
  }

  @Override
  public AdminUISearchIndex getIndex() {
    return env.getIndex();
  }

  @Override
  public UrlSigningService getUrlSigningService() {
    return env.getUrlSigningService();
  }

  @Override
  public AdminUIConfiguration getAdminUIConfiguration() {
    return env.getAdminUIConfiguration();
  }

  @Override
  public long getUrlSigningExpireDuration() {
    return DEFAULT_URL_SIGNING_EXPIRE_DURATION;
  }

  @Override
  public Boolean signWithClientIP() {
    return false;
  }

}
