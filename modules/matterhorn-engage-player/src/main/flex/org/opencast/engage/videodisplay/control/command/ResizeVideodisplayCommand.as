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
	import mx.core.FlexGlobals;
	import org.opencast.engage.videodisplay.control.event.ResizeVideodisplayEvent;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.swizframework.Swiz;
	/**
	 *   ResizeVideodisplayCommand
	 */
	public class ResizeVideodisplayCommand
	{

		/**
		 * Constructor
		 */
		public function ResizeVideodisplayCommand()
		{
			Swiz.autowire(this);
		}

		[Autowire]
		public var model:VideodisplayModel;

		/**
		 * execute
		 * When the user resize the Videodisplay in the browser.
		 * @eventType ResizeVideodisplayEvent event
		 */
		public function execute(event:ResizeVideodisplayEvent):void
		{
			if (model.mediaContainer != null)
			{
				model.mediaContainer.height=FlexGlobals.topLevelApplication.height;
				model.mediaContainer.width=FlexGlobals.topLevelApplication.width;
			}

			var divisor:int=50;
			var fontSize:int=FlexGlobals.topLevelApplication.width / divisor;

			if (fontSize > 16)
			{
				model.fontSizeCaptions=16;
				model.endIndexSubtitle=100;
			}
			else if (fontSize < 13 && fontSize >= 9)
			{
				model.endIndexSubtitle=80;
				model.fontSizeCaptions=fontSize;
			}
			else if (fontSize < 9 && fontSize > 7)
			{
				model.endIndexSubtitle=70;
				model.fontSizeCaptions=9;
			}
			else if (fontSize < 8)
			{
				model.endIndexSubtitle=50;
				model.fontSizeCaptions=9;
			}
			else
			{
				model.fontSizeCaptions=fontSize;
			}
		}
	}
}

