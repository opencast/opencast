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
	/**
	 * 	LanguageVO
	 */
	[Bindable]
	public class LanguageVO
	{
		/**
		 * Constructor
		 */
		public function LanguageVO(short_name:String, long_name:String)
		{
			this.short_name=short_name;
			this.long_name=long_name;
		}

		public var long_name:String;

		public var short_name:String;

		/**
		 * toString
		 * return the long_name
		 */
		public function toString():String
		{
			return long_name;
		}
	}
}


