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


package org.opencastproject.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencastproject.util.IoSupport.fileInputStream;
import static org.opencastproject.util.IoSupport.locked;
import static org.opencastproject.util.IoSupport.withFile;
import static org.opencastproject.util.IoSupport.withResource;
import static org.opencastproject.util.data.functions.Misc.chuck;

import org.opencastproject.util.data.Either;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

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

  @TempDir
  private File testFolder;

  /**
   * @throws URISyntaxException
   * @throws IOException
   */
  @BeforeAll
  public static void setUpBeforeClass() throws IOException, URISyntaxException {
    baseDir = new File(IoSupportTest.class.getResource("/").toURI()).getCanonicalFile();
    spacesDir = new File(baseDir, "spaces in name").getCanonicalFile();
    spacesFile = new File(spacesDir, "testfile.txt").getCanonicalFile();
    spacesFileURL = spacesFile.toURI().toURL();
  }

  @BeforeEach
  public void setUp() {
    assertTrue(spacesDir.mkdir(), "Couldn't create directory");
  }

  @AfterEach
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
    Assertions.assertTrue(spacesFile.isFile(),
        "File " + spacesFile.getAbsolutePath() + " was not created or is not valid");
    String line = null;
    StringBuilder sb = new StringBuilder();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(spacesFileURL.openStream()));
      while ((line = br.readLine()) != null) {
        sb.append(line).append('\n');
      }
    } catch (IOException ioe) {
      throw ioe;
    } finally {
      if (br != null) {
        br.close();
      }
    }
    Assertions.assertEquals(sampleText, sb.toString(), "File contents comparison");
  }

  /**
   * Test method for {@link org.opencastproject.util.IoSupport#writeUTF8File(java.io.File, java.lang.String)}.
   *
   * @throws IOException
   */
  @Test
  public void testWriteUTF8FileFileString() throws IOException {
    IoSupport.writeUTF8File(spacesFile, sampleText);
    Assertions.assertTrue(spacesFile.isFile(),
        "File " + spacesFile.getAbsolutePath() + " was not created or is not valid");
    String line = null;
    StringBuilder sb = new StringBuilder();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(spacesFileURL.openStream()));
      while ((line = br.readLine()) != null) {
        sb.append(line).append('\n');
      }
    } catch (IOException ioe) {
      throw ioe;
    } finally {
      if (br != null) {
        br.close();
      }
    }
    Assertions.assertEquals(sampleText, sb.toString(), "File contents comparison");
  }

  @Test
  public void testWithResource() throws Exception {
    assertThrows(IOException.class, () -> {
      final InputStream r = this.getClass().getResourceAsStream("/cover.png");
      final String s = withResource(r, in -> "read");
      assertEquals("read", s);
      // this will throw an IOException
      r.read();
    });
  }

  @Test
  public void testWithResourceLazy() {
    final Function<Exception, String> errorHandler = e -> "error";
    final Either<String, Integer> r = withResource(
        () -> fileInputStream(new File("i-do-not-exist")),
        errorHandler,
        in -> 1
    );
    assertEquals("error", r.left().value());
  }

  @Test
  public void testWithFile() throws Exception {
    final File f1 = new File(this.getClass().getResource("/dublincore.xml").toURI());
    final Optional<String> r1 = withFile(f1, (in, file) -> {
      try {
        return IOUtils.readLines(in, StandardCharsets.UTF_8).get(0);
      } catch (Exception e) {
        return chuck(e);
      }
    });
    assertEquals(Optional.of("<?xml version=\"1.0\"?>"), r1);
    final File f2 = new File("i-do-not-exist");
    final Optional<String> r2 = withFile(f2, (in, file) -> {
      try {
        return IOUtils.readLines(in, StandardCharsets.UTF_8).get(0);
      } catch (Exception e) {
        return chuck(e);
      }
    });
    assertEquals(Optional.empty(), r2);
  }

  @Test
  public void testDeleteLockedFile() throws Exception {
    final File dst = newFile(testFolder, "test.tmp");
    final File src = new File(this.getClass().getResource("/dublincore.xml").toURI());
    locked(dst, file -> {
      try {
        file.delete();
        return FileSupport.copy(src, file);
      } catch (IOException e) {
        return chuck(e);
      }
    });
    assertTrue(dst.isFile());
    assertEquals(src.length(), dst.length());
  }

  @Test
  public void testLockedNotFoundException() throws IOException {
    assertThrows(NotFoundException.class, () -> {
      final File file = IoSupport.file("foo/bar.txt");
      // Just call locked — it should throw NotFoundException because the path does not exist
      IoSupport.locked(file, f ->
        // this lambda won't be executed
          null);
    });
  }

  @Test
  public void testLockedParentDirNotExist() throws IOException {
    assertThrows(NotFoundException.class, () -> {
      File parent = newFolder(testFolder, "foo");
      File file = IoSupport.file(parent.toPath().toString(), "bar/some.txt");

      try {
        // Just call locked — it should throw NotFoundException because "bar" does not exist
        IoSupport.locked(file, f -> null);
      } finally {
        FileUtils.deleteQuietly(parent);
      }
    });
  }

  private static File newFile(File parent, String child) throws IOException {
    File result = new File(parent, child);
    result.createNewFile();
    return result;
  }

  private static File newFolder(File root, String... subDirs) throws IOException {
    String subFolder = String.join("/", subDirs);
    File result = new File(root, subFolder);
    if (!result.mkdirs()) {
      throw new IOException("Couldn't create folders " + root);
    }
    return result;
  }
}
