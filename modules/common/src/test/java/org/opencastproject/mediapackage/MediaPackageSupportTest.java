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

package org.opencastproject.mediapackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opencastproject.mediapackage.MediaPackageSupport.containsPathTraversal;
import static org.opencastproject.mediapackage.MediaPackageSupport.loadFromClassPath;
import static org.opencastproject.mediapackage.MediaPackageSupport.sanityCheck;

import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.mediapackage.identifier.IdImpl;

import org.junit.Test;

import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.transform.stream.StreamSource;

public class MediaPackageSupportTest {
  @Test
  public void testSanityCheck() {
    assertTrue("Media package must pass check", sanityCheck(loadFromClassPath("/manifest.xml")).isEmpty());
    // A missing id attribute is completed with a generated identifier, so the package passes
    // the check; identifiers that are empty, '.', '..' or contain path separators cannot be
    // parsed at all (see testParseRejectsUnsafeMediaPackageIdentifiers)
    assertTrue(sanityCheck(loadFromClassPath("/manifest-insane.xml")).isEmpty());
    MediaPackage noId = new MediaPackageImpl();
    noId.setIdentifier(null);
    assertTrue("A media package without identifier must be reported", sanityCheck(noId).isPresent());
  }

  @Test
  public void testContainsPathTraversal() {
    // Unsafe identifiers: path separators, the current and parent directory references and
    // the empty identifier
    assertTrue(containsPathTraversal(""));
    assertTrue(containsPathTraversal("."));
    assertTrue(containsPathTraversal(".."));
    assertTrue(containsPathTraversal("/"));
    assertTrue(containsPathTraversal("\\"));
    assertTrue(containsPathTraversal("a/b"));
    assertTrue(containsPathTraversal("a\\b"));
    // Safe identifiers: in particular, a .. sequence inside a component is a harmless
    // literal file system name
    assertFalse(containsPathTraversal(null));
    assertFalse(containsPathTraversal("track..1"));
    assertFalse(containsPathTraversal("mp-1"));
    assertFalse(containsPathTraversal("mp_1.x"));
    assertFalse(containsPathTraversal("a..b"));
    assertFalse(containsPathTraversal("id:1"));
  }

  @Test
  public void testParseRejectsUnsafeMediaPackageIdentifiers() {
    for (String id : new String[] { "", ".", "..", "a/b", "a\\b" }) {
      assertParseRejected(mpXml(id, "track-1"), "media package identifier '" + id + "'");
    }
  }

  @Test
  public void testParseRejectsUnsafeElementIdentifiers() {
    for (String id : new String[] { "", ".", "..", "a/b", "a\\b" }) {
      assertParseRejected(mpXml("mp-1", id), "element identifier '" + id + "'");
    }
  }

  @Test
  public void testParseAcceptsSafeIdentifiers() throws Exception {
    // The rule is a traversal rule, not a character set restriction: identifiers that
    // contain unusual characters but no traversal sequences must stay loadable, since
    // they may already exist in the repository
    for (String mpId : new String[] { "bad id with space", "id@x", "mp-1" }) {
      MediaPackage mp = MediaPackageParser.getFromXml(mpXml(mpId, "track-1"));
      assertEquals(mpId, mp.getIdentifier().toString());
    }
    // A .. sequence inside a component is a harmless literal file system name
    MediaPackage mp = MediaPackageParser.getFromXml(mpXml("mp-1", "track..1"));
    assertEquals(1, mp.getTracks().length);
    assertEquals("track..1", mp.getTracks()[0].getIdentifier());
  }

  @Test
  public void testCheckPathSafetyAcceptsSafeMediaPackages() throws Exception {
    MediaPackage mp = MediaPackageParser.getFromXml(mpXml("mp-1", "track..1"));
    MediaPackageSupport.checkPathSafety(mp);
  }

  @Test
  public void testParseRejectsUnsafePublicationSubElementIdentifiers() {
    // Tracks, attachments and catalogs nested inside a publication are bound directly by
    // JAXB, bypassing the element-level setters, so the parse boundary must check them
    for (String id : new String[] { "", ".", "..", "a/b", "a\\b" }) {
      String xml = publicationXml("mp-1", "pub-1", "track-1");
      assertParseRejected(xml.replace("track-1", id), "publication track identifier '" + id + "'");
      assertParseRejected(xml.replace("attachment-1", id), "publication attachment identifier '" + id + "'");
      assertParseRejected(xml.replace("catalog-1", id), "publication catalog identifier '" + id + "'");
    }
  }

