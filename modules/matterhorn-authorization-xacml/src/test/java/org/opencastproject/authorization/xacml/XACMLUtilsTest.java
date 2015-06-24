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

package org.opencastproject.authorization.xacml;

import static org.junit.Assert.assertThat;

import static org.xmlmatchers.XmlMatchers.isEquivalentTo;
import static org.junit.Assert.assertEquals;
import static org.xmlmatchers.transform.XmlConverters.the;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.identifier.IdImpl;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for class {@link XACMLUtils}
 */
public class XACMLUtilsTest {

  private static final String MP_IDENTIFIER = "mediapackage-1";

  private String xacml;
  private AccessControlList acl;
  private MediaPackage mp;

  @Before
  public void setUp() throws Exception {
    xacml = IOUtils.toString(this.getClass().getResourceAsStream("/xacml.xml"));
    acl = AccessControlParser.parseAcl(this.getClass().getResourceAsStream("/acl.xml"));
    mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(new IdImpl(MP_IDENTIFIER));
  }

  /**
   * Unit test for method {@link XACMLUtils#getXacml(MediaPackage, AccessControlList)}
   */
  @Test
  public void testGetXacml() throws Exception {
    String newXacml = XACMLUtils.getXacml(mp, acl);
    assertThat(the(xacml), isEquivalentTo(the(newXacml)));
  }

  /**
   * Unit test for method {@link XACMLUtils#parseXacml(java.io.InputStream}
   */
  @Test
  public void testParseXacml() throws Exception {
    assertEquals(acl.getEntries(), XACMLUtils.parseXacml(this.getClass().getResourceAsStream("/xacml.xml")).getEntries());
  }

}
