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
package org.opencast.engage.videodisplay.control.event
{
	import flash.events.Event;
	/**
	 *   InitMediaPlayerEvent
	 */
	public class InitMediaPlayerEvent extends Event
	{
		public static var EVENT_NAME:String='InitMediaPlayerEvent';

		/**
		 * Constructor
		 */
		public function InitMediaPlayerEvent(coverURLOne:String, coverURLTwo:String, mediaURLOne:String, mediaURLTwo:String, mimetypeOne:String, mimetypeTwo:String, playerMode:String, slideLength:int, bufferTime:Number, bubbles:Boolean=false, cancelable:Boolean=false)
		{
			super(EVENT_NAME, bubbles, cancelable);
			_mediaURLOne=mediaURLOne;
			_mediaURLTwo=mediaURLTwo;
			_coverURLOne=coverURLOne;
			_coverURLTwo=coverURLTwo;
			_mimetypeOne=mimetypeOne;
			_mimetypeTwo=mimetypeTwo;
			_playerMode=playerMode;
			_slideLength=slideLength;
			_bufferTime=bufferTime;
		}

		private var _coverURLOne:String;

		private var _coverURLTwo:String;

		private var _mediaURLOne:String;

		private var _mediaURLTwo:String;

		private var _mimetypeOne:String;

		private var _mimetypeTwo:String;

		private var _playerMode:String;

		private var _slideLength:int;

		private var _bufferTime:Number;

		/**
		 * clone
		 * Override the inherited clone() method.
		 * @return InitMediaPlayerEvent
		 */
		override public function clone():Event
		{
			return new InitMediaPlayerEvent(coverURLOne, coverURLTwo, mediaURLOne, mediaURLTwo, mimetypeOne, mimetypeTwo, playerMode, slideLength, bufferTime, bubbles, cancelable);
		}

		/**
		 * coverURLOne
		 * Get the coverURLOne.
		 * @return String _coverURLOne
		 */
		public function get coverURLOne():String
		{
			return _coverURLOne;
		}

		/**
		 * coverURLTwo
		 * Get the coverURLTwo.
		 * @return _coverURLTwo
		 *  */
		public function get coverURLTwo():String
		{
			return _coverURLTwo;
		}

		/**
		 * mediaURLOne
		 * Get the mediaURLOne.
		 * @return String _mediaURLOne
		 */
		public function get mediaURLOne():String
		{
			return _mediaURLOne;
		}

		/**
		 * mediaURLTwo
		 * Get the mediaURLTwo.
		 * @return String _mediaURLTwo
		 */
		public function get mediaURLTwo():String
		{
			return _mediaURLTwo;
		}

		/**
		 * mimetypeOne
		 * Get the mimetypeOne.
		 * @return _mimetypeOne
		 */
		public function get mimetypeOne():String
		{
			return _mimetypeOne;
		}

		/**
		 * mimetypeTwo
		 * Get the mimetypeTwo.
		 * @return _mimetypeTwo
		 */
		public function get mimetypeTwo():String
		{
			return _mimetypeTwo;
		}

		/**
		 * playerMode
		 * Get the playerMode.
		 * @return _playerMode
		 */
		public function get playerMode():String
		{
			return _playerMode;
		}

		/**
		 * slideLength
		 * Get the slideLength.
		 * @return _slideLength
		 */
		public function get slideLength():int
		{
			return _slideLength;
		}

		/**
		 * bufferTime
		 * Get the bufferTime
		 * @return _bufferTime
		 */
		public function get bufferTime():Number
		{
			return _bufferTime;
		}
	}
}


