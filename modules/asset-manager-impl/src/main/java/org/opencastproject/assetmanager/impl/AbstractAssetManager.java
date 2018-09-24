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
package org.opencastproject.assetmanager.impl;

import static com.entwinemedia.fn.Prelude.chuck;
import static com.entwinemedia.fn.Stream.$;
import static java.lang.String.format;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.hasNoChecksum;
import static org.opencastproject.mediapackage.MediaPackageSupport.Filters.isNotPublication;
import static org.opencastproject.mediapackage.MediaPackageSupport.getFileName;
import static org.opencastproject.mediapackage.MediaPackageSupport.getMediaPackageElementId;

import org.opencastproject.assetmanager.api.Asset;
import org.opencastproject.assetmanager.api.AssetId;
import org.opencastproject.assetmanager.api.AssetManager;
import org.opencastproject.assetmanager.api.AssetManagerException;
import org.opencastproject.assetmanager.api.Availability;
import org.opencastproject.assetmanager.api.Property;
import org.opencastproject.assetmanager.api.Snapshot;
import org.opencastproject.assetmanager.api.Version;
import org.opencastproject.assetmanager.api.query.AQueryBuilder;
import org.opencastproject.assetmanager.api.query.ARecord;
import org.opencastproject.assetmanager.api.query.AResult;
import org.opencastproject.assetmanager.impl.persistence.AssetDtos;
import org.opencastproject.assetmanager.impl.persistence.Database;
import org.opencastproject.assetmanager.impl.persistence.SnapshotDto;
import org.opencastproject.assetmanager.impl.query.AQueryBuilderImpl;
import org.opencastproject.assetmanager.impl.storage.AssetStore;
import org.opencastproject.assetmanager.impl.storage.Source;
import org.opencastproject.assetmanager.impl.storage.StoragePath;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageElement;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.mediapackage.MediaPackageSupport;
import org.opencastproject.util.Checksum;
import org.opencastproject.util.ChecksumType;
import org.opencastproject.util.MimeTypes;
import org.opencastproject.util.NotFoundException;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.Fx;
import com.entwinemedia.fn.P1;
import com.entwinemedia.fn.P1Lazy;
import com.entwinemedia.fn.Pred;
import com.entwinemedia.fn.data.Opt;
import com.entwinemedia.fn.fns.Booleans;
import com.entwinemedia.fn.fns.Strings;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

/**
 * Core implementation of the asset manager interface.
 * <p>
 * This implementation features only basic functionality and does not
 * cover security or messaging aspects.
 */
public abstract class AbstractAssetManager implements AssetManager {
  /** Log facility */
  private static final Logger logger = LoggerFactory.getLogger(AbstractAssetManager.class);

  /* ------------------------------------------------------------------------------------------------------------------ */
  //
  // Dependencies
  //

  public abstract Database getDb();

  public abstract HttpAssetProvider getHttpAssetProvider();

  public abstract AssetStore getLocalAssetStore();

  /** The workspace is used to download assets from their URIs. */
  protected abstract Workspace getWorkspace();

  /** Return the organization ID of the currently executing thread. */
  protected abstract String getCurrentOrgId();

  /* ------------------------------------------------------------------------------------------------------------------ */

  @Override public Snapshot takeSnapshot(final String owner, final MediaPackage mp) {
    if (owner == null)
      return takeSnapshot(mp);

    return handleException(new P1Lazy<Snapshot>() {
      @Override public Snapshot get1() {
        try {
          final Snapshot archived = addInternal(owner, MediaPackageSupport.copy(mp)).toSnapshot();
          return getHttpAssetProvider().prepareForDelivery(archived);
        } catch (Exception e) {
          return chuck(e);
        }
      }
    });
  }

