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

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.opencastproject.util.data.Tuple.tuple;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.impl.AbstractAssetManager;
import org.opencastproject.assetmanager.impl.HttpAssetProvider;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.AssetStoreException;
import org.opencastproject.assetmanager.impl.storage.DeletionSelector;
import org.opencastproject.assetmanager.impl.storage.Source;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.attachment.AttachmentImpl;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.UrlSupport;
import org.opencastproject.util.data.Option;
import org.opencastproject.util.persistencefn.PersistenceEnv;
import org.opencastproject.util.persistencefn.PersistenceEnvs;
import org.opencastproject.util.persistencefn.PersistenceUtil;
import org.opencastproject.workflow.api.WorkflowDatabaseException;
import org.opencastproject.workflow.api.WorkflowDefinition;
import org.opencastproject.workflow.api.WorkflowDefinitionImpl;
import org.opencastproject.workflow.api.WorkflowInstanceImpl;
import org.opencastproject.workflow.api.WorkflowService;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.junit.Ignore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.persistence.EntityManagerFactory;
import javax.ws.rs.Path;

@Path("/")
@Ignore
public class TestTasksEndpoint extends TasksEndpoint {

  private final File baseDir = new File(new File(IoSupport.getSystemTmpDir()), "tasksendpointtest");

  public TestTasksEndpoint() throws Exception {
    WorkflowDefinition wfD = new WorkflowDefinitionImpl();
    wfD.setTitle("Full");
    wfD.setId("full");
    wfD.addTag("archive");

    WorkflowDefinitionImpl wfD2 = new WorkflowDefinitionImpl();
    wfD2.setTitle("Full HTML5");
    wfD2.setId("full-html5");
    wfD2.setDescription("Test description");
    wfD2.setConfigurationPanel("<h2>Test</h2>");
    wfD2.addTag("archive");

    WorkflowDefinitionImpl wfD3 = new WorkflowDefinitionImpl();
    wfD3.setTitle("Hidden");
    wfD3.setId("hidden");

    WorkflowInstanceImpl wI1 = new WorkflowInstanceImpl();
    wI1.setTitle(wfD.getTitle());
    wI1.setTemplate(wfD.getId());
    wI1.setId(5);
    WorkflowInstanceImpl wI2 = new WorkflowInstanceImpl();
    wI2.setTitle(wfD2.getTitle());
    wI2.setTemplate(wfD2.getId());
    wI2.setId(10);

    Workspace workspace = createNiceMock(Workspace.class);
    expect(workspace.get(anyObject(URI.class)))
            .andReturn(new File(getClass().getResource("/processing-properties.xml").toURI())).anyTimes();
    expect(workspace.get(anyObject(URI.class), EasyMock.anyBoolean())).andAnswer(() -> {
        File tmp = new File(baseDir, UUID.randomUUID().toString() + "-processing-properties.xml");
        FileUtils.copyFile(new File(getClass().getResource("/processing-properties.xml").toURI()), tmp);
        return tmp;
      }).anyTimes();

    WorkflowService workflowService = createNiceMock(WorkflowService.class);
    expect(workflowService.listAvailableWorkflowDefinitions()).andReturn(Arrays.asList(wfD, wfD2, wfD3)).anyTimes();
    expect(workflowService.getWorkflowDefinitionById("full")).andReturn(wfD).anyTimes();
    expect(workflowService.getWorkflowDefinitionById("exception")).andThrow(new WorkflowDatabaseException()).anyTimes();
    expect(workflowService.start(anyObject(WorkflowDefinition.class), anyObject(MediaPackage.class),
            anyObject(Map.class))).andReturn(wI1);
    expect(workflowService.start(anyObject(WorkflowDefinition.class), anyObject(MediaPackage.class),
            anyObject(Map.class))).andReturn(wI2);
    replay(workspace, workflowService);

    AssetManager assetManager = mkAssetManager(workspace);
    MediaPackage mp1 = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(new IdImpl("id1"));
    AttachmentImpl attachment = new AttachmentImpl();
    attachment.setFlavor(MediaPackageElements.PROCESSING_PROPERTIES);
    mp1.add(attachment);

    MediaPackage mp2 = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(new IdImpl("id2"));
    assetManager.takeSnapshot(AssetManager.DEFAULT_OWNER, mp1);
    assetManager.takeSnapshot(AssetManager.DEFAULT_OWNER, mp2);

    this.setWorkflowService(workflowService);
    this.setAssetManager(assetManager);
    this.setWorkspace(workspace);
    this.activate(null);
  }

