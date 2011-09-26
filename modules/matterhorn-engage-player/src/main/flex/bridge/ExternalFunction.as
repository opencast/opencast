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
package bridge
{
	/*
	 * External javascript functions available for the Videodisplay
	 * Example: Change the html play/pause state
	 * ExternalInterface.call(ExternalFunction.SETPLAYPAUSESTATE, currentPlayPauseState);
	 */
	public class ExternalFunction
	{
		public static const SETDURATION:String='Opencast.Player.setDuration';

		public static const SETTOTALTIME:String='Opencast.Player.setTotalTime';

		public static const SETPROGRESS:String='Opencast.Player.setProgress';

		public static const SETCURRENTTIME:String='Opencast.Player.setCurrentTime';

		public static const SETPLAYHEAD:String='Opencast.Player.setPlayhead';

		public static const SETPLAYHEADFULLSCREEN:String='Opencast.Player.setPlayheadFullscreen';

		public static const SETVOLUME:String='Opencast.Player.setVolume';

		public static const SETVOLUMESLIDER:String='Opencast.Player.setVolumeSlider';

		public static const SETPLAYERVOLUME:String='Opencast.Player.setPlayerVolume';

		public static const SETPLAYPAUSESTATE:String='Opencast.Player.setPlayPauseState';

		public static const SETCAPTIONS:String='Opencast.Player.setCaptions';

		public static const MUTE:String='Opencast.Player.doToggleMute';

		public static const CURRENTTIME:String='Opencast.Player.currentTime';

		public static const TOGGLESHORTCUTS:String='Opencast.Player.doToggleShortcuts';

		public static const OPENCASTVOLUME:String='Opencast.Player.setOpencastVolume';

		public static const PLAYPAUSE:String='Opencast.Player.doTogglePlayPause';

		public static const DOSETVOLUME:String='Opencast.Player.doSetVolume';

		//public static const ONPLAYERREADY : String = 'Opencast.Watch.onPlayerReady';

		public static const ONPLAYERREADY:String='Opencast.Initialize.setPlayerReady';

		public static const LOWSOUND:String='Opencast.Player.lowSound';

		public static const NONESOUND:String='Opencast.Player.noneSound';

		public static const HIGHSOUND:String='Opencast.Player.highSound';

		public static const MUTESOUND:String='Opencast.Player.muteSound';

		public static const SETCCICONON:String='Opencast.Player.setCCIconOn';

		public static const SETCCICONOFF:String='Opencast.Player.setCCIconOff';

		public static const SKIPBACKWARD:String='Opencast.Player.doSkipBackward';

		public static const SKIPFORWARD:String='Opencast.Player.doSkipForward';

		public static const FLEXAJAXBRIDGE:String='Opencast.Player.loadFlexAjaxBridge';

	}
}

