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
package org.opencastproject.composer.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.opencastproject.composer.api.EncoderException;
import org.opencastproject.composer.api.EncodingProfile;

import org.junit.Test;

import java.io.File;
import java.util.List;

/**
 * @author John Crossman
 */
public class AbstractCmdlineEncoderEngineTest {

  private MyCmdlineEncoderEngine engine;

  @Before
  public void before() {
    engine = new MyCmdlineEncoderEngine();
  }

  @Test
  public void testSplitCommandArgsWithCareWithDoubleQuotes() throws EncoderException {
    final String quotedArg = "\"[0:v:0]pad=iw*2:ih[bg]; [bg][1:v:0]overlay=w\"";
    engine.setCmdlineOptions("-i path/foo.mpg -i path/bar.mpg -filter_complex " + quotedArg + " path/baz.mpg");
    final List<String> list = engine.buildArgumentList(null);
    assertEquals(7, list.size());
    final String quotedArgParsed = list.get(5);
    assertEquals(quotedArg, '"' + quotedArgParsed + '"');
  }

  @Test
  public void testSplitCommandArgsWithCare() throws EncoderException {
    final String lastArg = "bar/baz.mpg";
    engine.setCmdlineOptions("-y -ss 120 -i path/foo.mpg -strict experimental -r 1 -vframes 1 -vf scale=eq(pict_type\\,I),-1:54 -deinterlace -f image2 " + lastArg);
    final List<String> list = engine.buildArgumentList(null);
    assertEquals(17, list.size());
    assertEquals(lastArg, list.get(16));
  }

  @Test
  public void testSplitCommandArgsWithCareSimple() throws EncoderException {
    final String[] args = new String[] {
        "a", "bbb", "c", "ddddddddddd", "98,128"
    };
    engine.setCmdlineOptions(" " + args[0] + "  " + args[1] + "     " + args[2] + "  " + args[3] + "  " + args[4] + "     ");
    final List<String> list = engine.buildArgumentList(null);
    assertEquals(args.length, list.size());
    int index = 0;
    for (final String arg : args) {
      assertEquals(arg, list.get(index++));
    }
  }

  /**
   * This class is needed in order to make {@link AbstractCmdlineEncoderEngine#buildArgumentList(org.opencastproject.composer.api.EncodingProfile)}
   * public and available for testing.
   */
  private final class MyCmdlineEncoderEngine extends AbstractCmdlineEncoderEngine {

    private MyCmdlineEncoderEngine() {
      super("foo");
    }

    @Override
    protected File getOutputFile(File source, EncodingProfile profile) {
      throw new UnsupportedOperationException("Not necessary for unit test");
    }

  }

}
