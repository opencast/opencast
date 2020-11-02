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

package org.opencastproject.mediapackage;


import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Function2;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * HLS-VOD
 *
 * This interface describes methods and fields for an adaptive manifest playlist. as defined in
 * https://tools.ietf.org/html/draft-pantos-http-live-streaming-20 This is text file which references media tracks or
 * playlists in the same mediapackage using relative path names (usual) or absolute URI. Master Playlist tags MUST NOT
 * appear in a Media Playlist; Media Segment tag MUST NOT appear in a Master Playlist.
 */
public interface AdaptivePlaylist extends Track {

  /**
   * Media package element type.
   */
  // String COLLECTION = "AdaptivePlaylist";
  Logger logger = LoggerFactory.getLogger(AdaptivePlaylist.class);

  Pattern uriPatt = Pattern.compile("URI=\"([^\"]+)\"");
  Pattern filePatt = Pattern.compile("([a-zA-Z0-9_.\\-\\/]+)\\.(\\w+)$");
  // Known tags that references other files include the following - but we only use EXT-X-MAP here
  // "#EXT-X-MAP:", "#EXT-X-MEDIA:", "#EXT-X-I-FRAME-STREAM-INF:", "#EXT-X-SESSION-DATA:",
  // Variant tags: see Section 4.4.2 in draft
  List<String> extVariant = new ArrayList<String>(
          Arrays.asList("#EXT-X-MAP:", "#EXT-X-TARGETDURATION:", "EXTINF", "#EXT-X-BYTERANGE:"));
  // Master tags: see Section 4.4.4
  List<String> extMaster = new ArrayList<String>(
          Arrays.asList("#EXT-X-MEDIA:", "#EXT-X-STREAM-INF:", "#EXT-X-I-FRAME-STREAM-INF:", "#EXT-X-SESSION-DATA:"));
  Pattern masterPatt = Pattern.compile(extMaster.stream().collect(Collectors.joining("|")), Pattern.CASE_INSENSITIVE);
  Pattern variantPatt = Pattern.compile(extVariant.stream().collect(Collectors.joining("|")), Pattern.CASE_INSENSITIVE);
  Predicate<String> masterTags = f -> masterPatt.matcher(f) != null;
  Predicate<String> variantTags = f -> variantPatt.matcher(f) != null;
  Predicate<File> isHLSFilePred = f -> "m3u8".equalsIgnoreCase(FilenameUtils.getExtension(f.getName()));
  Predicate<String> isPlaylistPred = f -> "m3u8".equalsIgnoreCase(FilenameUtils.getExtension(f));
  Predicate<Track> isHLSTrackPred = f -> "m3u8".equalsIgnoreCase(FilenameUtils.getExtension(f.getURI().getPath()));

  static boolean isPlaylist(String filename) {
    return filename != null && isPlaylistPred.test(filename);
  }

  static boolean isPlaylist(File file) {
    return file != null && isHLSFilePred.test(file);
  }

  static boolean isPlaylist(Track track) {
    return track != null && isHLSTrackPred.test(track);
  }

  // Return true if any elements in a collection is a m3u8 playlist
  static boolean hasHLSPlaylist(Collection<MediaPackageElement> elements) {
    return elements.stream().filter(e -> e.getElementType() == MediaPackageElement.Type.Track)
            .anyMatch(t -> isHLSTrackPred.test((Track) t));
  }

  static List<Track> getSortedTracks(List<Track> files, boolean segmentsOnly) {
    List<Track> fmp4 = files;
    if (segmentsOnly)
      fmp4 = files.stream().filter(isHLSTrackPred.negate()).collect(Collectors.toList());
    Collections.sort(fmp4, new Comparator<Track>() {
      @Override
      public int compare(Track lhs, Track rhs) {
        // -1 - less than, 1 - greater than, 0 - equal, all inverted for descending
        return FilenameUtils.getBaseName(lhs.getURI().getPath())
                .compareTo(FilenameUtils.getBaseName(rhs.getURI().getPath()));
      }
    });
    return fmp4;
  }

  /**
   * Return true if this is a master manifest (contains variants manifest and no media segments)
   *
   * @param file
   *          - media file
   * @return true if is a master manifest
   * @throws IOException
   *           if bad file
   */
  static boolean checkForMaster(File file) throws IOException {
    if (!isPlaylist(file))
      return false;
    Optional<Matcher> m = Files.lines(file.toPath()).map(masterPatt::matcher).filter(Matcher::find).findFirst();
    if (m.isPresent()) {
      m = null;
      return true;
    }
    return false;
  }

