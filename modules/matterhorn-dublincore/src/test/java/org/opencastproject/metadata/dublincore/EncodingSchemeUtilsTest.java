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

package org.opencastproject.metadata.dublincore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodeDate;
import static org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodeDuration;
import static org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodePeriod;
import static org.opencastproject.metadata.dublincore.EncodingSchemeUtils.decodeTemporal;
import static org.opencastproject.metadata.dublincore.EncodingSchemeUtils.encodeDate;
import static org.opencastproject.metadata.dublincore.EncodingSchemeUtils.encodeDuration;
import static org.opencastproject.metadata.dublincore.EncodingSchemeUtils.encodePeriod;
import static org.opencastproject.metadata.dublincore.TestUtil.createDate;
import static org.opencastproject.metadata.dublincore.TestUtil.precisionDay;
import static org.opencastproject.metadata.dublincore.TestUtil.precisionSecond;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Test cases for {@link org.opencastproject.metadata.dublincore.EncodingSchemeUtils}.
 */
public class EncodingSchemeUtilsTest {

  private Logger logger = LoggerFactory.getLogger(EncodingSchemeUtilsTest.class);

  @Test
  public void printTimeZone() {
    // Not a test case...
    logger.info("Time zone = " + TimeZone.getDefault());
  }

  @Test
  public void testEncodeDate() {
    Date now = new Date();
    assertEquals(4, encodeDate(now, Precision.Year).getValue().length());
    assertEquals(3, encodeDate(now, Precision.Day).getValue().split("-").length);
    assertEquals("2009-01-01T00:00:00Z".length(), encodeDate(now, Precision.Second).getValue().length());
    assertEquals(DublinCore.ENC_SCHEME_W3CDTF, encodeDate(now, Precision.Year).getEncodingScheme());
    // Test symmetry
    assertEquals(decodeDate(encodeDate(now, Precision.Second)), precisionSecond(now));
    assertEquals(decodeDate(encodeDate(createDate(1999, 3, 21, 14, 0, 0), Precision.Day)), precisionDay(createDate(
            1999, 3, 21, 14, 0, 0)));
    assertEquals("1724-04-22", encodeDate(createDate(1724, 4, 22, 18, 30, 0), Precision.Day).getValue());
    assertEquals("1724-04-22T18:30:00Z", encodeDate(createDate(1724, 4, 22, 18, 30, 0, "UTC"), Precision.Second)
            .getValue());
    assertEquals("1724-04-22T17:30:10Z", encodeDate(createDate(1724, 4, 22, 17, 30, 10, "UTC"), Precision.Second)
            .getValue());
    assertEquals("1724-04-22T17:30Z", encodeDate(createDate(1724, 4, 22, 17, 30, 25, "UTC"), Precision.Minute)
            .getValue());
    assertEquals("1999-03-21", encodeDate(createDate(1999, 3, 21, 18, 30, 25), Precision.Day).getValue());
    //
    logger.info(encodeDate(now, Precision.Day).getValue());
    logger.info(encodeDate(now, Precision.Second).getValue());
  }

  @Test
  public void testEncodeFraction() {
    Date a = new Date(1);
    Date b = new Date(125);
    Date c = new Date(100);
    assertEquals("1970-01-01T00:00:00.001Z", encodeDate(a, Precision.Fraction).getValue());
    assertEquals("1970-01-01T00:00:00.125Z", encodeDate(b, Precision.Fraction).getValue());
    assertEquals("1970-01-01T00:00:00.100Z", encodeDate(c, Precision.Fraction).getValue());
  }

  @Test
  public void testEncodePeriod() {
    DublinCoreValue a = encodePeriod(new DCMIPeriod(createDate(2007, 2, 10, 12, 0, 0), createDate(2009, 12, 24, 10, 0,
            0), "long time"), Precision.Day);
    assertEquals("start=2007-02-10; end=2009-12-24; name=long time; scheme=W3C-DTF;", a.getValue());
    assertEquals(DublinCore.ENC_SCHEME_PERIOD, a.getEncodingScheme());
    DublinCoreValue b = encodePeriod(new DCMIPeriod(createDate(2007, 2, 10, 12, 0, 0), null), Precision.Day);
    assertEquals("start=2007-02-10; scheme=W3C-DTF;", b.getValue());
  }

