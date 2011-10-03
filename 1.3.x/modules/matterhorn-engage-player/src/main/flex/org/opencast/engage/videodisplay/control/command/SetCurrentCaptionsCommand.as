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
package org.opencast.engage.videodisplay.control.command
{
	import org.opencast.engage.videodisplay.control.event.SetCurrentCaptionsEvent;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.videodisplay.vo.CaptionSetVO;
	import org.opencast.engage.videodisplay.vo.LanguageVO;
	import org.swizframework.Swiz;
	/**
	 *   SetCurrentCaptionsCommand
	 */
	public class SetCurrentCaptionsCommand
	{

		/**
		 * Constructor
		 */
		public function SetCurrentCaptionsCommand()
		{
			Swiz.autowire(this);
		}

		[Autowire]
		public var model:VideodisplayModel;

		/**
		 * execute
		 * When the user changes the subtitles.
		 * @eventType SetCurrentCaptionsEvent event
		 * */
		public function execute(event:SetCurrentCaptionsEvent):void
		{
			for (var i:int; i < model.languages.length; i++)
			{
				if (LanguageVO(model.languages.getItemAt(i)).long_name == event.language)
				{
					for (var j:int=0; j < model.captionSets.length; j++)
					{
						if (CaptionSetVO(model.captionSets.getItemAt(j)).lang == LanguageVO(model.languages.getItemAt(i)).short_name)
						{
							// set current capitons
							model.currentCaptionSet=CaptionSetVO(model.captionSets.getItemAt(j)).captions.toArray();
						}
					}
				}
			}
		}
	}
}

