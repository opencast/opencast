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
package org.opencast.engage.videodisplay.control.responder
{
	import mx.collections.ArrayCollection;
	import mx.rpc.IResponder;
	import org.opencast.engage.videodisplay.control.event.SetCurrentCaptionsEvent;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.opencast.engage.videodisplay.vo.CaptionSetVO;
	import org.opencast.engage.videodisplay.vo.CaptionVO;
	import org.swizframework.Swiz;
	/**
	 *   LoadDFXPXMLResponder
	 */
	public class LoadDFXPXMLResponder implements IResponder
	{

		/**
		 * Constructor
		 */
		public function LoadDFXPXMLResponder()
		{
			Swiz.autowire(this);
		}

		[Autowire]
		public var model:VideodisplayModel;

		/**
		 * fault
		 */
		public function fault(info:Object):void
		{
			// do nothing
		}

		/**
		 * result
		 * Convert the xml file
		 * @param Object data
		 */
		public function result(data:Object):void
		{
			model.captionSets=new ArrayCollection();
			model.languageComboBox=new Array();
			var xData:XMLList=new XMLList(data.result);
			var divs:XMLList=xData.children().children();
			var div:XML;

			for each (div in divs)
			{
				var lang:String=div.attributes()[0];
				var style:String=div.attributes()[1];

				if (lang != null)
				{
					var captionSet:CaptionSetVO=new CaptionSetVO();
					captionSet.lang=lang;
					captionSet.style=style;
					var ps:XMLList=div.children();
					var p:XML;

					for each (p in ps)
					{
						var begin:String=p.attribute("begin");
						var end:String=p.attribute("end");
						var text:String;
						var posBegin:int=begin.lastIndexOf(".");
						var posEnd:int=end.lastIndexOf(".");

						if (posBegin != -1)
						{
							begin=begin.substring(0, posBegin);
						}
						if (posEnd != -1)
						{
							end=end.substring(0, posEnd);
						}

						if (p.hasSimpleContent())
						{
							text=p.toString();
						}
						else
						{
							text=p.children().toString();
						}
						text=text.replace(new RegExp("\n", "g"), "");
						// Replace All line breaks
						var caption:CaptionVO=new CaptionVO();
						caption.begin=stringToNumber(begin);
						caption.end=stringToNumber(end);
						caption.text=text;
						captionSet.captions.addItem(caption);
					}

					// Add the captionSet to the array
					model.captionSets.addItem(captionSet);


				}
			}

			// set current Captions, the first language in dfxp
			Swiz.dispatchEvent(new SetCurrentCaptionsEvent('English'));




		}

		/**
		 * stringToNumber
		 * Convert the string to a number.
		 * @param String timestamp
		 */
		public function stringToNumber(timestamp:String):Number
		{
			var result:Number=0.0;
			var parts:Array=timestamp.split(":");

			for (var i:int=parts.length - 1; i >= 0; i--)
			{
				switch (i)
				{
					case parts.length - 1:
						var secondParts:Array=String(parts[i]).split(".");
						result+=secondParts[0] * 1000;

						if (secondParts[1] != null)
						{
							if (secondParts[1] > 10)
							{
								result+=secondParts[1] * 10;
							}
							else if (secondParts[1] > 0)
							{
								result+=secondParts[1] * 100;
							}
						}
						break;

					case parts.length - 2:
						result+=parts[i] * 60000;
						break;

					case parts.length - 3:
						result+=parts[i] * 360000;
						break;

					default:
						break;
				}
			}

			return result;
		}
	}
}


