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
package org.opencastproject.workflow.handler.composer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.opencastproject.util.UrlSupport.uri
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.limit
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.toSeconds
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.validateTargetBaseNameFormat

import org.opencastproject.composer.api.EncodingProfile
import org.opencastproject.mediapackage.MediaPackageElementFlavor
import org.opencastproject.mediapackage.Track
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.Cfg
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.Extractor
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.MediaPosition
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.MediaPositionParser
import org.opencastproject.workflow.handler.composer.ImageWorkflowOperationHandler.PositionType

import com.entwinemedia.fn.data.ListBuilder
import com.entwinemedia.fn.data.ListBuilders
import com.entwinemedia.fn.data.Opt
import com.entwinemedia.fn.parser.Result

import org.easymock.EasyMock
import org.junit.Test

class ImageWorkflowOperationHandlerTest {

    @Test
    fun testMediaPositionParserSuccess() {
        test("75900000000001", sec(75900000000001.0))
        test("10, 20.3", sec(10.0), sec(20.3))
        test("10 20.3", sec(10.0), sec(20.3))
        test("1020.3", sec(1020.3))
        test("10 , 20.3", sec(10.0), sec(20.3))
        test("10    20.3", sec(10.0), sec(20.3))
        test("15", sec(15.0))
        test("11%", percent(11.0))
        test(" 19.70% , 20.3", percent(19.7), sec(20.3))
        test(" 10.62% 20.3 14.2     ", percent(10.62), sec(20.3), sec(14.2))
        test("10.62% 20.3 ,14.213%", percent(10.62), sec(20.3), percent(14.213))
    }

    @Test
    fun testMediaPositionParserFailure() {
        assertFalse(testSuccess("  ") || testSuccess(" ") || testSuccess(",, ,") || testSuccess("")
                || testSuccess("10a,10b"))
    }

    @Test
    fun testFileNameGeneration() {
        val dummy = ImageWorkflowOperationHandler()
        assertEquals("thumbnail_12_5p_small.jpg",
                Extractor(dummy, cfg(Opt.none(), Opt.some("thumbnail_%.1fp%s"))).createFileName("_small.jpg",
                        uri("http://localhost/path/filename.mp4"), MediaPosition(PositionType.Percentage, 12.5)))
        assertEquals("thumbnail_0p.jpg",
                Extractor(dummy, cfg(Opt.none(), Opt.some("thumbnail_%.0fp%s"))).createFileName(".jpg",
                        uri("http://localhost/path/filename.mp4"), MediaPosition(PositionType.Percentage, 0.0)))
        assertEquals("video_14_200s.jpg",
                Extractor(dummy, cfg(Opt.none(), Opt.none())).createFileName(".jpg",
                        uri("http://localhost/path/video.mp4"), MediaPosition(PositionType.Seconds, 14.2)))
        assertEquals("video_15_110s_medium.jpg",
                Extractor(dummy, cfg(Opt.none(), Opt.none())).createFileName("_medium.jpg",
                        uri("http://localhost/path/video.mp4"), MediaPosition(PositionType.Seconds, 15.1099)))
        assertEquals("thumbnail_15_110s_large.jpg",
                Extractor(dummy, cfg(Opt.some("thumbnail_%.3fs%s"), Opt.none())).createFileName("_large.jpg",
                        uri("http://localhost/path/video.mp4"), MediaPosition(PositionType.Seconds, 15.1099)))
        assertEquals("thumbnail", Extractor(dummy, cfg(Opt.some("thumbnail"), Opt.none())).createFileName(
                "_large.jpg", uri("http://localhost/path/video.mp4"), MediaPosition(PositionType.Seconds, 15.1099)))
        assertEquals("thumbnail_large.jpg",
                Extractor(dummy, cfg(Opt.some("thumbnail%2\$s"), Opt.none())).createFileName("_large.jpg",
                        uri("http://localhost/path/video.mp4"), MediaPosition(PositionType.Seconds, 15.1099)))
    }