  AssetManager mkAssetManager(final Workspace workspace) throws Exception {
    final PersistenceEnv penv = PersistenceEnvs.mk(mkEntityManagerFactory("org.opencastproject.assetmanager.impl"));
    final Database db = new Database(penv);
    return new AbstractAssetManager() {
      @Override
      public HttpAssetProvider getHttpAssetProvider() {
        // identity provider
        return new HttpAssetProvider() {
          @Override
          public Snapshot prepareForDelivery(Snapshot snapshot) {
            return snapshot;
          }
        };
      }

      @Override
      public Database getDb() {
        return db;
      }

      @Override
      protected Workspace getWorkspace() {
        return workspace;
      }

      @Override
      public AssetStore getLocalAssetStore() {
        return mkAssetStore(workspace);
      }

      @Override
      protected String getCurrentOrgId() {
        return DefaultOrganization.DEFAULT_ORGANIZATION_ID;
      }
    };
  }

  AssetStore mkAssetStore(final Workspace workspace) {
    return new AssetStore() {

      @Override
      public Option<Long> getUsedSpace() {
        return Option.none();
      }

      @Override
      public Option<Long> getUsableSpace() {
        return Option.none();
      }

      @Override
      public Option<Long> getTotalSpace() {
        return Option.none();
      }

      @Override
      public void put(StoragePath path, Source source) throws AssetStoreException {
        File destFile = new File(baseDir, UrlSupport.concat(path.getMediaPackageId(), path.getMediaPackageElementId(),
                path.getVersion().toString()));
        try {
          FileUtils.copyFile(workspace.get(source.getUri()), destFile);
        } catch (IOException e) {
          throw new RuntimeException(e);
        } catch (NotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public Opt<InputStream> get(StoragePath path) throws AssetStoreException {
        File file = new File(baseDir, UrlSupport.concat(path.getMediaPackageId(), path.getMediaPackageElementId(),
                path.getVersion().toString()));
        InputStream inputStream;
        try {
          inputStream = new ByteArrayInputStream(FileUtils.readFileToByteArray(file));
          return Opt.some(inputStream);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public boolean delete(DeletionSelector sel) throws AssetStoreException {
        return false;
      }

      @Override
      public boolean copy(StoragePath from, StoragePath to) throws AssetStoreException {
        File file = new File(baseDir, UrlSupport.concat(from.getMediaPackageId(), from.getMediaPackageElementId(),
                from.getVersion().toString()));
        File destFile = new File(baseDir,
                UrlSupport.concat(to.getMediaPackageId(), to.getMediaPackageElementId(), to.getVersion().toString()));
        try {
          FileUtils.copyFile(file, destFile);
          return true;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public boolean contains(StoragePath path) throws AssetStoreException {
        return false;
      }

      @Override
      public String getStoreType() {
        return "test_store";
      }
    };
  }

  static EntityManagerFactory mkEntityManagerFactory(String persistenceUnit) {
    if ("mysql".equals(System.getProperty("useDatabase"))) {
      return mkMySqlEntityManagerFactory(persistenceUnit);
    } else {
      return mkH2EntityManagerFactory(persistenceUnit);
    }
  }

  static EntityManagerFactory mkH2EntityManagerFactory(String persistenceUnit) {
    return PersistenceUtil.mkTestEntityManagerFactory(persistenceUnit, true);
  }

  static EntityManagerFactory mkMySqlEntityManagerFactory(String persistenceUnit) {
    return PersistenceUtil.mkEntityManagerFactory(persistenceUnit, "MySQL", "com.mysql.jdbc.Driver",
            "jdbc:mysql://localhost/test_scheduler", "matterhorn", "matterhorn",
            org.opencastproject.util.data.Collections.map(tuple("eclipselink.ddl-generation", "drop-and-create-tables"),
                    tuple("eclipselink.ddl-generation.output-mode", "database"),
                    tuple("eclipselink.logging.level.sql", "FINE"), tuple("eclipselink.logging.parameters", "true")),
            PersistenceUtil.mkTestPersistenceProvider());
  }

}