  /**
   * Return true if this is a variant manifest (contains media segments only)
   *
   * @param file
   *          - media file
   * @return true if is a HLS playlist but not master
   * @throws IOException
   *           if bad file - can't access or otherwise
   */
  static boolean checkForVariant(File file) throws IOException {
    if (!isPlaylist(file))
      return false;
    Optional<Matcher> m = Files.lines(file.toPath()).map(variantPatt::matcher).filter(Matcher::find).findFirst();
    if (m.isPresent()) {
      m = null;
      return true;
    }
    return false;
  }

  /**
   * Given a master or variant playlist/manifest - get referenced files. This does not deal with files referenced by
   * tags yet.
   *
   * @param file
   *          to parse
   * @return Set of names referenced
   * @throws IOException
   *           if can't access file
   */
  static Set<String> getVariants(File file) throws IOException {
    Set<String> files = new HashSet<String>();
    try (BufferedReader br = Files.newBufferedReader(file.toPath())) {
      files = (br.lines().map(l -> {
        if (!l.startsWith("#")) {
          Matcher m = filePatt.matcher(l);
          if (m != null && m.matches())
            return m.group(0);
        }
        return null;
      }).collect(Collectors.toSet()));
    } catch (IOException e) {
      throw new IOException("Cannot read file " + file + e.getMessage());
    }
    files.remove(null);
    return files;
  }

  /**
   * Given a playlist - recursively get all referenced files in the same filesystem with relative links
   *
   * @param file
   *          media file
   * @return Set of names referenced
   * @throws IOException
   *           if can't access file
   */
  static Set<String> getReferencedFiles(File file, boolean segmentsOnly) throws IOException {
    Set<String> allFiles = new HashSet<String>(); // don't include playlist variants
    Set<String> segments = getVariants(file).stream().filter(isPlaylistPred.negate())
            .collect(Collectors.toSet());
    Set<String> variants = getVariants(file).stream().filter(isPlaylistPred).collect(Collectors.toSet());

    if (!segmentsOnly)
      allFiles.addAll(variants); // include the playlist
    allFiles.addAll(segments);

    for (String f : variants) {
      try {
        new URL(f); // ignore external paths
      } catch (MalformedURLException e) {
        // is relative path - read the variant playlist
        String name = FilenameUtils.concat(FilenameUtils.getFullPath(file.getAbsolutePath()), f);
        allFiles.addAll(getReferencedFiles(new File(name), true));
      }
    }
    return allFiles;
  }

  /***
   * Set the path of the url as the logical name
   *
   * @param track
   *          - tag with name
   */
  static void setLogicalName(Track track) {
    track.setLogicalName(FilenameUtils.getName(track.getURI().getPath()));
  }

  /**
   * Set HLS Tracks references to point to immediate parent, post inspection
   *
   * @param tracks
   *          - all tracks in an HLS adaptive playlist
   * @param getFileFromURI
   *          - a way to map uri to file
   * @throws IOException
   *           if failed to read files
   */
  static void hlsSetReferences(List<Track> tracks, Function<URI, File> getFileFromURI) throws IOException {
    final Optional<Track> master = tracks.stream().filter(t -> t.isMaster()).findAny();
    final List<Track> variants = tracks.stream().filter(t -> t.getElementType() == MediaPackageElement.Type.Manifest)
            .collect(Collectors.toList());
    final List<Track> segments = tracks.stream().filter(t -> t.getElementType() != MediaPackageElement.Type.Manifest)
            .collect(Collectors.toList());
    tracks.forEach(track -> setLogicalName(track));
    if (master.isPresent())
      variants.forEach(t -> t.referTo(master.get())); // variants refer to master
    HashMap<String, Track> map = new HashMap<String, Track>();
    for (Track t : variants) {
      File f = getFileFromURI.apply(t.getURI());
      Set<String> seg = getReferencedFiles(f, true); // Find segment
      // Should be one only
      seg.forEach(s -> map.put(s, t));
    }
    segments.forEach(t -> { // segments refer to variants
      t.referTo(map.get(t.getLogicalName()));
    });
  }

