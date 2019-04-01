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
 * http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */


package org.opencastproject.mediapackage

/**
 * Contains all well-known definitions, names and symbols REPLAY relies on as constants for an easy usage and as a
 * documentation.
 */
interface MediaPackageElements {
    companion object {

        /** The manifest file name  */
        val MANIFEST_FILENAME = "index.xml"

        /** Cover art flavor  */
        val MEDIAPACKAGE_COVER_FLAVOR = MediaPackageElementFlavor("cover", "source")

        // Track flavors

        /** Track containing the presenter/s  */
        val PRESENTER_SOURCE = MediaPackageElementFlavor("presenter", "source")

        /** Track containing presentational material  */
        val PRESENTATION_SOURCE = MediaPackageElementFlavor("presentation", "source")

        /** Track containing the presenter/s partial material  */
        val PRESENTER_SOURCE_PARTIAL = MediaPackageElementFlavor("presenter", "source+partial")

        /** Track containing presentational partial material  */
        val PRESENTATION_SOURCE_PARTIAL = MediaPackageElementFlavor("presentation",
                "source+partial")

        /** Track capturing the audience  */
        val AUDIENCE_SOURCE = MediaPackageElementFlavor("audience", "source")

        /** Track capturing the contents of a document camera  */
        val DOCUMENTS_SOURCE = MediaPackageElementFlavor("documents", "source")

        /** Track without any known semantics  */
        val INDEFINITE_SOURCE = MediaPackageElementFlavor("indefinite", "source")

        // Dublin core catalog flavors

        /** Dublin core catalog describing an episode  */
        val EPISODE = MediaPackageElementFlavor("dublincore", "episode")

        /** Dublin core catalog describing a series  */
        val SERIES = MediaPackageElementFlavor("dublincore", "series")

        // Mpeg-7 catalog flavors

        /** The flavor produced by video segmentation  */
        val SEGMENTS = MediaPackageElementFlavor("mpeg-7", "segments")

        /** The flavor produced by text extraction  */
        val TEXTS = MediaPackageElementFlavor("mpeg-7", "text")

        /** The flavor produced by speech recognition  */
        val SPEECH = MediaPackageElementFlavor("mpeg-7", "speech")

        /** A flavor for MPEG-7 chapter catalogs  */
        val CHAPTERING = MediaPackageElementFlavor("mpeg-7", "chapter")

        // Engage flavors

        /** Presenter player preview image flavor  */
        val PRESENTER_PLAYER_PREVIEW = MediaPackageElementFlavor("presenter", "player+preview")

        /** Presentation player preview image flavor  */
        val PRESENTATION_PLAYER_PREVIEW = MediaPackageElementFlavor("presentation",
                "player+preview")

        /** Presenter search result preview image flavor  */
        val PRESENTER_SEARCHRESULT_PREVIEW = MediaPackageElementFlavor("presenter",
                "search+preview")

        /** Presentation search result preview image flavor  */
        val PRESENTATION_SEARCHRESULT_PREVIEW = MediaPackageElementFlavor("presentation",
                "search+preview")

        /** Presenter segment preview image flavor  */
        val PRESENTER_SEGMENT_PREVIEW = MediaPackageElementFlavor("presenter", "segment+preview")

        /** Presentation segment preview image flavor  */
        val PRESENTATION_SEGMENT_PREVIEW = MediaPackageElementFlavor("presentation",
                "segment+preview")

        // Feed flavors

        /** Presenter feed preview image flavor  */
        val PRESENTER_FEED_PREVIEW = MediaPackageElementFlavor("presenter", "feed+preview")

        /** Presentation feed preview image flavor  */
        val PRESENTATION_FEED_PREVIEW = MediaPackageElementFlavor("presentation", "feed+preview")

        // Security flavors

        /** Episode bound XACML policy flavor  */
        val XACML_POLICY_EPISODE = MediaPackageElementFlavor("security", "xacml+episode")

        /** Series bound XACML policy flavor  */
        val XACML_POLICY_SERIES = MediaPackageElementFlavor("security", "xacml+series")

        // Other flavors

        /** A default flavor for DFXP captions catalogs"  */
        val CAPTION_GENERAL = MediaPackageElementFlavor("captions", "timedtext")

        /** A flavor for DFXP caption files  */
        val CAPTION_DFXP_FLAVOR = MediaPackageElementFlavor("caption", "dfxp")

        /** OAI-PMH subtype flavor  */
        val OAIPMH = MediaPackageElementFlavor("*", "oaipmh")

        /** Comments metadata flavor  */
        val COMMENTS = MediaPackageElementFlavor("metadata", "comments")

        /** Notes metadata flavor  */
        val NOTES = MediaPackageElementFlavor("metadata", "notes")

        /** Partial SMIL flavor  */
        val SMIL = MediaPackageElementFlavor("smil", "source+partial")

        /** Processing properties flavor  */
        val PROCESSING_PROPERTIES = MediaPackageElementFlavor("processing", "defaults")
    }

}
