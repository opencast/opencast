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

import { utils } from 'paella-core';
// Customized icons
import BackwardIcon from './icons/backward-icon.svg';
import ForwardIcon from './icons/forward-icon.svg';
import CaptionsIcon from './icons/captions-icon.svg';
import CloseIcon from './icons/close.svg';
import FullscreenIcon from './icons/fullscreen-icon.svg';
import FullscreenExitIcon from './icons/fullscreen-exit.svg';
import MaximizeIcon from './icons/maximize.svg';
import MinimizeIcon from './icons/minimize.svg';
import PlayIcon from './icons/play-icon.svg';
import NextIcon from './icons/slide-next-icon.svg';
import PrevIcon from './icons/slide-prev-icon.svg';
import SlidesIcon from './icons/slides-icon.svg';
import ViewModeIcon from './icons/view-mode.svg';
import VolumeHighIcon from './icons/volume-base-icon.svg';
import VolumeMidIcon from './icons/volume-mid-icon.svg';
import VolumeLowIcon from './icons/volume-low-icon.svg';
import VolumeMuteIcon from './icons/volume-mute-icon.svg';
import { getUrlFromOpencastConfig } from './js/PaellaOpencast';


async function applyDefaultTheme(paella) {
  await utils.loadStyle('style.css');

  //// Customized icons
  //// fullscreen
  await paella.addCustomPluginIcon('es.upv.paella.fullscreenButton','fullscreenIcon',FullscreenIcon);
  await paella.addCustomPluginIcon('es.upv.paella.fullscreenButton','windowedIcon', FullscreenExitIcon);

  //// volume
  await paella.addCustomPluginIcon('es.upv.paella.volumeButtonPlugin','volumeHighIcon',VolumeHighIcon);
  await paella.addCustomPluginIcon('es.upv.paella.volumeButtonPlugin','volumeMidIcon',VolumeMidIcon);
  await paella.addCustomPluginIcon('es.upv.paella.volumeButtonPlugin','volumeLowIcon',VolumeLowIcon);
  await paella.addCustomPluginIcon('es.upv.paella.volumeButtonPlugin','volumeMuteIcon',VolumeMuteIcon);

  //// layout icons
  await paella.addCustomPluginIcon('es.upv.paella.dualVideoDynamic','iconMaximize', MaximizeIcon);
  await paella.addCustomPluginIcon('es.upv.paella.dualVideoDynamic','iconMimimize', MinimizeIcon);
  await paella.addCustomPluginIcon('es.upv.paella.dualVideoDynamic','iconClose', CloseIcon);
  //// play button
  await paella.addCustomPluginIcon('es.upv.paella.playPauseButton','play', PlayIcon);

  //// layout selector
  await paella.addCustomPluginIcon('es.upv.paella.layoutSelector','layoutIcon', ViewModeIcon);

  //// backward 30 segonds
  await paella.addCustomPluginIcon('es.upv.paella.backwardButtonPlugin','backwardIcon', BackwardIcon);
  //// forward 30 segonds
  await paella.addCustomPluginIcon('es.upv.paella.forwardButtonPlugin','forwardIcon', ForwardIcon);

  //// captions icon
  await paella.addCustomPluginIcon('es.upv.paella.captionsSelectorPlugin','captionsIcon',CaptionsIcon);

  //// slides icon
  await paella.addCustomPluginIcon('es.upv.paella.frameControlButtonPlugin','photoIcon',SlidesIcon);

  //// slide navigation
  await paella.addCustomPluginIcon('es.upv.paella.nextSlideNavigatorButton','arrowRightIcon', NextIcon);
  await paella.addCustomPluginIcon('es.upv.paella.prevSlideNavigatorButton','arrowLeftIcon', PrevIcon);
}


export async function applyOpencastTheme(paella) {
  await applyDefaultTheme(paella);

  //// Load custom theme
  const config = await fetch(getUrlFromOpencastConfig('config.json'));
  const configJson = await config.json();

  const u = new URL(window.location.href);
  const ocTheme = u.searchParams.get('oc.theme')
    ?? configJson?.opencast?.theme
    ?? 'default_theme'  ;
  try {
    await paella.skin.loadSkin(getUrlFromOpencastConfig(`${ocTheme}/theme.json`));
  }
  catch (err) {
    paella.log.info(`Error applying opencast theme '${ocTheme}'. Using default theme!`);
  }
}
