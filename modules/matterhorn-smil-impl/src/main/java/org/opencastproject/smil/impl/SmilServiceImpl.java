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
package org.opencastproject.smil.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.opencastproject.mediapackage.MediaPackage;
import org.opencastproject.mediapackage.Track;
import org.opencastproject.smil.api.SmilException;
import org.opencastproject.smil.api.SmilResponse;
import org.opencastproject.smil.api.SmilService;
import org.opencastproject.smil.entity.SmilBodyImpl;
import org.opencastproject.smil.entity.SmilHeadImpl;
import org.opencastproject.smil.entity.SmilImpl;
import org.opencastproject.smil.entity.SmilObjectImpl;
import org.opencastproject.smil.entity.api.Smil;
import org.opencastproject.smil.entity.api.SmilMeta;
import org.opencastproject.smil.entity.api.SmilObject;
import org.opencastproject.smil.entity.media.container.SmilMediaParallelImpl;
import org.opencastproject.smil.entity.media.container.SmilMediaSequenceImpl;
import org.opencastproject.smil.entity.media.container.api.SmilMediaContainer;
import org.opencastproject.smil.entity.media.element.SmilMediaAudioImpl;
import org.opencastproject.smil.entity.media.element.SmilMediaElementImpl;
import org.opencastproject.smil.entity.media.element.SmilMediaReferenceImpl;
import org.opencastproject.smil.entity.media.element.SmilMediaVideoImpl;
import org.opencastproject.smil.entity.media.param.SmilMediaParamGroupImpl;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParam;
import org.opencastproject.smil.entity.media.param.api.SmilMediaParamGroup;

/**
 * Implement {@link SmilService} interface.
 */
