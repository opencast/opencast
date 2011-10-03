package org.opencast.engage.videodisplay.control.util
{
	import org.osmf.elements.ProxyElement;
	import org.osmf.events.MediaElementEvent;
	import org.osmf.media.MediaElement;
	import org.osmf.traits.AudioTrait;
	import org.osmf.traits.MediaTraitType;
	public class OProxyElement extends ProxyElement
	{

		public function OProxyElement(proxiedElement:MediaElement=null)
		{
			super(proxiedElement);
			var blockedTraits:Vector.<String>=new Vector.<String>();
			blockedTraits.push(MediaTraitType.AUDIO);
			super.blockedTraits=blockedTraits;
		}

		override public function set proxiedElement(value:MediaElement):void
		{

			super.proxiedElement=value;
			if (proxiedElement != null)
			{
				var audioTrait:AudioTrait=proxiedElement.getTrait(MediaTraitType.AUDIO) as AudioTrait;

				if (audioTrait != null)
				{

					audioTrait.muted=true;
				}
				else
				{
					// Wait for the trait to become available.
					proxiedElement.addEventListener(MediaElementEvent.TRAIT_ADD, onTraitAdd);
				}
			}
		}

		private function onTraitAdd(event:MediaElementEvent):void
		{
			if (event.traitType == MediaTraitType.AUDIO)
			{
				proxiedElement.removeEventListener(MediaElementEvent.TRAIT_ADD, onTraitAdd);
				var audioTrait:AudioTrait=proxiedElement.getTrait(MediaTraitType.AUDIO) as AudioTrait;
				audioTrait.volume=0;
				audioTrait.muted=true;
			}
		}
	}
}

