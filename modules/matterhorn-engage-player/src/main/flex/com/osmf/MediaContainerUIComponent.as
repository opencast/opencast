/*****************************************************
 *
 *  Copyright 2010 Adobe Systems Incorporated.  All Rights Reserved.
 *
 *****************************************************
 *  The contents of this file are subject to the Mozilla Public License
 *  Version 1.1 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *  http://www.mozilla.org/MPL/
 *
 *  Software distributed under the License is distributed on an "AS IS"
 *  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 *  License for the specific language governing rights and limitations
 *  under the License.
 *
 *
 *  The Initial Developer of the Original Code is Adobe Systems Incorporated.
 *  Portions created by Adobe Systems Incorporated are Copyright (C) 2010 Adobe Systems
 *  Incorporated. All Rights Reserved.
 *
 *****************************************************/

package com.osmf
{
    import mx.core.UIComponent;

    import org.osmf.containers.MediaContainer;
    import org.osmf.events.DisplayObjectEvent;
    import org.osmf.layout.LayoutRendererBase;

    /**
     * UIComponent that exposes an OSMF MediaContainer.  Useful for integrating
     * OSMF into a Flex application.
     **/
    public class MediaContainerUIComponent extends UIComponent
    {
        /** 
        * Constructor 
        */
        public function MediaContainerUIComponent() : void
        {
            // do nothing
        }

        // Public Interface
        //

        public function set container( value : MediaContainer ) : void
        {
            if( value != _container )
            {
                if( _container && contains( _container ) )
                {
                    _container.removeEventListener( DisplayObjectEvent.MEDIA_SIZE_CHANGE, onMediaSizeChange );
                    removeChild( _container );
                }

                _container = value;

                if( _container )
                {
                    _container.addEventListener( DisplayObjectEvent.MEDIA_SIZE_CHANGE, onMediaSizeChange );
                    _container.width = unscaledWidth;
                    _container.height = unscaledHeight;
                    addChild( _container );
                }

                invalidateSize();
                invalidateDisplayList();
            }
        }

        public function get container() : MediaContainer
        {
            return _container;
        }

        // Overrides
        //

        override protected function measure() : void
        {
            super.measure();

            if( _container )
            {
                var renderer : LayoutRendererBase = _container.layoutRenderer;

                if( renderer )
                {
                    measuredWidth = renderer.measuredWidth;
                    measuredHeight = renderer.measuredHeight;
                }
            }

            if( measuredWidth == 0 || isNaN( measuredWidth ) || measuredHeight == 0 || isNaN( measuredHeight ) )
            {
                measuredWidth = DEFAULT_WIDTH;
                measuredHeight = DEFAULT_HEIGHT;
            }

        }

        override protected function updateDisplayList( unscaledWidth : Number, unscaledHeight : Number ) : void
        {
            if( _container )
            {
                _container.width = unscaledWidth;
                _container.height = unscaledHeight;
            }

            super.updateDisplayList( unscaledWidth, unscaledHeight );
        }

        // Internals
        //

        private function onMediaSizeChange( event : DisplayObjectEvent ) : void
        {
            invalidateSize();
            invalidateDisplayList();
        }

        private static const DEFAULT_WIDTH : Number = 320;

        private static const DEFAULT_HEIGHT : Number = 240;


        private var _container : MediaContainer;
    }
}