  @Test
  public void testCheckPathSafetyRejectsUnsafePublicationSubElementIdentifiers() throws Exception {
    // A package with an unsafe identifier nested in a publication cannot be parsed, so build
    // the state the unmarshaller would have created by setting the identifier directly
    MediaPackage mp = MediaPackageParser.getFromXml(publicationXml("mp-1", "pub-1", "track-1"));
    AbstractMediaPackageElement nested =
            (AbstractMediaPackageElement) mp.getPublications()[0].getTracks()[0];
    nested.id = "a/b";
    assertRejected(() -> MediaPackageSupport.checkPathSafety(mp), "nested publication element identifier");
  }

  @Test
  public void testSanityCheckReportsUnsafeIdentifiers() throws Exception {
    // The parse boundary rejects these identifiers, so build the state directly to cover the
    // traversal branches of the sanity check
    MediaPackage mp = MediaPackageParser.getFromXml(mpXml("mp-1", "track-1"));
    ((AbstractMediaPackageElement) mp.getTracks()[0]).id = "a/b";
    mp.setIdentifier(unsafeId("a/b"));
    List<String> errors = sanityCheck(mp).orElseThrow();
    assertEquals(2, errors.size());
    assertTrue(errors.toString(), errors.stream().allMatch(e -> e.contains("path separators")));
  }

  /**
   * Unmarshals a media package that is nested inside another root element, so that none of
   * the {@code MediaPackageImpl.valueOf} methods is involved. Such a media package is
   * rejected by the afterUnmarshal callback of MediaPackageImpl, which JAXB also invokes for
   * nested media packages; the check in {@link MediaPackageImpl.Adapter#unmarshal} is tested
   * separately in {@link #testAdapterRejectsUnsafeIdentifiers()}.
   */
  @Test
  public void testNestedUnmarshalRejectsUnsafeIdentifiers() {
    for (String id : new String[] { "", ".", "..", "a/b", "a\\b" }) {
      try {
        unmarshalRaw(nestedMpXml(id));
        fail("Expected nested media package identifier '" + id + "' to be rejected");
      } catch (JAXBException | IllegalArgumentException e) {
        // expected
      }
    }
    try {
      MpContainer container = unmarshalRaw(nestedMpXml("mp-1"));
      assertEquals("mp-1", container.getMediaPackage().getIdentifier().toString());
    } catch (Exception e) {
      fail("The nested identifier mp-1 must be accepted: " + e);
    }
  }

  /**
   * The adapter check is not reachable through an unmarshal (the afterUnmarshal callback
   * rejects the media package first), so it is called directly.
   */
  @Test
  public void testAdapterRejectsUnsafeIdentifiers() throws Exception {
    for (String id : new String[] { "", ".", "..", "a/b", "a\\b" }) {
      MediaPackageImpl mp = new MediaPackageImpl();
      mp.setIdentifier(unsafeId(id));
      assertRejected(() -> adaptNested(mp), "media package identifier '" + id + "' in the adapter");
    }
    MediaPackageImpl unsafeElement = (MediaPackageImpl) MediaPackageParser.getFromXml(mpXml("mp-1", "track-1"));
    ((AbstractMediaPackageElement) unsafeElement.getTracks()[0]).id = "a/b";
    assertRejected(() -> adaptNested(unsafeElement), "element identifier 'a/b' in the adapter");

    assertEquals(null, adaptNested(null));
    MediaPackageImpl safe = (MediaPackageImpl) MediaPackageParser.getFromXml(mpXml("mp-1", "track..1"));
    assertEquals(safe, adaptNested(safe));
  }

