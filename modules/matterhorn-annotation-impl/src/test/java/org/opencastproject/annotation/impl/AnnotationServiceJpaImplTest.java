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
package org.opencastproject.annotation.impl;

import static org.opencastproject.security.api.SecurityConstants.DEFAULT_ORGANIZATION_ANONYMOUS;

import org.opencastproject.annotation.api.Annotation;
import org.opencastproject.annotation.api.AnnotationList;
import org.opencastproject.security.api.SecurityService;
import org.opencastproject.security.api.User;
import org.opencastproject.util.NotFoundException;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import org.easymock.EasyMock;
import org.eclipse.persistence.jpa.PersistenceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests the JPA implementation of the annotation service
 */
public class AnnotationServiceJpaImplTest {
  private ComboPooledDataSource pooledDataSource = null;
  private AnnotationServiceJpaImpl annotationService = null;

  @Before
  public void setUp() throws Exception {
    // Set up the database
    pooledDataSource = new ComboPooledDataSource();
    pooledDataSource.setDriverClass("org.h2.Driver");
    pooledDataSource.setJdbcUrl("jdbc:h2:./target/db" + System.currentTimeMillis());
    pooledDataSource.setUser("sa");
    pooledDataSource.setPassword("sa");

    // Set up the persistence properties
    Map<String, Object> props = new HashMap<String, Object>();
    props.put("javax.persistence.nonJtaDataSource", pooledDataSource);
    props.put("eclipselink.ddl-generation", "create-tables");
    props.put("eclipselink.ddl-generation.output-mode", "database");
    
    // Set up a mock security service that always returns "me" as the current user
    User me = new User("me", "opencast.org", new String[] { DEFAULT_ORGANIZATION_ANONYMOUS });
    SecurityService securityService = EasyMock.createNiceMock(SecurityService.class);
    EasyMock.expect(securityService.getUser()).andReturn(me).anyTimes();
    EasyMock.replay(securityService);

    // Set up the annotation service
    annotationService = new AnnotationServiceJpaImpl();
    annotationService.setPersistenceProvider(new PersistenceProvider());
    annotationService.setPersistenceProperties(props);
    annotationService.setSecurityService(securityService);
    annotationService.activate(null);
  }
  
  @After
  public void tearDown() throws Exception {
    annotationService.deactivate();
  }
  
  @Test
  public void testAnnotationPersistence() throws Exception {
    // Add an annotation
    AnnotationImpl a = new AnnotationImpl();
    a.setInpoint(10);
    a.setOutpoint(100);
    a.setMediapackageId("mp123");
    a.setSessionId("session123");
    a.setType("ugc");
    a.setValue("This is some user generated content");
    annotationService.addAnnotation(a);
    
    // Ensure that by persisting the annotation, we now have an ID
    Assert.assertNotNull(a.getAnnotationId());
    
    // Ensure that the annotation was saved and retrieved properly
    Annotation a1FromDb = annotationService.getAnnotation(a.getAnnotationId());
    Assert.assertEquals(a.getType(), a1FromDb.getType());
    Assert.assertEquals(a.getValue(), a1FromDb.getValue());
    Assert.assertEquals(a.getMediapackageId(), a1FromDb.getMediapackageId());
    Assert.assertEquals(a.getSessionId(), a1FromDb.getSessionId());
    Assert.assertEquals(a.getUserId(), a1FromDb.getUserId());
    Assert.assertEquals(a.getUserId(), annotationService.securityService.getUser().getUserName());
  }

  @Test
  public void removeAnnotation() throws Exception {
    // Add an annotation
    AnnotationImpl a = new AnnotationImpl();
    a.setInpoint(10);
    a.setOutpoint(100);
    a.setMediapackageId("mp123");
    a.setSessionId("session123");
    a.setType("ugc");
    a.setValue("This is some user generated content");
    annotationService.addAnnotation(a);
  
    //remove annotation
    Assert.assertTrue(annotationService.removeAnnotation(a));  

    //ensure that annotation was removed
    Annotation a1FromDb = null;
    try {
      a1FromDb = annotationService.getAnnotation(a.getAnnotationId());
    } catch (NotFoundException e) {

    }
    Assert.assertNull(a1FromDb);
  }

  @Test
  public void testGetAnnotationsByTypeAndMediapackageId() throws Exception {
    String type = "a type of annotation, such as 'bookmark' or 'note'";
    
    // Add an annotation
    AnnotationImpl a1 = new AnnotationImpl();
    a1.setType(type);
    a1.setInpoint(10);
    a1.setOutpoint(100);
    a1.setMediapackageId("mp");
    a1.setSessionId("session");
    a1.setValue("This is some user generated content");
    annotationService.addAnnotation(a1);
    
    // Add another annotation of the same type to a different mediapackage
    AnnotationImpl a2 = new AnnotationImpl();
    a2.setType(type);
    a2.setInpoint(10);
    a2.setOutpoint(100);
    a2.setMediapackageId("a different mediapackage");
    a2.setSessionId("a different session");
    a2.setValue("More user generated content");
    annotationService.addAnnotation(a2);
    
    AnnotationList annotations = annotationService.getAnnotationsByTypeAndMediapackageId(type, "mp", 0, 100);
    Assert.assertEquals(1, annotations.getAnnotations().size());
  }

  @Test
  public void testGetAnnotationsByMediapackageId() throws Exception {
    
    // Add an annotation
    AnnotationImpl a1 = new AnnotationImpl();
    a1.setType("note");
    a1.setInpoint(10);
    a1.setOutpoint(100);
    a1.setMediapackageId("mp");
    a1.setSessionId("session 1");
    a1.setValue("This is some user generated content 1");
    annotationService.addAnnotation(a1);

    // Add another annotation to the same mediapackage
    AnnotationImpl a2 = new AnnotationImpl();
    a2.setType("comment");
    a2.setInpoint(15);
    a2.setOutpoint(105);
    a2.setMediapackageId("mp");
    a2.setSessionId("session 2");
    a2.setValue("This is some user generated content 2");
    annotationService.addAnnotation(a2);
    
    // Add another annotation to a different mediapackage
    AnnotationImpl a3 = new AnnotationImpl();
    a3.setType("bookmark");
    a3.setInpoint(16);
    a3.setOutpoint(106);
    a3.setMediapackageId("a different mediapackage");
    a3.setSessionId("a different session");
    a3.setValue("More user generated content");
    annotationService.addAnnotation(a3);
    

    // Test method
    AnnotationList annotationResult = annotationService.getAnnotationsByMediapackageId("mp", 0, 200);

    Assert.assertEquals(2, annotationResult.getAnnotations().size());
  }
  
  // TODO: Many more queries need to be tested
}
