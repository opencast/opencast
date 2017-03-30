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
package org.opencastproject.message.broker.api.assetmanager;

import static com.entwinemedia.fn.Prelude.chuck;

import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.MediaPackageException;
import org.opencastproject.mediapackage.MediaPackageParser;
import org.opencastproject.message.broker.api.MessageItem;
import org.opencastproject.metadata.dublincore.DublinCoreCatalog;
import org.opencastproject.metadata.dublincore.DublinCoreUtil;
import org.opencastproject.metadata.dublincore.DublinCores;
import org.opencastproject.security.api.AccessControlList;
import org.opencastproject.security.api.AccessControlParser;
import org.opencastproject.util.RequireUtil;
import org.opencastproject.workspace.api.Workspace;

import com.entwinemedia.fn.Fn;
import com.entwinemedia.fn.data.Opt;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * {@link Serializable} class that represents all of the possible messages sent through an AssetManager queue.
 */
@ParametersAreNonnullByDefault
public abstract class AssetManagerItem implements MessageItem, Serializable {
  private static final long serialVersionUID = 5440420510139202434L;

  public static final String ASSETMANAGER_QUEUE_PREFIX = "ASSETMANAGER.";

  public static final String ASSETMANAGER_QUEUE = ASSETMANAGER_QUEUE_PREFIX + "QUEUE";

  public enum Type {
    Update, Delete
  }

  // common fields

  private final String mediaPackageId;
  private final Date date;

  private AssetManagerItem(String mediaPackageId, Date date) {
    this.mediaPackageId = RequireUtil.notNull(mediaPackageId, "mediaPackageId");
    this.date = RequireUtil.notNull(date, "date");
  }

  public abstract Type getType();

  public abstract <A> A decompose(Fn<? super TakeSnapshot, ? extends A> takeSnapshot,
          Fn<? super DeleteSnapshot, ? extends A> deleteSnapshot, Fn<? super DeleteEpisode, ? extends A> deleteEpisode);

  public final Date getDate() {
    return date;
  }

  @Override
  public final String getId() {
    return mediaPackageId;
  }

  /*
   * ------------------------------------------------------------------------------------------------------------------
   */

  /**
   * An event for taking a snapshot of a media package.
   */
  public static final class TakeSnapshot extends AssetManagerItem {
    private static final long serialVersionUID = 3530625835200867594L;

    private final String mediapackage;
    private final String acl;
    private final long version;
    private final String episodeDublincore;
    private final String owner;

    private TakeSnapshot(String mediaPackageId, String mediapackage, String episodeDublincore, String acl, long version,
            Date date, String owner) {
      super(mediaPackageId, date);
      this.mediapackage = mediapackage;
      this.episodeDublincore = episodeDublincore;
      this.acl = acl;
      this.version = version;
      this.owner = owner;
    }

    @Override
    public <A> A decompose(Fn<? super TakeSnapshot, ? extends A> takeSnapshot,
            Fn<? super DeleteSnapshot, ? extends A> deleteSnapshot,
            Fn<? super DeleteEpisode, ? extends A> deleteEpisode) {
      return takeSnapshot.apply(this);
    }

    @Override
    public Type getType() {
      return Type.Update;
    }

    public MediaPackage getMediapackage() {
      try {
        return MediaPackageParser.getFromXml(mediapackage);
      } catch (MediaPackageException e) {
        return chuck(e);
      }
    }

    public AccessControlList getAcl() {
      return AccessControlParser.parseAclSilent(acl);
    }

    public Opt<DublinCoreCatalog> getEpisodeDublincore() {
      if (episodeDublincore == null)
        return Opt.none();

      try (InputStream is = IOUtils.toInputStream(episodeDublincore, "UTF-8")) {
        return Opt.some(DublinCores.read(is));
      } catch (IOException e) {
        return chuck(e);
      }
    }

    public long getVersion() {
      return version;
    }

    public String getOwner() {
      return owner;
    }

    //

    public static final Fn<TakeSnapshot, MediaPackage> getMediaPackage = new Fn<TakeSnapshot, MediaPackage>() {
      @Override
      public MediaPackage apply(TakeSnapshot a) {
        return a.getMediapackage();
      }
    };

    public static final Fn<TakeSnapshot, Opt<DublinCoreCatalog>> getEpisodeDublincore = new Fn<TakeSnapshot, Opt<DublinCoreCatalog>>() {
      @Override
      public Opt<DublinCoreCatalog> apply(TakeSnapshot a) {
        return a.getEpisodeDublincore();
      }
    };

    public static final Fn<TakeSnapshot, AccessControlList> getAcl = new Fn<TakeSnapshot, AccessControlList>() {
      @Override
      public AccessControlList apply(TakeSnapshot a) {
        return a.getAcl();
      }
    };

