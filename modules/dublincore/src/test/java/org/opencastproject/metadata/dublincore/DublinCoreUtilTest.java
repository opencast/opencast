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
package org.opencastproject.metadata.dublincore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;
import static org.opencastproject.metadata.dublincore.TestUtil.read;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class DublinCoreUtilTest {
  @Test
  public void testChecksumDistinct() throws Exception {
    assertThat(
        Arrays.asList(
            checksum("/checksum/dublincore1-1.xml"),
            checksum("/checksum/dublincore1-2.xml"),
            checksum("/checksum/dublincore1-3.xml"),
            checksum("/checksum/dublincore1-4.xml"),
            checksum("/checksum/dublincore1-5.xml"),
            checksum("/checksum/dublincore1-6.xml"),
            checksum("/checksum/dublincore1-7.xml"),
            checksum("/checksum/dublincore1-8.xml"),
            checksum("/checksum/dublincore1-9.xml"),
            checksum("/checksum/dublincore1-A.xml")
        ),
        this.<String>isDistinct());
  }

  @Test
  public void testChecksumEqual() throws Exception {
    assertThat(
        Arrays.asList(
            checksum("/checksum/dublincore2-1.xml"),
            checksum("/checksum/dublincore2-2.xml"),
            checksum("/checksum/dublincore2-3.xml"),
            checksum("/checksum/dublincore2-4.xml")
        ),
        this.<String>allEqual());
  }

  /** Make sure no character contains a null byte, so that it is safe to use 0 as a separator. */
  @Test
  public void testUtf8CodePointsDoNotContainNullByte() throws Exception {
    for (int i = Character.MIN_VALUE + 1; i <= Character.MAX_VALUE; i++) {
      for (byte b : Character.valueOf((char) i).toString().getBytes(StandardCharsets.UTF_8)) {
        assertNotEquals(0, b);
      }
    }
  }

  @Test
  public void testDigestingSplitStrings() throws Exception {
    final MessageDigest md1 = MessageDigest.getInstance("md5");
    md1.update("haus".getBytes(StandardCharsets.UTF_8));
    md1.update("meister".getBytes(StandardCharsets.UTF_8));
    byte[] digest1 = md1.digest();
    final MessageDigest md2 = MessageDigest.getInstance("md5");
    md2.update("hausmeister".getBytes(StandardCharsets.UTF_8));
    byte[] digest2 = md2.digest();
    assertArrayEquals(digest1, digest2);
  }

  //
  //
  //

  private String checksum(String dcFile) throws Exception {
    return DublinCoreUtil.calculateChecksum(read(dcFile)).getValue();
  }

  private <A> Matcher<List<A>> isDistinct() {
    return new CustomTypeSafeMatcher<List<A>>("a list containing distinct elements") {
      @Override protected boolean matchesSafely(List<A> list) {
        return list.size() == new HashSet<>(list).size();
      }
    };
  }

  private <A> Matcher<List<A>> allEqual() {
    return new CustomTypeSafeMatcher<List<A>>("a list containing equal elements") {
      @Override protected boolean matchesSafely(List<A> list) {
        return new HashSet<>(list).size() <= 1;
      }
    };
  }
}
