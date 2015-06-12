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
package org.opencast.engage.videodisplay.model
{
	import mx.collections.ArrayCollection;
	import mx.controls.ProgressBar;
	import mx.core.FlexGlobals;

	import org.opencast.engage.videodisplay.control.util.OpencastMediaPlayer;
	import org.opencast.engage.videodisplay.control.util.TimeCode;
	import org.opencast.engage.videodisplay.state.CoverState;
	import org.opencast.engage.videodisplay.state.MediaState;
	import org.opencast.engage.videodisplay.state.SoundState;
	import org.opencast.engage.videodisplay.state.VideoSizeState;
	import org.opencast.engage.videodisplay.state.VideoState;
	import org.opencast.engage.videodisplay.vo.LanguageVO;
	import org.osmf.containers.MediaContainer;
	import org.osmf.layout.LayoutMetadata;
	[Bindable]
	public class VideodisplayModel
	{

		/**
		 * Constructor
		 */
		public function VideodisplayModel()
		{
		}

		// AUDIOPLAYER
		public var AUDIOPLAYER:String="Audioplayer";

		// HTML
		public var HTML:String='html';

		// MULTIPLAYER
		public var MULTIPLAYER:String="Multiplayer";

		// RTMP
		public var RTMP:String='rtmp';

		// SINGLEPLAYER
		public var SINGLEPLAYER:String="Singleplayer";

		// SINGLEPLAYERWITHSLIDES
		public var SINGLEPLAYERWITHSLIDES:String="SingleplayerWithSlides";

		// audioURLaudioURL
		public var audioURL:String="";

		// bytesLoaded
		public var bytesLoaded:Number=0;

		// bytesLoadedOne
		public var bytesLoadedOne:Number=0;

		// bytesLoadedTwo
		public var bytesLoadedTwo:Number=0;

		// bytesTotal
		public var bytesTotal:Number=0;

		// bytesTotalOne
		public var bytesTotalOne:Number=0;

		// bytesTotalTwo
		public var bytesTotalTwo:Number=0;

		// An Array with different caption data
		public var captionSets:ArrayCollection;

		// Height of the captions
		public var captionsHeight:int=50;

		// captionsURL
		public var captionsURL:String='';

		// Close Caption Boolean
		public var ccBoolean:Boolean=false;

		// CC Button
		public var ccButtonBool:Boolean=false;

		// Close Caption Button Boolean
		public var ccButtonBoolean:Boolean=false;

		// coverState
		public var coverState:String=CoverState.ONECOVER;

		// coverURLOne
		public var coverURLOne:String='';

		// coverURLSingle
		public var coverURLSingle:String='';


		// coverURLTwo
		public var coverURLTwo:String='';

		// Current Caption Set
		public var currentCaptionSet:Array;

		// Current Duration
		public var currentDuration:Number=0;

		// Current Duration String
		public var currentDurationString:String='';

		// Current Player State
		public var currentPlayerState:String;

		// Current Playhead
		public var currentPlayhead:Number=0;

		// currentPlayheadPlayerOne
		public var currentPlayheadPlayerOne:Number=0;

		// currentPlayheadPlayerTwo
		public var currentPlayheadPlayerTwo:Number=0;

		// Current PlayheadSingle
		public var currentPlayheadSingle:Number=0;

		// currentSeekPosition
		public var currentSeekPosition:Number=0;

		// The current Subtitle
		public var currentSubtitle:String='';

		// durationPlayerOne
		public var durationPlayerOne:Number;

		// durationPlayerTwo
		public var durationPlayerTwo:Number;

		// endIndexSubtitle
		public var endIndexSubtitle:int=90;

		// errorDetail
		public var errorDetail:String='';

		// errorId
		public var errorId:String=''

		// errorMessage
		public var errorMessage:String='';

		// Skip Fast Forward -change time
		public var fastForwardTime:Number=10;

		// Captions font size
		public var fontSizeCaptions:int=12;

		// formatMediaOne
		public var formatMediaOne:Number=0;

		// formatMediaTwo
		public var formatMediaTwo:Number=0;

		// Fullscreen Mode
		public var fullscreenMode:Boolean=false;

		// fullscreenProgressWidth
		public var fullscreenProgressWidth:Number=575;

		// fullscreenThumbDrag
		public var fullscreenThumbDrag:Boolean=false;

		// An Array width the language from the dfxp file
		public var languageComboBox:Array=new Array();

		// Array of LanguageVO
		public var languages:ArrayCollection=new ArrayCollection([new LanguageVO('de', "German"), new LanguageVO('en', "English"), new LanguageVO('es', "Spain")]);

		// layoutMetadataOne
		public var layoutMetadataOne:LayoutMetadata;

		// layoutMetadataTwo
		public var layoutMetadataTwo:LayoutMetadata;

		// loader
		public var loader:Boolean=false;

		// MediaContainer
		public var mediaContainer:MediaContainer;

		// mediaContainerLeftWidth
		public var mediaContainerLeftWidth:int=(FlexGlobals.topLevelApplication.width - 10) / 2;

		// mediaContainerOne
		public var mediaContainerOne:MediaContainer;

		// mediaContainerRightWidth
		public var mediaContainerRightWidth:int=(FlexGlobals.topLevelApplication.width - 10) / 2;

		// mediaContainerTwo
		public var mediaContainerTwo:MediaContainer;

		// mediaOneHeight
		public var mediaOneHeight:int=0;

		// mediaOneWidth
		public var mediaOneWidth:int=0;

		// mediaPlayer
		public var mediaPlayer:OpencastMediaPlayer;

		// mediaState
		public var mediaState:String=MediaState.MEDIA;

		// mediaTwoHeight
		public var mediaTwoHeight:int=0;

		// mediaTwoWidth
		public var mediaTwoWidth:int=0;

		// mediaType
		public var mediaType:String;

		// mediaTypeOne
		public var mediaTypeOne:String;

		// mediaTypeSingle
		public var mediaTypeSingle:String;

		// mediaTypeTwo
		public var mediaTypeTwo:String;

		// mediaWidth
		public var mediaWidth:int=0;

		// multiMediaContainerBottom
		public var multiMediaContainerBottom:int=0;

		// multiMediaContainerLeft
		public var multiMediaContainerLeft:int=0;

		// multiMediaContainerLeftFullscreen
		public var multiMediaContainerLeftNormalscreen:int=0;

		// multiMediaContainerRight
		public var multiMediaContainerRight:int=0;

		// multiMediaContainerRightFullscreen
		public var multiMediaContainerRightNormalscreen:int=0;

		// The old Subtitle
		public var oldSubtitle:String='';

		// onBufferChangeMediaOneTime
		public var onBufferChangeMediaOneTime:Number;

		// onBufferChangeMediaTwoTime
		public var onBufferChangeMediaTwoTime:Number;

		// onBufferingChangeMediaOne
		public var onBufferingChangeMediaOne:Boolean;

		// onBufferingChangeMediaTwo
		public var onBufferingChangeMediaTwo:Boolean;

		// playerMode
		public var playerMode:String='';

		// playerSeekBool
		public var playerSeekBool:Boolean=false;

		// player volume
		public var playerVolume:Number=1.0;

		// previewPlayer
		public var previewPlayer:Boolean=false;

		// progress
		public var progress:Number=0;

		// Progress Bar
		public var progressBar:ProgressBar=new ProgressBar();

		// progressFullscreen
		public var progressFullscreen:Number=0;

		// progressMediaOne
		public var progressMediaOne:Number=0;

		// progressMediaTwo
		public var progressMediaTwo:Number=0;

		// Rewind Time
		public var rewindTime:Number=10;

		// singleState
		public var singleState:String='';

		// slideLength
		public var slideLength:int;

		// soundState
		public var soundState:String=SoundState.VOLUMEMAX;

		// startPlay
		public var startPlay:Boolean=false;

		// startPlayOne
		public var startPlayOne:Boolean=false;

		// startPlaySingle
		public var startPlaySingle:Boolean=false;

		// startPlayTwo
		public var startPlayTwo:Boolean=false;

		// startSeek
		public var startSeek:Number=0;

		// statePlayerOne
		public var statePlayerOne:String='';

		// statePlayerOne
		public var statePlayerTwo:String='';

		// stateSinglePlayer
		public var stateSinglePlayer:String='';

		// Time Code
		public var timeCode:TimeCode=new TimeCode();

		// videoSizeState
		public var videoSizeState:String=VideoSizeState.CENTER;

		// videoState
		public var videoState:String=VideoState.COVER;

		// video Volume
		public var videoVolume:Number=1;

	}
}


