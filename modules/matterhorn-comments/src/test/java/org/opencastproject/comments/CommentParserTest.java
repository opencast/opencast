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
package org.opencastproject.comments;

import static org.junit.Assert.assertThat;
import static org.opencastproject.util.IoSupport.loadFileFromClassPathAsString;
import static org.xmlmatchers.XmlMatchers.similarTo;
import static org.xmlmatchers.transform.XmlConverters.the;

import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.DateTimeSupport;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class CommentParserTest {

  private UserDirectoryService userDirectoryService;
  private JaxbUser testUser;

  @Before
  public void setUp() throws Exception {
    userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    testUser = new JaxbUser("test", "test", new DefaultOrganization());
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject(String.class))).andReturn(testUser).anyTimes();
    EasyMock.replay(userDirectoryService);
  }

  @Test
  public void testParseAndSerializeComment() throws Exception {
    String serializedCommentXml = IOUtils.toString(getClass().getResource("/comment.xml").toURI());

    Comment comment = CommentParser.getCommentFromXml(serializedCommentXml, userDirectoryService);
    Assert.assertNotNull(comment);
    Assert.assertEquals(2L, comment.getId().get().longValue());
    Assert.assertEquals("Test comment", comment.getText());
    Assert.assertEquals("Test", comment.getReason());
    Assert.assertTrue(comment.isResolvedStatus());
    Assert.assertEquals(DateTimeSupport.fromUTC("2014-05-21T10:09:45Z"), comment.getCreationDate().getTime());
    Assert.assertEquals(DateTimeSupport.fromUTC("2014-05-21T12:09:45Z"), comment.getModificationDate().getTime());
    Assert.assertEquals(testUser, comment.getAuthor());
    Assert.assertEquals(2, comment.getReplies().size());

    // Reply 1
    CommentReply reply = comment.getReplies().get(0);
    Assert.assertEquals(1L, reply.getId().get().longValue());
    Assert.assertEquals("Test reply", reply.getText());
    Assert.assertEquals(DateTimeSupport.fromUTC("2014-06-21T10:09:45Z"), reply.getCreationDate().getTime());
    Assert.assertEquals(DateTimeSupport.fromUTC("2014-07-21T12:09:45Z"), reply.getModificationDate().getTime());
    Assert.assertEquals(testUser, reply.getAuthor());

    // Reply 2
    reply = comment.getReplies().get(1);
    Assert.assertTrue(reply.getId().isNone());
    Assert.assertEquals("Test reply 2", reply.getText());
    Assert.assertEquals(DateTimeSupport.fromUTC("2014-08-21T10:09:45Z"), reply.getCreationDate().getTime());
    Assert.assertEquals(DateTimeSupport.fromUTC("2014-09-21T12:09:45Z"), reply.getModificationDate().getTime());
    Assert.assertEquals(testUser, reply.getAuthor());

    try {
      String serializedComment = CommentParser.getAsXml(comment);
      assertThat(the(serializedComment), similarTo(the(loadFileFromClassPathAsString("/comment.xml").get())));
    } catch (CommentException e) {
      Assert.fail(e.getMessage());
    }

    try {
      String serializedReply = CommentParser.getAsXml(comment.getReplies().get(0));
      assertThat(the(serializedReply), similarTo(the(loadFileFromClassPathAsString("/comment-reply.xml").get())));
    } catch (CommentException e) {
      Assert.fail(e.getMessage());
    }
  }

}
