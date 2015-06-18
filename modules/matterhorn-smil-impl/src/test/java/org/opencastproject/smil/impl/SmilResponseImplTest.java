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

package org.opencastproject.smil.impl;

import javax.xml.bind.JAXBException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.entity.SmilImpl;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.api.SmilObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmilResponseImplTest {

  private static final Logger logger = LoggerFactory.getLogger(SmilResponseImplTest.class);

  /**
   * Test of getSmil method, of class SmilResponseImpl.
   */
  @Test
  public void testGetSmil() {
    Smil smil = new SmilImpl();
    SmilResponse response = new SmilResponseImpl(smil);
    assertEquals(smil, response.getSmil());
  }

  /**
   * Test of getEntitiesCount method, of class SmilResponseImpl.
   */
  @Test
  public void testGetEntitiesCount() {
    Smil smil = new SmilImpl();
    SmilResponse response = new SmilResponseImpl(smil);
    assertSame(0, response.getEntitiesCount());
    response = new SmilResponseImpl(smil, smil.getBody());
    assertSame(1, response.getEntitiesCount());
    response = new SmilResponseImpl(smil, new SmilObject[]{smil.getHead(), smil.getBody()});
    assertSame(2, response.getEntitiesCount());
  }

  /**
   * Test of getEntity method, of class SmilResponseImpl.
   */
  @Test
  public void testGetEntity() throws Exception {
    Smil smil = new SmilImpl();
    SmilResponse response = new SmilResponseImpl(smil);
    try {
      response.getEntity();
      fail("getEntity should fail, if entity count is zero");
    } catch (SmilException ex) {
    }

    response = new SmilResponseImpl(smil, smil.getBody());
    try {
      assertSame(smil.getBody(), response.getEntity());
      assertSame(1, response.getEntities().length);
      assertSame(smil.getBody(), response.getEntities()[0]);
    } catch (SmilException ex) {
      fail("getEntity should return the entity");
    }

    response = new SmilResponseImpl(smil, new SmilObject[]{smil.getHead(), smil.getBody()});
    try {
      response.getEntity();
      fail("get entity should fail if there are more then one entities set.");
    } catch (SmilException ex) {
    }
  }

  /**
   * Test of getEntities method, of class SmilResponseImpl.
   */
  @Test
  public void testGetEntities() throws Exception {
    Smil smil = new SmilImpl();
    SmilResponse response = new SmilResponseImpl(smil);
    try {
      response.getEntities();
      fail("getEntities should fail, if entity count is zero");
    } catch (SmilException ex) {
    }

    response = new SmilResponseImpl(smil, smil.getBody());
    try {
      SmilObject[] entities = response.getEntities();
      assertSame(1, entities.length);
      assertSame(smil.getBody(), entities[0]);
    } catch (SmilException ex) {
      fail("getEntities should not throw an Exception if some entities are set");
    }

    response = new SmilResponseImpl(smil, new SmilObject[]{smil.getHead(), smil.getBody()});
    try {
      SmilObject[] entities = response.getEntities();
      assertSame(2, entities.length);
      assertSame(smil.getHead(), entities[0]);
      assertSame(smil.getBody(), entities[1]);
    } catch (SmilException ex) {
      fail("getEntities should not throw an Exception if some entities are set");
    }
  }

  /**
   * Test of toXml and fromXml methods, of class SmilResponseImpl.
   */
  @Test
  public void testXml() throws Exception {
    Smil smil = new SmilImpl();
    SmilResponse response = new SmilResponseImpl(smil,
            new SmilObject[]{smil.getHead(), smil.getBody()});

    try {
      String xml = response.toXml();
      // logger.info(xml);
      assertNotNull(xml);
      // test xml contains smil element
      assertTrue(xml.contains("<smil"));
      // test xml contains head entity
      assertTrue(xml.contains("entity><head"));
      // test xml contains body entity
      assertTrue(xml.contains("entity><body"));

      SmilResponse responseUnmarshalled = SmilResponseImpl.fromXml(xml);
      assertNotNull(responseUnmarshalled);
      // test smil object id
      assertEquals(response.getSmil().getId(), responseUnmarshalled.getSmil().getId());
      // test entities
      assertSame(response.getEntitiesCount(), responseUnmarshalled.getEntitiesCount());
      assertEquals(response.getEntities()[0].getId(), responseUnmarshalled.getEntities()[0].getId());
      assertEquals(response.getEntities()[1].getId(), responseUnmarshalled.getEntities()[1].getId());
    } catch (JAXBException ex) {
      fail("can't (de-)serialize SmilResponse");
    }
  }
}