  /**
   * Fix all the playlists locations and references based on a file map from old name to new name.
   *
   * @param hlsFiles
   *          - List of all files in a playlist including playlists
   * @param map
   *          - the mapping of the references to the actual file location
   * @return the fixed files
   * @throws IOException
   *           if failed to read files
   */
  static List<File> hlsRenameAllFiles(List<File> hlsFiles, Map<File, File> map) throws IOException {
    for (Map.Entry<File, File> entry : map.entrySet()) {
      if (entry.getKey().toPath() != entry.getValue().toPath()) { // if different
        logger.debug("Move file from " + entry.getKey() + " to " + entry.getValue());
        if (entry.getValue().exists())
          FileUtils.forceDelete(entry.getValue()); // can redo this
        FileUtils.moveFile(entry.getKey(), entry.getValue());
      }
    }
    // rename all files to new names if needed
    HashMap<String, String> nameMap = new HashMap<String, String>();
    map.forEach((k, v) -> nameMap.put(k.getName(), v.getName()));
    for (File f : map.values()) {
      if (isPlaylist(f))
        hlsRewriteFileReference(f, nameMap); // fix references
    }
    return new ArrayList<File>(map.values());
  }


  /**
   * Fix all the HLS file references in a manifest when a referenced file is renamed
   *
   * @param srcFile
   *          - srcFile to be rewritten
   * @param mapNames
   *          - mapped from old name to new name
   * @throws IOException
   *           if failed
   */
  static void hlsRewriteFileReference(File srcFile, Map<String, String> mapNames) throws IOException {
    File tmpFile = new File(srcFile.getAbsolutePath() + UUID.randomUUID() + ".tmp");
    try { // rewrite src file
      FileUtils.moveFile(srcFile, tmpFile);
      hlsRewriteFileReference(tmpFile, srcFile, mapNames); // fix references
    } catch (IOException e) {
      throw new IOException("Cannot rewrite " + srcFile + " " + e.getMessage());
    } finally {
      FileUtils.deleteQuietly(tmpFile); // delete temp file
      tmpFile = null;
    }
  }

  /**
   * Fix all the HLS file references in a manifest when a referenced file is renamed All the mapped files should be in
   * the same directory, make sure they are not workspace files (no md5)
   *
   * @param srcFile
   *          - source file to change
   * @param destFile
   *          - dest file to hold results
   * @param mapNames
   *          - mapping from oldName to newName
   * @throws IOException
   *           if failed
   */
  static void hlsRewriteFileReference(File srcFile, File destFile, Map<String, String> mapNames) throws IOException {
    FileWriter hlsReWriter = null;
    BufferedReader br = null;
    // Many tags reference URIs - not all are dealt with in this code, eg:
    // "#EXT-X-MAP:", "#EXT-X-MEDIA:", "#EXT-X-I-FRAME-STREAM-INF:", "#EXT-X-SESSION-DATA:", "#EXT-X-KEY:",
    // "#EXT-X-SESSION-DATA:"
    try {
      hlsReWriter = new FileWriter(destFile.getAbsoluteFile(), false);
      br = new BufferedReader(new FileReader(srcFile));
      String line = null;
      while ((line = br.readLine()) != null) {
        // https://tools.ietf.org/html/draft-pantos-http-live-streaming-20
        // Each line is a URI, blank, or starts with the character #
        if (!line.trim().isEmpty()) {
          if (line.startsWith("#")) {
            // eg: #EXT-X-MAP:URI="39003_segment_0.mp4",BYTERANGE="1325@0"
            if (line.startsWith("#EXT-X-MAP:") || line.startsWith("#EXT-X-MEDIA:")) {
              String tmpLine = line;
              Matcher matcher = uriPatt.matcher(line);
              // replace iff file is mapped
              if (matcher.find() && mapNames.containsKey(matcher.group(1))) {
                tmpLine = line.replaceFirst(matcher.group(1), mapNames.get(matcher.group(1)));
              }
              hlsReWriter.write(tmpLine);
            } else
              hlsReWriter.write(line);
          } else {
            line = line.trim();
            String filename = FilenameUtils.getName(line);
            if (mapNames.containsKey(line)) {
              hlsReWriter.write(mapNames.get(line));
            } else if (mapNames.containsKey(filename)) {
              String newFileName = mapNames.get(FilenameUtils.getName(filename));
              String newPath = FilenameUtils.getPath(line);
              if (newPath.isEmpty())
                hlsReWriter.write(newFileName);
              else
                hlsReWriter.write(FilenameUtils.concat(newPath, newFileName));
            } else
              hlsReWriter.write(line);
          }
        }
        hlsReWriter.write(System.lineSeparator()); // new line
      }
    } catch (Exception e) {
      logger.error("Failed to rewrite hls references " + e.getMessage());
      throw new IOException(e);
    } finally {
      br.close();
      hlsReWriter.close();
      br = null;
      hlsReWriter = null;
    }
  }

