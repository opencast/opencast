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

package org.opencastproject.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.data.Tuple.tuple;

import com.entwinemedia.fn.data.Iterators;
import com.entwinemedia.fn.data.ListBuilders;
import org.opencastproject.util.data.Collections;

import org.junit.Test;

import java.util.List;

import javax.xml.XMLConstants;

public class XmlNamespaceContextTest {
  @Test
  public void testGetNamespaceURI() throws Exception {
    testGetNamespaceURI(new XmlNamespaceContext(Collections.map(
            tuple("foo", "http://foo.org"),
            tuple("bar", "http://bar.org"))));
    testGetNamespaceURI(XmlNamespaceContext.mk(asList(
            new XmlNamespaceBinding("foo", "http://foo.org"),
            new XmlNamespaceBinding("bar", "http://bar.org"))));
  }

  private void testGetNamespaceURI(XmlNamespaceContext ctx) {
    assertEquals("http://foo.org", ctx.getNamespaceURI("foo"));
    assertEquals("http://bar.org", ctx.getNamespaceURI("bar"));
    assertEquals(XMLConstants.NULL_NS_URI, ctx.getNamespaceURI("baz"));
    assertEquals(XMLConstants.XML_NS_URI, ctx.getNamespaceURI(XMLConstants.XML_NS_PREFIX));
    assertEquals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, ctx.getNamespaceURI(XMLConstants.XMLNS_ATTRIBUTE));
  }

  @Test
  public void testGetPrefix() throws Exception {
    testGetPrefix(new XmlNamespaceContext(Collections.map(
            tuple("foo", "http://foo.org"),
            tuple("bar", "http://bar.org"))));
    testGetPrefix(XmlNamespaceContext.mk(asList(
            new XmlNamespaceBinding("foo", "http://foo.org"),
            new XmlNamespaceBinding("bar", "http://bar.org"))));
  }

  private void testGetPrefix(XmlNamespaceContext ctx) {
    assertEquals("foo", ctx.getPrefix("http://foo.org"));
    assertEquals("bar", ctx.getPrefix("http://bar.org"));
    assertNull(ctx.getPrefix("http://baz.org"));
  }

  @Test
  public void testGetPrefixes() throws Exception {
    testGetPrefixes(new XmlNamespaceContext(Collections.map(
                            tuple("foo", "http://foo.org"),
                            tuple("bar", "http://bar.org"),
                            tuple("baz", "http://bar.org"))),
                    asList("bar", "baz"));
    testGetPrefixes(XmlNamespaceContext.mk(asList(
                            new XmlNamespaceBinding("foo", "http://foo.org"),
                            new XmlNamespaceBinding("bar", "http://bar.org"),
                            new XmlNamespaceBinding("baz", "http://bar.org"))),
                    asList("bar", "baz"));
  }

  private void testGetPrefixes(XmlNamespaceContext ctx, List<String> barPrefixes) {
    assertTrue(Iterators.eq(asList("foo").iterator(), ctx.getPrefixes("http://foo.org")));
    assertTrue(EqualsUtil.eqListUnsorted(barPrefixes, ListBuilders.LIA.mk(ctx.getPrefixes("http://bar.org"))));
    assertTrue(Iterators.eq(asList("xml").iterator(), ctx.getPrefixes(XMLConstants.XML_NS_URI)));
    assertTrue(Iterators.eq(asList("xmlns").iterator(), ctx.getPrefixes(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)));
    assertFalse(ctx.getPrefixes("http://baz.org").hasNext());
  }

  @Test
  public void testAdd() {
    final XmlNamespaceContext ctx = XmlNamespaceContext.mk("gnu", "http://gnu.org");
    final XmlNamespaceContext added = ctx.add(
            new XmlNamespaceBinding("foo", "http://foo.org"),
            new XmlNamespaceBinding("bar", "http://bar.org"));
    testGetNamespaceURI(added);
    testGetPrefix(added);
    testGetPrefixes(added, asList("bar"));
    assertEquals("gnu", added.getPrefix("http://gnu.org"));
    assertNotEquals(ctx, added);
  }

  @Test
  public void testAddOverwrite() {
    final XmlNamespaceContext ctx = XmlNamespaceContext.mk("gnu", "http://gnu.org");
    final XmlNamespaceContext added = ctx.add(new XmlNamespaceBinding("gnu", "http://gnoo.org"));
    assertNull("Existing binding should not be overwritten.", added.getPrefix("http://gnoo.org"));
    assertEquals("gnu", added.getPrefix("http://gnu.org"));
    // test other add method
    final XmlNamespaceContext added2 = ctx.add(XmlNamespaceContext.mk("gnu", "http://gnoo.org"));
    assertNull("Existing binding should not be overwritten.", added2.getPrefix("http://gnoo.org"));
    assertEquals("gnu", added2.getPrefix("http://gnu.org"));
  }
}
