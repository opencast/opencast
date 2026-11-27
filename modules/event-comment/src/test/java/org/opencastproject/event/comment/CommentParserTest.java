/*
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

package org.opencastproject.event.comment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.opencastproject.util.IoSupport.loadFileFromClassPathAsString;
import static org.xmlmatchers.XmlMatchers.similarTo;
import static org.xmlmatchers.transform.XmlConverters.the;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

public class CommentParserTest {

  private UserDirectoryService userDirectoryService;
  private JaxbUser testUser;

  @BeforeEach
  public void setUp() throws Exception {
    userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    testUser = new JaxbUser("test", "test", new DefaultOrganization());
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject(String.class))).andReturn(testUser).anyTimes();
    EasyMock.replay(userDirectoryService);
  }

  @Test
  public void testParseAndSerializeComment() throws Exception {
    String serializedCommentXml = IOUtils.toString(getClass().getResource("/comment.xml").toURI(),
        StandardCharsets.UTF_8);

    EventComment comment = EventCommentParser.getCommentFromXml(serializedCommentXml, userDirectoryService);
    Assertions.assertNotNull(comment);
    Assertions.assertEquals(2L, comment.getId().get().longValue());
    Assertions.assertEquals("Test comment", comment.getText());
    Assertions.assertEquals("Test", comment.getReason());
    Assertions.assertTrue(comment.isResolvedStatus());
    Assertions.assertEquals(DateTimeSupport.fromUTC("2014-05-21T10:09:45Z"), comment.getCreationDate().getTime());
    Assertions.assertEquals(DateTimeSupport.fromUTC("2014-05-21T12:09:45Z"), comment.getModificationDate().getTime());
    Assertions.assertEquals(testUser, comment.getAuthor());
    Assertions.assertEquals(2, comment.getReplies().size());

    // Reply 1
    EventCommentReply reply = comment.getReplies().get(0);
    Assertions.assertEquals(1L, reply.getId().get().longValue());
    Assertions.assertEquals("Test reply", reply.getText());
    Assertions.assertEquals(DateTimeSupport.fromUTC("2014-06-21T10:09:45Z"), reply.getCreationDate().getTime());
    Assertions.assertEquals(DateTimeSupport.fromUTC("2014-07-21T12:09:45Z"), reply.getModificationDate().getTime());
    Assertions.assertEquals(testUser, reply.getAuthor());

    // Reply 2
    reply = comment.getReplies().get(1);
    Assertions.assertTrue(reply.getId().isEmpty());
    Assertions.assertEquals("Test reply 2", reply.getText());
    Assertions.assertEquals(DateTimeSupport.fromUTC("2014-08-21T10:09:45Z"), reply.getCreationDate().getTime());
    Assertions.assertEquals(DateTimeSupport.fromUTC("2014-09-21T12:09:45Z"), reply.getModificationDate().getTime());
    Assertions.assertEquals(testUser, reply.getAuthor());

    try {
      String serializedComment = EventCommentParser.getAsXml(comment);
      assertThat(the(serializedComment), similarTo(the(loadFileFromClassPathAsString("/comment.xml").get())));
    } catch (EventCommentException e) {
      Assertions.fail(e.getMessage());
    }

    try {
      String serializedReply = EventCommentParser.getAsXml(comment.getReplies().get(0));
      assertThat(the(serializedReply), similarTo(the(loadFileFromClassPathAsString("/comment-reply.xml").get())));
    } catch (EventCommentException e) {
      Assertions.fail(e.getMessage());
    }
  }

}