  /**
   * Return logical name mapped to file
   *
   * @param tracks
   *          from a HLS manifest
   * @param getFileFromURI
   *          is a function to get file from an URI
   * @return names mapped to file
   */
  static Map<String, File> logicalNameFileMap(List<Track> tracks, Function<URI, File> getFileFromURI) {
    Map<String, File> nameMap = tracks.stream().collect(Collectors.<Track, String, File> toMap(
            track -> track.getLogicalName(), track -> getFileFromURI.apply(track.getURI())));
    return nameMap;
  }

  static Map<String, URI> logicalNameURLMap(List<Track> tracks) {
    HashMap<String, URI> nameMap = new HashMap<String, URI>();
    for (Track track : tracks) {
      nameMap.put(track.getLogicalName(), track.getURI());
    }
    return nameMap;
  }

  /**
   *
   * Return track urls as relative to the master playlist (only one in the list)
   *
   * @param tracks
   *          from an HLS playlist
   * @return track urls as relative to the master playlist
   */
  static HashMap<String, String> urlRelativeToMasterMap(List<Track> tracks) {
    HashMap<String, String> nameMap = new HashMap<String, String>();
    Optional<Track> master = tracks.stream().filter(t -> t.isMaster()).findAny();
    List<Track> others = tracks.stream().filter(t -> !t.isMaster()).collect(Collectors.toList());
    if (master.isPresent()) // Relativize all the files from the master playlist
      others.forEach(track -> {
        nameMap.put(track.getLogicalName(), track.getURI().relativize(master.get().getURI()).toString());
      });
    return nameMap;
  }

  // Representation of a track/playlist for internal use only
  class Rep {
    private Track track;
    private String name; // reference name
    private boolean isPlaylist = false;
    private boolean isMaster = false;
    private File origMpfile;
    private String newfileName;
    private URI origMpuri;
    private URI newMpuri = null;
    private String relPath;

    // Get file relative to the mediapackage directory
    Rep(Track track, File mpdir) throws NotFoundException, IOException {
      this.track = track;
      origMpuri = track.getURI();
      origMpfile = getFilePath(origMpuri, mpdir);
      name = FilenameUtils.getName(origMpuri.getPath()).trim();
      isPlaylist = AdaptivePlaylist.isPlaylist(track.getURI().getPath()); // check suffix
    }

    // Get file based on a look up, eg: workspace.get()
    Rep(Track track, Function<URI, File> getFileFromURI) {
      this.track = track;
      origMpuri = track.getURI();
      origMpfile = getFileFromURI.apply(origMpuri);
      name = FilenameUtils.getName(origMpfile.getPath());
      isPlaylist = AdaptivePlaylist.isPlaylist(track.getURI().getPath());
    }

    private File getFilePath(URI uri, File mpDir) {
      String mpid = mpDir.getName();
      String path = uri.getPath();
      final Matcher matcher = Pattern.compile(mpid).matcher(path);
      if (matcher.find()) {
        return new File(mpDir, path.substring(matcher.end()).trim());
      }
      // If there is no mpDir, it may be a relative path
      return new File(mpDir, path);
    }

    public boolean isMaster() {
      return this.track.isMaster();
    }

    public boolean parseForMaster() {
      try {
        setMaster(checkForMaster(origMpfile));
      } catch (IOException e) {
        logger.error("Cannot open file for check for master:{}", origMpfile);
      }
      return isMaster;
    }

    public void setMaster(boolean isMaster) {
      this.isMaster = isMaster;
      this.track.setMaster(isMaster);
    }

    @Override
    public String toString() {
      return track.toString();
    }
  }

