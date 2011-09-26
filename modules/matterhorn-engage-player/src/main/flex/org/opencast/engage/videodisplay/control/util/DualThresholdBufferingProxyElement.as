package org.opencast.engage.videodisplay.control.util
{
	import org.osmf.elements.ProxyElement;
	import org.osmf.events.BufferEvent;
	import org.osmf.events.MediaElementEvent;
	import org.osmf.events.PlayEvent;
	import org.osmf.events.SeekEvent;
	import org.osmf.media.MediaElement;
	import org.osmf.traits.BufferTrait;
	import org.osmf.traits.MediaTraitType;
	import org.osmf.traits.PlayState;
	import org.osmf.traits.TraitEventDispatcher;

	/**
	 * Proxy class which sets the IBufferable.bufferTime property to
	 * an initial value when the IBufferable trait is available, and
	 * an expanded value when the proxied MediaElement first exits
	 * the buffer state.
	 **/
	public class DualThresholdBufferingProxyElement extends ProxyElement
	{
		public function DualThresholdBufferingProxyElement(initialBufferTime:Number, expandedBufferTime:Number, wrappedElement:MediaElement)
		{
			super(wrappedElement);

			this.initialBufferTime = initialBufferTime;
			this.expandedBufferTime = expandedBufferTime;

			var dispatcher:TraitEventDispatcher = new TraitEventDispatcher();
			dispatcher.media = wrappedElement;

			wrappedElement.addEventListener(MediaElementEvent.TRAIT_ADD, processTraitAdd);
			dispatcher.addEventListener(BufferEvent.BUFFERING_CHANGE, processBufferingChange);
			dispatcher.addEventListener(SeekEvent.SEEKING_CHANGE, processSeekingChange);
			dispatcher.addEventListener(PlayEvent.PLAY_STATE_CHANGE, processPlayStateChange);

		}

		private function processTraitAdd(traitType:String):void
		{
			if (traitType == MediaTraitType.BUFFER)
			{
				// As soon as we can buffer, set the initial buffer time.
				var bufferTrait:BufferTrait = getTrait(MediaTraitType.BUFFER) as BufferTrait;
				bufferTrait.bufferTime = initialBufferTime;
			}
		}

		private function processBufferingChange(event:BufferEvent):void
		{
			// As soon as we stop buffering, make sure our buffer time is
			// set to the maximum.
			if (event.buffering == false)
			{
				var bufferTrait:BufferTrait = getTrait(MediaTraitType.BUFFER) as BufferTrait;
				bufferTrait.bufferTime = expandedBufferTime;
			}
		}

		private function processSeekingChange(event:SeekEvent):void
		{
			// Whenever we seek, reset our buffer time to the minimum so that
			// playback starts quickly after the seek.
			if (event.seeking == true)
			{
				var bufferTrait:BufferTrait = getTrait(MediaTraitType.BUFFER) as BufferTrait;
				bufferTrait.bufferTime = initialBufferTime;
			}
		}

		private function processPlayStateChange(event:PlayEvent):void
		{
			// Whenever we pause, reset our buffer time to the minimum so that
			// playback starts quickly after the unpause.
			if (event.playState == PlayState.PAUSED)
			{
				var bufferTrait:BufferTrait = getTrait(MediaTraitType.BUFFER) as BufferTrait;
				bufferTrait.bufferTime = initialBufferTime;
			}
		}

		private var initialBufferTime:Number;
		private var expandedBufferTime:Number;
	}
}