  @Override
  public Snapshot takeSnapshot(MediaPackage mediaPackage) {
    final String mediaPackageId = mediaPackage.getIdentifier().toString();
    AQueryBuilder queryBuilder = createQuery();
    AResult result = queryBuilder.select(queryBuilder.snapshot())
            .where(queryBuilder.mediaPackageId(mediaPackageId).and(queryBuilder.version().isLatest())).run();
    Opt<ARecord> record = result.getRecords().head();
    if (record.isSome()) {
      Opt<Snapshot> snapshot = record.get().getSnapshot();
      if (snapshot.isSome()) {
        return takeSnapshot(snapshot.get().getOwner(), mediaPackage);
      }
    }
    return takeSnapshot(DEFAULT_OWNER, mediaPackage);
  }

  @Override public void setAvailability(Version version, String mpId, Availability availability) {
    getDb().setAvailability(RuntimeTypes.convert(version), mpId, availability);
  }

  @Override public boolean setProperty(Property property) {
    return getDb().saveProperty(property);
  }

  @Override public AQueryBuilder createQuery() {
    return new AQueryBuilderImpl(this);
  }

  @Override public Opt<Asset> getAsset(Version version, final String mpId, final String mpeId) {
    // try to fetch the asset
    for (final AssetDtos.Medium asset : getDb().getAsset(RuntimeTypes.convert(version), mpId, mpeId)) {
      for (final InputStream assetStream : getLocalAssetStore().get(StoragePath.mk(asset.getOrganizationId(), mpId, version, mpeId))) {
        final Asset a = new AssetImpl(
                AssetId.mk(version, mpId, mpeId),
                assetStream,
                asset.getAssetDto().getMimeType(),
                asset.getAssetDto().getSize(),
                asset.getStorageId(),
                asset.getAvailability());
        return Opt.some(a);
      }
    }
    return Opt.none();
  }

