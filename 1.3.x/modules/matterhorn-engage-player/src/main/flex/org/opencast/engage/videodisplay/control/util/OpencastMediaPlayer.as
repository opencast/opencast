package org.opencast.engage.videodisplay.control.util
{
	import bridge.ExternalFunction;

	import flash.events.TimerEvent;
	import flash.external.ExternalInterface;
	import flash.utils.Timer;

	import org.opencast.engage.videodisplay.control.command.VideoControlCommand;
	import org.opencast.engage.videodisplay.control.event.DisplayCaptionEvent;
	import org.opencast.engage.videodisplay.control.event.VideoControlEvent;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.videodisplay.state.DefaultPlayerState;
	import org.opencast.engage.videodisplay.state.MediaState;
	import org.opencast.engage.videodisplay.state.PlayerModeState;
	import org.opencast.engage.videodisplay.state.PlayerState;
	import org.opencast.engage.videodisplay.state.SoundState;
	import org.opencast.engage.videodisplay.state.VideoState;
	import org.osmf.elements.ParallelElement;
	import org.osmf.events.AudioEvent;
	import org.osmf.events.BufferEvent;
	import org.osmf.events.LoadEvent;
	import org.osmf.events.MediaErrorEvent;
	import org.osmf.events.MediaPlayerStateChangeEvent;
	import org.osmf.events.TimeEvent;
	import org.osmf.layout.HorizontalAlign;
	import org.osmf.layout.LayoutMetadata;
	import org.osmf.layout.ScaleMode;
	import org.osmf.layout.VerticalAlign;
	import org.osmf.media.MediaElement;
	import org.osmf.media.MediaPlayer;
	import org.osmf.metadata.MetadataWatcher;
	import org.swizframework.Swiz;

	/**
	 *   OpencastMediaPlayer
	 */
	public class OpencastMediaPlayer
	{

		/**
		 * Constructor
		 * Create new single or multi media player. Add the listeners to the media player.
		 * @param String value
		 */
		public function OpencastMediaPlayer(value:String)
		{
			Swiz.autowire(this);

			videoState=value;

			parallelElement=new ParallelElement();

			// initialize the timeCode
			_time=new TimeCode();

			// initialize the media player
			if (videoState == VideoState.SINGLE)
			{

				mediaPlayerSingle=new MediaPlayer();
				mediaPlayerSingle.autoRewind=true;
				mediaPlayerSingle.autoPlay=false;
				mediaPlayerSingle.volume=0;

				// Add MediaPlayerSingle event handlers..
				mediaPlayerSingle.addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, onStateChange);
				mediaPlayerSingle.addEventListener(TimeEvent.DURATION_CHANGE, onDurationChange);
				mediaPlayerSingle.addEventListener(AudioEvent.MUTED_CHANGE, muteChange);
				mediaPlayerSingle.addEventListener(AudioEvent.VOLUME_CHANGE, volumeChange);
				mediaPlayerSingle.addEventListener(TimeEvent.CURRENT_TIME_CHANGE, onCurrentTimeChange);
				mediaPlayerSingle.addEventListener(MediaErrorEvent.MEDIA_ERROR, onMediaError);
				mediaPlayerSingle.addEventListener(LoadEvent.BYTES_TOTAL_CHANGE, onBytesTotalChange);
				mediaPlayerSingle.addEventListener(LoadEvent.BYTES_LOADED_CHANGE, onBytesLoadedChange);
				mediaPlayerSingle.addEventListener(BufferEvent.BUFFERING_CHANGE, onBufferingChange);
				mediaPlayerSingle.addEventListener(BufferEvent.BUFFER_TIME_CHANGE, onBufferTimeChange);
				mediaPlayerSingle.addEventListener(TimeEvent.COMPLETE, _videocomplete, false, 0, true);
			}
			else if (videoState == VideoState.MULTI)
			{

				mediaPlayerOne=new MediaPlayer();
				mediaPlayerOne.autoRewind=true;
				mediaPlayerOne.autoPlay=false;
				mediaPlayerOne.volume=0;

				mediaPlayerTwo=new MediaPlayer();
				mediaPlayerTwo.autoRewind=true;
				mediaPlayerTwo.autoPlay=false;
				mediaPlayerTwo.volume=0;

				// Set the default Player
				setDefaultPlayer(DefaultPlayerState.PLAYERONE);

				// Add MediaPlayerOne event handlers..
				mediaPlayerOne.addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, playerOneOnStateChange);
				mediaPlayerOne.addEventListener(TimeEvent.DURATION_CHANGE, playerOneOnDurationChange);
				mediaPlayerOne.addEventListener(AudioEvent.MUTED_CHANGE, playerOneMuteChange);
				mediaPlayerOne.addEventListener(AudioEvent.VOLUME_CHANGE, playerOneVolumeChange);
				mediaPlayerOne.addEventListener(TimeEvent.CURRENT_TIME_CHANGE, playerOneOnCurrentTimeChange);
				mediaPlayerOne.addEventListener(MediaErrorEvent.MEDIA_ERROR, onMediaError);
				mediaPlayerOne.addEventListener(LoadEvent.BYTES_TOTAL_CHANGE, playerOneOnBytesTotalChange);
				mediaPlayerOne.addEventListener(LoadEvent.BYTES_LOADED_CHANGE, playerOneOnBytesLoadedChange);
				mediaPlayerOne.addEventListener(BufferEvent.BUFFERING_CHANGE, playerOneOnBufferingChange);
				mediaPlayerOne.addEventListener(BufferEvent.BUFFER_TIME_CHANGE, playerOneOnBufferTimeChange);
				mediaPlayerOne.addEventListener(TimeEvent.COMPLETE, _videocomplete, false, 0, true);

				// Add MediaPlayerTwo event handlers..
				mediaPlayerTwo.addEventListener(MediaPlayerStateChangeEvent.MEDIA_PLAYER_STATE_CHANGE, playerTwoOnStateChange);
				mediaPlayerTwo.addEventListener(TimeEvent.DURATION_CHANGE, playerTwoOnDurationChange);
				mediaPlayerTwo.addEventListener(AudioEvent.MUTED_CHANGE, playerTwoMuteChange);
				mediaPlayerTwo.addEventListener(AudioEvent.VOLUME_CHANGE, playerTwoVolumeChange);
				mediaPlayerTwo.addEventListener(TimeEvent.CURRENT_TIME_CHANGE, playerTwoOnCurrentTimeChange);
				mediaPlayerTwo.addEventListener(MediaErrorEvent.MEDIA_ERROR, onMediaError);
				mediaPlayerTwo.addEventListener(LoadEvent.BYTES_TOTAL_CHANGE, playerTwoOnBytesTotalChange);
				mediaPlayerTwo.addEventListener(LoadEvent.BYTES_LOADED_CHANGE, playerTwoOnBytesLoadedChange);
				mediaPlayerTwo.addEventListener(BufferEvent.BUFFERING_CHANGE, playerTwoOnBufferingChange);
				mediaPlayerTwo.addEventListener(BufferEvent.BUFFER_TIME_CHANGE, playerTwoOnBufferTimeChange);


			}
			// Reset the current time in html
			//ExternalInterface.call( ExternalFunction.SETCURRENTTIME, '00:00:00' );
		}

		[Autowire]
		public var model:VideodisplayModel;

		private var _time:TimeCode;

		private var bufferTimer:Timer=new Timer(500);

		private var count:Number=0;

		private var currentDurationString:String="00:00:00";

		private var defaultPlayer:String;

		private var defaultPlayerBackup:String;

		private var firstStart:Boolean=false;

		private var formatMediaOne:Number=0;

		private var formatMediaTwo:Number=0;

		private var lastNewPositionPlayerOneString:String="00:00:00";

		private var lastNewPositionPlayerTwoString:String="00:00:00";

		private var lastNewPositionString:String="00:00:00";

		private var maxDurationPlayer:String='';

		private var mediaPlayerOne:MediaPlayer;

		private var mediaPlayerSingle:MediaPlayer;

		private var mediaPlayerTwo:MediaPlayer;

		private var oProxyElementTwo:OProxyElement;

		private var parallelElement:ParallelElement;

		private var recommendationsWatcher:MetadataWatcher;

		private var rewindBool:Boolean=false;

		private var videoState:String;

		/**
		 * setMuted
		 * Get the mute boolean
		 * @return Boolean muted
		 */
		public function getMuted():Boolean
		{
			var muted:Boolean;

			if (videoState == VideoState.SINGLE)
			{
				muted=mediaPlayerSingle.muted;
			}
			else if (videoState == VideoState.MULTI)
			{
				if (defaultPlayer == DefaultPlayerState.PLAYERONE)
				{
					muted=mediaPlayerOne.muted;
				}
				/*else if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
				   {
				   muted = mediaPlayerTwo.muted;
				 }*/
			}

			return muted;
		}

		/**
		 * getVideoState
		 * Get the videoState
		 * @return String videoState
		 */
		public function getVideoState():String
		{
			return videoState;
		}

		/**
		 * getVolume
		 * Get the media volume.
		 * @return volume:Number
		 */
		public function getVolume():Number
		{
			var volume:Number;

			if (videoState == VideoState.SINGLE)
			{
				volume=mediaPlayerSingle.volume;
			}
			else if (videoState == VideoState.MULTI)
			{
				if (defaultPlayer == DefaultPlayerState.PLAYERONE)
				{
					volume=mediaPlayerOne.volume;
				}
			}

			return volume;
		}

		/**
		 * onBuffer
		 * Start the buffer timer
		 * @return volume:Number
		 */
		public function onBuffer():void
		{
			bufferTimer=new Timer(3000, 1);
			bufferTimer.addEventListener(TimerEvent.TIMER_COMPLETE, onBufferTimerComplete);
			bufferTimer.start();
		}

		/**
		 * pause
		 * Paused the media file.
		 */
		public function pause():void
		{
			if (videoState == VideoState.SINGLE)
			{
				mediaPlayerSingle.pause();

			}
			else if (videoState == VideoState.MULTI)
			{
				mediaPlayerOne.pause();
					//mediaPlayerTwo.pause();
			}

			model.loader=false;
			model.currentSeekPosition=model.currentPlayhead;
		}

		/**
		 * play
		 * Play the media file.
		 */
		public function play():void
		{
			if (model.startPlay == true)
			{
				if (videoState == VideoState.SINGLE)
				{
					mediaPlayerSingle.play();

					if (model.mediaTypeSingle == model.RTMP)
					{

						if (model.playerSeekBool == true)
						{
							mediaPlayerSingle.seek(model.currentSeekPosition + 1);
							mediaPlayerSingle.seek(model.currentSeekPosition - 1);
							model.playerSeekBool=false;
						}
						else
						{
							mediaPlayerSingle.seek(model.currentPlayhead);
						}
					}
				}
				else if (videoState == VideoState.MULTI)
				{
					mediaPlayerOne.play();
					//mediaPlayerTwo.play();

					if (model.playerSeekBool == true)
					{
						model.playerSeekBool=false;
					}
					else
					{
						try
						{
							mediaPlayerOne.seek(model.currentPlayhead);
								//mediaPlayerTwo.seek( model.currentPlayhead );
						}
						catch (error:Error)
						{
							// do nothing
						}
					}
				}
			}
			else // first start
			{
				if (videoState == VideoState.SINGLE)
				{
					try
					{
						model.startPlay=true;
						mediaPlayerSingle.play();
						mediaPlayerSingle.seek(model.startSeek);
					}
					catch (error:Error)
					{
						// do nothing;
					}

				}
				else if (videoState == VideoState.MULTI)
				{
					try
					{
						model.startPlay=true;
						mediaPlayerOne.play();
						mediaPlayerOne.seek(model.startSeek);
							//mediaPlayerTwo.play();
							//mediaPlayerTwo.seek( model.startSeek );
					}
					catch (error:Error)
					{
						// do nothing;
					}
				}
				model.mediaPlayer.setVolume(1);
			}

			if (firstStart == false)
			{
				firstStart=true;
			}
		}

		/**
		 * playing
		 * Return the playing mode
		 * @return playing:Boolean
		 */
		public function playing():Boolean
		{
			var playing:Boolean=false;

			if (videoState == VideoState.SINGLE)
			{
				playing=mediaPlayerSingle.playing;

			}
			else if (videoState == VideoState.MULTI)
			{
				if (maxDurationPlayer == DefaultPlayerState.PLAYERONE)
				{
					playing=mediaPlayerOne.playing;
				}

				/*if( maxDurationPlayer == DefaultPlayerState.PLAYERTWO )
				   {
				   playing = mediaPlayerTwo.playing;
				 }*/
			}
			return playing;
		}

		/**
		 * seek
		 * Seek the media files to the new value position.
		 * @param Number value
		 */
		public function seek(value:Number):void
		{
			model.currentSeekPosition=value;

			if (videoState == VideoState.SINGLE)
			{
				if (model.playerSeekBool == false && mediaPlayerSingle.paused && model.mediaTypeSingle == model.RTMP)
				{
					model.playerSeekBool=true;
				}

				if (value >= model.currentDuration)
				{
					value=model.currentDuration - 1;
					model.currentSeekPosition=model.currentDuration - 1;
				}

				if (mediaPlayerSingle.canSeekTo(value) == true)
				{
					mediaPlayerSingle.seek(value);
				}
			}
			else if (videoState == VideoState.MULTI)
			{
				var valueOne:Number=value;
				var valueTwo:Number=value;

				if (model.mediaTypeOne == model.RTMP || model.mediaTypeTwo == model.RTMP)
				{
					//if( model.playerSeekBool == false && mediaPlayerOne.paused || mediaPlayerTwo.paused )
					if (model.playerSeekBool == false && mediaPlayerOne.paused)
					{
						model.playerSeekBool=true;
					}
				}

				if (valueOne >= model.durationPlayerOne)
				{
					valueOne=model.durationPlayerOne - 1;
					model.currentSeekPosition=model.durationPlayerOne - 1;
				}

				if (valueTwo >= model.durationPlayerTwo)
				{
					valueTwo=model.durationPlayerTwo - 1;
					model.currentSeekPosition=model.durationPlayerTwo - 1;
				}

				if (mediaPlayerOne.canSeekTo(valueOne) == true)
				{
					mediaPlayerOne.seek(valueOne);
				}

				/*if( mediaPlayerTwo.canSeekTo( valueTwo ) == true )
				   {
				   mediaPlayerTwo.seek( valueTwo );
				 }*/
			}
		}

		/**
		 * seeking
		 * Return the seeking mode.
		 * @return seeking:Boolean
		 */
		public function seeking():Boolean
		{
			var seeking:Boolean;
			var seekingOne:Boolean;
			var seekingTwo:Boolean;

			if (videoState == VideoState.SINGLE)
			{
				seeking=mediaPlayerSingle.seeking;
			}
			else if (videoState == VideoState.MULTI)
			{
				seekingOne=mediaPlayerOne.seeking;
				//seekingTwo = mediaPlayerTwo.seeking;

				//if( seekingOne == true || seekingTwo == true )
				if (seekingOne == true)
				{
					seeking=true;
				}
				else
				{
					seeking=false;
				}
			}
			return seeking;
		}

		/**
		 * setDefaultPlayer
		 * Set the default media player.
		 * @param String value
		 */
		public function setDefaultPlayer(value:String):void
		{
			defaultPlayer=value;

			if (value == DefaultPlayerState.PLAYERONE)
			{
				mediaPlayerTwo.muted;
			}
			else if (value == DefaultPlayerState.PLAYERTWO)
			{
				mediaPlayerOne.muted;
			}
		}


		/*
		   Using OSMFs ParalellElement to sync videos
		 */
		public function setMediaElement(mediaElement1:MediaElement, mediaElement2:MediaElement):void
		{



			// If there's no explicit layout metadata, center the content. 
			model.layoutMetadataOne=mediaElement1.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;

			if (model.layoutMetadataOne == null)
			{
				model.layoutMetadataOne=new LayoutMetadata();
				model.layoutMetadataOne.scaleMode=ScaleMode.LETTERBOX;
				model.layoutMetadataOne.percentHeight=100;
				model.layoutMetadataOne.percentWidth=100;
				model.layoutMetadataOne.horizontalAlign=HorizontalAlign.RIGHT;
				model.layoutMetadataOne.verticalAlign=VerticalAlign.BOTTOM;
				mediaElement1.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataOne);
			}

			model.layoutMetadataTwo=mediaElement2.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;

			if (model.layoutMetadataTwo == null)
			{
				model.layoutMetadataTwo=new LayoutMetadata();
				model.layoutMetadataTwo.scaleMode=ScaleMode.LETTERBOX;
				model.layoutMetadataTwo.percentHeight=100;
				model.layoutMetadataTwo.percentWidth=100;
				model.layoutMetadataTwo.horizontalAlign=HorizontalAlign.LEFT;
				model.layoutMetadataTwo.verticalAlign=VerticalAlign.BOTTOM;
				mediaElement2.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataTwo);
			}

			// use this proxy Elemt to add a audio trait to a video elemet - audio trait: mute
			oProxyElementTwo=new OProxyElement(mediaElement2);
			parallelElement.addChild(mediaElement1);
			parallelElement.addChild(oProxyElementTwo);

			model.mediaContainerOne.addMediaElement(mediaElement1);
			model.mediaContainerTwo.addMediaElement(oProxyElementTwo);
			mediaPlayerOne.media=parallelElement;

			ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, 100);
		}


		/**
		 * setMediaElementOne
		 * Set the media element one.
		 * @param value:MediaElement
		 */
		public function setMediaElementOne(value:MediaElement):void
		{
			if (mediaPlayerOne.media != null)
			{
				recommendationsWatcher.unwatch();
				model.mediaContainer.removeMediaElement(mediaPlayerOne.media);
			}

			if (value != null)
			{
				// If there's no explicit layout metadata, center the content. 
				model.layoutMetadataOne=value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;

				if (model.layoutMetadataOne == null)
				{
					model.layoutMetadataOne=new LayoutMetadata();
					model.layoutMetadataOne.scaleMode=ScaleMode.LETTERBOX;
					model.layoutMetadataOne.percentHeight=100;
					model.layoutMetadataOne.percentWidth=100;
					model.layoutMetadataOne.horizontalAlign=HorizontalAlign.RIGHT;
					model.layoutMetadataOne.verticalAlign=VerticalAlign.BOTTOM;
					value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataOne);
				}
				model.mediaContainerOne.addMediaElement(value);
			}
			mediaPlayerOne.media=value;

			// Set the volume Slider
			if (defaultPlayer == DefaultPlayerState.PLAYERONE)
			{
				ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, 100);
			}
		}

		/**
		 * setMediaElementTwo
		 * Set the media element two.
		 * @param MediaElement value
		 */
		public function setMediaElementTwo(value:MediaElement):void
		{
			if (mediaPlayerTwo.media != null)
			{
				recommendationsWatcher.unwatch();
				model.mediaContainerTwo.removeMediaElement(mediaPlayerTwo.media);
			}

			if (value != null)
			{
				// If there's no explicit layout metadata, center the content. 
				model.layoutMetadataTwo=value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;

				if (model.layoutMetadataTwo == null)
				{
					model.layoutMetadataTwo=new LayoutMetadata();
					model.layoutMetadataTwo.scaleMode=ScaleMode.LETTERBOX;
					model.layoutMetadataTwo.percentHeight=100;
					model.layoutMetadataTwo.percentWidth=100;
					model.layoutMetadataTwo.horizontalAlign=HorizontalAlign.LEFT;
					model.layoutMetadataTwo.verticalAlign=VerticalAlign.BOTTOM;
					value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, model.layoutMetadataTwo);
				}
				model.mediaContainerTwo.addMediaElement(value);
			}

			mediaPlayerTwo.media=value;

			// Set the volume Slider
			if (defaultPlayer == DefaultPlayerState.PLAYERTWO)
			{
				ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, 100);
			}
		}

		/**
		 * setMuted
		 * Mute the media file.
		 * @param muted:Boolean
		 */
		public function setMuted(muted:Boolean):void
		{
			if (videoState == VideoState.SINGLE)
			{
				mediaPlayerSingle.muted=muted;
			}
			else if (videoState == VideoState.MULTI)
			{
				if (defaultPlayer == DefaultPlayerState.PLAYERONE)
				{
					mediaPlayerOne.muted=muted;
				}
			}
		}

		/**
		 * setSingleMediaElement
		 * Set the single media element.
		 * @param MediaElement value
		 */
		public function setSingleMediaElement(value:MediaElement):void
		{
			if (mediaPlayerSingle.media != null)
			{
				recommendationsWatcher.unwatch();
				model.mediaContainer.removeMediaElement(mediaPlayerSingle.media);
			}

			if (value != null)
			{
				// If there's no explicit layout metadata, center the content. 
				var layoutMetadata:LayoutMetadata=value.getMetadata(LayoutMetadata.LAYOUT_NAMESPACE) as LayoutMetadata;

				if (layoutMetadata == null)
				{
					layoutMetadata=new LayoutMetadata();
					layoutMetadata.scaleMode=ScaleMode.LETTERBOX;
					layoutMetadata.percentHeight=100;
					layoutMetadata.percentWidth=100;
					layoutMetadata.horizontalAlign=HorizontalAlign.CENTER;
					layoutMetadata.verticalAlign=VerticalAlign.MIDDLE;
					value.addMetadata(LayoutMetadata.LAYOUT_NAMESPACE, layoutMetadata);
				}
				model.mediaContainer.addMediaElement(value);
			}
			mediaPlayerSingle.media=value;
			ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, 100);
		}

		/**
		 * setVolume
		 * Set the media volume
		 * @param Number value
		 */
		public function setVolume(value:Number):void
		{
			if (videoState == VideoState.SINGLE)
			{
				mediaPlayerSingle.volume=value;
			}
			else if (videoState == VideoState.MULTI)
			{
				if (defaultPlayer == DefaultPlayerState.PLAYERONE)
				{
					mediaPlayerOne.volume=value;
				}
				/*else if( defaultPlayer == DefaultPlayerState.PLAYERTWO )
				   {
				   mediaPlayerTwo.volume = value;
				 }*/
			}
		}

		/**
		 * muteChange
		 * When the player is mute or unmute.
		 * @eventType AudioEvent event
		 */
		private function muteChange(event:AudioEvent):void
		{
			if (event.muted)
			{
				ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, 0);
				model.playerVolume=0;
				ExternalInterface.call(ExternalFunction.MUTESOUND, '');
				model.soundState=SoundState.VOLUMEMUTE;

				if (model.ccButtonBoolean == false)
				{
					model.ccBoolean=true;
					ExternalInterface.call(ExternalFunction.SETCCICONON, '');
				}
			}
			else
			{
				ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, mediaPlayerSingle.volume * 100);
				model.playerVolume=mediaPlayerSingle.volume;

				if (mediaPlayerSingle.volume > 0.50)
				{
					ExternalInterface.call(ExternalFunction.HIGHSOUND, '');
					model.soundState=SoundState.VOLUMEMAX;
				}

				if (mediaPlayerSingle.volume <= 0.50)
				{
					ExternalInterface.call(ExternalFunction.LOWSOUND, '');
					model.soundState=SoundState.VOLUMEMED;
				}

				if (mediaPlayerSingle.volume == 0)
				{
					ExternalInterface.call(ExternalFunction.NONESOUND, '');
					model.soundState=SoundState.VOLUMEMIN;
				}

				if (model.ccButtonBoolean == false)
				{
					model.ccBoolean=false;
					ExternalInterface.call(ExternalFunction.SETCCICONOFF, '');
				}

			}
		}

		/**
		 * onBufferTimeChange
		 * @eventType BufferEvent event
		 */
		private function onBufferTimeChange(event:BufferEvent):void
		{
			// ignore
		}

		/**
		 * onBufferTimerComplete
		 * When one of the state is buffering or loading the loader is visible.
		 * @eventType TimerEvent timer
		 */
		private function onBufferTimerComplete(event:TimerEvent):void
		{
			if (firstStart == true)
			{
				if (model.singleState == PlayerState.BUFFERING || model.statePlayerOne == PlayerState.BUFFERING || model.statePlayerTwo == PlayerState.BUFFERING || model.singleState == PlayerState.LOADING || model.statePlayerOne == PlayerState.LOADING || model.statePlayerTwo == PlayerState.LOADING)
				{
					model.loader=true;
				}
			}
			bufferTimer.stop();
		}

		/**
		 * onBufferingChange
		 * @eventType BufferEvent event
		 */
		private function onBufferingChange(event:BufferEvent):void
		{
			// ignore
		}

		/**
		 * onBytesLoadedChange
		 * When the loaded bytes change.
		 * @eventType LoadEvent event
		 */
		private function onBytesLoadedChange(event:LoadEvent):void
		{
			var progress:Number=0;
			model.bytesLoaded=event.bytes;

			try
			{
				progress=Math.floor(event.bytes / model.bytesTotal * 100);
				ExternalInterface.call(ExternalFunction.SETPROGRESS, progress);
				model.progressBar.setProgress(progress, 100);
				model.progress=progress;
				model.progressFullscreen=model.fullscreenProgressWidth * (progress / 100);
			}
			catch (e:TypeError)
			{
				// ignore
			}
		}

		/**
		 * onBytesTotalChange
		 * Save the total bytes of the video
		 * @eventType LoadEvent event
		 */
		private function onBytesTotalChange(event:LoadEvent):void
		{
			model.bytesTotal=event.bytes;
		}

		/**
		 * onCurrentTimeChange
		 * When the current time is change.
		 * @eventType TimeEvent event
		 */
		private function onCurrentTimeChange(event:TimeEvent):void
		{
			model.currentPlayheadSingle=event.time;

			if (model.startPlay == true)
			{
				var newPositionString:String='';

				if (model.currentPlayheadSingle <= model.currentDuration)
				{
					newPositionString=_time.getTC(model.currentPlayheadSingle);
				}
				else
				{
					newPositionString=_time.getTC(model.currentDuration);
				}

				if (newPositionString != lastNewPositionString)
				{
					ExternalInterface.call(ExternalFunction.SETCURRENTTIME, newPositionString);
					lastNewPositionString=newPositionString;
				}

				if (!mediaPlayerSingle.seeking)
				{
					ExternalInterface.call(ExternalFunction.SETPLAYHEAD, model.currentPlayheadSingle);
				}

				if (model.captionsURL != null)
				{
					Swiz.dispatchEvent(new DisplayCaptionEvent(model.currentPlayheadSingle));
				}

				if (model.fullscreenThumbDrag == false)
				{
					model.currentPlayhead=model.currentPlayheadSingle;
				}
			}
		}

		/**
		 * onDurationChange
		 * When the duration is change.
		 * @eventType TimeEvent event
		 * */
		private function onDurationChange(event:TimeEvent):void
		{
			// Store new duration as current duration in the videodisplay model
			model.currentDuration=event.time;
			model.currentDurationString=_time.getTC(event.time);
			ExternalInterface.call(ExternalFunction.SETDURATION, event.time);
			ExternalInterface.call(ExternalFunction.SETTOTALTIME, model.currentDurationString);
			ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, 100);


		}

		/**
		 * onMediaError
		 * When the media file is not available.
		 * @eventType MediaErrorEvent event
		 */
		private function onMediaError(event:MediaErrorEvent):void
		{
			model.mediaState=MediaState.ERROR;
			model.errorId=event.error.errorID.toString();
			model.errorMessage=event.error.message;
			model.errorDetail=event.error.detail;
		}

		/**
		 * onStateChange
		 * When the state is change at the player.
		 * @eventType MediaPlayerStateChangeEvent event
		 */
		private function onStateChange(event:MediaPlayerStateChangeEvent):void
		{
			if (event.state == PlayerState.LOADING)
			{
				model.loader=true;
			}

			if (model.startPlay == true)
			{
				model.singleState=event.state;

				if (event.state == PlayerState.READY)
				{
					model.currentPlayerState=PlayerState.PAUSED;
					ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING);
				}

				if ((event.state == PlayerState.BUFFERING || event.state == PlayerState.LOADING) && bufferTimer.running == false)
				{
					onBuffer();
				}
				else
				{
					model.loader=false;
				}
			}
			else
			{
				if (event.state == PlayerState.READY)
				{
					model.startPlaySingle=true;

					if (model.playerMode == PlayerModeState.ADVANCED)
					{
						startAdvancedPlayer();
					}
					else
					{
						startEmbedPlayer();
					}
					model.loader=false;
				}
			}
		}

		/**
		 * playerOneMuteChange
		 * When the player is mute or unmute.
		 * @eventType AudioEvent event
		 */
		private function playerOneMuteChange(event:AudioEvent):void
		{
			if (defaultPlayer == DefaultPlayerState.PLAYERONE)
			{
				if (event.muted)
				{
					ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, 0);
					model.playerVolume=0;
					ExternalInterface.call(ExternalFunction.MUTESOUND, '');
					model.soundState=SoundState.VOLUMEMUTE;

					if (model.ccButtonBoolean == false)
					{
						model.ccBoolean=true;
						ExternalInterface.call(ExternalFunction.SETCCICONON, '');
					}
				}
				else
				{
					ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, mediaPlayerOne.volume * 100);
					model.playerVolume=mediaPlayerOne.volume;

					if (mediaPlayerOne.volume > 0.50)
					{
						ExternalInterface.call(ExternalFunction.HIGHSOUND, '');
						model.soundState=SoundState.VOLUMEMAX;
					}

					if (mediaPlayerOne.volume <= 0.50)
					{
						ExternalInterface.call(ExternalFunction.LOWSOUND, '');
						model.soundState=SoundState.VOLUMEMED;
					}

					if (mediaPlayerOne.volume == 0)
					{
						ExternalInterface.call(ExternalFunction.NONESOUND, '');
						model.soundState=SoundState.VOLUMEMIN;
					}

					if (model.ccButtonBoolean == false)
					{
						model.ccBoolean=false;
						ExternalInterface.call(ExternalFunction.SETCCICONOFF, '');
					}

				}
			}
		}

		/**
		 * playerOneOnBufferTimeChange
		 * @eventType BufferEvent event
		 */
		private function playerOneOnBufferTimeChange(event:BufferEvent):void
		{
			// do nothing
		}

		/**
		 * playerOneOnBufferingChange
		 * When the buffering is change.
		 * @eventType BufferEvent event
		 */
		private function playerOneOnBufferingChange(event:BufferEvent):void
		{
			model.onBufferingChangeMediaOne=event.buffering;
		}

		/**
		 * playerOneOnBytesLoadedChange
		 * When the loaded bytes change
		 * @eventType LoadEvent event
		 */
		private function playerOneOnBytesLoadedChange(event:LoadEvent):void
		{
			if (model.mediaTypeTwo == model.RTMP)
			{
				onBytesLoadedChange(event);
				model.bytesTotal=model.bytesTotalOne;
			}
			else
			{
				model.bytesLoadedOne=event.bytes;
				model.progressMediaOne=Math.floor(event.bytes / model.bytesTotalOne * 100);
				ExternalInterface.call(ExternalFunction.SETPROGRESS, model.progressMediaOne);
				model.progressBar.setProgress(model.progressMediaOne, 100);
				model.progress=model.progressMediaOne;
				model.progressFullscreen=model.fullscreenProgressWidth * (model.progress / 100);
			}
		}

		/**
		 * playerOneOnBytesTotalChange
		 * Save the total bytes of the video.
		 * @eventType LoadEvent event
		 */
		private function playerOneOnBytesTotalChange(event:LoadEvent):void
		{
			model.bytesTotalOne=event.bytes;
		}

		/**
		 * playerOneOnCurrentTimeChange
		 * When the current time is change.
		 * @eventType TimeEvent event
		 */
		private function playerOneOnCurrentTimeChange(event:TimeEvent):void
		{
			model.currentPlayheadPlayerOne=event.time;

			if (model.startPlay == true)
			{
				if (maxDurationPlayer == DefaultPlayerState.PLAYERONE)
				{
					var newPositionString:String='';

					if (model.currentPlayheadPlayerOne <= model.durationPlayerOne)
					{
						newPositionString=_time.getTC(model.currentPlayheadPlayerOne);
					}
					else
					{
						newPositionString=_time.getTC(model.durationPlayerOne);
					}

					if (newPositionString != lastNewPositionString)
					{
						ExternalInterface.call(ExternalFunction.SETCURRENTTIME, newPositionString);
						lastNewPositionString=newPositionString;
					}

					if (!mediaPlayerOne.seeking)
					{
						ExternalInterface.call(ExternalFunction.SETPLAYHEAD, model.currentPlayheadPlayerOne);
					}

					if (model.captionsURL != null)
					{
						Swiz.dispatchEvent(new DisplayCaptionEvent(model.currentPlayheadPlayerOne));
					}

					if (model.fullscreenThumbDrag == false)
					{
						model.currentPlayhead=model.currentPlayheadPlayerOne;
					}
				}
			}
		}

		//fired once the video ends
		private function _videocomplete(event:TimeEvent):void
		{
			Swiz.dispatchEvent(new VideoControlEvent(VideoControlEvent.STOP));
		}

		/**
		 * playerOneOnDurationChange
		 * When the duration is change.
		 * @eventType TimeEvent event
		 */
		private function playerOneOnDurationChange(event:TimeEvent):void
		{
			model.durationPlayerOne=event.time;

			if (model.currentDuration < event.time)
			{
				onDurationChange(event);
				maxDurationPlayer=DefaultPlayerState.PLAYERONE;
			}
		}



		/**
		 *
		 *
		 * Player One
		 *
		 *
		 * */




		/**
		 * playerOneOnStateChange
		 * When the state is change at the player one.
		 * @eventType MediaPlayerStateChangeEvent event
		 */
		private function playerOneOnStateChange(event:MediaPlayerStateChangeEvent):void
		{
			model.statePlayerOne=event.state;

			if (event.state == PlayerState.LOADING)
			{
				model.loader=true;
			}

			if (model.startPlay == true)
			{
				//if( event.state == PlayerState.READY && mediaPlayerTwo.state == PlayerState.READY )
				if (event.state == PlayerState.READY)
				{
					model.currentPlayerState=PlayerState.PAUSED;
					ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING);
				}

				if ((event.state == PlayerState.BUFFERING || event.state == PlayerState.LOADING) && bufferTimer.running == false)
				{
					onBuffer();
				}
				else
				{
					model.loader=false;
				}
			}
			else
			{
				if (event.state == PlayerState.READY)
				{
					model.startPlayOne=true;

					if (model.playerMode == PlayerModeState.ADVANCED)
					{
						startAdvancedPlayer();
					}
					else
					{
						startEmbedPlayer();
					}

					model.loader=false;

				}
			}
		}

		/**
		 * playerOneVolumeChange
		 * When the volume is change in the video.
		 * @eventType AudioEvent event
		 */
		private function playerOneVolumeChange(event:AudioEvent):void
		{
			if (defaultPlayer == DefaultPlayerState.PLAYERONE)
			{
				if (mediaPlayerOne.muted == true)
				{
					mediaPlayerOne.muted=false;
				}

				if (mediaPlayerOne.volume > 0.50)
				{
					ExternalInterface.call(ExternalFunction.HIGHSOUND, '');
					model.soundState=SoundState.VOLUMEMAX;
				}

				if (mediaPlayerOne.volume <= 0.50)
				{
					ExternalInterface.call(ExternalFunction.LOWSOUND, '');
					model.soundState=SoundState.VOLUMEMED;
				}

				if (mediaPlayerOne.volume == 0)
				{
					ExternalInterface.call(ExternalFunction.NONESOUND, '');
					model.soundState=SoundState.VOLUMEMIN;
				}

				if (model.ccButtonBoolean == false && model.ccBoolean == true)
				{
					model.ccBoolean=false;
					ExternalInterface.call(ExternalFunction.SETCCICONOFF, '');
					model.soundState=SoundState.VOLUMEMUTE;
				}
			}
		}

		/**
		 * playerTwoMuteChange
		 * When the player is mute or unmute.
		 * @eventType AudioEvent event
		 */
		private function playerTwoMuteChange(event:AudioEvent):void
		{
			if (defaultPlayer == DefaultPlayerState.PLAYERTWO)
			{
				if (event.muted)
				{
					ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, 0);
					model.playerVolume=0;
					ExternalInterface.call(ExternalFunction.MUTESOUND, '');
					model.soundState=SoundState.VOLUMEMUTE;

					if (model.ccButtonBoolean == false)
					{
						model.ccBoolean=true;
						ExternalInterface.call(ExternalFunction.SETCCICONON, '');
					}
				}
				else
				{
					if (model.ccButtonBoolean == false)
					{
						model.ccBoolean=false;
						ExternalInterface.call(ExternalFunction.SETCCICONOFF, '');
					}

				}
			}
		}

		/**
		 * playerTwoOnBufferTimeChange
		 * @eventType BufferEvent event
		 */
		private function playerTwoOnBufferTimeChange(event:BufferEvent):void
		{
			// do nothing
		}

		/**
		 * playerTwoOnBufferingChange
		 * When the buffering is change.
		 * @eventType BufferEvent event
		 */
		private function playerTwoOnBufferingChange(event:BufferEvent):void
		{
			model.onBufferingChangeMediaTwo=event.buffering;
		}

		/**
		 * playerTwoOnBytesLoadedChange
		 * When the loaded bytes change.
		 * @eventType LoadEvent event
		 */
		private function playerTwoOnBytesLoadedChange(event:LoadEvent):void
		{
			if (model.mediaTypeOne == model.RTMP)
			{
				onBytesLoadedChange(event);
				model.bytesTotal=model.bytesTotalTwo;
			}
			else
			{
				model.bytesLoadedTwo=event.bytes;
				model.progressMediaTwo=Math.floor(event.bytes / model.bytesTotalTwo * 100);

				if (model.progressMediaTwo <= model.progressMediaOne)
				{
					ExternalInterface.call(ExternalFunction.SETPROGRESS, model.progressMediaTwo);
					model.progressBar.setProgress(model.progressMediaTwo, 100);
					model.progress=model.progressMediaTwo;
				}
				else
				{
					ExternalInterface.call(ExternalFunction.SETPROGRESS, model.progressMediaOne);
					model.progressBar.setProgress(model.progressMediaOne, 100);
					model.progress=model.progressMediaOne;
				}
				model.progressFullscreen=model.fullscreenProgressWidth * (model.progress / 100);
			}
		}



		/**
		 * playerTwoOnBytesTotalChange
		 * Save the total bytes of the video.
		 * @eventType LoadEvent event
		 */
		private function playerTwoOnBytesTotalChange(event:LoadEvent):void
		{
			model.bytesTotalTwo=event.bytes;
		}

		/**
		 * playerTwoOnCurrentTimeChange
		 * When the current time is change.
		 * @eventType TimeEvent event
		 */
		private function playerTwoOnCurrentTimeChange(event:TimeEvent):void
		{
			model.currentPlayheadPlayerTwo=event.time;

			if (model.startPlay == true)
			{
				if (maxDurationPlayer == DefaultPlayerState.PLAYERTWO)
				{
					var newPositionString:String='';

					if (model.currentPlayheadPlayerTwo <= model.durationPlayerTwo)
					{
						newPositionString=_time.getTC(model.currentPlayheadPlayerTwo);
					}
					else
					{
						newPositionString=_time.getTC(model.durationPlayerTwo);
					}

					if (newPositionString != lastNewPositionString)
					{
						ExternalInterface.call(ExternalFunction.SETCURRENTTIME, newPositionString);
						lastNewPositionString=newPositionString;
					}

					if (model.captionsURL != null)
					{
						Swiz.dispatchEvent(new DisplayCaptionEvent(model.currentPlayheadPlayerTwo));
					}

					if (model.fullscreenThumbDrag == false)
					{
						model.currentPlayhead=model.currentPlayheadPlayerTwo;
					}

					//if( mediaPlayerOne.playing && mediaPlayerTwo.playing )
					if (mediaPlayerOne.playing)
					{
						var timeDifference:Number=model.currentPlayheadPlayerTwo - model.currentPlayheadPlayerOne;

						if (timeDifference < -1 || timeDifference > 1)
						{
							if (mediaPlayerOne.canSeekTo(model.currentPlayheadPlayerTwo) == true)
							{
								mediaPlayerOne.seek(model.currentPlayheadPlayerTwo);
							}
						}
					}
				}
			}
		}

		/**
		 * playerTwoOnDurationChange
		 * When the duration is change.
		 * @eventType TimeEvent event
		 */
		private function playerTwoOnDurationChange(event:TimeEvent):void
		{
			model.durationPlayerTwo=event.time;

			if (model.currentDuration < event.time)
			{
				onDurationChange(event);
				maxDurationPlayer=DefaultPlayerState.PLAYERTWO;
			}
		}





		/**
		 *
		 *
		 * Player Two
		 *
		 *
		 * */




		/**
		 * playerTwoOnStateChange
		 * When the state is change at the player two.
		 * @eventType MediaPlayerStateChangeEvent event
		 */
		private function playerTwoOnStateChange(event:MediaPlayerStateChangeEvent):void
		{
			model.statePlayerTwo=event.state;

			if (event.state == PlayerState.LOADING)
			{
				model.loader=true;
			}

			if (model.startPlay == true)
			{
				if (event.state == PlayerState.READY && mediaPlayerOne.state == PlayerState.READY)
				{
					model.currentPlayerState=PlayerState.PAUSED;
					ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PLAYING);
				}

				if ((event.state == PlayerState.BUFFERING || event.state == PlayerState.LOADING) && bufferTimer.running == false)
				{
					onBuffer();
				}
				else
				{
					model.loader=false;
				}
			}
			else
			{

				if (event.state == PlayerState.READY)
				{
					model.startPlayTwo=true;

					if (model.playerMode == PlayerModeState.ADVANCED)
					{
						startAdvancedPlayer();
					}
					else
					{
						startEmbedPlayer();
					}
					model.loader=false;
				}
			}
		}

		/**
		 * playerTwoVolumeChange
		 * When the volume is change in the video.
		 * @eventType AudioEvent event
		 */
		private function playerTwoVolumeChange(event:AudioEvent):void
		{
			//ignore
		}

		/**
		 * startAdvancedPlayer
		 * Start the advanced player media files.
		 */
		private function startAdvancedPlayer():void
		{
			if (model.startPlaySingle == true)
			{
				model.startPlay=true;
				mediaPlayerSingle.play();
				mediaPlayerSingle.pause();
				model.mediaPlayer.setVolume(1);
			}

			//if( model.statePlayerOne == PlayerState.READY && model.statePlayerTwo == PlayerState.READY )
			if (model.statePlayerOne == PlayerState.READY)
			{
				model.startPlay=true;
				mediaPlayerOne.play();
				//mediaPlayerTwo.play();
				mediaPlayerOne.pause();
				//mediaPlayerTwo.pause();
				model.mediaPlayer.setVolume(1);
			}
		}

		/**
		 * startEmbedPlayer
		 * Start the embed player media files
		 */
		private function startEmbedPlayer():void
		{
			if (model.startPlaySingle == true)
			{
				if (model.videoState == VideoState.COVER)
				{
					model.videoState=model.mediaPlayer.getVideoState();
				}
				model.startPlay=true;
				mediaPlayerSingle.play();
				model.currentPlayerState=PlayerState.PLAYING;
				ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PAUSED);

			}

			if (model.statePlayerOne == PlayerState.READY && model.statePlayerTwo == PlayerState.READY)
			{
				if (model.videoState == VideoState.COVER)
				{
					model.videoState=model.mediaPlayer.getVideoState();
				}

				model.startPlay=true;
				mediaPlayerOne.play();
				mediaPlayerTwo.play();

				model.currentPlayerState=PlayerState.PLAYING;
				ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, PlayerState.PAUSED);

			}
			model.mediaPlayer.setVolume(1);
			ExternalInterface.call(ExternalFunction.SETVOLUMESLIDER, 100);
		}

		/**
		 * volumeChange
		 * When the volume is change in the video.
		 * @eventType AudioEvent event
		 */
		private function volumeChange(event:AudioEvent):void
		{
			if (mediaPlayerSingle.muted == true)
			{
				mediaPlayerSingle.muted=false;
			}

			if (mediaPlayerSingle.volume > 0.50)
			{
				ExternalInterface.call(ExternalFunction.HIGHSOUND, '');
				model.soundState=SoundState.VOLUMEMAX;
			}

			if (mediaPlayerSingle.volume <= 0.50)
			{
				ExternalInterface.call(ExternalFunction.LOWSOUND, '');
				model.soundState=SoundState.VOLUMEMED;
			}

			if (mediaPlayerSingle.volume == 0)
			{
				ExternalInterface.call(ExternalFunction.NONESOUND, '');
				model.soundState=SoundState.VOLUMEMIN;
			}

			if (model.ccButtonBoolean == false && model.ccBoolean == true)
			{
				model.ccBoolean=false;
				ExternalInterface.call(ExternalFunction.SETCCICONOFF, '');
				model.soundState=SoundState.VOLUMEMUTE;
			}
		}
	}
}

