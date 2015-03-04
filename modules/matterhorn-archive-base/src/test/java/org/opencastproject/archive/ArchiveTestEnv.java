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
package org.opencastproject.archive;

import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.UrlSupport.uri;
import static org.opencastproject.util.data.Collections.map;
import static org.opencastproject.util.data.Tuple.tuple;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.archive.api.Archive;
import org.opencastproject.archive.api.UriRewriter;
import org.opencastproject.archive.api.Version;
import org.opencastproject.archive.base.ArchiveBase;
import org.opencastproject.archive.base.StoragePath;
import org.opencastproject.archive.base.persistence.AbstractArchiveDb;
import org.opencastproject.archive.base.persistence.ArchiveDb;
import org.opencastproject.archive.base.storage.DeletionSelector;
import org.opencastproject.archive.base.storage.ElementStore;
import org.opencastproject.inspection.api.MediaInspectionService;
import org.opencastproject.job.api.JaxbJob;
import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.metadata.api.StaticMetadataService;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.StaticMetadataServiceDublinCoreImpl;
import org.opencastproject.metadata.mpeg7.Mpeg7CatalogService;
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
import org.opencastproject.security.api.Role;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.series.impl.SeriesServiceDatabaseException;
import org.opencastproject.series.impl.SeriesServiceImpl;
import org.opencastproject.series.impl.solr.SeriesServiceSolrIndex;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.solr.SolrServerFactory;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Collections;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Tuple;
import org.opencastproject.util.persistence.PersistenceEnv;
import org.opencastproject.util.persistence.PersistenceUtil;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.easymock.EasyMock;
import org.easymock.IAnswer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ArchiveTestEnv {
  /** The search service */
  private ArchiveBase service = null;

  /** The solr root directory */
  private final String solrRoot = "target" + File.separator + "opencast" + File.separator + "searchindex";

  /** The access control list returned by the mocked authorization service */
  private AccessControlList acl = null;

  private final JaxbOrganization defaultOrganization = new DefaultOrganization();

  private final JaxbOrganization opencastOrganization = new JaxbOrganization("opencastproject.org");

  /** A user with permissions. */
  private final User userWithPermissions = new JaxbUser("sample", "test", defaultOrganization, new JaxbRole(
          "ROLE_STUDENT", defaultOrganization), new JaxbRole("ROLE_OTHERSTUDENT", defaultOrganization), new JaxbRole(
          defaultOrganization.getAnonymousRole(), defaultOrganization));

  /** A user without permissions. */
  private final User userWithoutPermissions = new JaxbUser("sample", "test", opencastOrganization, new JaxbRole(
          "ROLE_NOTHING", opencastOrganization));

  private final User defaultUser = userWithPermissions;

  private SolrServer solrServer;
  private Responder<User> userResponder;
  private Responder<Organization> organizationResponder;
  private ArchiveDb episodeDatabase;
  private SecurityService securityService;
  private PersistenceEnv penv;
  private String storage;

  private UriRewriter rewriter = new UriRewriter() {
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

  public ArchiveTestEnv() {
    try {
      newEnv();
    } catch (Exception e) {
      chuck(e);
    }
  }

  private void newEnv() throws Exception {
    // workspace
    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get((URI) EasyMock.anyObject())).andAnswer(new IAnswer<File>() {
      @Override
      public File answer() throws Throwable {
        final URI uri = (URI) EasyMock.getCurrentArguments()[0];
        if ("file".equals(uri.getScheme())) {
          return new File(uri);
        } else {
          return new File(getClass().getResource("/" + uri.toString()).toURI());
        }
      }
    }).anyTimes();
    EasyMock.replay(workspace);
    // service registry
    final ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(
            serviceRegistry.createJob((String) EasyMock.anyObject(), (String) EasyMock.anyObject(),
                    (List<String>) EasyMock.anyObject(), (String) EasyMock.anyObject(), EasyMock.anyBoolean()))
            .andReturn(new JaxbJob()).anyTimes();
    EasyMock.expect(serviceRegistry.updateJob((Job) EasyMock.anyObject())).andReturn(new JaxbJob()).anyTimes();
    EasyMock.expect(serviceRegistry.getJobs((String) EasyMock.anyObject(), (Job.Status) EasyMock.anyObject()))
            .andReturn(new ArrayList<Job>()).anyTimes();
    EasyMock.replay(serviceRegistry);
    // element store
    final ElementStore elementStore = EasyMock.createNiceMock(ElementStore.class);
    EasyMock.expect(elementStore.delete(EasyMock.<DeletionSelector> anyObject())).andReturn(true).once();
    EasyMock.expect(elementStore.copy(EasyMock.<StoragePath> anyObject(), EasyMock.<StoragePath> anyObject()))
            .andReturn(true).anyTimes();
    EasyMock.replay(elementStore);
    // mpeg7 service
    final Mpeg7CatalogService mpeg7CatalogService = new Mpeg7CatalogService();
    // security service
    userResponder = new Responder<User>(defaultUser);
    organizationResponder = new Responder<Organization>(defaultOrganization);
    securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andAnswer(userResponder).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andAnswer(organizationResponder).anyTimes();
    EasyMock.replay(securityService);
    //
    long currentTime = System.currentTimeMillis();
    storage = PathSupport.concat("target", "db" + currentTime + ".h2.db");
    // Persistence storage
    penv = PersistenceUtil.newTestPersistenceEnv("org.opencastproject.archive.base.persistence");
    episodeDatabase = new AbstractArchiveDb() {
      @Override
      protected PersistenceEnv getPenv() {
        return penv;
      }

      @Override
      protected SecurityService getSecurityService() {
        return securityService;
      }
    };
    // media inspection service
    final MediaInspectionService mediaInspectionService = EasyMock.createMock(MediaInspectionService.class);
    EasyMock.expect(mediaInspectionService.enrich(EasyMock.<MediaPackageElement> anyObject(), EasyMock.anyBoolean()))
            .andAnswer(new IAnswer<Job>() {
              @Override
              public Job answer() throws Throwable {
                // enrich with dummy checksum (the id)
                final MediaPackageElement mpe = (MediaPackageElement) ((MediaPackageElement) EasyMock
                        .getCurrentArguments()[0]).clone();
                mpe.setChecksum(Checksum.create("md5", mpe.getIdentifier()));
                // create job
                final Job job = new JaxbJob(1L);
                job.setPayload(MediaPackageElementParser.getAsXml(mpe));
                job.setStatus(Job.Status.FINISHED);
                return job;
              }
            }).anyTimes();
    EasyMock.replay(mediaInspectionService);
    // acl
    final String anonymousRole = securityService.getOrganization().getAnonymousRole();
    acl = new AccessControlList(new AccessControlEntry(anonymousRole, "read", true));
    /* The authorization service */
    final AuthorizationService authorizationService = EasyMock.createNiceMock(AuthorizationService.class);
    EasyMock.expect(authorizationService.getActiveAcl((MediaPackage) EasyMock.anyObject()))
            .andReturn(new Tuple<AccessControlList, AclScope>(acl, AclScope.Episode)).anyTimes();
    EasyMock.expect(
            authorizationService.hasPermission((MediaPackage) EasyMock.anyObject(), (String) EasyMock.anyObject()))
            .andReturn(true).anyTimes();
    EasyMock.replay(authorizationService);
    // Org directory
    OrganizationDirectoryService orgDirectory = EasyMock.createNiceMock(OrganizationDirectoryService.class);
    EasyMock.expect(orgDirectory.getOrganization((String) EasyMock.anyObject())).andReturn(new DefaultOrganization())
            .anyTimes();
    EasyMock.replay(orgDirectory);
    // episode service
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
    };
    seriesService.setIndex(seriesServiceSolrIndex);
    // episode service
    // todo
    // solrServer = EpisodeServicePublisher.setupSolr(new File(solrRoot));
    // final StaticMetadataService mdService = newStaticMetadataService(workspace);
    // service = new ArchiveBase(new SolrRequester(solrServer),
    // new SolrIndexManager(solrServer,
    // workspace,
    // cell(Arrays.asList(mdService)),
    // seriesService,
    // mpeg7CatalogService,
    // securityService),
    // securityService,
    // authorizationService,
    // orgDirectory,
    // serviceRegistry,
    // null,
    // workspace,
    // mediaInspectionService,
    // episodeDatabase,
    // elementStore,
    // "System Admin");
  }

  @SuppressWarnings("unchecked")
  public void setReadWritePermissions() {
    Role role = Collections.toList(getUserWithPermissions().getRoles()).get(0);
    getAcl().getEntries().add(new AccessControlEntry(role.getName(), Archive.READ_PERMISSION, true));
    getAcl().getEntries().add(new AccessControlEntry(role.getName(), Archive.WRITE_PERMISSION, true));
  }

  private StaticMetadataService newStaticMetadataService(Workspace workspace) {
    StaticMetadataServiceDublinCoreImpl service = new StaticMetadataServiceDublinCoreImpl();
    service.setWorkspace(workspace);
    return service;
  }

  public void tearDown() {
    penv.close();
    FileUtils.deleteQuietly(new File(storage));
    episodeDatabase = null;
    SolrServerFactory.shutdown(solrServer);
    try {
      FileUtils.deleteDirectory(new File(solrRoot));
    } catch (IOException e) {
      chuck(e);
    }
    service = null;
  }

  public ArchiveBase getService() {
    return service;
  }

  public AccessControlList getAcl() {
    return acl;
  }

  public User getUserWithPermissions() {
    return userWithPermissions;
  }

  public User getUserWithoutPermissions() {
    return userWithoutPermissions;
  }

  public Organization getDefaultOrganization() {
    return defaultOrganization;
  }

  public User getDefaultUser() {
    return defaultUser;
  }

  public Responder<User> getUserResponder() {
    return userResponder;
  }

  public Responder<Organization> getOrganizationResponder() {
    return organizationResponder;
  }

  public JaxbOrganization getOpencastOrganization() {
    return opencastOrganization;
  }

  public ArchiveDb getEpisodeDatabase() {
    return episodeDatabase;
  }

  public SecurityService getSecurityService() {
    return securityService;
  }

  public UriRewriter getRewriter() {
    return rewriter;
  }
}
