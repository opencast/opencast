/**
 *  Copyright 2009 The Regents of the University of California
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
package org.opencast.engage.videodisplay.business
{
	import flash.events.KeyboardEvent;
	import flash.external.ExternalInterface;
	import bridge.ExternalFunction;
	import org.opencast.engage.videodisplay.control.event.ClosedCaptionsEvent;
	import org.opencast.engage.videodisplay.control.event.InitMediaPlayerEvent;
	import org.opencast.engage.videodisplay.control.event.LoadDFXPXMLEvent;
	import org.opencast.engage.videodisplay.control.event.SetVolumeEvent;
	import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
	import org.opencast.engage.videodisplay.control.util.TimeCode;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.videodisplay.state.CoverState;
	import org.opencast.engage.videodisplay.state.VideoSizeState;
	import org.osmf.layout.ScaleMode;
	import org.swizframework.Swiz;
	/*
	 * Functions that can be called using javascript!
	 * Javascript uses the bridge (Videodisplay.js) found in the shared resources to call the functions
	 * The functions are dispatching the corresponding events - see swiz/cairngorm frameworks for further explainaitions for event handling
	 */
	public class FlexAjaxBridge
	{

		/**
		 * Constructor
		 */
		public function FlexAjaxBridge()
		{
			Swiz.autowire(this);
		}

		[Autowire]
		[Bindable]
		public var model:VideodisplayModel;

		private var _time:TimeCode;

		/**
		 * setClosedCaptions
		 * To see the captions. Call the event ClosedCaptionsEvent.
		 */
		public function closedCaptions():void
		{
			Swiz.dispatchEvent(new ClosedCaptionsEvent());
		}

		/**
		 * fastForward
		 * When the learnder click on the fast forward button. Call the event VideoControlEvent.FASTFORWARD.
		 */
		public function fastForward():void
		{
			Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.FASTFORWARD));
		}

		/**
		 * getMediaHeight
		 * Get the current media height.
		 * The return value is available in the calling javascript.
		 * @return model.mediaContainer.measuredHeight
		 */
		public function getMediaHeight():Number
		{
			return model.mediaContainer.measuredHeight;
		}

		/**
		 * getViewState
		 * Get the current media state.
		 * The return value is available in the calling javascript.
		 * @return String model.mediaState
		 */
		public function getViewState():String
		{
			return model.mediaState;
		}

		/**
		 * mute
		 * When the user mutes the video.
		 * The return value is available in the calling javascript. Call the event VideoControlEvent.SKIPFORWARD.
		 * @return Number model.playerVolume
		 */
		public function mute():Number
		{
			Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.MUTE));
			return model.playerVolume;
		}

		/**
		 * onBridgeReady
		 * When the birdge ist ready call the external function ONPLAYERREADY.
		 */
		public function onBridgeReady():void
		{
			ExternalInterface.call(ExternalFunction.ONPLAYERREADY, true);
		}

		/**
		 * passCharCode
		 * When the user press any key for the mediaplayer
		 * @param Int charCode
		 */
		public function passCharCode(charCode:int):void
		{
			// Play or pause the video
			if (charCode == 80 || charCode == 112) // P or p
			{
				if (model.mediaPlayer.playing())
				{
					Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.PAUSE));
				}
				else
				{
					Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.PLAY));
				}
			}

			// Mute the video
			if (charCode == 83 || charCode == 115) // S or s
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.STOP));
			}

			// Mute the video
			if (charCode == 77 || charCode == 109) // M or m
			{
				ExternalInterface.call(ExternalFunction.MUTE, '')
			}

			// Volume up
			if (charCode == 85 || charCode == 117) // U or u
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.VOLUMEUP));
			}

			// Volume down
			if (charCode == 68 || charCode == 100) // D or d
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.VOLUMEDOWN));
			}

			// Seek 0
			if (charCode == 48) // 0
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKZERO));
			}

			// Seek 1
			if (charCode == 49) // 1
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKONE));
			}

			// Seek 2
			if (charCode == 50) // 2
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKTWO));
			}

			// Seek 3
			if (charCode == 51) // 3
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKTHREE));
			}

			// Seek 4
			if (charCode == 52) // 4
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKFOUR));
			}

			// Seek 5
			if (charCode == 53) // 5
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKFIVE));
			}

			// Seek 6
			if (charCode == 54) // 6
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKSIX));
			}

			// Seek 7
			if (charCode == 55) // 7
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKSEVEN));
			}

			// Seek 8
			if (charCode == 56) // 8
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKEIGHT));
			}

			// Seek 9
			if (charCode == 57) // 9
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SEEKNINE));
			}

			// Closed Caption
			if (charCode == 67 || charCode == 99) // C or c
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.CLOSEDCAPTIONS));
			}

			// rewind
			if (charCode == 82 || charCode == 114) // R or r
			{

				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.REWIND));

			}

			// Fast forward
			if (charCode == 70 || charCode == 102) // F or f
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.FASTFORWARD));

			}

			// time
			if (charCode == 84 || charCode == 116) // T or t
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.HEARTIMEINFO));
			}

			// Information
			if (charCode == 73 || charCode == 105) // I or i
			{
				Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SHORTCUTS));
			}
		}

		/**
		 * pause
		 * When the learnder click the pause button. Call the event VideoControlEvent.PAUSE.
		 * The return value is available in the calling javascript.
		 * @return String model.currentPlayerState
		 */
		public function pause():String
		{
			Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.PAUSE));
			return model.currentPlayerState;
		}

		/**
		 * play
		 * When the learnder click the play button. Call the event VideoControlEvent.PLAY.
		 * The return value is available in the calling javascript
		 * @return String model.currentPlayerState
		 */
		public function play():String
		{
			Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.PLAY));
			return model.currentPlayerState;
		}

		/**
		 * reportKeyDown
		 * When the user press a key.
		 * @eventType KeyboardEvent event
		 */
		public function reportKeyUp(event:KeyboardEvent):void
		{
			if (event.altKey && event.ctrlKey)
			{
				passCharCode(event.keyCode);
			}
		}

		/**
		 * rewind
		 * When the learnder click on the rewind button. Call the event VideoControlEvent.REWIND.
		 */
		public function rewind():void
		{
			Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.REWIND));
		}

		/**
		 * seek
		 * When the user seek the video. Set the new postion in the html.
		 * @param Number time
		 * @return Number time
		 */
		public function seek(time:Number):Number
		{
			if (model.startPlay == false)
			{
				model.startSeek=time;
				_time=new TimeCode();
				var newPositionString:String=_time.getTC(time);
				ExternalInterface.call(ExternalFunction.SETCURRENTTIME, newPositionString);
			}
			model.mediaPlayer.seek(time);


			return time;
		}

		/**
		 * setCaptionsURL
		 * Set captions URL and load the file. Call the event LoadDFXPXMLEvent.
		 * @param String captionsURL
		 */
		public function setCaptionsURL(captionsURL:String):void
		{
			if (captionsURL != model.captionsURL)
			{
				model.captionsURL=captionsURL;
				Swiz.dispatchEvent(new LoadDFXPXMLEvent(captionsURL));
			}
		}

		/**
		 * setMediaResolution
		 * Set the media resolution.
		 * @param Number newWidthMediaOne, Number newHeightMediaOne, Number newWidthMediaTwo, Number newHeightMediaTwo, Number multiMediaContainerLeft
		 */
		public function setMediaResolution(newWidthMediaOne:Number, newHeightMediaOne:Number, newWidthMediaTwo:Number, newHeightMediaTwo:Number, multiMediaContainerLeft:Number):void
		{
			if (newWidthMediaOne == 0 && newHeightMediaOne == 0 && newWidthMediaTwo == 0 && newHeightMediaTwo == 0 && multiMediaContainerLeft == 0)
			{
				model.previewPlayer=true;
			}
			else
			{
				model.mediaOneWidth=parseInt(newWidthMediaOne.toString());
				model.mediaOneHeight=parseInt(newHeightMediaOne.toString());
				model.mediaTwoWidth=parseInt(newWidthMediaTwo.toString());
				model.mediaTwoHeight=parseInt(newHeightMediaTwo.toString());
				model.mediaWidth=parseInt((newWidthMediaOne + newWidthMediaTwo).toString());
				model.multiMediaContainerLeft=parseInt(multiMediaContainerLeft.toString());
				model.multiMediaContainerRight=0;
				model.formatMediaOne=model.mediaOneWidth / model.mediaOneHeight;
				model.formatMediaTwo=model.mediaTwoWidth / model.mediaTwoHeight;

				if (model.videoSizeState == VideoSizeState.ONLYLEFT || model.videoSizeState == VideoSizeState.BIGLEFT)
				{
					model.multiMediaContainerRight=multiMediaContainerLeft;
					model.multiMediaContainerLeft=0;
				}
			}
		}

		/**
		 * setMediaURL
		 * Set media URL. Call the event InitMediaPlayerEvent.
		 * Developer: You can change your own urls here.
		 * @param String coverURLOne, String coverURLTwo, String mediaURLOne, String mediaURLTwo, String mimetypeOne, String mimetypeTwo, String playerMode,
		 */
		public function setMediaURL(coverURLOne:String, coverURLTwo:String, mediaURLOne:String, mediaURLTwo:String, mimetypeOne:String, mimetypeTwo:String, playerMode:String, slideLength:int, bufferTime:Number):void
		{
			Swiz.dispatchEvent(new InitMediaPlayerEvent(coverURLOne, coverURLTwo, mediaURLOne, mediaURLTwo, mimetypeOne, mimetypeTwo, playerMode, slideLength, bufferTime));
		}

		/**
		 * setVolumeSlider
		 * Set the volume slider. Call the event SetVolumeEvent.
		 * @param Number newVolume
		 */
		public function setVolumePlayer(newVolume:Number):void
		{
			Swiz.dispatchEvent(new SetVolumeEvent(newVolume));
		}

		/**
		 * skipBackward
		 * When the learnder click the skip backward button. Call the event VideoControlEvent.SKIPBACKWARD.
		 */
		public function skipBackward():void
		{
			Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SKIPBACKWARD));
		}

		/**
		 * skipForward
		 * When the learnder click on the skip forward button. Call the event VideoControlEvent.SKIPFORWARD.
		 */
		public function skipForward():void
		{
			Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.SKIPFORWARD));
		}

		/**
		 * stop
		 * When the learnder click the stop button. Call the event VideoControlEvent.STOP.
		 */
		public function stop():void
		{
			Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.STOP));
		}

		/**
		 * stopFastForward
		 * Reset the fas forward time
		 */
		public function stopFastForward():void
		{
			model.fastForwardTime=10;
		}

		/**
		 * stopRewind
		 * Reset the rewind time.
		 */
		public function stopRewind():void
		{
			model.rewindTime=10;
		}

		/**
		 * videoSizeControl
		 * When the user press the video size control button. Set the vide size state.
		 * @param Number sizeLeft, Number sizeRight
		 */
		public function videoSizeControl(sizeLeft:Number, sizeRight:Number):void
		{
			if (sizeLeft == sizeRight)
			{
				model.layoutMetadataOne.scaleMode=ScaleMode.LETTERBOX;
				model.layoutMetadataTwo.scaleMode=ScaleMode.LETTERBOX;
				model.videoSizeState=VideoSizeState.CENTER;
				model.coverState=CoverState.TWOCOVERS;
			}
			else if (sizeLeft > sizeRight && sizeRight > 0)
			{
				model.layoutMetadataOne.scaleMode=ScaleMode.LETTERBOX;
				model.layoutMetadataTwo.scaleMode=ScaleMode.LETTERBOX;
				model.videoSizeState=VideoSizeState.BIGLEFT;
				model.coverState=CoverState.TWOCOVERS;
			}
			else if (sizeLeft < sizeRight && sizeLeft > 0)
			{
				model.layoutMetadataOne.scaleMode=ScaleMode.LETTERBOX;
				model.layoutMetadataTwo.scaleMode=ScaleMode.LETTERBOX;
				model.videoSizeState=VideoSizeState.BIGRIGHT;
				model.coverState=CoverState.TWOCOVERS;
			}
			else if (sizeRight == 0)
			{
				model.layoutMetadataOne.scaleMode=ScaleMode.LETTERBOX;
				model.layoutMetadataTwo.scaleMode=ScaleMode.LETTERBOX;
				model.videoSizeState=VideoSizeState.ONLYLEFT;
				model.coverState=CoverState.ONECOVER;

				if (model.coverURLTwo == '')
				{
					model.coverURLTwo == model.coverURLOne;
				}
				model.coverURLSingle=model.coverURLOne;
			}
			else if (sizeLeft == 0)
			{
				model.layoutMetadataOne.scaleMode=ScaleMode.LETTERBOX;
				model.layoutMetadataTwo.scaleMode=ScaleMode.LETTERBOX;
				model.videoSizeState=VideoSizeState.ONLYRIGHT;
				model.coverState=CoverState.ONECOVER;
				model.coverURLSingle=model.coverURLTwo;
			}
		}
	}
}

