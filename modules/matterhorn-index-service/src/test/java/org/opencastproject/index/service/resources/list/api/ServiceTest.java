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
package org.opencastproject.index.service.resources.list.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.index.service.resources.list.provider.TestServiceStatistics;
import org.opencastproject.index.service.resources.list.query.ResourceListQueryImpl;
import org.opencastproject.index.service.resources.list.query.StringListFilter;

import org.junit.Before;
import org.junit.Test;

public class ServiceTest {
  private Service service;

  @Before
  public void setUp() {
    service = new Service(new TestServiceStatistics());
  }

  @Test
  public void testPositiveFreetextFilter() {
    ResourceListQueryImpl query = makeQuery(new StringListFilter("HOST 1"));
    assertTrue(service.isCompliant(query));
  }

  private ResourceListQueryImpl makeQuery(StringListFilter filter) {
    ResourceListQueryImpl query = new ResourceListQueryImpl();
    query.addFilter(filter);
    return query;
  }

  @Test
  public void testNegativeFreetextFilter() {
    ResourceListQueryImpl query = makeQuery(new StringListFilter("doesnt exist"));
    assertFalse(service.isCompliant(query));
  }

  @Test
  public void testConcreteFilter() {
    ResourceListQueryImpl query = makeQuery(new StringListFilter("host", "HOST 1"));
    assertTrue(service.isCompliant(query));
  }

  @Test
  public void testNegativeConcreteFilter() {
    ResourceListQueryImpl query = makeQuery(new StringListFilter("name", "HOST 1"));
    assertFalse(service.isCompliant(query));
  }

  @Test
  public void testWrongCriterion() {
    ResourceListQueryImpl query = makeQuery(new StringListFilter("doesntExist", "HOST 1"));
    assertFalse(service.isCompliant(query));
  }
}