  /**
   * Replace the content of a playlist file in place, use in composer only - not in workspace
   *
   * @param file
   *          as playlist
   * @param map
   *          - mapping from reference/logical name to new path
   * @return playlist with changed file names based on the map
   * @throws IOException
   *           if can't access file
   * @throws NotFoundException
   *           if file not found
   */
  static File replaceTrackFileInPlace(File file, Map<String, String> map) throws IOException, NotFoundException {
    File newFile = new File(file.getAbsolutePath() + UUID.randomUUID() + ".tmp");
    try {
      // move old file to tmp
      FileUtils.moveFile(file, newFile);
      // Write over old file with fixed references
      hlsRewriteFileReference(newFile, file, map);
    } catch (IOException e) {
      logger.error("Cannot rewrite " + file + ": " + e.getMessage());
      throw (e);
    } finally {
      FileUtils.deleteQuietly(newFile); // not needed anymore
      newFile = null;
    }
    return file;

  }

  /**
   * Find relative path to referee URL if a link is in the referer page
   *
   * @param referer
   *          - pointer to file
   * @param referee
   *          - pointee
   * @return referee path as a relative path from referer URL
   * @throws URISyntaxException
   *           if bad URI
   */

  static String relativize(URI referer, URI referee) throws URISyntaxException {
    URI u1 = referer.normalize();
    URI u2 = referee.normalize();
    File f = relativizeF(u1.getPath(), u2.getPath());
    return f.getPath(); // relative name to use in manifest
  }

  // They should all be relative paths at this point in the working file repo
  static File relativizeF(String s1, String s2) throws URISyntaxException {
    String fp = new File(s1).getParent(); // get s1 folder
    Path p2 = Paths.get(s2);
    if (fp != null) {
      Path p1 = Paths.get(fp);
      try {
        Path rp = p1.relativize(p2);
        return rp.toFile();
      } catch (IllegalArgumentException e) {
        logger.info("Not a relative path " + p1 + " to " + p2);
        return p2.toFile();
      }
    } else
      return p2.toFile();
  }

  /**
   * Fix the playlist references in a publication. The playlist files are replaced in place using relative link instead
   * of the filename
   *
   * @param tracks
   *          - tracks that represent a HLS playlist
   * @param mpDir
   *          - distribution media package file directory which represents the file storage of the URI used in the
   *          tracks
   * @return the tracks with the files updated
   * @throws MediaPackageException
   *           if files do not conform to HLS spec.
   * @throws NotFoundException
   *           if files are missing
   * @throws IOException
   *           if can't read
   * @throws URISyntaxException
   *           if bad URI
   */
  static List<Track> fixReferences(List<Track> tracks, File mpDir)
          throws MediaPackageException, NotFoundException, IOException, URISyntaxException {
    HashMap<String, Rep> nameMap = new HashMap<String, Rep>();
    Rep master = null;
    Rep segment = null;
    List<Track> newTracks = new ArrayList<Track>();
    if (tracks.size() < 2) {
      logger.debug("At least 2 files in an HLS distribution");
      throw new MediaPackageException("Not enough files in a playlist");
    }
    // map logical name to track representation
    for (Track track : tracks) {
      Rep rep = new Rep(track, mpDir);
      nameMap.put(track.getLogicalName(), rep); // add all to nameMap
      if (track.isMaster())
        master = rep; // track.getLogicalname();
      if (!rep.isPlaylist)
        segment = rep; // find any segment
    }
    if (segment == null) { // must have at least one segment
      throw new MediaPackageException("No playable media segment in mediapackage");

    }
    // Try to find master or use any playlist, if not found, throw exception
    Optional<Rep> oprep = nameMap.values().stream().filter(r -> r.parseForMaster()).findFirst();
    if (!oprep.isPresent())
      oprep = nameMap.values().parallelStream().filter(r -> r.isPlaylist).findFirst();
    oprep.orElseThrow(() -> new MediaPackageException("No playlist found, not HLS distribution"));
    master = oprep.get();

    HashMap<String, String> newNames = new HashMap<String, String>();
    for (String logName : nameMap.keySet()) { // map original name
      Rep rep = nameMap.get(logName);
      // segments are fixed, fix variant references to segments based on segments
      String relPath;
      if (!segment.origMpuri.equals(rep.origMpuri)) { // not itself
        relPath = relativize(segment.origMpuri, rep.origMpuri);
      } else { // only element id is different
        relPath = relativize(master.origMpuri, rep.origMpuri);
      }
      newNames.put(logName, relPath);
    }
    // on variant playlists, rewrite references to segments
    for (String logName : nameMap.keySet()) {
      Rep rep = nameMap.get(logName);
      if (rep == master) // deal with master later
        continue;
      if (!rep.isPlaylist) {
        newTracks.add(rep.track); // segments are unchanged
        continue;
      }
      replaceTrackFileInPlace(rep.origMpfile, newNames);
      rep.newMpuri = rep.origMpuri;
      newTracks.add(rep.track); // add changed variants
    }
    // remap logical name to the new id for the variant files from above
    for (String logName : nameMap.keySet()) {
      Rep rep = nameMap.get(logName);
      if (!rep.isPlaylist || rep == master)
        continue;
      String relPath = relativize(segment.origMpuri, rep.newMpuri);
      newNames.put(logName, relPath);
    }
    // on master, fix references to variant playlists
    replaceTrackFileInPlace(master.origMpfile, newNames);
    master.newMpuri = master.track.getURI();
    newTracks.add(master.track);
    // Update the logical names to keep referential integrity
    for (Track track : newTracks) {
      String newpath = newNames.get(track.getLogicalName());
      if (newpath != null && track != master) // no file refers to master
        track.setLogicalName(newpath);
    }
    newNames = null;
    return newTracks;
  }

