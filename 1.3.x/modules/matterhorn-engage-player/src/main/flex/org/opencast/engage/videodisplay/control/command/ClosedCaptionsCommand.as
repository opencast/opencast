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
	import flash.external.ExternalInterface;
	import bridge.ExternalFunction;
	import org.opencast.engage.videodisplay.control.event.ClosedCaptionsEvent;
	import org.opencast.engage.videodisplay.model.VideodisplayModel;
	import org.swizframework.Swiz;
	/**
	 *   ClosedCaptionsCommand
	 */
	public class ClosedCaptionsCommand
	{

		/**
		 * Constructor
		 */
		public function ClosedCaptionsCommand()
		{
			Swiz.autowire(this);
		}

		[Autowire]
		public var model:VideodisplayModel;

		/**
		 * execute
		 * When the user toggle the cc button.
		 * @eventType ClosedCaptionsEvent event
		 */
		public function execute(event:ClosedCaptionsEvent):void
		{
			if (model.ccBoolean == true)
			{
				model.ccBoolean=false;
				model.ccButtonBoolean=false;
				ExternalInterface.call(ExternalFunction.SETCCICONOFF, '');
			}
			else
			{
				model.ccBoolean=true;
				model.ccButtonBoolean=true;
				ExternalInterface.call(ExternalFunction.SETCCICONON, '');
			}
		}
	}
}

