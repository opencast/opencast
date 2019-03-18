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
package org.opencastproject.migration;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.UrlSupport.uri;

import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.PropertyId;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Value;
import org.opencastproject.assetmanager.impl.AbstractAssetManager;
import org.opencastproject.assetmanager.impl.HttpAssetProvider;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.Organization;
import org.opencastproject.security.api.OrganizationDirectoryService;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.util.SecurityUtil;
import org.opencastproject.util.persistencefn.PersistenceUtil;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;
import com.google.gson.Gson;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

public class SchedulerMigrationServiceTest {

  @ClassRule
  public static final TemporaryFolder testFolder = new TemporaryFolder();

  private static File baseDir;
  private SchedulerMigrationService schedulerMigrationService = new SchedulerMigrationService();
  private AssetManager assetManager;
  private Organization currentOrg = new DefaultOrganization();
  private final EntityManagerFactory emf = mkMigrationEntityManagerFactory();
  private final Gson gson = new Gson();

  @BeforeClass
  public static void setupClass() throws Exception {
    baseDir = testFolder.newFolder();
  }

  @Before
  public void setUp() throws Exception {
    OrganizationDirectoryService orgDirService = createNiceMock(OrganizationDirectoryService.class);
    expect(orgDirService.getOrganization(anyString())).andReturn(currentOrg).anyTimes();
    expect(orgDirService.getOrganizations()).andReturn(Collections.singletonList(new DefaultOrganization())).anyTimes();
    replay(orgDirService);

    schedulerMigrationService.setOrgDirectoryService(orgDirService);

    final SecurityService securityService = createNiceMock(SecurityService.class);
    expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    expect(securityService.getUser()).andReturn(new JaxbUser()).anyTimes();
    replay(securityService);

    schedulerMigrationService.setSecurityService(securityService);
    schedulerMigrationService.setEntityManagerFactory(emf);

    assetManager = mkAssetManager();
    schedulerMigrationService.setAssetManager(assetManager);


    // fill assetmanager with testdata
    assetManager.takeSnapshot(SchedulerMigrationService.SNAPSHOT_OWNER, generateEvent(Opt.some("mp1")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.RECORDING_LAST_HEARD_CONFIG), Value.mk(new Long(100))));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.RECORDING_STATE_CONFIG), Value.mk("state of mp1")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.SOURCE_CONFIG), Value.mk("source of mp1")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.PRESENTERS_CONFIG), Value.mk("presenter of mp1")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.AGENT_CONFIG), Value.mk("agent of mp1")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.START_DATE_CONFIG), Value.mk(new Date(102))));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.END_DATE_CONFIG), Value.mk(new Date(103))));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.LAST_MODIFIED_DATE), Value.mk(new Date(104))));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.CHECKSUM), Value.mk("checksum of mp1")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.WORKFLOW_NAMESPACE, "workflow testproperty"), Value.mk("wf prop 1")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp1", SchedulerMigrationService.CA_NAMESPACE, "agent testproperty"), Value.mk("ca prop 1")));

    assetManager.takeSnapshot(SchedulerMigrationService.SNAPSHOT_OWNER, generateEvent(Opt.some("mp2")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.RECORDING_LAST_HEARD_CONFIG), Value.mk(new Long(200))));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.RECORDING_STATE_CONFIG), Value.mk("state of mp2")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.SOURCE_CONFIG), Value.mk("source of mp2")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.PRESENTERS_CONFIG), Value.mk("presenter of mp2")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.AGENT_CONFIG), Value.mk("agent of mp2")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.START_DATE_CONFIG), Value.mk(new Date(202))));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.END_DATE_CONFIG), Value.mk(new Date(203))));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.LAST_MODIFIED_DATE), Value.mk(new Date(204))));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.SCHEDULER_NAMESPACE, SchedulerMigrationService.CHECKSUM), Value.mk("checksum of mp2")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.WORKFLOW_NAMESPACE, "workflow testproperty"), Value.mk("wf prop 2")));
    assetManager.setProperty(Property.mk(PropertyId.mk("mp2", SchedulerMigrationService.CA_NAMESPACE, "agent testproperty"), Value.mk("ca prop 2")));
  }

  @Test
  public void testSchedulerMigration() throws Exception {
    final BundleContext bundleContext = createNiceMock(BundleContext.class);
    final Bundle bundle = createNiceMock(Bundle.class);
    final Version version = new Version(7, 0, 0);
    final ConfigurationAdmin configurationAdmin = createNiceMock(ConfigurationAdmin.class);
    final Configuration configuration = createNiceMock(Configuration.class);
    final Hashtable<String, Object> properties = new Hashtable<>();
    properties.put("maintenance", true);
    expect(bundleContext.getProperty(SecurityUtil.PROPERTY_KEY_SYS_USER)).andReturn("root").anyTimes();
    expect(bundleContext.getBundle()).andReturn(bundle).anyTimes();
    expect(bundleContext.getService(anyObject())).andReturn(configurationAdmin).anyTimes();
    expect(bundle.getVersion()).andReturn(version).anyTimes();
    expect(configurationAdmin.getConfiguration("org.opencastproject.scheduler.impl.SchedulerServiceImpl")).andReturn(configuration).anyTimes();
    expect(configuration.getProperties()).andReturn(properties).anyTimes();

    final ComponentContext cc = EasyMock.createNiceMock(ComponentContext.class);
    EasyMock.expect(cc.getBundleContext()).andReturn(bundleContext).anyTimes();

    EasyMock.replay(cc, bundleContext, bundle, configurationAdmin, configuration);

    schedulerMigrationService.activate(cc);

    EntityManager em = emf.createEntityManager();
    Opt<ExtendedEventDto> entityOpt;
    ExtendedEventDto entity;
    Map map;

    entityOpt = Opt.nul(em.find(ExtendedEventDto.class, new EventIdPK("mp1", currentOrg.toString())));
    assertTrue(entityOpt.isSome());
    entity = entityOpt.get();
    assertEquals(new Long(100), entity.getRecordingLastHeard());
    assertEquals("state of mp1", entity.getRecordingState());
    assertEquals("source of mp1", entity.getSource());
    assertEquals("presenter of mp1", entity.getPresenters());
    assertEquals("agent of mp1", entity.getCaptureAgentId());
    assertEquals(new Date(102), entity.getStartDate());
    assertEquals(new Date(103), entity.getEndDate());
    assertEquals(new Date(104), entity.getLastModifiedDate());
    assertEquals("checksum of mp1", entity.getChecksum());
    map = gson.fromJson(entity.getCaptureAgentProperties(), HashMap.class);
    assertEquals("ca prop 1", map.get("agent testproperty"));
    map = gson.fromJson(entity.getWorkflowProperties(), HashMap.class);
    assertEquals("wf prop 1", map.get("workflow testproperty"));

    entityOpt = Opt.nul(em.find(ExtendedEventDto.class, new EventIdPK("mp2", currentOrg.toString())));
    assertTrue(entityOpt.isSome());
    entity = entityOpt.get();
    assertEquals(new Long(200), entity.getRecordingLastHeard());
    assertEquals("state of mp2", entity.getRecordingState());
    assertEquals("source of mp2", entity.getSource());
    assertEquals("presenter of mp2", entity.getPresenters());
    assertEquals("agent of mp2", entity.getCaptureAgentId());
    assertEquals(new Date(202), entity.getStartDate());
    assertEquals(new Date(203), entity.getEndDate());
    assertEquals(new Date(204), entity.getLastModifiedDate());
    assertEquals("checksum of mp2", entity.getChecksum());
    map = gson.fromJson(entity.getCaptureAgentProperties(), HashMap.class);
    assertEquals("ca prop 2", map.get("agent testproperty"));
    map = gson.fromJson(entity.getWorkflowProperties(), HashMap.class);
    assertEquals("wf prop 2", map.get("workflow testproperty"));
  }

  private AssetManager mkAssetManager() {
    final Database db = new Database(mkAssetManagerEntityManagerFactory());
    final AssetStore assetStore = mkAssetStore();
    return new AbstractAssetManager() {

      @Override
      public HttpAssetProvider getHttpAssetProvider() {
        // identity provider
        return new HttpAssetProvider() {
          @Override
          public Snapshot prepareForDelivery(Snapshot snapshot) {
            return AbstractAssetManager.rewriteUris(snapshot, new Fn<MediaPackageElement, URI>() {
              @Override
              public URI apply(MediaPackageElement mpe) {
                String baseName = getFileNameFromUrn(mpe).getOr(mpe.getElementType().toString());

                // the returned uri must match the path of the {@link #getAsset} method
                return uri(baseDir.toURI(),
                    mpe.getMediaPackage().getIdentifier().toString(),
                    mpe.getIdentifier(),
                    baseName);
              }
            });
          }
        };
      }

      @Override
      public Database getDb() {
        return db;
      }

      @Override
      protected Workspace getWorkspace() {
        return EasyMock.niceMock(Workspace.class);
      }

      @Override
      public AssetStore getLocalAssetStore() {
        return assetStore;
      }

      @Override
      protected String getCurrentOrgId() {
        return currentOrg.getId();
      }
    };
  }

  private static AssetStore mkAssetStore() {
    final AssetStore result = EasyMock.niceMock(AssetStore.class);
    EasyMock.expect(result.getStoreType()).andReturn("test_store").anyTimes();
    EasyMock.replay(result);
    return result;
  }

  private static MediaPackage generateEvent(Opt<String> id) throws MediaPackageException {
    MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    if (id.isSome())
      mp.setIdentifier(new IdImpl(id.get()));
    return mp;
  }

  private static EntityManagerFactory mkAssetManagerEntityManagerFactory() {
    return PersistenceUtil.mkTestEntityManagerFactory("org.opencastproject.assetmanager.impl", true);
  }

  private static EntityManagerFactory mkMigrationEntityManagerFactory() {
    return PersistenceUtil.mkTestEntityManagerFactory("org.opencastproject.migration", true);
  }
}
