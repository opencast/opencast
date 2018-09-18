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
package org.opencastproject.scheduler.impl;

import org.opencastproject.mediapackage.identifier.Id;
import org.opencastproject.util.FileSupport;
import org.opencastproject.util.IoSupport;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.util.data.Option;
import org.opencastproject.workspace.api.Workspace;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.UUID;

/**
 * A {@link Workspace} implementation suitable for unit tests.
 * <p/>
 * Call {@link #clean()} on test tear down to remove files again.
 */
public class UnitTestWorkspace implements Workspace {
  private static final Logger logger = LoggerFactory.getLogger(UnitTestWorkspace.class);

  private final File baseDir;

  public UnitTestWorkspace() {
    baseDir = SchedulerServiceImplTest.baseDir;
    logger.info("Creating workspace under " + baseDir.getAbsolutePath());
    baseDir.mkdirs();
    clean();
    baseDir.deleteOnExit();
  }

  public void clean() {
    logger.info("Cleaning workspace " + baseDir.getAbsolutePath());
    FileSupport.deleteQuietly(baseDir, true);
  }

  @Override
  public File get(URI uri) throws NotFoundException, IOException {
    return get(uri, false);
  }

  @Override
  public File get(URI uri, boolean uniqueFilename) throws NotFoundException, IOException {
    File src = new File(uri);
    if (uniqueFilename) {
      File target = new File(baseDir, UUID.randomUUID().toString());
      FileUtils.copyFile(src, target);
      return target;
    }
    return src;
  }

  @Override
  public InputStream read(URI uri) throws NotFoundException, IOException {
    return new FileInputStream(get(uri));
  }

  @Override
  public URI getBaseUri() {
    return baseDir.toURI();
  }

  @Override
  public URI put(String mediaPackageID, String mediaPackageElementID, String fileName, InputStream in)
          throws IOException, IllegalArgumentException {
    final File file = IoSupport.file(baseDir.getAbsolutePath(), mediaPackageID, mediaPackageElementID, fileName);
    file.getParentFile().mkdirs();
    FileUtils.copyInputStreamToFile(in, file);
    return file.toURI();
  }

  @Override
  public URI putInCollection(String collectionId, String fileName, InputStream in)
          throws IOException, IllegalArgumentException {
    final File file = IoSupport.file(baseDir.getAbsolutePath(), "COLLECTIONS", collectionId, fileName);
    file.getParentFile().mkdirs();
    FileUtils.copyInputStreamToFile(in, file);
    return file.toURI();
  }

  @Override
  public URI[] getCollectionContents(String collectionId) throws NotFoundException, IllegalArgumentException {
    return new URI[0];
  }

  @Override
  public void delete(URI uri) throws NotFoundException, IOException {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public void delete(String mediaPackageID, String mediaPackageElementID) throws NotFoundException, IOException {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public void deleteFromCollection(String collectionId, String fileName) throws NotFoundException, IOException {
    final File file = IoSupport.file(baseDir.getAbsolutePath(), "COLLECTIONS", collectionId, fileName);
    if (file.isFile()) {
      if (!file.delete()) {
        throw new IOException("Cannot delete file " + file.getAbsolutePath());
      }
    } else {
      throw new NotFoundException("Cannot find file " + file.getAbsolutePath());
    }
  }

  @Override
  public void deleteFromCollection(String collectionId, String fileName, boolean removeCollection) throws NotFoundException, IOException {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public URI getURI(String mediaPackageID, String mediaPackageElementID) throws IllegalArgumentException {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public URI getURI(String mediaPackageID, String mediaPackageElementID, String filename)
          throws IllegalArgumentException {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public URI getCollectionURI(String collectionID, String fileName) throws IllegalArgumentException {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public URI moveTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
          throws NotFoundException, IOException, IllegalArgumentException {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public URI copyTo(URI collectionURI, String toMediaPackage, String toMediaPackageElement, String toFileName)
          throws NotFoundException, IOException, IllegalArgumentException {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public void cleanup(int maxAge) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public Option<Long> getTotalSpace() {
    return Option.none();
  }

  @Override
  public Option<Long> getUsableSpace() {
    return Option.none();
  }

  @Override
  public Option<Long> getUsedSpace() {
    return Option.none();
  }

  @Override
  public void cleanup(Id mediaPackageId) throws IOException {
    // Nothing to do
  }

  @Override
  public void cleanup(Id mediaPackageId, boolean filesOnly) throws IOException {
    // Nothing to do
  }

  @Override
  public String rootDirectory() {
    return null;
  }
}
