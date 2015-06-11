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
package org.opencast.engage.videodisplay.control.command
{
	import flash.external.ExternalInterface;
	import bridge.ExternalFunction;
	import org.opencast.engage.videodisplay.control.event.ClosedCaptionsEvent;
	import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.videodisplay.state.PlayerState;
	import org.opencast.engage.videodisplay.state.VideoState;
	import org.swizframework.Swiz;
	import mx.controls.Alert;

	/**
	 * VideoControlCommand
	 * Class handles the player commands like play/pause/stop etc.
	 */
	public class VideoControlCommand
	{

		/**
		 * Constructor
		 */
		public function VideoControlCommand()
		{
			Swiz.autowire(this);
		}

		[Autowire]
		public var model:VideodisplayModel;

		/**
		 * execute
		 * When the user press a button, or use the keyboard shurtcuts.
		 * @eventType VideoControlEvent event
		 * */
		public function execute(event:VideoControlEvent):void
		{
			var currentPlayPauseState:String;
			var percent:int=100;
			var skipVolume:Number=0.1;
			var playState:Boolean=false;

			switch (event.videoControlType)
			{
				case VideoControlEvent.PLAY:
					model.mediaPlayer.play();
					if (model.mediaPlayer.playing())
					{
						model.currentPlayerState=PlayerState.PLAYING;
						currentPlayPauseState=PlayerState.PAUSED;
						ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
					}

					if (model.videoState == VideoState.COVER)
					{
						model.videoState=model.mediaPlayer.getVideoState();
					}

					break;

				case VideoControlEvent.PAUSE:
					if (model.mediaPlayer.playing())
					{
						model.mediaPlayer.pause();
						//see http://trac.red5.org/ticket/656
						//ok for other servers
						//model.mediaPlayer.seek(model.currentPlayhead);
						model.mediaPlayer.seek(model.currentSeekPosition);
						if (!model.mediaPlayer.playing()) 
						{
							model.currentPlayerState=PlayerState.PAUSED;
							currentPlayPauseState=PlayerState.PLAYING;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}

					break;

				case VideoControlEvent.STOP:
					if (model.mediaPlayer.playing())
					{
						model.mediaPlayer.pause();
						model.mediaPlayer.seek(0);
					}
						//if the video has been watched completely - OSMF TimeEvent.COMPLETE - mediaPlayer not playing anymore
					else
					{
						model.mediaPlayer.play();
						model.mediaPlayer.pause();
						model.mediaPlayer.seek(0);
					}

					if (!model.mediaPlayer.playing()) 
					{
						model.currentPlayerState=PlayerState.PAUSED;
						currentPlayPauseState=PlayerState.PLAYING;
						ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
					}
					break;

				case VideoControlEvent.SKIPBACKWARD:

					break;

				case VideoControlEvent.REWIND:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);						
						}
					}
					if (model.startPlay == true)
					{
						if (model.currentSeekPosition - model.rewindTime > 0)
						{
							model.mediaPlayer.seek(model.currentSeekPosition - model.rewindTime);
						}
						else
						{
							model.mediaPlayer.seek(0);
						}
					}
					if (model.rewindTime < (model.currentDuration * 0.1))
					{
						model.rewindTime=model.rewindTime + model.rewindTime;
					}
					break;

				case VideoControlEvent.FASTFORWARD:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}

					if (model.startPlay == true)
					{
						var newPlayhead:Number=model.currentSeekPosition + model.fastForwardTime;
						if (newPlayhead > model.currentDuration)
						{
							model.mediaPlayer.seek(model.currentDuration);
						}
						else
						{
							model.mediaPlayer.seek(newPlayhead);
						}
					}
					if (model.fastForwardTime < (model.currentDuration * 0.1))
					{
						model.fastForwardTime=model.fastForwardTime + model.fastForwardTime;
					}
					break;

				case VideoControlEvent.SKIPFORWARD:
					break;

				case VideoControlEvent.MUTE:

					if (model.mediaPlayer.getMuted())
					{
						model.mediaPlayer.setMuted(false);
					}
					else
					{
						model.mediaPlayer.setMuted(true);
					}
					break;

				case VideoControlEvent.VOLUMEUP:

					if (model.mediaPlayer.getVolume() != 1)
					{
						model.mediaPlayer.setVolume(model.mediaPlayer.getVolume() + skipVolume);
					}
					ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, Math.round(model.mediaPlayer.getVolume() * percent));
					break;

				case VideoControlEvent.VOLUMEDOWN:

					if (model.mediaPlayer.getVolume() != 0)
					{
						model.mediaPlayer.setVolume(model.mediaPlayer.getVolume() - skipVolume);

						if (model.mediaPlayer.getVolume() < 0)
						{
							model.mediaPlayer.setVolume(0);
						}
					}
					ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, Math.round(model.mediaPlayer.getVolume() * percent));
					break;

				case VideoControlEvent.SEEKZERO:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 0);
					break;

				case VideoControlEvent.SEEKONE:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 1);
					break;

				case VideoControlEvent.SEEKTWO:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 2);
					break;

				case VideoControlEvent.SEEKTHREE:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 3);
					break;

				case VideoControlEvent.SEEKFOUR:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 4);
					break;

				case VideoControlEvent.SEEKFIVE:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 5);
					break;

				case VideoControlEvent.SEEKSIX:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 6);
					break;

				case VideoControlEvent.SEEKSEVEN:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 7);
					break;

				case VideoControlEvent.SEEKEIGHT:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 8);
					break;

				case VideoControlEvent.SEEKNINE:
					//if player in pause mode start playing
					if (!model.mediaPlayer.playing())
					{
						model.mediaPlayer.play();
						if (model.mediaPlayer.playing())
						{
							model.currentPlayerState=PlayerState.PLAYING;
							currentPlayPauseState=PlayerState.PAUSED;
							ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
						}
					}
					model.mediaPlayer.seek((model.currentDuration / 10) * 9);
					break;

				case VideoControlEvent.CLOSEDCAPTIONS:

					if (model.ccBoolean)
					{
						Swiz.dispatchEvent(new ClosedCaptionsEvent(false));
						model.ccButtonBool=false;
					}
					else
					{
						Swiz.dispatchEvent(new ClosedCaptionsEvent(true));
						model.ccButtonBool=true;
					}

					break;

				case VideoControlEvent.HEARTIMEINFO:
					Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.PAUSE));
					ExternalInterface.call(ExternalFunction.CURRENTTIME, model.timeCode.getTC(model.currentPlayhead));
					break;

				case VideoControlEvent.SHORTCUTS:
					ExternalInterface.call(ExternalFunction.TOGGLESHORTCUTS);
					break;

				default:
					break;
			}
		}
	}
}

