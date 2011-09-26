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
package org.opencast.engage.videodisplay.control.event
{
	import flash.events.Event;
	/**
	 *   VideoControlEvent
	 */
	public class VideoControlEvent extends Event
	{
		public static var CLOSEDCAPTIONS:String="closeCaptions";

		public static var EVENT_NAME:String='VideoControlEvent';

		public static var FASTFORWARD:String="fastForward";

		public static var HEARTIMEINFO:String="hearTimeInfo";

		public static var MUTE:String="mute";

		public static var PAUSE:String="pause";

		public static var PLAY:String="play";

		public static var REWIND:String="rewind";

		public static var SEEKEIGHT:String="seekEight";

		public static var SEEKFIVE:String="seekFive";

		public static var SEEKFOUR:String="seekFour";

		public static var SEEKNINE:String="seekNine";

		public static var SEEKONE:String="seekOne";

		public static var SEEKSEVEN:String="seekSeven";

		public static var SEEKSIX:String="seekSix";

		public static var SEEKTHREE:String="seekThree";

		public static var SEEKTWO:String="seekTwo";

		public static var SEEKZERO:String="seekZero";

		public static var SHORTCUTS:String="shortcuts";

		public static var SKIPBACKWARD:String="skipBackward";

		public static var SKIPFORWARD:String="skipForward";

		public static var STOP:String="stop";

		public static var UNMUTE:String="unmute";

		public static var VOLUMEDOWN:String="volumeDown";

		public static var VOLUMEUP:String="volumeUp";

		/**
		 * Constructor
		 */
		public function VideoControlEvent(videoControlType:String, bubbles:Boolean=false, cancelable:Boolean=false)
		{
			super(EVENT_NAME, bubbles, cancelable);
			_videoControlType=videoControlType;
		}

		private var _videoControlType:String;

		/**
		 * clone
		 * Override the inherited clone() method.
		 * @return VideoControlEvent
		 */
		override public function clone():Event
		{
			return new VideoControlEvent(videoControlType, bubbles, cancelable);
		}

		/**
		 * videoControlType
		 * Get the video control type.
		 * @return String _videoControlType
		 */
		public function get videoControlType():String
		{
			return _videoControlType;
		}
	}
}