  /**
   * Fix HLS playlists/media already in the workspace as the result of an ingest This builds the hierarchies of a HLS
   * playlist with masters as the roots. This is useful if mixed files are ingested into a mediapackage. HLS files with
   * relative links will fail in an inspection unless the relative paths are fixed. Logical names should be preserved if
   * they exists.
   */
  class HLSMediaPackageCheck {
    private HashMap<String, String> fileMap = new HashMap<String, String>();
    private HashMap<String, Rep> repMap = new HashMap<String, Rep>();;
    private List<Rep> reps;
    private List<Rep> playlists;
    private List<Rep> segments;
    private List<Rep> masters = new ArrayList<Rep>();

    /**
     * Builds a map of files in the mediapackage so that it can be analyzed and fixed if needed
     *
     * @param tracks
     *          - list of tracks from a media package
     * @param getFileFromURI
     *          - a function to get files from the media package by URI
     * @throws IOException
     *           if can't read file
     * @throws URISyntaxException
     *           if bad URI
     * @throws MediaPackageException
     *           - if mediapackage is incomplete and missing segments
     */
    public HLSMediaPackageCheck(List<Track> tracks, Function<URI, File> getFileFromURI)
            throws IOException, MediaPackageException, URISyntaxException {
      this.reps = tracks.stream().map(t -> new Rep(t, getFileFromURI)).collect(Collectors.toList());
      for (Rep rep : reps)
        repMap.put(rep.name, rep);
      this.playlists = reps.stream().filter(r -> r.isPlaylist).collect(Collectors.toList());
      for (Rep trackRep : playlists) {
        if (checkForMaster(trackRep.origMpfile)) {
          this.masters.add(trackRep);
          trackRep.setMaster(true); // Track.master is set by inspection
        }
        mapTracks(trackRep); // find relationships of playlist segments
      }
      this.segments = reps.stream().filter(r -> !r.isPlaylist).collect(Collectors.toList());
      if (this.segments.size() < 1)
        throw new MediaPackageException("No media segments");
    }

    // File references need to be fixed
    public boolean needsRewriting() {
      if (this.playlists.size() == 0) // not HLS
        return false;
      for (String s : fileMap.keySet()) { // paths are already corrected
        if (!s.equals(fileMap.get(s)))
          return true;
      }
      return false;
    }

    /**
     * Rewrite the playlist file from master on down, this has to be done in multiple steps because the act of putting a
     * file into a collection changes the path and new path is not known in advance. The two functions are passed in to
     * this function to manage the tracks in its storage
     *
     * @param mp
     *          to be rewrittem
     * @param replaceTrackFileInWS
     *          A function that creates a new track with the file using the metadata in the track, returning a new track
     *          in the media package.
     * @param removeFromWS
     *          A function that removes() the track from the media package in the workspace
     * @return old tracks that are removed from the media package
     * @throws MediaPackageException
     *           if bad mp
     */

