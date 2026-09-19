
import { defaultGetVideoIdFunc, defaultLoadConfigFunc, defaultLoadVideoManifestFunc, OpencastPaellaPlayer, type OpencastInitParams } from '@asicupv/paella-opencast-core';
import { Events } from '@asicupv/paella-core';
import { applyQueryParams } from './applyQueryParams';


// Importing plugins
import { basicPlugins } from '@asicupv/paella-basic-plugins';
import { slidePlugins } from '@asicupv/paella-slide-plugins';
import { zoomPlugins } from '@asicupv/paella-zoom-plugin';
import { webglPlugins } from '@asicupv/paella-webgl-plugins';
import { videoPlugins } from '@asicupv/paella-video-plugins';
import { extraPlugins } from '@asicupv/paella-extra-plugins';
import{ opencastPlugins } from '@asicupv/paella-opencast-plugins';

// const { opencastPlugins } = await import('@asicupv/paella-opencast-plugins');

// Importing styles
import '@asicupv/paella-core/paella-core.css';
import '@asicupv/paella-basic-plugins/paella-basic-plugins.css';
import '@asicupv/paella-slide-plugins/paella-slide-plugins.css';
import '@asicupv/paella-zoom-plugin/paella-zoom-plugin.css';
// import '@asicupv/paella-webgl-plugins/paella-webgl-plugins.css';
// import '@asicupv/paella-video-plugins/paella-video-plugins.css';
import '@asicupv/paella-extra-plugins/paella-extra-plugins.css';
import '@asicupv/paella-ai-plugins/paella-ai-plugins.css';
import '@asicupv/paella-opencast-core/paella-opencast-core.css';
import '@asicupv/paella-opencast-plugins/paella-opencast-plugins.css';
import defaultDictionaries from './i18n/all';

const USE_OC_SERVER_FROM_URL = import.meta.env.USE_OC_SERVER_FROM_URL === 'true' || false;
const OC_PRESENTATION_URL = getOcPresentationUrl();
// const OC_PAELLA8_BASE_URL = import.meta.env.OC_PAELLA8_BASE_URL || '/paella8/ui/';
const CONFIG_FOLDER_URL = import.meta.env.CONFIG_FOLDER || '/ui/config/paella8/';


function getOcPresentationUrl(): string {
    let url = import.meta.env.OC_PRESENTATION_URL || '/';
    
    if (USE_OC_SERVER_FROM_URL) {    
        const params = new URLSearchParams(window.location.search);
        const ocServerUrl = params.get('ocServer');
        if (ocServerUrl) {
            url = ocServerUrl;
        }
    }
    
    return url;
}



window.addEventListener("load", async () => {  
  const opencastInitParams: OpencastInitParams = {
    configResourcesUrl: CONFIG_FOLDER_URL,
    configUrl: `${CONFIG_FOLDER_URL}config.json`,

    loadConfig: defaultLoadConfigFunc,
    getVideoId: defaultGetVideoIdFunc,
    loadVideoManifest: defaultLoadVideoManifestFunc,
    plugins: [
      ...basicPlugins,
      ...slidePlugins,
      ...zoomPlugins,
      ...videoPlugins,
      ...webglPlugins,
      ...extraPlugins,
      ...opencastPlugins
    ],
    opencast: {
      presentationUrl: OC_PRESENTATION_URL,
      // auth:
    }
  };

  const ocPlayer = new OpencastPaellaPlayer('playerContainer', opencastInitParams);
  // Load default dictionaries
  Object.entries(defaultDictionaries).forEach(([lang, dict]) =>  ocPlayer.addDictionary(lang, dict));
  
  // Update the browser tab title.
  ocPlayer.bindEvent(Events.MANIFEST_LOADED, async () => {
    ocPlayer.log.info('Player manifest loaded, updating document title.', 'engage-paella-player');
    const event = ocPlayer.getEvent();
    const videoTitle = event?.metadata?.title ?? ocPlayer.translate('Unknown video title');
    const seriesTitle = event?.metadata?.seriesTitle ?? ocPlayer.translate('No series');
    document.title = `${videoTitle} - ${seriesTitle} | Opencast`;
  });
  // Once the player is loaded, apply query parameters.
  ocPlayer.bindEvent(Events.PLAYER_LOADED, async () => {
    ocPlayer.log.info('Player loaded, applying query params', 'engage-paella-player');
    await applyQueryParams(ocPlayer);
  });

  await ocPlayer.applyOpencastTheme();
  await ocPlayer.loadManifest();
  ocPlayer.log.info('Paella player loaded successfully!', 'opencast-engage-player');
  // window.ocPlayer = ocPlayer;
});
