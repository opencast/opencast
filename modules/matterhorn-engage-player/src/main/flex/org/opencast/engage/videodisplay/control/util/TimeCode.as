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
package org.opencast.engage.videodisplay.control.util
{
	/**
	 *   TimeCode
	 */
	public class TimeCode
	{

		/**
		 * Constructor
		 */
		public function TimeCode()
		{
			_showHours=true;
		}

		private var _showHours:Boolean;

		/**
		 * getTC
		 * Get the time, like HH:MM:SS.
		 * @param Number seconds
		 * @return String result
		 * */
		public function getTC(seconds:Number):String
		{
			if (typeof(seconds) != "number")
			{
				return "00:";
			}
			var result:String="";
			var myTime:Date=new Date(2007, 0, 1, 0, 0, seconds);

			// hours
			if (_showHours)
			{
				if (myTime.getHours() < 10)
				{
					result="0" + myTime.getHours() + ":";
				}
				else
				{
					result=myTime.getHours() + ":";
				}
			}

			// minutes
			if (myTime.getMinutes() < 10)
			{
				result+="0" + myTime.getMinutes() + ":";
			}
			else
			{
				result+=myTime.getMinutes() + ":";
			}

			// seconds
			if (myTime.getSeconds() < 10)
			{
				result+="0" + myTime.getSeconds();
			}
			else
			{
				result+=myTime.getSeconds();
			}

			return result;
		}

		/**
		 * showHours
		 * Set the _showHours Boolean
		 * @param Boolean show
		 */
		public function showHours(show:Boolean):void
		{
			_showHours=show;
		}
	}
}


