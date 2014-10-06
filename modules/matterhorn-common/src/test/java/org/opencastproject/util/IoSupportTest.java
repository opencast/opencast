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

package org.opencastproject.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.opencastproject.util.IoSupport.fileInputStream;
import static org.opencastproject.util.IoSupport.locked;
import static org.opencastproject.util.IoSupport.withFile;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.data.Option.none;
import static org.opencastproject.util.data.Option.some;

import org.opencastproject.util.data.Effect;
import org.opencastproject.util.data.Either;
import org.opencastproject.util.data.Function;
import org.opencastproject.util.data.Function2;
import org.opencastproject.util.data.Option;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Test for the IoSupporTest class
 */
public class IoSupportTest {

  private static File baseDir;
  private static File spacesDir;
  private static File spacesFile;
  private static URL spacesFileURL;
  private static final String sampleText = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme,\n"
          + "no ha mucho que vivia un hidalgo de los de lanza en astillero,\n"
          + "adarga antigua, rocin flaco y galgo corredor.\n\n"
          + "                                 Miguel de Cervantes Saavedra\n"
          + "                      \"El ingenioso hidalgo don Quijote de la Mancha\"\n";

  /**
   * @throws URISyntaxException
   * @throws IOException
   */
  @BeforeClass
  public static void setUpBeforeClass() throws IOException, URISyntaxException {
    baseDir = new File(IoSupportTest.class.getResource("/").toURI()).getCanonicalFile();
    spacesDir = new File(baseDir, "spaces in name").getCanonicalFile();
    spacesFile = new File(spacesDir, "testfile.txt").getCanonicalFile();
    spacesFileURL = spacesFile.toURI().toURL();
  }

  @Before
  public void setUp() {
    assertTrue("Couldn't create directory", spacesDir.mkdir());
  }

  @After
  public void tearDown() throws IOException {
    FileUtils.deleteDirectory(spacesDir);
  }

  /**
   * Test method for {@link org.opencastproject.util.IoSupport#writeUTF8File(java.net.URL, java.lang.String)}.
   *
   * @throws IOException
   */
  @Test
  public void testWriteUTF8FileURLString() throws IOException {
    IoSupport.writeUTF8File(spacesFileURL, sampleText);
    Assert.assertTrue("File " + spacesFile.getAbsolutePath() + " was not created or is not valid", spacesFile.isFile());
    String line = null;
    StringBuilder sb = new StringBuilder();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(spacesFileURL.openStream()));
      while ((line = br.readLine()) != null)
        sb.append(line).append('\n');
    } catch (IOException ioe) {
      throw ioe;
    } finally {
      if (br != null)
        br.close();
    }
    Assert.assertEquals("File contents comparison", sampleText, sb.toString());
  }

  /**
   * Test method for {@link org.opencastproject.util.IoSupport#writeUTF8File(java.io.File, java.lang.String)}.
   *
   * @throws IOException
   */
  @Test
  public void testWriteUTF8FileFileString() throws IOException {
    IoSupport.writeUTF8File(spacesFile, sampleText);
    Assert.assertTrue("File " + spacesFile.getAbsolutePath() + " was not created or is not valid", spacesFile.isFile());
    String line = null;
    StringBuilder sb = new StringBuilder();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(spacesFileURL.openStream()));
      while ((line = br.readLine()) != null)
        sb.append(line).append('\n');
    } catch (IOException ioe) {
      throw ioe;
    } finally {
      if (br != null)
        br.close();
    }
    Assert.assertEquals("File contents comparison", sampleText, sb.toString());
  }

  @Test(expected = IOException.class)
  public void testWithResource() throws Exception {
    final InputStream r = this.getClass().getResourceAsStream("/cover.png");
    final String s = withResource(r, new Function<InputStream, String>() {
      @Override
      public String apply(InputStream in) {
        return "read";
      }
    });
    assertEquals("read", s);
    // this will throw an IOException
    r.read();
  }

  @Test
  public void testWithResourceLazy() {
    final Function<Exception, String> errorHandler = new Function<Exception, String>() {
      @Override
      public String apply(Exception e) {
        return "error";
      }
    };
    final Either<String, Integer> r = withResource(fileInputStream(new File("i-do-not-exist")), errorHandler,
            new Function<InputStream, Integer>() {
              @Override
              public Integer apply(InputStream in) {
                return 1;
              }
            });
    assertEquals("error", r.left().value());
  }

  @Test
  public void testWithFile() throws Exception {
    final File f1 = new File(this.getClass().getResource("/dublincore.xml").toURI());
    final Option<String> r1 = withFile(f1, new Function2.X<InputStream, File, String>() {
      @Override
      public String xapply(InputStream in, File file) throws IOException {
        return IOUtils.readLines(in).get(0);
      }
    });
    assertEquals(some("<?xml version=\"1.0\"?>"), r1);
    final File f2 = new File("i-do-not-exist");
    final Option<String> r2 = withFile(f2, new Function2.X<InputStream, File, String>() {
      @Override
      public String xapply(InputStream in, File file) throws IOException {
        return IOUtils.readLines(in).get(0);
      }
    });
    assertEquals(none(String.class), r2);
  }

  @Test
  public void testDeleteLockedFile() throws Exception {
    final File dst = File.createTempFile("test", "tmp");
    final File src = new File(this.getClass().getResource("/dublincore.xml").toURI());
    dst.deleteOnExit();
    locked(dst, new Effect.X<File>() {
      @Override
      protected void xrun(File file) throws IOException {
        file.delete();
        FileSupport.copy(src, file);
      }
    });
    assertTrue(dst.isFile());
    assertEquals(src.length(), dst.length());
  }
}