  private static MediaPackage adaptNested(MediaPackageImpl mp) {
    try {
      // Not an XML parse: the adapter is handed an already unmarshalled media package
      // CHECKSTYLE:OFF checkstyle:Regexp
      return new MediaPackageImpl.Adapter().unmarshal(mp);
      // CHECKSTYLE:ON checkstyle:Regexp
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @XmlRootElement(name = "container")
  private static final class MpContainer {
    @XmlElement(name = "mediapackage", namespace = "http://mediapackage.opencastproject.org")
    private MediaPackage mediaPackage;

    MediaPackage getMediaPackage() {
      return mediaPackage;
    }
  }

  private static MpContainer unmarshalRaw(String xml) throws JAXBException {
    // CHECKSTYLE:OFF checkstyle:Regexp
    JAXBContext context = JAXBContext.newInstance(MpContainer.class);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    return unmarshaller.unmarshal(new StreamSource(new StringReader(xml)), MpContainer.class).getValue();
    // CHECKSTYLE:ON checkstyle:Regexp
  }

  /**
   * Unmarshals the given media package XML directly with JAXB, without any of the parse
   * choke points (MediaPackageImpl.valueOf, MediaPackageSupport.checkPathSafety). This
   * proves that the two-argument afterUnmarshal callback of MediaPackageImpl rejects unsafe
   * media package identifiers on its own.
   */
  @Test
  public void testUnmarshalCallbackRejectsUnsafeIdentifiers() {
    for (String id : new String[] { "", ".", "..", "a/b", "a\\b" }) {
      try {
        unmarshalRawMp(mpXml(id, "track-1"));
        fail("Expected media package identifier '" + id + "' to be rejected by the unmarshal hook");
      } catch (JAXBException | IllegalArgumentException e) {
        // expected
      }
    }
    try {
      MediaPackageImpl mp = unmarshalRawMp(mpXml("mp-1", "track..1"));
      assertEquals("mp-1", mp.getIdentifier().toString());
    } catch (Exception e) {
      fail("The identifier track..1 must be accepted by the unmarshal hook: " + e);
    }
  }

  private static MediaPackageImpl unmarshalRawMp(String xml) throws JAXBException {
    // The raw unmarshal is the point of the hook test: it must go through the unmarshaller
    // without the XmlSafeParser guard so that only the afterUnmarshal callback is exercised.
    Unmarshaller unmarshaller = MediaPackageImpl.context.createUnmarshaller();
    // CHECKSTYLE:OFF checkstyle:Regexp
    return unmarshaller.unmarshal(new StreamSource(new StringReader(xml)), MediaPackageImpl.class).getValue();
    // CHECKSTYLE:ON checkstyle:Regexp
  }

  private static void assertParseRejected(String xml, String description) {
    try {
      MediaPackageParser.getFromXml(xml);
      fail("Expected " + description + " to be rejected");
    } catch (MediaPackageException | IllegalArgumentException e) {
      // expected
    }
  }

  private static void assertRejected(Runnable action, String description) {
    try {
      action.run();
      fail("Expected " + description + " to be rejected");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  /**
   * Builds an identifier that bypasses the {@link IdImpl#IdImpl(String)} validation, to
   * represent an unsafe identifier as bound directly by JAXB.
   */
  private static Id unsafeId(String value) {
    return new IdImpl() {
      {
        this.id = value;
      }
    };
  }

  private static String mpXml(String mpId, String elementId) {
    return "<mediapackage xmlns=\"http://mediapackage.opencastproject.org\" id=\"" + mpId + "\">\n"
            + "  <media>\n"
            + "    <track id=\"" + elementId + "\" type=\"presenter/source\">\n"
            + "      <url>file:///tmp/media.mp4</url>\n"
            + "    </track>\n"
            + "  </media>\n"
            + "</mediapackage>\n";
  }

  private static String publicationXml(String mpId, String publicationId, String trackId) {
    return "<mediapackage xmlns=\"http://mediapackage.opencastproject.org\" id=\"" + mpId + "\">\n"
            + "  <publications>\n"
            + "    <publication id=\"" + publicationId + "\" channel=\"channel-1\">\n"
            + "      <media>\n"
            + "        <track id=\"" + trackId + "\" type=\"presenter/source\">\n"
            + "          <url>file:///tmp/media.mp4</url>\n"
            + "        </track>\n"
            + "      </media>\n"
            + "      <attachments>\n"
            + "        <attachment id=\"attachment-1\" type=\"dublincore/episode\">\n"
            + "          <url>file:///tmp/dc.xml</url>\n"
            + "        </attachment>\n"
            + "      </attachments>\n"
            + "      <metadata>\n"
            + "        <catalog id=\"catalog-1\" type=\"dublincore/episode\">\n"
            + "          <url>file:///tmp/dc.xml</url>\n"
            + "        </catalog>\n"
            + "      </metadata>\n"
            + "    </publication>\n"
            + "  </publications>\n"
            + "</mediapackage>\n";
  }

  private static String nestedMpXml(String mpId) {
    return "<container>\n" + mpXml(mpId, "track-1") + "</container>\n";
  }
}