public class SmilServiceImpl implements SmilService {

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse createNewSmil() {
    return new SmilResponseImpl(new SmilImpl());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse createNewSmil(MediaPackage mediaPackage) {
    SmilImpl smil = new SmilImpl();
    ((SmilHeadImpl) smil.getHead()).addMeta(
            SmilMeta.SMIL_META_NAME_MEDIA_PACKAGE_ID,
            mediaPackage.getIdentifier().compact());
    return new SmilResponseImpl(smil);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse addParallel(Smil smil) throws SmilException {
    return addParallel(smil, smil.getBody().getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse addParallel(Smil smil, String parentId) throws SmilException {
    SmilMediaContainer par = new SmilMediaParallelImpl();
    ((SmilBodyImpl) smil.getBody()).addMediaElement(par, parentId);
    return new SmilResponseImpl(smil, par);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse addSequence(Smil smil) throws SmilException {
    return addSequence(smil, smil.getBody().getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse addSequence(Smil smil, String parentId) throws SmilException {
    SmilMediaContainer seq = new SmilMediaSequenceImpl();
    ((SmilBodyImpl) smil.getBody()).addMediaElement(seq, parentId);
    return new SmilResponseImpl(smil, seq);
  }

  @Override
  public SmilResponse addClip(Smil smil, String parentId, Track track, long start, long duration) throws SmilException {
     return addClip(smil, parentId, track, start, duration, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse addClip(Smil smil, String parentId, Track track, long start, long duration, String pgId) throws SmilException {
    if (start < 0) {
      throw new SmilException("Start position should be positive.");
    }
    if (duration < 0) {
      throw new SmilException("Duration should be positive.");
    }
    if (track.getURI() == null) {
      throw new SmilException("Track URI isn't set.");
    }
    if (track.getFlavor() == null) {
      throw new SmilException("Track flavor isn't set.");
    }

    if (track.getDuration() != null) {
      if (!track.hasAudio() && !track.hasVideo()) {
        throw new SmilException("Track should have at least one audio or video stream.");
      }
      if (start >= track.getDuration()) {
        throw new SmilException("Start value is bigger than track length.");
      }
      if (start + duration > track.getDuration()) {
        duration = track.getDuration() - start;
      }
    }

    SmilMediaParamGroup trackParamGroup = null;
    for (SmilMediaParamGroup paramGroup : smil.getHead().getParamGroups()) {
      // support for adding multiple tracks to the same param group
      if (pgId != null && paramGroup.getId().equals(pgId)) {
        trackParamGroup = paramGroup;
        break;
      }
      SmilMediaParam param = ((SmilMediaParamGroupImpl) paramGroup).getParamByName(SmilMediaParam.PARAM_NAME_TRACK_ID);
      if (param != null && param.getValue().equals(track.getIdentifier())) {
        trackParamGroup = paramGroup;
        break;
      }
    }
    boolean newTrack = trackParamGroup == null;
    if (newTrack) {
      // add paramgroup for new Track
      trackParamGroup = new SmilMediaParamGroupImpl();
      ((SmilMediaParamGroupImpl) trackParamGroup).addParam(SmilMediaParam.PARAM_NAME_TRACK_ID, track.getIdentifier());
      ((SmilMediaParamGroupImpl) trackParamGroup).addParam(SmilMediaParam.PARAM_NAME_TRACK_SRC, track.getURI().toString());
      ((SmilMediaParamGroupImpl) trackParamGroup).addParam(SmilMediaParam.PARAM_NAME_TRACK_FLAVOR, track.getFlavor().toString());
      ((SmilHeadImpl) smil.getHead()).addParamGroup(trackParamGroup);
    }

    SmilMeta durationMeta = null;
    for (SmilMeta meta : smil.getHead().getMetas()) {
      if (SmilMeta.SMIL_META_NAME_TRACK_DURATION.equals(meta.getName())) {
        durationMeta = meta;
        break;
      }
    }

    if (track.getDuration() != null) {
      // set track-duration meta if not set or the trackduration is longer than old value
      if (durationMeta == null) {
        ((SmilHeadImpl) smil.getHead()).addMeta(SmilMeta.SMIL_META_NAME_TRACK_DURATION,
                String.format("%dms", track.getDuration()));
      } else {
        long durationOld = Long.parseLong(durationMeta.getContent().replace("ms", ""));
        if (track.getDuration() > durationOld) {
          ((SmilHeadImpl) smil.getHead()).addMeta(SmilMeta.SMIL_META_NAME_TRACK_DURATION,
                  String.format("%dms", track.getDuration()));
        }
      }
    }

    SmilMediaElementImpl media = null;
    if (track.hasVideo()) {
      media = new SmilMediaVideoImpl(track.getURI(), start, start + duration);
    } else if (track.hasAudio()) {
      media = new SmilMediaAudioImpl(track.getURI(), start, start + duration);
    } else {
      media = new SmilMediaReferenceImpl(track.getURI(), start, start + duration);
    }
    media.setParamGroup(trackParamGroup.getId());
    if (parentId == null || "".equals(parentId)) {
      parentId = smil.getBody().getId();
    }

    // add new media element
    ((SmilBodyImpl) smil.getBody()).addMediaElement(media, parentId);
    if (newTrack) {
      return new SmilResponseImpl(smil, new SmilObject[]{media, trackParamGroup});
    } else {
      return new SmilResponseImpl(smil, media);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse addClips(Smil smil, String parentId, Track[] tracks, long start, long duration) throws SmilException {
    List<SmilObject> trackEntities = new ArrayList<SmilObject>(tracks.length);
    List<SmilObject> otherEntities = new ArrayList<SmilObject>(tracks.length);
    for (Track track : tracks) {
      // add single clip and collect entities from response
      SmilResponse response = addClip(smil, parentId, track, start, duration);
      if (response.getEntitiesCount() == 1) {
        trackEntities.add(response.getEntity());
      } else {
        trackEntities.add(response.getEntities()[0]);
        for (int e = 1; e < response.getEntitiesCount(); e++) {
          otherEntities.add(response.getEntities()[e]);
        }
      }
    }
    // merge entities (track entities first)
    SmilObject[] entities = new SmilObject[trackEntities.size() + otherEntities.size()];
    for (int e = 0; e < entities.length; e++) {
      entities[e] = (e < trackEntities.size()
              ? trackEntities.get(e)
              : otherEntities.get(e - trackEntities.size()));
    }
    // create new response
    return new SmilResponseImpl(smil, entities);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse addMeta(Smil smil, String name, String content) {
    SmilMeta meta = ((SmilHeadImpl) smil.getHead()).addMeta(name, content);
    return new SmilResponseImpl(smil, meta);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse removeSmilElement(Smil smil, String elementId) {
    SmilObject removed = ((SmilObjectImpl) smil).removeElement(elementId);
    if (removed == null) {
      return new SmilResponseImpl(smil);
    } else {
      return new SmilResponseImpl(smil, removed);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse fromXml(String smilXml) throws SmilException {
    try {
      return new SmilResponseImpl(SmilImpl.fromXML(smilXml));
    } catch (JAXBException ex) {
      throw new SmilException("Parsing SMIL document failed.", ex);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SmilResponse fromXml(File smilXmlFile) throws SmilException {
    try {
      return new SmilResponseImpl(SmilImpl.fromXML(smilXmlFile));
    } catch (JAXBException ex) {
      throw new SmilException("Parsing SMIL document failed.", ex);
    }
  }
}
