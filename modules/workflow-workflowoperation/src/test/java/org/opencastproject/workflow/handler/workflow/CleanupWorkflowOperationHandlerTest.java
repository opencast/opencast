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
package org.opencastproject.workflow.handler.workflow;

import org.opencastproject.job.api.Job;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilder;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.selector.SimpleElementSelector;
import org.opencastproject.security.api.TrustedHttpClient;
import org.opencastproject.serviceregistry.api.ServiceRegistration;
import org.opencastproject.serviceregistry.api.ServiceRegistry;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.workflow.api.WorkflowInstance;
import org.opencastproject.workflow.api.WorkflowOperationException;
import org.opencastproject.workflow.api.WorkflowOperationInstance;
import org.opencastproject.workflow.api.WorkflowOperationInstanceImpl;
import org.opencastproject.workingfilerepository.api.WorkingFileRepository;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpUriRequest;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Test class for {@link CleanupWorkflowOperationHandler}
 */
public class CleanupWorkflowOperationHandlerTest {

  private static final String HOSTNAME_NODE1 = "http://node1.opencast.org";
  private static final String HOSTNAME_NODE2 = "http://node2.opencast.org";
  private static final String WFR_URL_PREFIX = "/files";

  private CleanupWorkflowOperationHandler cleanupWOH = null;
  private List<URI> deletedFilesURIs = new ArrayList<>();

