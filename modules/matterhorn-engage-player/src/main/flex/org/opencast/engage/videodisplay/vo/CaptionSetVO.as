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
package org.opencast.engage.videodisplay.vo
{
	import mx.collections.ArrayCollection;
	/**
	 * 	CaptionSetVO
	 */
	[Bindable]
	public class CaptionSetVO
	{
		/**
		 * Constructor
		 */
		public function CaptionSetVO()
		{
			super();
			this.captions=new ArrayCollection();
		}

		public var captions:ArrayCollection;

		public var lang:String;

		public var style:String;

		/**
		 * toString
		 * Return the captions of the video
		 * @return String result
		 *  */
		public function toString():String
		{
			var result:String="";

			for (var i:int=0; i < captions.length; i++)
			{
				result+=lang + " " + captions.getItemAt(i).toString();
			}

			return result;
		}
	}
}


