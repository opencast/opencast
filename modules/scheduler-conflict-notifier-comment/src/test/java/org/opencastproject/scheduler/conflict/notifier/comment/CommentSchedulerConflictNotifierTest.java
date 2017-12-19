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
package org.opencastproject.scheduler.conflict.notifier.comment;

import org.opencastproject.event.comment.EventComment;
import org.opencastproject.event.comment.EventCommentService;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.metadata.dublincore.EventCatalogUIAdapter;
import org.opencastproject.scheduler.api.ConflictResolution.Strategy;
import org.opencastproject.scheduler.api.ConflictingEvent;
import org.opencastproject.scheduler.api.SchedulerEvent;
import org.opencastproject.scheduler.api.TechnicalMetadata;
import org.opencastproject.scheduler.api.TechnicalMetadataImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbRole;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.util.IoSupport;
import org.opencastproject.workspace.api.Workspace;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommentSchedulerConflictNotifierTest {

  private CommentSchedulerConflictNotifier conflictNotifier;

  @Before
  public void setUp() throws Exception {
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(new JaxbUser("admin", "provider", new DefaultOrganization(),
            new JaxbRole("admin", new DefaultOrganization(), "test"))).anyTimes();
    EasyMock.expect(securityService.getOrganization()).andReturn(new DefaultOrganization()).anyTimes();
    EasyMock.replay(securityService);

    Workspace workspace = EasyMock.createNiceMock(Workspace.class);
    EasyMock.expect(workspace.get(EasyMock.anyObject(URI.class)))
            .andReturn(IoSupport.classPathResourceAsFile("/dublincore.xml").get()).anyTimes();
    EasyMock.replay(workspace);

    EventCatalogUIAdapter episodeAdapter = EasyMock.createMock(EventCatalogUIAdapter.class);
    EasyMock.expect(episodeAdapter.getFlavor()).andReturn(new MediaPackageElementFlavor("dublincore", "episode"))
            .anyTimes();
    EasyMock.expect(episodeAdapter.getOrganization()).andReturn(new DefaultOrganization().getId()).anyTimes();
    EasyMock.replay(episodeAdapter);

    EventCatalogUIAdapter extendedAdapter = EasyMock.createMock(EventCatalogUIAdapter.class);
    EasyMock.expect(extendedAdapter.getFlavor()).andReturn(new MediaPackageElementFlavor("extended", "episode"))
            .anyTimes();
    EasyMock.expect(extendedAdapter.getOrganization()).andReturn(new DefaultOrganization().getId()).anyTimes();
    EasyMock.replay(extendedAdapter);

    conflictNotifier = new CommentSchedulerConflictNotifier();
    conflictNotifier.setSecurityService(securityService);
    conflictNotifier.setWorkspace(workspace);
    conflictNotifier.addCatalogUIAdapter(episodeAdapter);
    conflictNotifier.addCatalogUIAdapter(extendedAdapter);
  }

  @Test
  public void testCommentSchedulerConflict() throws Exception {
    Set<String> userIds = new HashSet<>();
    userIds.add("user1");
    userIds.add("user2");

    Map<String, String> caProperties = new HashMap<String, String>();
    caProperties.put("test", "true");
    caProperties.put("clear", "all");

    Map<String, String> wfProperties = new HashMap<String, String>();
    wfProperties.put("test", "false");
    wfProperties.put("skip", "true");

    final String mpId = "1234";
    final TechnicalMetadata technicalMetadata = new TechnicalMetadataImpl(mpId, "demo", new Date(),
            new Date(new Date().getTime() + 10 * 60 * 1000), false, userIds, wfProperties, caProperties, null);
    final MediaPackage mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew();
    mp.setIdentifier(new IdImpl(mpId));
    mp.add(DublinCores.mkOpencastEpisode().getCatalog());
    DublinCoreCatalog extendedEvent = DublinCores.mkStandard();
    extendedEvent.setFlavor(new MediaPackageElementFlavor("extended", "episode"));
    mp.add(extendedEvent);

    final SchedulerEvent schedulerEvent = EasyMock.createNiceMock(SchedulerEvent.class);
    EasyMock.expect(schedulerEvent.getTechnicalMetadata()).andReturn(technicalMetadata).anyTimes();
    EasyMock.expect(schedulerEvent.getMediaPackage()).andReturn(mp).anyTimes();
    EasyMock.expect(schedulerEvent.getEventId()).andReturn(mpId).anyTimes();
    EasyMock.expect(schedulerEvent.getVersion()).andReturn("2").anyTimes();
    EasyMock.replay(schedulerEvent);

    ConflictingEvent conflictingEvent = EasyMock.createNiceMock(ConflictingEvent.class);
    EasyMock.expect(conflictingEvent.getOldEvent()).andReturn(schedulerEvent).anyTimes();
    EasyMock.expect(conflictingEvent.getNewEvent()).andReturn(schedulerEvent).anyTimes();
    EasyMock.expect(conflictingEvent.getConflictStrategy()).andReturn(Strategy.NEW).anyTimes();
    EasyMock.replay(conflictingEvent);

    List<ConflictingEvent> conflicts = new ArrayList<>();
    conflicts.add(conflictingEvent);

    EventCommentService eventCommentService = EasyMock.createNiceMock(EventCommentService.class);
    EasyMock.expect(eventCommentService.updateComment(EasyMock.anyObject(EventComment.class))).andReturn(null).once();
    EasyMock.replay(eventCommentService);

    conflictNotifier.setEventCommentService(eventCommentService);
    conflictNotifier.notifyConflicts(conflicts);

    EasyMock.verify(eventCommentService);
  }

}
