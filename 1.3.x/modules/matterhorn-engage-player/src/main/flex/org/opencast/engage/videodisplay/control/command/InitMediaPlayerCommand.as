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
package org.opencast.engage.videodisplay.control.command
{
	import flash.external.ExternalInterface;
	import mx.core.Application;
	import bridge.ExternalFunction;
	import org.opencast.engage.videodisplay.control.event.InitMediaPlayerEvent;
	import org.opencast.engage.videodisplay.control.util.OpencastMediaPlayer;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.videodisplay.state.CoverState;
	import org.opencast.engage.videodisplay.state.MediaState;
	import org.opencast.engage.videodisplay.state.PlayerState;
	import org.opencast.engage.videodisplay.state.VideoState;
	import org.osmf.elements.AudioElement;
	import org.osmf.elements.LightweightVideoElement;
	import org.osmf.elements.VideoElement;
	import org.osmf.media.MediaElement;
	import org.osmf.media.URLResource;
	import org.swizframework.Swiz;
	/**
	 * InitMediaPlayerCommand
	 * This Command tries to initialize the player and the videos
	 * It also handles errors, connection or stream not found
	 */
	public class InitMediaPlayerCommand
	{

		/**
		 * Constructor
		 */
		public function InitMediaPlayerCommand()
		{
			Swiz.autowire(this);
		}

		[Autowire]
		public var model:VideodisplayModel;

		/**
		 * execute
		 * Init the video player. Set the cover urls and set the video urls.
		 * @eventType InitPlayerEvent event
		 */
		public function execute(event:InitMediaPlayerEvent):void
		{
			model.currentPlayerState=PlayerState.PAUSED;
			ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING);
			model.playerMode=event.playerMode;
			model.slideLength=event.slideLength;

			// set the cover URL One
			model.coverURLOne=event.coverURLOne;

			// set the cover URL Two
			model.coverURLTwo=event.coverURLTwo;

			// set the cover state
			if (event.coverURLOne != '' && event.coverURLTwo == '')
			{
				model.coverState=CoverState.ONECOVER;
				model.coverURLSingle=event.coverURLOne;
			}
			else
			{
				model.coverState=CoverState.TWOCOVERS;
				model.coverURLSingle=event.coverURLOne;
			}


			// Single Video/Audio
			if (event.mediaURLOne != '' && (event.mediaURLTwo == '' || event.mediaURLTwo == ' '))
			{
				//
				model.mediaPlayer=new OpencastMediaPlayer(VideoState.SINGLE);
				var pos:int=event.mimetypeOne.lastIndexOf("/");

				var fileType:String=event.mimetypeOne.substring(0, pos);

				switch (fileType)
				{
					case "video":

						//var newVideoElement : VideoElement = new VideoElement( new URLResource( event.mediaURLOne ) );
						// Using OSMFs new LightweightVideoElelemt -> red5 rtmp stream not starting
						var newVideoElement:LightweightVideoElement=new LightweightVideoElement(new URLResource(event.mediaURLOne));
						newVideoElement.smoothing=true;
						//newVideoElement.defaultDuration = 1000;
						var mediaElementVideo:MediaElement=newVideoElement;

						model.mediaPlayer.setSingleMediaElement(mediaElementVideo);

						if (event.mediaURLOne.charAt(0) == 'h' || event.mediaURLOne.charAt(0) == 'H')
						{
							model.mediaTypeSingle=model.HTML;
							model.mediaType=model.HTML;
						}
						else if (event.mediaURLOne.charAt(0) == 'r' || event.mediaURLOne.charAt(0) == 'R')
						{
							model.mediaTypeSingle=model.RTMP;
						}
						break;

					case "audio":
						var mediaElementAudio:MediaElement=new AudioElement(new URLResource(event.mediaURLOne));
						model.mediaPlayer.setSingleMediaElement(mediaElementAudio);
						var position:int=event.mediaURLOne.lastIndexOf('/');
						model.audioURL=event.mediaURLOne.substring(position + 1);
						Application.application.bx_audio.startVisualization();
						model.mediaState=MediaState.AUDIO;
						break;

					default:
						errorMessage("Error", "TRACK COULD NOT BE FOUND");
						break;
				}
			}
			else if (event.mediaURLOne != '' && event.mediaURLTwo != '')
			{
				if (event.mediaURLOne.charAt(0) == 'h' || event.mediaURLOne.charAt(0) == 'H')
				{
					model.mediaTypeOne=model.HTML;
					model.mediaType=model.HTML;
				}
				else if (event.mediaURLOne.charAt(0) == 'r' || event.mediaURLOne.charAt(0) == 'R')
				{
					model.mediaTypeOne=model.RTMP;
				}

				if (event.mediaURLTwo.charAt(0) == 'h' || event.mediaURLTwo.charAt(0) == 'H')
				{
					model.mediaTypeTwo=model.HTML;
					model.mediaType=model.HTML;
				}
				else if (event.mediaURLTwo.charAt(0) == 'r' || event.mediaURLTwo.charAt(0) == 'R')
				{
					model.mediaTypeTwo=model.RTMP;
				}


				model.mediaPlayer=new OpencastMediaPlayer(VideoState.MULTI);

				//var newVideoElementOne : VideoElement = new VideoElement( new URLResource( event.mediaURLOne ) );
				var newVideoElementOne:LightweightVideoElement=new LightweightVideoElement(new URLResource(event.mediaURLOne));
				newVideoElementOne.smoothing=true;
				//newVideoElementOne.defaultDuration = 1000;
				var mediaElementVideoOne:MediaElement=newVideoElementOne;
				//model.mediaPlayer.setMediaElementOne( mediaElementVideoOne );

				var newVideoElementTwo:LightweightVideoElement=new LightweightVideoElement(new URLResource(event.mediaURLTwo));
				newVideoElementTwo.smoothing=true;
				//newVideoElementTwo.defaultDuration = 1000;
				var mediaElementVideoTwo:MediaElement=newVideoElementTwo;
				//model.mediaPlayer.setMediaElementTwo( mediaElementVideoTwo );

				model.mediaPlayer.setMediaElement(mediaElementVideoOne, mediaElementVideoTwo);
			}
			else
			{
				errorMessage("Error", "TRACK COULD NOT BE FOUND");
			}
		}

		/**
		 * errorMessage
		 * Set the error Message and switch the stage.
		 * @param String name, String message
		 * */
		private function errorMessage(name:String, message:String):void
		{
			model.mediaState=MediaState.ERROR;
			model.errorMessage=name;
			model.errorDetail=message;
		}
	}
}