    public List<Track> rewriteHLS(MediaPackage mp, Function2<File, Track, Track> replaceTrackFileInWS,
            Function<Track, Void> removeFromWS) throws MediaPackageException {
      /* rewrite variants first, * segments are unchanged */
      List<Rep> variants = playlists.stream().filter(i -> !masters.contains(i)).collect(Collectors.toList());
      List<File> newFiles = new ArrayList<File>();
      List<Track> oldTracks = new ArrayList<Track>();
      List<Track> newTracks = new ArrayList<Track>();
      Rep rep = segments.get(0); // use segment dir as temp space


      // Lambda to rewrite a track using the passed in functions, using closure
      Function<Rep, Boolean> rewriteTrack = (trackRep) -> {
        File srcFile = trackRep.origMpfile;
        // Use first segment's folder as temp space
        File destFile = new File(rep.origMpfile.getAbsoluteFile().getParent(),
                FilenameUtils.getName(srcFile.getName()));
        try {
          hlsRewriteFileReference(srcFile, destFile, fileMap);
        } catch (IOException e) {
          logger.error("HLS Rewrite {} to {} failed", srcFile, destFile);
          return false;
        }
        newFiles.add(destFile);
        oldTracks.add(trackRep.track);
        Track copyTrack = (Track) trackRep.track.clone(); // get all the properties, id, etc
        mp.add(copyTrack); // add to mp and get new elementID
        Track newTrack = replaceTrackFileInWS.apply(destFile, copyTrack);
        if (newTrack == null) {
          logger.error("Cannot add HLS track tp MP: {}", trackRep.track);
          return false;
        }
        newTracks.add(newTrack);

        try { // Keep track of the new file's relative URI
          fileMap.put(trackRep.relPath, relativize(rep.origMpuri, newTrack.getURI()));
        } catch (URISyntaxException e) {
          logger.error("Cannot rewrite relativize track name: {}", trackRep.track);
          return false;
        }
        newTrack.setLogicalName(fileMap.get(trackRep.name)); // set logical name for publication
        return true;
      };


      try {
        // Rewrite the variants and masters tracks in order and throw exception if there are any failures
        if (!(variants.stream().map(t -> rewriteTrack.apply(t)).allMatch(Boolean::valueOf)
                && masters.stream().map(t -> rewriteTrack.apply(t)).allMatch(Boolean::valueOf)))
          throw new IOException("Cannot rewrite track");

        // if segments are referenced by variant - set the logical name used
        for (Rep segment : segments) {
          if (fileMap.containsValue(segment.newfileName)) {
            segment.track.setLogicalName(segment.newfileName);
          }
        }

        oldTracks.forEach(t -> {
          mp.remove(t);
          removeFromWS.apply(t);
        }); // remove old tracks if successful

      } catch (IOException /* | URISyntaxException */ e) {

        logger.error("Cannot rewrite HLS tracks files:", e);
        newTracks.forEach(t -> {
          mp.remove(t);
          removeFromWS.apply(t);
        }); // remove new Tracks if any of them failed
        throw new MediaPackageException("Cannot rewrite HLS tracks files", e);

      } finally {
        newFiles.forEach(f -> f.delete()); // temp files not needed anymore
      }
      return oldTracks;
    }

    /**
     * Look for track by filename, assuming that all the variant playlists and segments are uniquely named. It is
     * possible that someone ingests a set of published playlists so the paths are nested. Then referenced names are
     * mapped to tracks.
     *
     * @param trackRep
     *          - playlist to examine
     * @throws IOException
     *           - bad files
     * @throws URISyntaxException
     */
    private void mapTracks(Rep trackRep) throws IOException, URISyntaxException {
      Set<String> paths = getVariants(trackRep.origMpfile); // Get all tracks it points to by path
      for (String path : paths) { // Check each file name
        String name = FilenameUtils.getName(path);
        if (repMap.containsKey(name)) {
          Rep rep = repMap.get(name);
          rep.newMpuri = trackRep.track.getURI().relativize(rep.origMpuri);
          rep.newfileName = relativize(trackRep.origMpuri, rep.origMpuri);
          fileMap.put(path, rep.newfileName);
          rep.relPath = path; // for reverse lookup
        } else {
          logger.warn("Adaptive Playlist referenced track not found in mediapackage");
        }
      }
    }
  }
}
