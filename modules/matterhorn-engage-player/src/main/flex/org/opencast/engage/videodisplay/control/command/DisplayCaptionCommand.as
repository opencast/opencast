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
	import flash.external.ExternalInterface;
	import bridge.ExternalFunction;
	import org.opencast.engage.videodisplay.control.event.DisplayCaptionEvent;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.videodisplay.vo.CaptionVO;
	import org.swizframework.Swiz;
	/**
	 *   ClosedCaptionsCommand
	 */
	public class DisplayCaptionCommand
	{

		/**
		 * Constructor
		 */
		public function DisplayCaptionCommand()
		{
			Swiz.autowire(this);
		}

		[Autowire]
		public var model:VideodisplayModel;

		private var endIndex:Number=90;

		private var startIndex:Number=0;

		/**
		 * execute
		 * The event gives the new position in the video.
		 * Find the right caption in the currentCaptionSet and display the caption using the ExternalInterface.
		 * @eventType DisplayCaptionEvent event
		 */
		public function execute(event:DisplayCaptionEvent):void
		{
			var time:Number=event.position * 1000;
			var tmpCaption:CaptionVO=new CaptionVO();
			var lastPos:int=0;
			var subtitle:String='';
			var pattern:String='<br';
			var patternResult:int=0;

			// Find the caption
			if (model.currentCaptionSet != null)
			{
				for (var i:int=0; i < model.currentCaptionSet.length; i++)
				{
					tmpCaption=CaptionVO(model.currentCaptionSet[(lastPos + i) % model.currentCaptionSet.length]);

					if (tmpCaption.begin < time && time < tmpCaption.end)
					{
						lastPos+=i;

						subtitle=tmpCaption.text;

						break;
					}
				}

				// caption activated 
				if (model.ccBoolean)
				{
					// When the capion different, than send new captions
					if (model.oldSubtitle != subtitle)
					{
						model.currentSubtitle='';
						patternResult=subtitle.search(pattern);

						if (subtitle.length > model.endIndexSubtitle && patternResult == -1)
						{
							subtitle=editSubtitle(subtitle);
						}

						ExternalInterface.call(ExternalFunction.SETCAPTIONS, subtitle);
						model.currentSubtitle=subtitle;
						model.oldSubtitle=subtitle;
					}
				}
				else
				{
					model.currentSubtitle='';
					model.oldSubtitle='default';
					ExternalInterface.call(ExternalFunction.SETCAPTIONS, '');
				}
			}
		}

		/**
		 * editSubtitle
		 * Edit the current Subtitle
		 * @param String subtitle
		 * @return String _subtitle
		 */
		private function editSubtitle(subtitle:String):String
		{
			var _subtitle:String='';
			var editSubtitle:String='';
			var editBool:Boolean=false;

			editSubtitle=subtitle;

			while (editBool == false)
			{
				endIndex=model.endIndexSubtitle;

				while (editSubtitle.charAt(endIndex - 1) != ' ')
				{
					endIndex--;
				}
				_subtitle=_subtitle + editSubtitle.substring(startIndex, endIndex);
				editSubtitle=editSubtitle.substring(endIndex, editSubtitle.length);

				if (editSubtitle.length > model.endIndexSubtitle)
				{
					_subtitle=_subtitle + '<br>';
				}
				else
				{
					_subtitle=_subtitle + '<br>' + editSubtitle;
					editBool=true;
				}
			}

			return _subtitle;
		}
	}
}