  @Before
  public void setUp() throws Exception {
    cleanupWOH = new CleanupWorkflowOperationHandler();

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.getBaseUri()).andReturn(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX)).anyTimes();
    EasyMock.replay(workspace);
    cleanupWOH.setWorkspace(workspace);

    List<ServiceRegistration> wfrServiceRegistrations = new ArrayList<ServiceRegistration>();
    wfrServiceRegistrations.add(createWfrServiceRegistration(HOSTNAME_NODE1, WFR_URL_PREFIX));
    wfrServiceRegistrations.add(createWfrServiceRegistration(HOSTNAME_NODE2, WFR_URL_PREFIX));
    ServiceRegistry serviceRegistry = EasyMock.createNiceMock(ServiceRegistry.class);
    EasyMock.expect(serviceRegistry.getServiceRegistrationsByType(EasyMock.eq(WorkingFileRepository.SERVICE_TYPE)))
            .andReturn(wfrServiceRegistrations).anyTimes();
    Job currentJob = EasyMock.createNiceMock(Job.class);
    currentJob.setArguments((List<String>) EasyMock.anyObject());
    EasyMock.expect(serviceRegistry.getJob(EasyMock.anyLong())).andReturn(currentJob).anyTimes();
    EasyMock.expect(serviceRegistry.updateJob((Job) EasyMock.anyObject())).andReturn(currentJob).anyTimes();
    EasyMock.expect(serviceRegistry.getChildJobs(EasyMock.anyLong())).andReturn(new ArrayList<Job>()).anyTimes();
    EasyMock.replay(serviceRegistry, currentJob);
    cleanupWOH.setServiceRegistry(serviceRegistry);

    TrustedHttpClient httpClient = EasyMock.createNiceMock(TrustedHttpClient.class);
    HttpResponse httpResponse = EasyMock.createNiceMock(HttpResponse.class);
    StatusLine responseStatusLine = EasyMock.createNiceMock(StatusLine.class);
    EasyMock.expect(responseStatusLine.getStatusCode()).andReturn(HttpStatus.SC_OK).anyTimes();
    EasyMock.expect(httpResponse.getStatusLine()).andReturn(responseStatusLine).anyTimes();
    EasyMock.expect(httpClient.execute(StoreUrisArgumentMatcher.createMatcher(deletedFilesURIs,
            UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX)))).andReturn(httpResponse).anyTimes();
    EasyMock.replay(httpClient, httpResponse, responseStatusLine);
    cleanupWOH.setTrustedHttpClient(httpClient);
  }

  private ServiceRegistration createWfrServiceRegistration(String hostname, String path) {
    ServiceRegistration wfrServiceReg = EasyMock.createNiceMock(ServiceRegistration.class);
    EasyMock.expect(wfrServiceReg.getHost()).andReturn(hostname).anyTimes();
    EasyMock.expect(wfrServiceReg.getPath()).andReturn(path).anyTimes();
    EasyMock.replay(wfrServiceReg);
    return wfrServiceReg;
  }

  private WorkflowInstance createWorkflowInstance(Map<String, String> configuration, MediaPackage mp) {
    WorkflowOperationInstance wfOpInst = new WorkflowOperationInstanceImpl();
    if (configuration != null) {
      for (String confKey : configuration.keySet()) {
        wfOpInst.setConfiguration(confKey, configuration.get(confKey));
      }
    }
    wfOpInst.setId(1L);
    wfOpInst.setState(WorkflowOperationInstance.OperationState.RUNNING);
    WorkflowInstance wfInst = EasyMock.createNiceMock(WorkflowInstance.class);
    EasyMock.expect(wfInst.getMediaPackage()).andReturn(mp).anyTimes();
    EasyMock.expect(wfInst.getCurrentOperation()).andReturn(wfOpInst).anyTimes();
    EasyMock.expect(wfInst.getOperations()).andReturn(Arrays.asList(wfOpInst)).anyTimes();
    EasyMock.replay(wfInst);
    return wfInst;
  }

  private static MediaPackageElement addElementToMediaPackage(MediaPackage mp, MediaPackageElement.Type elemType,
          String flavorType, String flavorSubtype, URI uri) {
    MediaPackageElementBuilder mpeBuilder = MediaPackageElementBuilderFactory.newInstance().newElementBuilder();
    MediaPackageElement mpe = mpeBuilder.newElement(elemType, MediaPackageElementFlavor.flavor(
            flavorType, flavorSubtype));
    mpe.setIdentifier(UUID.randomUUID().toString());
    if (uri != null)
      mpe.setURI(uri);
    mp.add(mpe);
    return mpe;
  }

  @Test
  public void testCreanupWOHwithPreservedFlavorAndMediaPackagePathPrefix() throws WorkflowOperationException,
          MediaPackageException {
    Map<String, String> wfInstConfig = new Hashtable<>();
    wfInstConfig.put(CleanupWorkflowOperationHandler.PRESERVE_FLAVOR_PROPERTY, "*/source,smil/trimmed,security/*");
    wfInstConfig.put(CleanupWorkflowOperationHandler.DELETE_EXTERNAL, "true");

    MediaPackageBuilder mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    MediaPackage mp = mpBuilder.createNew();
    MediaPackageElement track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
            "presenter", "source", null);
    track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
            mp.getIdentifier().compact(), track1.getIdentifier(), "track.mp4"));
    MediaPackageElement track2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
            "presentation", "work", null);
    track2.setURI(UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
            mp.getIdentifier().compact(), track2.getIdentifier(), "track.mp4"));
    MediaPackageElement att1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Attachment,
            "presentation", "preview", null);
    att1.setURI(UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
            mp.getIdentifier().compact(), att1.getIdentifier(), "preview.png"));
    MediaPackageElement att2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Attachment,
            "smil", "trimmed", null);
    att2.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
            mp.getIdentifier().compact(), att2.getIdentifier(), "trimmed.smil"));
    MediaPackageElement cat1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Catalog,
            "dublincore", "episode", null);
    cat1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
            mp.getIdentifier().compact(), cat1.getIdentifier(), "dublincore.xml"));
    MediaPackageElement cat2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Catalog,
            "security", "xaml", null);
    cat2.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.MEDIAPACKAGE_PATH_PREFIX,
            mp.getIdentifier().compact(), cat2.getIdentifier(), "security.xml"));

    cleanupWOH.start(createWorkflowInstance(wfInstConfig, mp), null);
    Assert.assertEquals("Media package should contain at least tree elements", 3, mp.getElements().length);
    SimpleElementSelector elementSelector = new SimpleElementSelector();
    elementSelector.addFlavor("*/source");
    Assert.assertFalse("Media package doesn't contain an element with a preserved flavor '*/source'",
            elementSelector.select(mp, false).isEmpty());
    elementSelector = new SimpleElementSelector();
    elementSelector.addFlavor("smil/trimmed");
    Assert.assertFalse("Media package doesn't contain an element with a preserved flavor 'smil/trimmed'",
            elementSelector.select(mp, false).isEmpty());
    elementSelector = new SimpleElementSelector();
    elementSelector.addFlavor("security/*");
    Assert.assertFalse("Media package doesn't contain an element with a preserved flavor 'security/*'",
            elementSelector.select(mp, false).isEmpty());

    Assert.assertEquals("At least one file wasn't deleted on remote repository", 3, deletedFilesURIs.size());
  }

  @Test
  public void testCreanupWOHwithPreservedFlavorAndCollectionPathPrefix() throws WorkflowOperationException,
          MediaPackageException {
    Map<String, String> wfInstConfig = new Hashtable<>();
    wfInstConfig.put(CleanupWorkflowOperationHandler.PRESERVE_FLAVOR_PROPERTY, "*/source,smil/trimmed,security/*");
    wfInstConfig.put(CleanupWorkflowOperationHandler.DELETE_EXTERNAL, "true");

    MediaPackageBuilder mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    MediaPackage mp = mpBuilder.createNew();
    MediaPackageElement track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
            "presenter", "source", null);
    track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
            "asset", mp.getIdentifier().compact(), track1.getIdentifier(), "track.mp4"));
    MediaPackageElement track2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
            "presentation", "work", null);
    track2.setURI(UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
            "compose", mp.getIdentifier().compact(), track2.getIdentifier(), "track.mp4"));
    MediaPackageElement att1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Attachment,
            "presentation", "preview", null);
    att1.setURI(UrlSupport.uri(HOSTNAME_NODE2, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
            "compose", mp.getIdentifier().compact(), att1.getIdentifier(), "preview.png"));
    MediaPackageElement att2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Attachment,
            "smil", "trimmed", null);
    att2.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
            "silence", mp.getIdentifier().compact(), att2.getIdentifier(), "trimmed.smil"));
    MediaPackageElement cat1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Catalog,
            "dublincore", "episode", null);
    cat1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
            "asset", mp.getIdentifier().compact(), cat1.getIdentifier(), "dublincore.xml"));
    MediaPackageElement cat2 = addElementToMediaPackage(mp, MediaPackageElement.Type.Catalog,
            "security", "xaml", null);
    cat2.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
            "security", mp.getIdentifier().compact(), cat2.getIdentifier(), "security.xml"));

    cleanupWOH.start(createWorkflowInstance(wfInstConfig, mp), null);
    Assert.assertEquals("Media package should contain at least tree elements", 3, mp.getElements().length);
    SimpleElementSelector elementSelector = new SimpleElementSelector();
    elementSelector.addFlavor("*/source");
    Assert.assertFalse("Media package doesn't contain an element with a preserved flavor '*/source'",
            elementSelector.select(mp, false).isEmpty());
    elementSelector = new SimpleElementSelector();
    elementSelector.addFlavor("smil/trimmed");
    Assert.assertFalse("Media package doesn't contain an element with a preserved flavor 'smil/trimmed'",
            elementSelector.select(mp, false).isEmpty());
    elementSelector = new SimpleElementSelector();
    elementSelector.addFlavor("security/*");
    Assert.assertFalse("Media package doesn't contain an element with a preserved flavor 'security/*'",
            elementSelector.select(mp, false).isEmpty());

    Assert.assertEquals("At least one file wasn't deleted on remote repository", 3, deletedFilesURIs.size());
  }

  @Test
  public void testCreanupWOHwithoutPreservedFlavor() throws WorkflowOperationException, MediaPackageException {
    Map<String, String> wfInstConfig = new Hashtable<>();
    wfInstConfig.put(CleanupWorkflowOperationHandler.DELETE_EXTERNAL, "true");

    MediaPackageBuilder mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    MediaPackage mp = mpBuilder.createNew();
    MediaPackageElement track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
            "presenter", "source", null);
    track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
            "asset", mp.getIdentifier().compact(), track1.getIdentifier(), "track.mp4"));

    cleanupWOH.start(createWorkflowInstance(wfInstConfig, mp), null);
    Assert.assertEquals("Media package shouldn't contain any elements", 0, mp.getElements().length);
    Assert.assertEquals("One file wasn't deleted on remote repository", 1, deletedFilesURIs.size());
  }

  @Test
  public void testCreanupWOHwithoutPreservedFlavorAndWithoutDeleteExternal() throws WorkflowOperationException,
          MediaPackageException {
    Map<String, String> wfInstConfig = new Hashtable<>();

    MediaPackageBuilder mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    MediaPackage mp = mpBuilder.createNew();
    MediaPackageElement track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
            "presenter", "source", null);
    track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, WFR_URL_PREFIX, WorkingFileRepository.COLLECTION_PATH_PREFIX,
            "asset", mp.getIdentifier().compact(), track1.getIdentifier(), "track.mp4"));

    cleanupWOH.start(createWorkflowInstance(wfInstConfig, mp), null);
    Assert.assertEquals("Media package shouldn't contain any elements", 0, mp.getElements().length);
    Assert.assertEquals("Delete on remote repository not allowed", 0, deletedFilesURIs.size());
  }

  @Test
  public void testCreanupWOHwithsomeUnknowenUrl() throws WorkflowOperationException,
          MediaPackageException {
    Map<String, String> wfInstConfig = new Hashtable<>();

    MediaPackageBuilder mpBuilder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    MediaPackage mp = mpBuilder.createNew();
    MediaPackageElement track1 = addElementToMediaPackage(mp, MediaPackageElement.Type.Track,
            "presenter", "source", null);
    track1.setURI(UrlSupport.uri(HOSTNAME_NODE1, "asset", "asset",
            mp.getIdentifier().compact(), track1.getIdentifier(), 0, "track.mp4"));

    cleanupWOH.start(createWorkflowInstance(wfInstConfig, mp), null);
    Assert.assertEquals("Media package shouldn't contain any elements", 0, mp.getElements().length);
    Assert.assertEquals("Delete on remote repository not allowed", 0, deletedFilesURIs.size());
  }

  /** This class should cache all URIs that are passed to mocked {@link TrustedHttpClient} execute method */
  private static final class StoreUrisArgumentMatcher implements IArgumentMatcher {

    /** URI's cache */
    private List<URI> uriStore = null;

    /** Base URI to test matches */
    private URI matchBaseUri = null;

    /** Constructor */
    private StoreUrisArgumentMatcher(List<URI> uriCache, URI matchBaseUri) {
      this.uriStore = uriCache;
      this.matchBaseUri = matchBaseUri;
    }

    @Override
    public boolean matches(Object arg) {
      if (!(arg instanceof HttpUriRequest))
        return false;
      HttpUriRequest req = (HttpUriRequest) arg;
      uriStore.add(req.getURI());
      boolean result = StringUtils.startsWith(req.getURI().toString(), matchBaseUri.toString());
      return result;
    }

    @Override
    public void appendTo(StringBuffer sb) {
    }

    /**
     * Create and initialize {@link StoreUrisArgumentMatcher}
     *
     * @param uriStore List, where to store URI's that are passed as argument to the mocked object
     * @param matchUri base URI to test on passed URI's
     * @return null
     */
    public static HttpUriRequest createMatcher(final List<URI> uriStore, final URI matchUri) {
      EasyMock.reportMatcher(new StoreUrisArgumentMatcher(uriStore, matchUri));
      return null;
    }
  }

}
