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
	 *   SetVolumeEvent
	 */
	public class SetVolumeEvent extends Event
	{
		public static var EVENT_NAME:String='SetVolumeEvent';

		/**
		 * Constructor
		 */
		public function SetVolumeEvent(volume:Number, bubbles:Boolean=false, cancelable:Boolean=false)
		{
			super(EVENT_NAME, bubbles, cancelable);
			_volume=volume;
		}

		private var _volume:Number;

		/**
		 * clone
		 * Override the inherited clone() method.
		 * @return SetVolumeEvent
		 */
		override public function clone():Event
		{
			return new SetVolumeEvent(volume, bubbles, cancelable);
		}

		/**
		 * volume
		 * Get the volume.
		 * @return Number _volume
		 */
		public function get volume():Number
		{
			return _volume;
		}
	}
}


