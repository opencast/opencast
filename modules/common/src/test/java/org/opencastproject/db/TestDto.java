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

package org.opencastproject.db;

import static org.opencastproject.db.Queries.namedQuery;

import org.junit.Ignore;

import java.util.List;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity(name = "Test")
@Table(name = "test")
@NamedQueries(@NamedQuery(name = "Test.findAll", query = "select a from Test a"))
@Ignore
public class TestDto {
  @Id
  @GeneratedValue
  private long id;

  @Column(name = "key", length = 128, nullable = false)
  private String key;

  @Column(name = "value", length = 128, nullable = false)
  private String value;

  public static TestDto create(String key, String value) {
    final TestDto dto = new TestDto();
    dto.key = key;
    dto.value = value;
    return dto;
  }

  public long getId() {
    return id;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public static final Function<EntityManager, List<TestDto>> findAll = namedQuery.findAll("Test.findAll",
      TestDto.class);
}
