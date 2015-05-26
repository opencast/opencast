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

package org.opencastproject.comments;

import org.opencastproject.comments.persistence.CommentDatabaseImpl;
import org.opencastproject.security.api.DefaultOrganization;
import org.opencastproject.security.api.JaxbUser;
import org.opencastproject.security.api.UserDirectoryService;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.PathSupport;
import org.opencastproject.util.data.Option;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Test for Comment Service.
 */
public class CommentServiceImplTest {

  private ComboPooledDataSource pooledDataSource;
  private CommentDatabaseImpl commentDatabase;
  private String storage;

  private CommentServiceImpl commentService;
  private final JaxbUser testUser = new JaxbUser("user", "matterhorn", new DefaultOrganization());

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    long currentTime = System.currentTimeMillis();
    storage = PathSupport.concat("target", "db" + currentTime + ".h2.db");

    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + currentTime);
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Collect the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");

    // Mock up a security service
    UserDirectoryService userDirectoryService = EasyMock.createNiceMock(UserDirectoryService.class);
    EasyMock.expect(userDirectoryService.loadUser(EasyMock.anyObject(String.class))).andReturn(testUser).anyTimes();
    EasyMock.replay(userDirectoryService);

    commentDatabase = new CommentDatabaseImpl();
    commentDatabase.setPersistenceProperties(props);
    commentDatabase.setPersistenceProvider(new PersistenceProvider());
    commentDatabase.activate(null);

    commentService = new CommentServiceImpl();
    commentService.setPersistence(commentDatabase);
    commentService.setUserDirectoryService(userDirectoryService);
    commentService.activate(null);
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    commentDatabase.deactivate(null);
    DataSources.destroy(pooledDataSource);
    FileUtils.deleteQuietly(new File(storage));
    commentDatabase = null;
  }

  @Test
  public void testCRUDComment() {
    // Test create
    Comment updateComment = null;
    Comment comment = Comment.create(Option.<Long> none(), "Test comment", testUser, "Test", false);
    CommentReply reply = CommentReply.create(Option.<Long> none(), "Test reply", testUser);
    comment.addReply(reply);

    try {
      updateComment = commentService.updateComment(comment);
      Assert.assertNotNull(updateComment);
      Assert.assertEquals(comment.getText(), updateComment.getText());
      Assert.assertEquals(comment.getReason(), updateComment.getReason());
      Assert.assertEquals(comment.isResolvedStatus(), updateComment.isResolvedStatus());
      Assert.assertEquals(comment.getCreationDate(), updateComment.getCreationDate());
      Assert.assertEquals(comment.getModificationDate(), updateComment.getModificationDate());
      Assert.assertEquals(comment.getAuthor(), updateComment.getAuthor());
      Assert.assertEquals(comment.getReplies(), updateComment.getReplies());
    } catch (CommentException e) {
      Assert.fail(e.getMessage());
    }

    // Test read
    try {
      Comment c = commentService.getComment(updateComment.getId().get());
      Assert.assertNotNull(updateComment);
      Assert.assertEquals(updateComment.getText(), c.getText());
      Assert.assertEquals(updateComment.getReason(), c.getReason());
      Assert.assertEquals(updateComment.isResolvedStatus(), c.isResolvedStatus());
      Assert.assertEquals(updateComment.getCreationDate(), c.getCreationDate());
      Assert.assertEquals(updateComment.getModificationDate(), c.getModificationDate());
      Assert.assertEquals(updateComment.getAuthor(), c.getAuthor());
      Assert.assertEquals(updateComment.getReplies(), c.getReplies());
      Assert.assertNotNull(c);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    // Test update
    try {
      Comment newComment = Comment.create(updateComment.getId(), "NewComment", updateComment.getAuthor(), "NewReason",
              true, updateComment.getCreationDate(), updateComment.getModificationDate(), updateComment.getReplies());
      newComment.removeReply(reply);

      updateComment = commentService.updateComment(newComment);
      Assert.assertNotNull(updateComment);
      Assert.assertEquals(newComment.getText(), updateComment.getText());
      Assert.assertEquals(newComment.getReason(), updateComment.getReason());
      Assert.assertEquals(newComment.isResolvedStatus(), updateComment.isResolvedStatus());
      Assert.assertEquals(newComment.getCreationDate(), updateComment.getCreationDate());
      Assert.assertEquals(newComment.getModificationDate(), updateComment.getModificationDate());
      Assert.assertEquals(newComment.getAuthor(), updateComment.getAuthor());
      Assert.assertEquals(newComment.getReplies(), updateComment.getReplies());
    } catch (CommentException e) {
      Assert.fail(e.getMessage());
    }

    // Test read
    try {
      Comment c = commentService.getComment(updateComment.getId().get());
      Assert.assertNotNull(updateComment);
      Assert.assertEquals(updateComment.getText(), c.getText());
      Assert.assertEquals(updateComment.getReason(), c.getReason());
      Assert.assertEquals(updateComment.isResolvedStatus(), c.isResolvedStatus());
      Assert.assertEquals(updateComment.getCreationDate(), c.getCreationDate());
      Assert.assertEquals(updateComment.getModificationDate(), c.getModificationDate());
      Assert.assertEquals(updateComment.getAuthor(), c.getAuthor());
      Assert.assertEquals(updateComment.getReplies(), c.getReplies());
      Assert.assertNotNull(c);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    // Test delete
    try {
      commentService.deleteComment(updateComment.getId().get());
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    // Test read
    try {
      commentService.getComment(updateComment.getId().get());
      Assert.fail("Could still read deleted comment!");
    } catch (NotFoundException e) {
      Assert.assertNotNull(e);
    } catch (CommentException e) {
      Assert.fail(e.getMessage());
    }
  }
}