  @Override public Opt<Version> toVersion(String version) {
    try {
      return Opt.<Version>some(VersionImpl.mk(Long.parseLong(version)));
    } catch (NumberFormatException e) {
      return Opt.none();
    }
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /**
   * Make sure each of the elements has a checksum.
   */
  void calcChecksumsForMediaPackageElements(PartialMediaPackage pmp) {
    pmp.getElements().filter(hasNoChecksum.toFn()).each(addChecksum).run();
  }

  /** Mutates mp and its elements, so make sure to work on a copy. */
  private SnapshotDto addInternal(String owner, final MediaPackage mp) throws Exception {
    final Date now = new Date();
    // claim a new version for the media package
    final String mpId = mp.getIdentifier().toString();
    final VersionImpl version = getDb().claimVersion(mpId);
    logger.info("Creating new version {} of media package {}", version, mp);
    final PartialMediaPackage pmp = assetsOnly(mp);
    // make sure they have a checksum
    calcChecksumsForMediaPackageElements(pmp);
    // download and archive elements
    storeAssets(pmp, version);
    // store mediapackage in db
    final SnapshotDto snapshotDto;
    try {
      rewriteUrisForArchival(pmp, version);
      snapshotDto = getDb().saveSnapshot(getCurrentOrgId(), pmp, now, version, Availability.ONLINE, getLocalAssetStore().getStoreType(), owner);
    } catch (AssetManagerException e) {
      logger.error("Could not take snapshot {}: {}", mpId, e);
      throw new AssetManagerException(e);
    }
    // save manifest to element store
    // this is done at the end after the media package element ids have been rewritten to neutral URNs
    storeManifest(pmp, version);
    return snapshotDto;
  }

  private final Fx<MediaPackageElement> addChecksum = new Fx<MediaPackageElement>() {
    @Override public void apply(MediaPackageElement mpe) {
      File file = null;
      try {
        logger.trace("Calculate checksum for {}", mpe.getURI());
        file = getWorkspace().get(mpe.getURI(), true);
        mpe.setChecksum(Checksum.create(ChecksumType.DEFAULT_TYPE, file));
      } catch (IOException | NotFoundException e) {
        throw new AssetManagerException(format("Cannot calculate checksum for media package element %s", mpe.getURI()), e);
      } finally {
        if (file != null) {
          FileUtils.deleteQuietly(file);
        }
      }
    }
  };

  /**
   * Store all elements of <code>pmp</code> under the given version.
   */
  private void storeAssets(final PartialMediaPackage pmp, final Version version) throws Exception {
    final String mpId = pmp.getMediaPackage().getIdentifier().toString();
    final String orgId = getCurrentOrgId();
    for (final MediaPackageElement e : pmp.getElements()) {
      logger.debug(format("Archiving %s %s %s", e.getFlavor(), e.getMimeType(), e.getURI()));
      final StoragePath storagePath = StoragePath.mk(orgId, mpId, version, e.getIdentifier());
      final Opt<StoragePath> existingAssetOpt = findAssetInVersions(e.getChecksum().toString());
      if (existingAssetOpt.isSome()) {
        final StoragePath existingAsset = existingAssetOpt.get();
        logger.debug("Content of asset {} with checksum {} has been archived before",
                    existingAsset.getMediaPackageElementId(), e.getChecksum());
        if (!getLocalAssetStore().copy(existingAsset, storagePath)) {
          throw new AssetManagerException(
                  format("An asset with checksum %s has already been archived but trying to copy or link asset %s to it failed",
                         e.getChecksum(), existingAsset));
        }
      } else {
        final Opt<Long> size = e.getSize() > 0 ? Opt.some(e.getSize()) : Opt.<Long>none();
        getLocalAssetStore().put(storagePath, Source.mk(e.getURI(), size, Opt.nul(e.getMimeType())));
      }
    }
  }

  /** Check if element <code>e</code> is already part of the history and in the local store. */
  private Opt<StoragePath> findAssetInVersions(final String checksum) throws Exception {
    return getDb().findAssetByChecksum(checksum).filter(new Fn<AssetDtos.Full, Boolean>() {
      @Override public Boolean apply(AssetDtos.Full dto) {
        if (getLocalAssetStore().getStoreType().equals(dto.getStorageId())) {
          return true;
        }
        return false;
      }
    }).map(new Fn<AssetDtos.Full, StoragePath>() {
      @Override public StoragePath apply(AssetDtos.Full dto) {
        return StoragePath.mk(dto.getOrganizationId(), dto.getMediaPackageId(), dto.getVersion(), dto.getAssetDto().getMediaPackageElementId());
      }
    });
  }

  private void storeManifest(final PartialMediaPackage pmp, final Version version) throws Exception {
    final String mpId = pmp.getMediaPackage().getIdentifier().toString();
    final String orgId = getCurrentOrgId();
    // store the manifest.xml
    // TODO make use of checksums
    logger.debug(format("Archiving manifest of media package %s", mpId));
    // temporarily save the manifest XML into the workspace to
    final String manifestFileName = format("manifest_%s.xml", pmp.getMediaPackage().getIdentifier().toString());
    final URI manifestTmpUri = getWorkspace().putInCollection(
            "archive",
            manifestFileName,
            IOUtils.toInputStream(MediaPackageParser.getAsXml(pmp.getMediaPackage()), "UTF-8"));
    try {
      getLocalAssetStore().put(
              StoragePath.mk(orgId, mpId, version, manifestAssetId(pmp, "manifest")),
              Source.mk(manifestTmpUri, Opt.<Long>none(), Opt.some(MimeTypes.XML)));
    } finally {
      // make sure to clean up the temporary file
      getWorkspace().deleteFromCollection("archive", manifestFileName);
    }
  }

  /**
   * Create a unique id for the manifest xml. This is to avoid an id collision in the rare case that the media package
   * contains an XML element with the id used for the manifest. A UUID could also be used but this is far less readable.
   *
   * @param seedId
   *          the id to start with
   */
  private String manifestAssetId(PartialMediaPackage pmp, String seedId) {
    if ($(pmp.getElements()).map(getMediaPackageElementId.toFn()).exists(Booleans.eq(seedId))) {
      return manifestAssetId(pmp, seedId + "_");
    } else {
      return seedId;
    }
  }

  /* ---------------------------------------------------------------------------------------------------------------- */

  /**
   * Unify exception handling by wrapping any occurring exception in an
   * {@link AssetManagerException}.
   */
  static <A> A handleException(final P1<A> p) throws AssetManagerException {
    try {
      return p.get1();
    } catch (Exception e) {
      logger.error("An error occurred", e);
      throw unwrapExceptionUntil(AssetManagerException.class, e).getOr(new AssetManagerException(e));
    }
  }

  /**
   * Walk up the stacktrace to find a cause of type <code>type</code>. Return none if no such
   * type can be found.
   */
  static <A extends Throwable> Opt<A> unwrapExceptionUntil(Class<A> type, Throwable e) {
    if (e == null) {
      return Opt.none();
    } else if (type.isAssignableFrom(e.getClass())) {
      return Opt.some((A) e);
    } else {
      return unwrapExceptionUntil(type, e.getCause());
    }
  }

  /**
   * Return a partial media package filtering assets. Assets are elements the archive is going to manager, i.e. all
   * non-publication elements.
   */
  static PartialMediaPackage assetsOnly(MediaPackage mp) {
    return PartialMediaPackage.mk(mp, isAsset);
  }

  static final Pred<MediaPackageElement> isAsset = Pred.mk(isNotPublication.toFn());

  /**
   * Create a URN for a media package element of a certain version.
   * Use this URN for the archived media package.
   */
  static Fn<MediaPackageElement, URI> createUrn(final String mpId, final Version version) {
    return new Fn<MediaPackageElement, URI>() {
      @Override
      public URI apply(MediaPackageElement mpe) {
        try {
          String fileName = getFileName(mpe).getOr("unknown");
          return new URI("urn", "matterhorn:" + mpId + ":" + version + ":" + mpe.getIdentifier() + ":" + fileName, null);
        } catch (URISyntaxException e) {
          throw new AssetManagerException(e);
        }
      }
    };
  }

  /**
   * Extract the file name from a media package elements URN.
   *
   * @return the file name or none if it could not be determined
   */
  public static Opt<String> getFileNameFromUrn(MediaPackageElement mpe) {
    Opt<URI> uri = Opt.nul(mpe.getURI());
    if (uri.isSome() && "urn".equals(uri.get().getScheme()))
      return uri.toStream().map(toString).bind(Strings.split(":")).drop(1).reverse().head();
    return Opt.none();
  }

  private static final Fn<URI, String> toString = new Fn<URI, String>() {
    @Override
    public String apply(URI uri) {
      return uri.toString();
    }
  };

  /**
   * Rewrite URIs of assets of media package elements. Please note that this method modifies the given media package.
   */
  static void rewriteUrisForArchival(PartialMediaPackage pmp, Version version) {
    rewriteUris(pmp, createUrn(pmp.getMediaPackage().getIdentifier().toString(), version));
  }

  /**
   * Rewrite URIs of all asset elements of a media package.
   * Please note that this method modifies the given media package.
   */
  static void rewriteUris(PartialMediaPackage pmp, Fn<MediaPackageElement, URI> uriCreator) {
    for (MediaPackageElement mpe : pmp.getElements()) {
      mpe.setURI(uriCreator.apply(mpe));
    }
  }

  /* ------------------------------------------------------------------------------------------------------------------ */

  /**
   * Rewrite URIs of all asset elements of a snapshot's media package.
   * This method does not mutate anything.
   */
  public static Snapshot rewriteUris(Snapshot snapshot, Fn<MediaPackageElement, URI> uriCreator) {
    final MediaPackage mpCopy = MediaPackageSupport.copy(snapshot.getMediaPackage());
    for (final MediaPackageElement mpe : assetsOnly(mpCopy).getElements()) {
      mpe.setURI(uriCreator.apply(mpe));
    }
    return new SnapshotImpl(
        snapshot.getVersion(),
        snapshot.getOrganizationId(),
        snapshot.getArchivalDate(),
        snapshot.getAvailability(),
        snapshot.getStorageId(),
        snapshot.getOwner(),
        mpCopy);
  }
}