    @Test
    fun testValidateTargetBaseNameFormat() {
        validateTargetBaseNameFormat("format-a").apply("thumbnail_%.1fs%s")
        validateTargetBaseNameFormat("format-a").apply("thumbnail_%1$.1fs%2\$s")
        validateTargetBaseNameFormat("format-a").apply("%2\$s_thumbnail_%1$.1fs%1$.1fs%2\$s")
        try {
            validateTargetBaseNameFormat("format-a").apply("thumbnail_%.1fs%.3f")
            fail("Invalid format passed check. Suffix format %s is missing.")
        } catch (ignore: Exception) {
        }

        try {
            validateTargetBaseNameFormat("format-a").apply("thumbnail_%.3f")
            fail("Invalid format passed check. Suffix format %s is missing.")
        } catch (ignore: Exception) {
        }

        try {
            validateTargetBaseNameFormat("format-a").apply("thumbnail_%s")
            fail("Invalid format passed check. Suffix is missing since %s does not have a positional parameter.")
        } catch (ignore: Exception) {
        }

        // omitting the position should pass
        validateTargetBaseNameFormat("format-a").apply("thumbnail_%2\$s")
    }

    @Test
    fun testLimit() {
        assertTrue(limit(track(1511), l.mk(sec(-1.0), sec(1511.0), sec(1512.0))).isEmpty())
        assertTrue(limit(track(1511), l.mk(percent(-0.2), percent(101.0))).isEmpty())
        assertEquals(4, limit(track(1511), l.mk(percent(0.0), percent(100.0), sec(0.0), sec(1510.0))).size.toLong())
        assertEquals(4, limit(track(1511), l.mk(percent(0.0), percent(10.0), sec(10.0), sec(1500.0), percent(200.0))).size.toLong())
    }

    @Test
    fun testToSeconds() {
        assertEquals(0.985, toSeconds(track(1970), percent(50.0), 0.0), 0.0)
        assertEquals(0.0, toSeconds(track(1970), percent(0.0), 0.0), 0.0)
        assertEquals(1.87, toSeconds(track(1970), percent(100.0), 100.0), 0.0)
        assertEquals(0.0, toSeconds(track(100), percent(100.0), 200.0), 0.0)
        assertEquals(1.77, toSeconds(track(1970), sec(1.970), 200.0), 0.0)
        assertEquals(1.77, toSeconds(track(1970), sec(1.870), 200.0), 0.0)
        assertEquals(1.77, toSeconds(track(1970), sec(1.770), 200.0), 0.0)
        assertEquals(1.769, toSeconds(track(1970), sec(1.769), 200.0), 0.0)
    }

    // ** ** **

    private fun cfg(targetBaseNamePatternSecond: Opt<String>, targetBaseNamePatternPercent: Opt<String>): Cfg {
        return Cfg(l.nil(), l.nil(), l.nil(),
                Opt.none(), l.nil(), targetBaseNamePatternSecond,
                targetBaseNamePatternPercent, 0)
    }

    private fun sec(a: Double): MediaPosition {
        return MediaPosition(PositionType.Seconds, a)
    }

    private fun percent(a: Double): MediaPosition {
        return MediaPosition(PositionType.Percentage, a)
    }

    private fun track(duration: Long): Track {
        val t = EasyMock.createNiceMock<Track>(Track::class.java)
        EasyMock.expect<Long>(t.duration).andReturn(duration).anyTimes()
        EasyMock.replay(t)
        return t
    }

    private fun test(expr: String, vararg expected: MediaPosition) {
        val r = MediaPositionParser.positions.parse(expr)
        assertTrue(r.isDefined)
        assertTrue("Rest:\"" + r.rest + "\"", r.rest.isEmpty())
        assertEquals(l.mk(*expected), r.result)
    }

    private fun testSuccess(expr: String): Boolean {
        val r = MediaPositionParser.positions.parse(expr)
        return r.isDefined && r.rest.isEmpty()
    }

    companion object {
        private val l = ListBuilders.strictImmutableArray
    }
}
