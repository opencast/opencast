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
package org.opencastproject.adminui.endpoint;

import static org.opencastproject.pm.api.Person.person;
import static org.opencastproject.security.api.SecurityConstants.GLOBAL_ADMIN_ROLE;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Collections.nil;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.adminui.endpoint.AbstractEventEndpointTest.TestEnv;
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
import org.opencastproject.comments.Comment;
import org.opencastproject.comments.CommentReply;
import org.opencastproject.comments.events.EventCommentService;
import org.opencastproject.fun.juc.Immutables;
import org.opencastproject.index.service.api.IndexService;
import org.opencastproject.index.service.catalog.adapter.events.CommonEventCatalogUIAdapter;
import org.opencastproject.index.service.catalog.adapter.events.EventCatalogUIAdapter;
import org.opencastproject.index.service.resources.list.api.ListProvidersService;
import org.opencastproject.index.service.resources.list.api.ResourceListQuery;
import org.opencastproject.ingest.api.IngestService;
import org.opencastproject.job.api.Incident;
import org.opencastproject.job.api.Incident.Severity;
import org.opencastproject.job.api.IncidentImpl;
import org.opencastproject.job.api.IncidentTree;
import org.opencastproject.job.api.IncidentTreeImpl;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.kernel.mail.EmailAddress;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderImpl;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.message.broker.api.MessageReceiver;
import org.opencastproject.message.broker.api.MessageSender;
import org.opencastproject.messages.MessageSignature;
import org.opencastproject.messages.MessageTemplate;
import org.opencastproject.messages.TemplateType;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogList;
import org.opencastproject.metadata.dublincore.DublinCoreCatalogService;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.StaticMetadataServiceDublinCoreImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
import org.opencastproject.pm.api.Action;
import org.opencastproject.pm.api.CaptureAgent;
import org.opencastproject.pm.api.Course;
import org.opencastproject.pm.api.Message;
import org.opencastproject.pm.api.Person;
import org.opencastproject.pm.api.PersonType;
import org.opencastproject.pm.api.Recording;
import org.opencastproject.pm.api.Room;
import org.opencastproject.pm.api.persistence.ParticipationManagementDatabase;
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
import org.opencastproject.series.api.SeriesService;
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
import org.joda.time.DateTime;
import org.junit.Ignore;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
          "test", defaultOrganization, new HashSet<JaxbRole>(Arrays.asList(new JaxbRole("ROLE_STUDENT",
                  defaultOrganization), new JaxbRole("ROLE_OTHERSTUDENT", defaultOrganization), new JaxbRole(
                  defaultOrganization.getAnonymousRole(), defaultOrganization))));

  /** A user without permissions. */
  private final User userWithoutPermissions = new JaxbUser("sample", null, "WithoutPermissions",
          "without@permissions.com", "test", opencastOrganization, new HashSet<JaxbRole>(Arrays.asList(new JaxbRole(
                  "ROLE_NOTHING", opencastOrganization))));

  private final User defaultUser = userWithPermissions;

  private final UriRewriter rewriter = new UriRewriter() {
    @Override
    public URI apply(Version version, MediaPackageElement mpe) {
      return uri("http://episodes", mpe.getMediaPackage().getIdentifier(), mpe.getIdentifier(), version, mpe
              .getElementType().toString().toLowerCase()
              + "." + mpe.getMimeType().getSuffix().getOrElse(".unknown"));

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

    // AdminUISearchIndex
    AdminUISearchIndex searchIndex = EasyMock.createNiceMock(AdminUISearchIndex.class);
    EasyMock.replay(searchIndex);
    env.setIndex(searchIndex);

    // Preview subtype
    env.setPreviewSubtype(PREVIEW_SUBTYPE);

    // acl
    final String anonymousRole = securityService.getOrganization().getAnonymousRole();
    final AccessControlList acl = new AccessControlList(new AccessControlEntry(anonymousRole,
            Permissions.Action.READ.toString(), true));
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
    JaxbJob job = new JaxbJob(12L);
    Date dateCreated = new Date(DateTimeSupport.fromUTC("2014-06-05T15:00:00Z"));
    Date dateCompleted = new Date(DateTimeSupport.fromUTC("2014-06-05T16:00:00Z"));
    job.setDateCreated(dateCreated);
    job.setDateCompleted(dateCompleted);
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(job).anyTimes();
    EasyMock.expect(
            serviceRegistry.createJob((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (List<String>) EasyMock.anyObject(), (String) EasyMock.anyObject(), EasyMock.anyBoolean()))
            .andReturn(new JaxbJob()).anyTimes();
    EasyMock.expect(serviceRegistry.updateJob((Job) EasyMock.anyObject())).andReturn(new JaxbJob()).anyTimes();
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
    env.setWorkspace(workspace);

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
      public AccessControlList getAccessControl(String seriesID) throws NotFoundException,
              SeriesServiceDatabaseException {
        return acl;
      }

    };
    seriesService.setIndex(seriesServiceSolrIndex);
    env.setSeriesService(seriesService);

    StaticMetadataServiceDublinCoreImpl metadataSvcs = new StaticMetadataServiceDublinCoreImpl();
    metadataSvcs.setWorkspace(workspace);

    final SolrIndexManager solrIndex = new SolrIndexManager(solrServer, workspace, VCell.cell(Arrays
            .asList((StaticMetadataService) metadataSvcs)), seriesService, mpeg7CatalogService, securityService);

    // Org directory
    MessageSender messageSender = EasyMock.createNiceMock(MessageSender.class);
    EasyMock.replay(messageSender);

    MessageReceiver messageReceiver = EasyMock.createNiceMock(MessageReceiver.class);
    EasyMock.replay(messageReceiver);

    OpencastArchive archive = new OpencastArchive(solrIndex, solrRequester, securityService, authorizationService,
            orgDirectory, serviceRegistry, workflowService, workspace, episodeDatabase, elementStore, "root",
            messageSender, messageReceiver);
    env.setArchive(archive);

    ParticipationManagementDatabase pmDatabase = EasyMock.createNiceMock(ParticipationManagementDatabase.class);
    EasyMock.expect(pmDatabase.getRecordingByEvent(EasyMock.anyLong())).andReturn(
            createRecording(1, "A", "Test title A"));
    EasyMock.expect(pmDatabase.getRecordingByEvent(EasyMock.anyLong())).andReturn(
            createRecording(2, "B", "Test title B"));
    EasyMock.expect(pmDatabase.getRecordingByEvent(EasyMock.anyLong())).andReturn(
            createRecording(3, "C", "Test title C"));
    EasyMock.expect(pmDatabase.getMessagesByRecordingId(EasyMock.anyLong(), EasyMock.anyObject(Option.class)))
            .andReturn(Arrays.asList(createMessage(1, "template1", "Titel 1", "Body 1")));
    EasyMock.expect(pmDatabase.getMessagesByRecordingId(EasyMock.anyLong(), EasyMock.anyObject(Option.class)))
            .andReturn(
                    Arrays.asList(createMessage(2, "template2", "Titel 2", "Body 2"),
                            createMessage(3, "template3", "Titel 3", "Body 3")));
    EasyMock.expect(pmDatabase.getMessagesByRecordingId(EasyMock.anyLong(), EasyMock.anyObject(Option.class)))
            .andReturn(Arrays.asList(createMessage(4, "template4", "Titel 4", "Body 4")));
    EasyMock.replay(pmDatabase);
    env.setParticipationManagementDatabase(pmDatabase);

    DublinCoreCatalogService dublinCoreCatalogService = EasyMock.createNiceMock(DublinCoreCatalogService.class);
    env.setDublinCoreCatalogService(dublinCoreCatalogService);

    Date now = new Date(DateTimeSupport.fromUTC("2014-06-05T09:15:56Z"));
    Comment comment = Comment.create(Option.some(65L), "Comment 1", userWithPermissions, "Sick", true, now, now);
    Comment comment2 = Comment.create(Option.some(65L), "Comment 2", userWithPermissions, "Defect", false, now, now);
    CommentReply reply = CommentReply.create(Option.some(78L), "Cant reproduce", userWithoutPermissions, now, now);
    comment2.addReply(reply);

    EventCommentService eventCommentService = EasyMock.createNiceMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.getComments(EasyMock.anyString())).andReturn(Arrays.asList(comment, comment2))
    .anyTimes();
    EasyMock.expect(eventCommentService.getComment(EasyMock.anyString(), EasyMock.anyLong())).andReturn(comment2);
    EasyMock.expect(eventCommentService.updateComment(EasyMock.anyString(), EasyMock.anyObject(Comment.class)))
    .andReturn(comment2);
    EasyMock.replay(eventCommentService);
    env.setEventCommentService(eventCommentService);

    HttpMediaPackageElementProvider httpMediaPackageElementProvider = EasyMock
            .createNiceMock(HttpMediaPackageElementProvider.class);
    EasyMock.expect(httpMediaPackageElementProvider.getUriRewriter()).andReturn(rewriter).anyTimes();
    EasyMock.replay(httpMediaPackageElementProvider);

    env.setHttpMediaPackageElementProvider(httpMediaPackageElementProvider);

    Map<String, Object> licences = new HashMap<>();
    licences.put("uuid-series1", "Series 1");
    licences.put("uuid-series2", "Series 2");

    ListProvidersService listProvidersService = EasyMock.createNiceMock(ListProvidersService.class);
    EasyMock.expect(
            listProvidersService.getList(EasyMock.anyString(), EasyMock.anyObject(ResourceListQuery.class),
                    EasyMock.anyObject(Organization.class))).andReturn(licences).anyTimes();
    EasyMock.replay(listProvidersService);
    env.setListProviderService(listProvidersService);

    final IncidentTree r = new IncidentTreeImpl(Immutables.list(mkIncident(Severity.INFO), mkIncident(Severity.INFO),
            mkIncident(Severity.INFO)), Immutables.<IncidentTree> list(new IncidentTreeImpl(Immutables.list(
            mkIncident(Severity.INFO), mkIncident(Severity.WARNING)), Immutables
            .<IncidentTree> list(new IncidentTreeImpl(Immutables.list(mkIncident(Severity.WARNING),
                    mkIncident(Severity.INFO)), Immutables.<IncidentTree> nil())))));

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
    catalogs.add(DublinCores.read(TestEventEndpoint.class.getResourceAsStream("/dublincore2.xml")));
    DublinCoreCatalogList dublinCoreCatalogList = new DublinCoreCatalogList(catalogs, 1);
    SchedulerService schedulerService = EasyMock.createNiceMock(SchedulerService.class);
    EasyMock.expect(
            schedulerService.findConflictingEvents(EasyMock.anyString(), EasyMock.anyObject(Date.class),
                    EasyMock.anyObject(Date.class))).andReturn(dublinCoreCatalogList).anyTimes();
    EasyMock.expect(
            schedulerService.findConflictingEvents(EasyMock.anyString(), EasyMock.anyString(),
                    EasyMock.anyObject(Date.class), EasyMock.anyObject(Date.class), EasyMock.anyLong(),
                    EasyMock.anyString())).andReturn(dublinCoreCatalogList).anyTimes();
    EasyMock.replay(schedulerService);
    env.setSchedulerService(schedulerService);

    CaptureAgentStateService captureAgentStateService = EasyMock.createNiceMock(CaptureAgentStateService.class);
    EasyMock.expect(captureAgentStateService.getAgent(EasyMock.anyString())).andReturn(getAgent()).anyTimes();
    EasyMock.replay(captureAgentStateService);
    env.setCaptureAgentStateService(captureAgentStateService);

    EventCatalogUIAdapter catalogUIAdapter = EasyMock.createNiceMock(EventCatalogUIAdapter.class);
    EasyMock.replay(catalogUIAdapter);
    env.setCatalogUIAdapter(catalogUIAdapter);

    CommonEventCatalogUIAdapter episodeDublinCoreCatalogUIAdapter = new CommonEventCatalogUIAdapter();
    episodeDublinCoreCatalogUIAdapter.activate();
    env.setEpisodeCatalogUIAdapter(episodeDublinCoreCatalogUIAdapter);

    // TODO ingest service
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
        return Option.some(OcDublinCoreUtil.create(
                DublinCores.read(TestEventEndpoint.class.getResourceAsStream("/dublincore2.xml"))).getDublinCore());
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
        return OcDublinCoreUtil.create(
                DublinCores.read(TestEventEndpoint.class.getResourceAsStream("/dublincore3.xml"))).getDublinCore();
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

  private Recording createRecording(long recordingId, String activityId, String title) {
    List<Person> staff = new ArrayList<>();
    List<PersonType> staffTypes = new ArrayList<>();
    staffTypes.add(new PersonType("group", "open door"));
    Person staff1 = person("Staff 1", "staff1@manchester.ac.uk", staffTypes);
    staff1.setId(9L);
    staff.add(staff1);
    Person staff2 = person("Staff 2", "staff2@manchester.ac.uk", staffTypes);
    staff2.setId(10L);
    staff.add(staff2);

    List<Person> students = new ArrayList<>();
    List<PersonType> studentTypes = new ArrayList<>();
    studentTypes.add(new PersonType("student", "learning"));
    students.add(person("Student 1", "student1@manchester.ac.uk", studentTypes));
    students.add(person("Student 2", "student2@manchester.ac.uk", studentTypes));

    Room room = new Room("Aula");
    CaptureAgent captureAgent = new CaptureAgent(room, CaptureAgent.getMhAgentIdFromRoom(room));
    Course course = new Course("mathe", "uuid", "Math", "Simple course about algebra.");

    Recording recording = Recording.recording(activityId, title, staff, some(course), room, new Date(), new DateTime()
            .plusHours(2).toDate(), new DateTime().plusHours(3).toDate(), students, nil(Message.class), some(4L),
            captureAgent, nil(Action.class), false, false);
    recording.setId(Option.some(recordingId));
    return recording;
  }

  private Message createMessage(long id, String name, String subject, String body) throws Exception {
    Date now = new Date(DateTimeSupport.fromUTC("2014-06-04T13:32:37Z"));
    List<PersonType> staffTypes = new ArrayList<>();
    PersonType personType = new PersonType("group", "open door");
    personType.setId(33L);
    staffTypes.add(personType);
    Person staff1 = person("Staff 1", "staff1@manchester.ac.uk", staffTypes);
    staff1.setId(9L);

    List<JaxbRole> roles = Arrays.asList(new JaxbRole(GLOBAL_ADMIN_ROLE, defaultOrganization), new JaxbRole(
            defaultOrganization.getAdminRole(), defaultOrganization));
    User user = new JaxbUser("admin", null, "Admin", "admin@test.com", "test", defaultOrganization,
            new HashSet<>(roles));
    MessageTemplate messageTemplate = new MessageTemplate(name, user, subject, body, TemplateType.INVITATION, now,
            Collections.EMPTY_LIST);
    messageTemplate.setId(id);
    MessageSignature signature = new MessageSignature(11L, "Default", user, EmailAddress.emailAddress(
            "sender@test.com", "Sender Address"), Option.none(EmailAddress.class), "Nothing", now,
            Collections.EMPTY_LIST);
    Message message = new Message(now, staff1, messageTemplate, signature, Collections.EMPTY_LIST);
    message.setId(id);
    return message;
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
  public Workspace getWorkspace() {
    return env.getWorkspace();
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
  public ListProvidersService getListProviderService() {
    return env.getListProviderService();
  }

  @Override
  public AclService getAclService() {
    return env.getAclService();
  }

  @Override
  public SeriesService getSeriesService() {
    return env.getSeriesService();
  }

  @Override
  public ParticipationManagementDatabase getPMPersistence() {
    return env.getPmPersistence();
  }

  @Override
  public DublinCoreCatalogService getDublinCoreService() {
    return env.getDublinCoreService();
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
  public IngestService getIngestService() {
    return env.getIngestService();
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
  public EventCatalogUIAdapter getEpisodeCatalogUIAdapter() {
    return env.getEpisodeCatalogUIAdapter();
  }

  @Override
  public List<EventCatalogUIAdapter> getEventCatalogUIAdapters(String organization) {
    return env.getCatalogUIAdapters();
  }

  @Override
  public AdminUISearchIndex getIndex() {
    return env.getIndex();
  }

  @Override
  public String getPreviewSubtype() {
    return env.getPreviewSubtype();
  }

}
