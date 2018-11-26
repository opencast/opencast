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

/**
 * Contains all well-known definitions, names and symbols REPLAY relies on as constants for an easy usage and as a
 * documentation.
 */
public interface MediaPackageElements {

  /** The manifest file name */
  String MANIFEST_FILENAME = "index.xml";

  /** Cover art flavor */
  MediaPackageElementFlavor MEDIAPACKAGE_COVER_FLAVOR = new MediaPackageElementFlavor("cover", "source");

  // Track flavors

  /** Track containing the presenter/s */
  MediaPackageElementFlavor PRESENTER_SOURCE = new MediaPackageElementFlavor("presenter", "source");

  /** Track containing presentational material */
  MediaPackageElementFlavor PRESENTATION_SOURCE = new MediaPackageElementFlavor("presentation", "source");

  /** Track containing the presenter/s partial material */
  MediaPackageElementFlavor PRESENTER_SOURCE_PARTIAL = new MediaPackageElementFlavor("presenter", "source+partial");

  /** Track containing presentational partial material */
  MediaPackageElementFlavor PRESENTATION_SOURCE_PARTIAL = new MediaPackageElementFlavor("presentation",
          "source+partial");

  /** Track capturing the audience */
  MediaPackageElementFlavor AUDIENCE_SOURCE = new MediaPackageElementFlavor("audience", "source");

  /** Track capturing the contents of a document camera */
  MediaPackageElementFlavor DOCUMENTS_SOURCE = new MediaPackageElementFlavor("documents", "source");

  /** Track without any known semantics */
  MediaPackageElementFlavor INDEFINITE_SOURCE = new MediaPackageElementFlavor("indefinite", "source");

  // Dublin core catalog flavors

  /** Dublin core catalog describing an episode */
  MediaPackageElementFlavor EPISODE = new MediaPackageElementFlavor("dublincore", "episode");

  /** Dublin core catalog describing a series */
  MediaPackageElementFlavor SERIES = new MediaPackageElementFlavor("dublincore", "series");

  // Mpeg-7 catalog flavors

  /** The flavor produced by video segmentation */
  MediaPackageElementFlavor SEGMENTS = new MediaPackageElementFlavor("mpeg-7", "segments");

  /** The flavor produced by text extraction */
  MediaPackageElementFlavor TEXTS = new MediaPackageElementFlavor("mpeg-7", "text");

  /** The flavor produced by speech recognition */
  MediaPackageElementFlavor SPEECH = new MediaPackageElementFlavor("mpeg-7", "speech");

  /** A flavor for MPEG-7 chapter catalogs */
  MediaPackageElementFlavor CHAPTERING = new MediaPackageElementFlavor("mpeg-7", "chapter");

  // Engage flavors

  /** Presenter player preview image flavor */
  MediaPackageElementFlavor PRESENTER_PLAYER_PREVIEW = new MediaPackageElementFlavor("presenter", "player+preview");

  /** Presentation player preview image flavor */
  MediaPackageElementFlavor PRESENTATION_PLAYER_PREVIEW = new MediaPackageElementFlavor("presentation",
    "player+preview");

  /** Presenter search result preview image flavor */
  MediaPackageElementFlavor PRESENTER_SEARCHRESULT_PREVIEW = new MediaPackageElementFlavor("presenter",
          "search+preview");

  /** Presentation search result preview image flavor */
  MediaPackageElementFlavor PRESENTATION_SEARCHRESULT_PREVIEW = new MediaPackageElementFlavor("presentation",
          "search+preview");

  /** Presenter segment preview image flavor */
  MediaPackageElementFlavor PRESENTER_SEGMENT_PREVIEW = new MediaPackageElementFlavor("presenter", "segment+preview");

  /** Presentation segment preview image flavor */
  MediaPackageElementFlavor PRESENTATION_SEGMENT_PREVIEW = new MediaPackageElementFlavor("presentation",
          "segment+preview");

  // Feed flavors

  /** Presenter feed preview image flavor */
  MediaPackageElementFlavor PRESENTER_FEED_PREVIEW = new MediaPackageElementFlavor("presenter", "feed+preview");

  /** Presentation feed preview image flavor */
  MediaPackageElementFlavor PRESENTATION_FEED_PREVIEW = new MediaPackageElementFlavor("presentation", "feed+preview");

  // Security flavors

  /** Episode bound XACML policy flavor */
  MediaPackageElementFlavor XACML_POLICY_EPISODE = new MediaPackageElementFlavor("security", "xacml+episode");

  /** Series bound XACML policy flavor */
  MediaPackageElementFlavor XACML_POLICY_SERIES = new MediaPackageElementFlavor("security", "xacml+series");

  // Other flavors

  /** A default flavor for DFXP captions catalogs" */
  MediaPackageElementFlavor CAPTION_GENERAL = new MediaPackageElementFlavor("captions", "timedtext");

  /** A flavor for DFXP caption files */
  MediaPackageElementFlavor CAPTION_DFXP_FLAVOR = new MediaPackageElementFlavor("caption", "dfxp");

  /** OAI-PMH subtype flavor */
  MediaPackageElementFlavor OAIPMH = new MediaPackageElementFlavor("*", "oaipmh");

  /** Comments metadata flavor */
  MediaPackageElementFlavor COMMENTS = new MediaPackageElementFlavor("metadata", "comments");

  /** Notes metadata flavor */
  MediaPackageElementFlavor NOTES = new MediaPackageElementFlavor("metadata", "notes");

  /** Partial SMIL flavor */
  MediaPackageElementFlavor SMIL = new MediaPackageElementFlavor("smil", "source+partial");

  /** Processing properties flavor */
  MediaPackageElementFlavor PROCESSING_PROPERTIES = new MediaPackageElementFlavor("processing", "defaults");

}
