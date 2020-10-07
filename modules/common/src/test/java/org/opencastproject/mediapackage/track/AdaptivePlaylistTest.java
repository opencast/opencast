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

package org.opencastproject.mediapackage.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.opencastproject.mediapackage.AdaptivePlaylist;
import org.opencastproject.mediapackage.AdaptivePlaylist.HLSMediaPackageCheck;
import org.opencastproject.mediapackage.DefaultMediaPackageSerializerImpl;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageBuilder;
import org.opencastproject.mediapackage.MediaPackageBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageElementBuilderFactory;
import org.opencastproject.mediapackage.MediaPackageElementFlavor;
import org.opencastproject.mediapackage.MediaPackageElementParser;
import org.opencastproject.mediapackage.MediaPackageElements;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.mediapackage.elementbuilder.TrackBuilderPlugin;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.MimeType;
import org.opencastproject.util.data.Function2;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.persistence.tools.file.FileUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;

/**
 * Test case to Test the implementation of {@link AdaptivePlaylistImpl}.
 */
public class AdaptivePlaylistTest {

  /** The tracks to test */
  private TrackImpl adaptiveplaylist = null;
  private TrackImpl fragmentedFile = null;
  private Unmarshaller unmarshaller;

  /** HLS track url */
  private URI hlsUrl = null;
  private URI fmp4Url = null;

  private File baseDir;
  private File srcDir;
  private File srcFile;
  private File srcFile1;
  private File variantFile;
  private File variantFile1;
  private File playlist;
  private File master2;
  private File mpxml;
  private static final String srcDirName = "src";
  private static final String hlsFileName = "playlist.m3u8";
  private static final String variantFileName = "variant_0.m3u8";
  private static final String variantFileName1 = "variant_1.m3u8";
  private static final String srcFileName = "file_0.mp4";
  private static final String srcFileName1 = "file_1.mp4";
  // 2 variants
  private static final String masterFileName = "master.m3u8";

  private static final String hls_mp_raw = "source-hls-manifest.xml";
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    baseDir = testFolder.newFolder();
    srcDir = new File(baseDir, srcDirName);
    srcFile = new File(srcDir, srcFileName);
    srcFile1 = new File(srcDir, srcFileName1);
    variantFile = new File(srcDir, variantFileName);
    variantFile1 = new File(srcDir, variantFileName1);
    playlist = new File(srcDir, hlsFileName);
    mpxml = new File(srcDir, hls_mp_raw);

