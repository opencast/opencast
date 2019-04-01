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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.opencastproject.authorization.xacml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.xmlmatchers.XmlMatchers.isEquivalentTo
import org.xmlmatchers.transform.XmlConverters.the

import org.opencastproject.mediapackage.MediaPackage
import org.opencastproject.mediapackage.MediaPackageBuilderFactory
import org.opencastproject.mediapackage.identifier.IdImpl
import org.opencastproject.security.api.AccessControlList
import org.opencastproject.security.api.AccessControlParser

import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for class [XACMLUtils]
 */
class XACMLUtilsTest {

    private var xacml: String? = null
    private var acl: AccessControlList? = null
    private var mp: MediaPackage? = null

    @Before
    @Throws(Exception::class)
    fun setUp() {
        xacml = IOUtils.toString(this.javaClass.getResourceAsStream("/xacml.xml"))
        acl = AccessControlParser.parseAcl(this.javaClass.getResourceAsStream("/acl.xml"))
        mp = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder().createNew(IdImpl(MP_IDENTIFIER))
    }

    /**
     * Unit test for method [XACMLUtils.getXacml]
     */
    @Test
    @Throws(Exception::class)
    fun testGetXacml() {
        val newXacml = XACMLUtils.getXacml(mp!!, acl!!)
        assertThat<Source>(the(xacml!!), isEquivalentTo(the(newXacml)))
    }

    /**
     * Unit test for method [}][XACMLUtils.parseXacml]
     */
    @Test
    @Throws(Exception::class)
    fun testParseXacml() {
        assertEquals(acl!!.entries, XACMLUtils.parseXacml(this.javaClass.getResourceAsStream("/xacml.xml")).entries)
    }

    companion object {

        private val MP_IDENTIFIER = "mediapackage-1"
    }

}