    public static final Fn<TakeSnapshot, Long> getVersion = new Fn<TakeSnapshot, Long>() {
      @Override
      public Long apply(TakeSnapshot a) {
        return a.getVersion();
      }
    };

    public static final Fn<TakeSnapshot, String> getOwner = new Fn<TakeSnapshot, String>() {
      @Override
      public String apply(TakeSnapshot a) {
        return a.getOwner();
      }
    };
  }

  /*
   * ------------------------------------------------------------------------------------------------------------------
   */

  /**
   * An event for deleting a single version of a media package (aka snapshot).
   */
  public static final class DeleteSnapshot extends AssetManagerItem {
    private static final long serialVersionUID = 4797196156230502250L;

    private final long version;

    private DeleteSnapshot(String mediaPackageId, long version, Date date) {
      super(mediaPackageId, date);
      this.version = version;
    }

    @Override
    public <A> A decompose(Fn<? super TakeSnapshot, ? extends A> takeSnapshot,
            Fn<? super DeleteSnapshot, ? extends A> deleteSnapshot,
            Fn<? super DeleteEpisode, ? extends A> deleteEpisode) {
      return deleteSnapshot.apply(this);
    }

    @Override
    public Type getType() {
      return Type.Delete;
    }

    public String getMediaPackageId() {
      return getId();
    }

    public long getVersion() {
      return version;
    }

    public static final Fn<DeleteSnapshot, String> getMediaPackageId = new Fn<DeleteSnapshot, String>() {
      @Override
      public String apply(DeleteSnapshot a) {
        return a.getMediaPackageId();
      }
    };

    public static final Fn<DeleteSnapshot, Long> getVersion = new Fn<DeleteSnapshot, Long>() {
      @Override
      public Long apply(DeleteSnapshot a) {
        return a.getVersion();
      }
    };
  }

  /*
   * ------------------------------------------------------------------------------------------------------------------
   */

  /**
   * A event that will be sent when all versions of a media package (aka the whole episode) have been deleted.
   */
  public static final class DeleteEpisode extends AssetManagerItem {
    private static final long serialVersionUID = -4906056424740181256L;

    private DeleteEpisode(String mediaPackageId, Date date) {
      super(mediaPackageId, date);
    }

    @Override
    public <A> A decompose(Fn<? super TakeSnapshot, ? extends A> takeSnapshot,
            Fn<? super DeleteSnapshot, ? extends A> deleteSnapshot,
            Fn<? super DeleteEpisode, ? extends A> deleteEpisode) {
      return deleteEpisode.apply(this);
    }

    @Override
    public Type getType() {
      return Type.Delete;
    }

    public String getMediaPackageId() {
      return getId();
    }

    public static final Fn<DeleteEpisode, String> getMediaPackageId = new Fn<DeleteEpisode, String>() {
      @Override
      public String apply(DeleteEpisode a) {
        return a.getMediaPackageId();
      }
    };
  }

  /*
   * ------------------------------------------------------------------------------------------------------------------
   */

  //
  // constructor methods
  //

  /**
   * @param workspace
   *          The workspace
   * @param mp
   *          The media package to update.
   * @param acl
   *          The access control list of the media package to update.
   * @param version
   *          The version of the media package.
   * @param date
   *          The modification date.
   * @return Builds a {@link AssetManagerItem} for taking a media package snapshot.
   */
  public static TakeSnapshot add(Workspace workspace, MediaPackage mp, AccessControlList acl, long version, Date date,
          String owner) {
    String dc = null;
    Opt<DublinCoreCatalog> episodeDublincore = DublinCoreUtil.loadEpisodeDublinCore(workspace, mp);
    if (episodeDublincore.isSome()) {
      try {
        dc = episodeDublincore.get().toXmlString();
      } catch (IOException e) {
        throw new IllegalStateException(
                String.format("Not able to serialize the episode dublincore catalog %s.", episodeDublincore.get()), e);
      }
    }
    return new TakeSnapshot(mp.getIdentifier().compact(), MediaPackageParser.getAsXml(mp), dc,
            AccessControlParser.toJsonSilent(acl), version, date, owner);
  }

  /**
   * @param mediaPackageId
   *          The unique id of the media package to delete.
   * @param version
   *          The episode's version.
   * @param date
   *          The modification date.
   * @return Builds {@link AssetManagerItem} for deleting a snapshot from the asset manager.
   */
  public static AssetManagerItem deleteSnapshot(String mediaPackageId, long version, Date date) {
    return new DeleteSnapshot(mediaPackageId, version, date);
  }

  /**
   * @param mediaPackageId
   *          The unique id of the media package to delete.
   * @param date
   *          The modification date.
   * @return Builds {@link AssetManagerItem} for deleting an episode from the asset manager.
   */
  public static AssetManagerItem deleteEpisode(String mediaPackageId, Date date) {
    return new DeleteEpisode(mediaPackageId, date);
  }
}