    FileUtils.copyURLToFile(this.getClass().getResource("/file_0.mp4"), srcFile);
    FileUtils.copyURLToFile(this.getClass().getResource("/file_1.mp4"), srcFile1);
    // 1 variant
    FileUtils.copyURLToFile(this.getClass().getResource("/master.m3u8"), playlist);
    FileUtils.copyURLToFile(this.getClass().getResource("/variant_0.m3u8"), variantFile);
    FileUtils.copyURLToFile(this.getClass().getResource("/variant_1.m3u8"), variantFile1);
    // 2 variants
    master2 = new File(srcDir, masterFileName);
    FileUtils.copyURLToFile(this.getClass().getResource("/master.m3u8"), master2);
    fmp4Url = new URI("http://downloads.opencastproject.org/media/segment_0.mp4");
    fragmentedFile = TrackImpl.fromURI(fmp4Url);
    hlsUrl = new URI("http://downloads.opencastproject.org/media/playlist.m3u8");
    adaptiveplaylist = TrackImpl.fromURI(hlsUrl);
    JAXBContext context = JAXBContext.newInstance(
            "org.opencastproject.mediapackage:org.opencastproject.mediapackage.track",
            MediaPackage.class.getClassLoader());
    unmarshaller = context.createUnmarshaller();
    // mediapackage with files
    FileUtils.copyURLToFile(this.getClass().getResource("/" + hls_mp_raw), mpxml);
  }

  public Track makeTrack(URI uri, String logicalName) throws JAXBException {
    String xml = "<oc:track xmlns:oc=\"http://mediapackage.opencastproject.org\" type=\"presentation/source\"><oc:tags/><oc:url>"
            + uri + "</oc:url><oc:duration>1</oc:duration><oc:logicalname>" + logicalName
            + "</oc:logicalname></oc:track>";
    InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    TrackImpl track;
    try {
      track = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class).getValue();
    } finally {
      IoSupport.closeQuietly(inputStream);
    }
    return track;
  }

  protected HashMap<File, File> hlsmoveFiles(List<File> files) throws Exception, IOException {
    // rename all the segments to match encoding profile
    HashMap<File, File> outputs = new HashMap<File, File>();
    for (File file : files) {
      File destFile = File.createTempFile(FilenameUtils.getBaseName(file.getName()),
              "." + FilenameUtils.getExtension(file.getName()),
              file.getAbsoluteFile().getParentFile());
      FileUtils.copyFile(file, destFile);
      outputs.put(file, destFile);
    }
    return outputs;
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.track.AdaptivePlaylistImpl#setDuration(long)}.
   */
  @Test
  public void testSetDuration() {
    adaptiveplaylist.setDuration(null);
    adaptiveplaylist.setDuration(new Long(10));
  }

  /**
   * Test method for {@link org.opencastproject.mediapackage.track.AdaptivePlaylistImpl#getDuration()}.
   */
  @Test
  public void testGetDuration() {
    assertEquals(null, adaptiveplaylist.getDuration());
  }

  /**
   * Test method for all the checks
   */
  @Test
  public void testPredicates() throws Exception {
    // Files
    assertTrue(AdaptivePlaylist.checkForMaster(playlist));
    assertTrue(AdaptivePlaylist.checkForVariant(variantFile));
    assertTrue(AdaptivePlaylist.isPlaylist(variantFile));
    assertFalse(AdaptivePlaylist.checkForMaster(variantFile));
    assertFalse(AdaptivePlaylist.checkForMaster(variantFile));
    assertFalse(AdaptivePlaylist.isPlaylist(srcFile));
    // Tracks
    Set<MediaPackageElement> elements = new TreeSet<MediaPackageElement>();
    Assert.assertFalse(AdaptivePlaylist.hasHLSPlaylist(elements));
    Track tm3u8 = makeTrack(variantFile.toURI(), variantFileName);
    assertTrue(AdaptivePlaylist.isHLSTrackPred.test(tm3u8));
    assertTrue(AdaptivePlaylist.isPlaylist(tm3u8));
    Track tmp4 = makeTrack(srcFile.toURI(), srcFileName);
    assertFalse(AdaptivePlaylist.isHLSTrackPred.test(tmp4));
    assertFalse(AdaptivePlaylist.isPlaylist(tmp4));
    elements.add(tmp4);
    Assert.assertFalse(AdaptivePlaylist.hasHLSPlaylist(elements));
    elements.add(tm3u8);
    Assert.assertTrue(AdaptivePlaylist.hasHLSPlaylist(elements));
  }

  @Test
  public void testFileReferences() throws Exception {
    Set<String> variants = AdaptivePlaylist.getVariants(master2);
    assertEquals(variants.size(), 2);
    Set<String> segments = AdaptivePlaylist.getVariants(variantFile);
    assertEquals(segments.size(), 1);
    Set<String> allsegments = AdaptivePlaylist.getReferencedFiles(master2, true);
    assertEquals(allsegments.size(), 2);
    Set<String> allFiles = AdaptivePlaylist.getReferencedFiles(master2, false);
    assertEquals(allFiles.size(), 4);
    List<File> files = new ArrayList<File>();
    files.add(master2);
    files.add(variantFile);
    files.add(variantFile1);
    files.add(srcFile);
    files.add(srcFile1);
    HashMap<File, File> movedFiles = hlsmoveFiles(files);
    HashMap<String, String> nameMap = new HashMap<String, String>();
    movedFiles.forEach((k, v) -> {
      nameMap.put(k.getName(), v.getName());
    });
    for (File f : movedFiles.values()) {
      if (AdaptivePlaylist.isPlaylist(f)) {
        File destFile = File.createTempFile(f.getName(), FilenameUtils.getExtension(f.getName()),
                f.getAbsoluteFile().getParentFile());
        Set<String> oldref = AdaptivePlaylist.getVariants(f);
        AdaptivePlaylist.hlsRewriteFileReference(f, destFile, nameMap);
        variants = AdaptivePlaylist.getVariants(destFile);
        assertTrue(variants.size() > 0);
        // the new name is used in the playlist
        assertTrue(variants.contains(nameMap.get(oldref.toArray()[0])));
      }
    }
    for (File f : movedFiles.values()) {
      FileUtil.delete(f);
    }
  }

  @Test
  public void testFileReWriteReference() throws Exception {
    Set<String> variants = AdaptivePlaylist.getVariants(master2);
    assertEquals(variants.size(), 2);
    Set<String> segments = AdaptivePlaylist.getVariants(variantFile);
    assertEquals(segments.size(), 1);
    Set<String> allsegments = AdaptivePlaylist.getReferencedFiles(master2, true);
    assertEquals(allsegments.size(), 2);
    Set<String> allFiles = AdaptivePlaylist.getReferencedFiles(master2, false);
    assertEquals(allFiles.size(), 4);
    List<File> files = new ArrayList<File>();
    files.add(master2);
    files.add(variantFile);
    files.add(variantFile1);
    files.add(srcFile);
    files.add(srcFile1);
    HashMap<File, File> movedFiles = hlsmoveFiles(files);
    HashMap<String, String> nameMap = new HashMap<String, String>();
    movedFiles.forEach((k, v) -> {
      nameMap.put(k.getName(), v.getName());
    });
    for (File f : movedFiles.values()) {
      if (AdaptivePlaylist.isPlaylist(f)) {
        Set<String> oldref = AdaptivePlaylist.getVariants(f);
        AdaptivePlaylist.hlsRewriteFileReference(f, nameMap);
        variants = AdaptivePlaylist.getVariants(f);
        assertTrue(variants.size() > 0);
        // the new name is used in the playlist
        assertTrue(variants.contains(nameMap.get(oldref.toArray()[0])));
      }
    }
    for (File f : movedFiles.values()) {
      FileUtil.delete(f);
    }
  }

  @Test
  public void testTrackReferences() throws Exception {
    // Set<String> variants = AdaptivePlaylist.getVariants(master2);
    Function<URI, File> getfile = (s) -> new File(s.getPath());
    List<Track> tracks = new ArrayList<Track>();
    tracks.add(makeTrack(master2.toURI(), masterFileName));
    tracks.add(makeTrack(variantFile.toURI(), variantFileName));
    tracks.add(makeTrack(variantFile1.toURI(), variantFileName1));
    tracks.add(makeTrack(srcFile.toURI(), srcFileName));
    tracks.add(makeTrack(srcFile1.toURI(), srcFileName1));
    Map<String, File> hm = AdaptivePlaylist.logicalNameFileMap(tracks, getfile);
    for (Track t : tracks) {
      File f = hm.get(t.getLogicalName());
      assertEquals(f.getPath(), t.getURI().getPath());
    }
  }

  /**
   * Test method for
   * {@link PresenterTrackBuilderPlugin#accept(URI, org.opencastproject.mediapackage.MediaPackageElement.Type, org.opencastproject.mediapackage.MediaPackageElementFlavor)}
   *
   * @throws Exception
   */
  @Test
  public void testPresenterTrackAccept() throws Exception {
    assertTrue(new TrackBuilderPlugin().accept(new URI("uri"), Track.TYPE, MediaPackageElements.PRESENTER_SOURCE));
  }

  @Test
  public void testFlavorMarshalling() throws Exception {
    adaptiveplaylist.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    JAXBContext context = JAXBContext.newInstance(
            "org.opencastproject.mediapackage:org.opencastproject.mediapackage.track",
            MediaPackage.class.getClassLoader());
    Marshaller marshaller = context.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(adaptiveplaylist, writer);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    InputStream inputStream = new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
    try {
      TrackImpl t1 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class)
              .getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t1.getFlavor());
      // Assert.assertTrue(t1.isMaster());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Now again without namespaces
    String xml = "<oc:track xmlns:oc=\"http://mediapackage.opencastproject.org\" type=\"presentation/source\"><oc:tags/><oc:url>"
            + hlsUrl.toString() + "</oc:url><oc:master>true</oc:master><oc:duration>-1</oc:duration></oc:track>";
    inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    try {
      TrackImpl t2 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class).getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t2.getFlavor());
      Assert.assertEquals(hlsUrl, t2.getURI());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Get the xml from the object itself
    String xmlFromTrack = MediaPackageElementParser.getAsXml(adaptiveplaylist);
    Assert.assertTrue(xmlFromTrack.contains(MediaPackageElements.PRESENTATION_SOURCE.toString()));

    // And finally, using the element builder
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

    Track t3 = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
    Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t3.getFlavor());
    Assert.assertEquals(hlsUrl.toURL().toExternalForm(), t3.getURI().toURL().toExternalForm());

  }

  @Test
  public void testLogicalNameMarshalling() throws Exception {
    String logicalName = "segmentFile";
    fragmentedFile.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    fragmentedFile.setLogicalName(logicalName);
    JAXBContext context = JAXBContext.newInstance(
            "org.opencastproject.mediapackage:org.opencastproject.mediapackage.track",
            MediaPackage.class.getClassLoader());
    Marshaller marshaller = context.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(fragmentedFile, writer);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    InputStream inputStream = IOUtils.toInputStream(writer.toString(), "UTF-8");
    try {
      TrackImpl t1 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class).getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t1.getFlavor());
      Assert.assertEquals(logicalName, t1.getLogicalName());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Now again without namespaces
    String xml = "<oc:track xmlns:oc=\"http://mediapackage.opencastproject.org\" type=\"presentation/source\"><oc:tags/><oc:url>"
            + fmp4Url + "</oc:url><oc:duration>-1</oc:duration><oc:logicalname>" + logicalName
            + "</oc:logicalname></oc:track>";
    inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    try {
      TrackImpl t2 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class).getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t2.getFlavor());
      Assert.assertEquals(fmp4Url, t2.getURI());
      Assert.assertEquals(logicalName, t2.getLogicalName());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Get the xml from the object itself
    String xmlFromTrack = MediaPackageElementParser.getAsXml(fragmentedFile);
    Assert.assertTrue(xmlFromTrack.contains(MediaPackageElements.PRESENTATION_SOURCE.toString()));
    Assert.assertTrue(xmlFromTrack.replaceAll("\\b+", "").contains("<logicalname>" + logicalName + "</logicalname>"));

    // And finally, using the element builder
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

    Track t3 = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
    Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t3.getFlavor());
    Assert.assertEquals(fmp4Url, t3.getURI());
    Assert.assertEquals(logicalName, t3.getLogicalName());
  }

  @Test
  public void testMasterMarshalling() throws Exception {
    String logicalName = "masterFile";
    adaptiveplaylist.setFlavor(MediaPackageElements.PRESENTATION_SOURCE);
    adaptiveplaylist.setMaster(true);
    adaptiveplaylist.setLogicalName(logicalName);
    JAXBContext context = JAXBContext.newInstance(
            "org.opencastproject.mediapackage:org.opencastproject.mediapackage.track",
            MediaPackage.class.getClassLoader());
    Marshaller marshaller = context.createMarshaller();
    StringWriter writer = new StringWriter();
    marshaller.marshal(adaptiveplaylist, writer);
    Unmarshaller unmarshaller = context.createUnmarshaller();
    InputStream inputStream = IOUtils.toInputStream(writer.toString(), "UTF-8");
    try {
      TrackImpl t1 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class)
              .getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t1.getFlavor());
      Assert.assertEquals(logicalName, t1.getLogicalName());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Now again without namespaces
    String xml = "<oc:track xmlns:oc=\"http://mediapackage.opencastproject.org\" type=\"presentation/source\"><oc:tags/><oc:url>http://downloads.opencastproject.org/media/movie.m3u8</oc:url><oc:logicalname>"
            + logicalName
            + "</oc:logicalname><oc:master>true</oc:master><oc:duration>-1</oc:duration><oc:live>true</oc:live></oc:track>";
    inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    try {
      TrackImpl t2 = unmarshaller.unmarshal(new StreamSource(inputStream), TrackImpl.class)
              .getValue();
      Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t2.getFlavor());
      Assert.assertEquals("http://downloads.opencastproject.org/media/movie.m3u8", t2.getURI().toString());
      Assert.assertEquals(logicalName, t2.getLogicalName());
      Assert.assertTrue(t2.isMaster());
    } finally {
      IoSupport.closeQuietly(inputStream);
    }

    // Get the xml from the object itself
    String xmlFromTrack = MediaPackageElementParser.getAsXml(adaptiveplaylist);
    Assert.assertTrue(xmlFromTrack.contains(MediaPackageElements.PRESENTATION_SOURCE.toString()));
    Assert.assertFalse(xmlFromTrack.replaceAll("\\b+", "").contains("<live>true</live>"));
    Assert.assertTrue(xmlFromTrack.replaceAll("\\b+", "").contains("<master>true</master>"));

    // And finally, using the element builder
    DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = docBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

    Track t3 = (Track) MediaPackageElementBuilderFactory.newInstance().newElementBuilder()
            .elementFromManifest(doc.getDocumentElement(), new DefaultMediaPackageSerializerImpl());
    Assert.assertEquals(MediaPackageElements.PRESENTATION_SOURCE, t3.getFlavor());
    Assert.assertEquals("http://downloads.opencastproject.org/media/movie.m3u8", t3.getURI().toURL().toExternalForm());
    Assert.assertEquals(logicalName, t3.getLogicalName());
  }

  @Test
  public void testHLSMediaPackageCheck() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    // test resources
    MediaPackage mp;
    Track[] tracks;
    String ingestedDir = "/ingested/"; // an HLS tree structure that needs rewriting
    mp = builder.loadFromXml(new FileInputStream(mpxml));
    tracks = mp.getTracks();
    int numTracks = tracks.length;

    Function<Track, Void> removeFromWS = new Function<Track, Void>() {
      @Override
      public Void apply(Track track) {
        File file = new File(srcDir, track.getURI().getPath());
        FileUtils.deleteQuietly(file);
        return null;
      }
    };

    Function2<File, Track, Track> replaceHLSPlaylistInWS = new Function2<File, Track, Track>() {
      @Override
      public Track apply(File file, Track track) {
        try {
          // put file into workspace
          URI uri = new URI(track.getIdentifier() + "/" + file.getName());
          track.setURI(uri); // point track to new URI
          File newFile = new File(srcDir, uri.getPath());
          FileUtils.copyFile(file, newFile);
          return track;
        } catch (Exception e) {
          return null;
        }
      }
    };
    // Get brand new files from test directory
    Function<URI, File> getFileFromWSURI = new Function<URI, File>() {
      @Override
      public File apply(URI uri) {
        File file = new File(srcDir, uri.getPath());
        URL url = this.getClass().getResource(ingestedDir + uri.getPath());
        try {
          FileUtils.copyURLToFile(url, file);
        } catch (IOException e) {
          return null;
        }
        return file;
      }
    };
    // Get already copied files from junit test directory
    Function<URI, File> getFileFromURI2 = new Function<URI, File>() {
      @Override
      public File apply(URI uri) {
        return new File(srcDir, uri.getPath());
      }
    };

    List<Track> pl = Arrays.asList(tracks).stream().filter(AdaptivePlaylist.isHLSTrackPred)
            .collect(Collectors.toList());
    // Check that metadata are loaded
    for (Track t : pl) {
      Assert.assertEquals(MimeType.mimeType("application", "x-mpegURL"), t.getMimeType());
      t.getMimeType();
      Assert.assertEquals(MediaPackageElementFlavor.flavor("hollandaise", "sauce"), t.getFlavor());
      Assert.assertTrue(1300L == t.getDuration());
    }
    Checksum cs = pl.get(0).getChecksum();

    // Create the object for testing
    HLSMediaPackageCheck hlsCheck = new HLSMediaPackageCheck(Arrays.asList(tracks),
            getFileFromWSURI);
    Assert.assertNotNull(hlsCheck);
    // Check that it read the files correctly and the files need fixing
    Assert.assertTrue(hlsCheck.needsRewriting());

    // Do the rewrite operation which replaces tracks in the WS
    hlsCheck.rewriteHLS(mp, replaceHLSPlaylistInWS, removeFromWS);
    Assert.assertNotNull(hlsCheck);

    tracks = mp.getTracks();
    Assert.assertEquals(tracks.length, numTracks);

    pl = Arrays.asList(tracks).stream().filter(AdaptivePlaylist.isHLSTrackPred).collect(Collectors.toList());
    // Check that metadata are preserved
    for (Track t : pl) {
      Assert.assertEquals(MimeType.mimeType("application", "x-mpegURL"), t.getMimeType());
    }
    for (Track t : tracks) {
      Assert.assertTrue(1300L == t.getDuration());
      Assert.assertEquals(MediaPackageElementFlavor.flavor("hollandaise", "sauce"), t.getFlavor());
      Assert.assertNotNull(t.getLogicalName());
    }
    Assert.assertTrue(cs != pl.get(0).getChecksum());
    for (Track track : pl) {
      Assert.assertTrue(track.getURI().getPath()
              .startsWith(
                      track.getIdentifier() + "/"));
      File file = new File(srcDir + "/" + track.getURI().getPath());
      for (String name : AdaptivePlaylist.getVariants(file)) {
        Assert.assertTrue(name.startsWith("../"));
      }
    }

    // Run it again with fixed tracks, it should know that the references are correct
    hlsCheck = new HLSMediaPackageCheck(Arrays.asList(tracks), getFileFromURI2);
    Assert.assertFalse(hlsCheck.needsRewriting());

  }

  @Test
  public void testHLSMediaPackageCheckUnneeded() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    // test resources
    MediaPackage mp;
    Track[] tracks;
    String ingestedDir = "/ingested_tree/"; // a fixed HLS tree structure
    mp = builder.loadFromXml(new FileInputStream(mpxml));
    tracks = mp.getTracks();
    // Get brand new files from test directory
    Function<URI, File> getFileFromWSURI = new Function<URI, File>() {
      @Override
      public File apply(URI uri) {
        File file = new File(srcDir, uri.getPath());
        URL url = this.getClass().getResource(ingestedDir + uri.getPath());
        try {
          FileUtils.copyURLToFile(url, file);
        } catch (IOException e) {
          return null;
        }
        return file;
      }
    };
    new Function<URI, File>() {
      @Override
      public File apply(URI uri) {
        return new File(srcDir, uri.getPath());
      }
    };

    // Run it with correctly referenced tracks in an WS, it should know that the references are correct
    HLSMediaPackageCheck hlsCheck = new HLSMediaPackageCheck(Arrays.asList(tracks), getFileFromWSURI);
    Assert.assertNotNull(hlsCheck);
    Assert.assertFalse(hlsCheck.needsRewriting());

  }

  /**
   * Repubbed tracks are multileveled and wrongly referenced. It should be correctly fixed.
   * 
   * @throws Exception
   */
  @Test
  public void testHLSMediaPackageRepubRepub() throws Exception {
    MediaPackageBuilder builder = MediaPackageBuilderFactory.newInstance().newMediaPackageBuilder();
    // test resources
    MediaPackage mp;
    Track[] tracks;
    String ingestedDir = "/repubbed_tree/"; // a fixed HLS tree structure
    mp = builder.loadFromXml(new FileInputStream(mpxml));
    tracks = mp.getTracks();
    // Get brand new files from test directory
    Function<URI, File> getFileFromWSURI = new Function<URI, File>() {
      @Override
      public File apply(URI uri) {
        File file = new File(srcDir, uri.getPath());
        URL url = this.getClass().getResource(ingestedDir + uri.getPath());
        try {
          FileUtils.copyURLToFile(url, file);
        } catch (IOException e) {
          return null;
        }
        return file;
      }
    };
    new Function<URI, File>() {
      @Override
      public File apply(URI uri) {
        return new File(srcDir, uri.getPath());
      }
    };
    Function<Track, Void> removeFromWS = new Function<Track, Void>() {
      @Override
      public Void apply(Track track) {
        File file = new File(srcDir, track.getURI().getPath());
        FileUtils.deleteQuietly(file);
        return null;
      }
    };
    // Get already copied files from junit test directory
    Function<URI, File> getFileFromURI2 = new Function<URI, File>() {
      @Override
      public File apply(URI uri) {
        return new File(srcDir, uri.getPath());
      }
    };

    Function2<File, Track, Track> replaceHLSPlaylistInWS = new Function2<File, Track, Track>() {
      @Override
      public Track apply(File file, Track track) {
        try {
          // put file into workspace
          URI uri = new URI(track.getIdentifier() + "/" + file.getName());
          track.setURI(uri); // point track to new URI
          File newFile = new File(srcDir, uri.getPath());
          FileUtils.copyFile(file, newFile);
          return track;
        } catch (Exception e) {
          return null;
        }
      }
    };

    List<Track> pl = Arrays.asList(tracks).stream().filter(AdaptivePlaylist.isHLSTrackPred)
            .collect(Collectors.toList());
    // Check that metadata are loaded
    for (Track t : pl) {
      Assert.assertEquals(MimeType.mimeType("application", "x-mpegURL"), t.getMimeType());
      t.getMimeType();
      Assert.assertEquals(MediaPackageElementFlavor.flavor("hollandaise", "sauce"), t.getFlavor());
      Assert.assertTrue(1300L == t.getDuration());
    }
    Checksum cs = pl.get(0).getChecksum();

    // Create the object for testing
    HLSMediaPackageCheck hlsCheck = new HLSMediaPackageCheck(Arrays.asList(tracks), getFileFromWSURI);
    Assert.assertNotNull(hlsCheck);
    // Check that it read the files correctly and the files need fixing
    Assert.assertTrue(hlsCheck.needsRewriting());

    // Do the rewrite operation which replaces tracks in the WS
    hlsCheck.rewriteHLS(mp, replaceHLSPlaylistInWS, removeFromWS);
    Assert.assertNotNull(hlsCheck);

    tracks = mp.getTracks();

    pl = Arrays.asList(tracks).stream().filter(AdaptivePlaylist.isHLSTrackPred).collect(Collectors.toList());
    // Check that metadata are preserved
    for (Track t : pl) {
      Assert.assertEquals(MimeType.mimeType("application", "x-mpegURL"), t.getMimeType());
    }
    for (Track t : tracks) {
      Assert.assertTrue(1300L == t.getDuration());
      Assert.assertEquals(MediaPackageElementFlavor.flavor("hollandaise", "sauce"), t.getFlavor());
      Assert.assertNotNull(t.getLogicalName());
    }
    Assert.assertTrue(cs != pl.get(0).getChecksum());
    for (Track track : pl) {
      Assert.assertTrue(track.getURI().getPath().startsWith(track.getIdentifier() + "/"));
      File file = new File(srcDir + "/" + track.getURI().getPath());
      for (String name : AdaptivePlaylist.getVariants(file)) {
        Assert.assertTrue(name.startsWith("../"));
      }
    }

    // Run it again with fixed tracks, it should know that the references are correct
    hlsCheck = new HLSMediaPackageCheck(Arrays.asList(tracks), getFileFromURI2);
    Assert.assertFalse(hlsCheck.needsRewriting());

  }
}