  @Test
  public void testDecodeDate() {
    assertEquals(createDate(2008, 10, 1, 0, 0, 0), decodeDate(new DublinCoreValue("2008-10-01")));
    assertEquals(createDate(1999, 3, 21, 14, 30, 0, "UTC"), decodeDate(new DublinCoreValue("1999-03-21T14:30Z")));
    assertEquals(createDate(1999, 3, 21, 14, 30, 0, "UTC"), decodeDate(new DublinCoreValue("1999-03-21T14:30:00Z")));
    assertEquals(createDate(1999, 3, 21, 14, 30, 15, "UTC"), decodeDate(new DublinCoreValue("1999-03-21T14:30:15Z")));
    assertEquals(createDate(2001, 9, 11, 0, 0, 0), decodeDate(new DublinCoreValue("2001-09-11")));
    logger.info(decodeDate(new DublinCoreValue("2009-03-31")).toString());
    logger.info(decodeDate(new DublinCoreValue("2009-09-11")).toString());
    logger.info(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(decodeDate(new DublinCoreValue(
            "2009-03-31"))));
  }

  @Test
  public void testDecodePeriod() {
    DCMIPeriod a = decodePeriod(new DublinCoreValue("start=2008-10-01; end=2009-01-01;"));
    assertEquals(createDate(2008, 10, 1, 0, 0, 0), a.getStart());
    assertEquals(createDate(2009, 1, 1, 0, 0, 0), a.getEnd());
    DCMIPeriod b = decodePeriod(new DublinCoreValue("start=2008-10-01; end=2009-01-01"));
    assertEquals(createDate(2008, 10, 1, 0, 0, 0), b.getStart());
    assertEquals(createDate(2009, 1, 1, 0, 0, 0), b.getEnd());
    DCMIPeriod c = decodePeriod(new DublinCoreValue("start=2008-10-01"));
    assertEquals(createDate(2008, 10, 1, 0, 0, 0), c.getStart());
    assertNull(c.getEnd());
    DCMIPeriod d = decodePeriod(new DublinCoreValue("start=2008-10-01T10:20Z; end=2009-01-01; scheme=UNKNOWN"));
    assertNull(d);
    DCMIPeriod e = decodePeriod(new DublinCoreValue("start=2008-10-01T10:20Z; end=2009-01-01; scheme=W3C-DTF"));
    assertNotNull(e);
    DCMIPeriod f = decodePeriod(new DublinCoreValue("start=2008-10-01ERR; end=2009-01-01; scheme=W3C-DTF"));
    assertNull(f);
  }

  @Test
  public void testDecodeTemporal() {
    Temporal.Match<Integer> match = new Temporal.Match<Integer>() {
      @Override
      public Integer period(DCMIPeriod period) {
        return 1;
  }

      @Override
      public Integer instant(Date instant) {
        return 2;
      }

      @Override
      public Integer duration(long duration) {
        return 3;
      }
    };
    Temporal.Match<Long> durationMatch = new Temporal.Match<Long>() {
      @Override
      public Long period(DCMIPeriod period) {
        throw new RuntimeException();
      }

      @Override
      public Long instant(Date instant) {
        throw new RuntimeException();
      }

      @Override
      public Long duration(long duration) {
        return duration;
      }
    };
    assertSame(1, decodeTemporal(new DublinCoreValue("start=2008-10-01; end=2009-01-01;")).fold(match));
    assertSame(2, decodeTemporal(new DublinCoreValue("2008-10-01")).fold(match));
    assertSame(2, decodeTemporal(new DublinCoreValue("2008-10-01T10:30:05Z")).fold(match));
    assertSame(1, decodeTemporal(new DublinCoreValue("start=2008-10-01T10:20Z; end=2009-01-01; scheme=W3C-DTF")).fold(match));
    assertSame(3, decodeTemporal(new DublinCoreValue("PT10H5M")).fold(match));
    assertEquals(10L * 60 * 60 * 1000 + 5 * 60 * 1000,
            (long) decodeTemporal(new DublinCoreValue("PT10H5M")).fold(durationMatch));
    assertEquals(10L * 60 * 60 * 1000 + 5 * 60 * 1000 + 28 * 1000,
            (long) decodeTemporal(new DublinCoreValue("PT10H5M28S")).fold(durationMatch));
  }

  @Test
  public void testEncodeDuration() {
    Long d1 = 2743414L;
    assertEquals(d1, decodeDuration(encodeDuration(d1).getValue()));
    Long d2 = 78534795325L;
    assertEquals(d2, decodeDuration(encodeDuration(d2).getValue()));
    Long d3 = 234L;
    assertEquals(d3, decodeDuration(encodeDuration(d3).getValue()));
    assertEquals(new Long(1 * 1000 * 60 * 60 + 10 * 1000 * 60 + 5 * 1000), decodeDuration("01:10:05"));

    assertEquals(DublinCore.ENC_SCHEME_ISO8601, encodeDuration(d3).getEncodingScheme());

    assertNull(decodeDuration(new DublinCoreValue("bla")));
    assertNull(decodeDuration(new DublinCoreValue(encodeDuration(d1).getValue(), DublinCore.LANGUAGE_UNDEFINED,
            DublinCore.ENC_SCHEME_BOX)));
  }

}
