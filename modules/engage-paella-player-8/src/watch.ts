
import { defaultGetVideoIdFunc, defaultLoadConfigFunc, opencastSearchResultToPaellaManifest, OpencastPaellaPlayer, type OpencastInitParams, type OpencastPaellaConfig } from '@asicupv/paella-opencast-core';
import { Events, type Manifest, type Paella } from '@asicupv/paella-core';
import { applyQueryParams } from './applyQueryParams';
import { jwt, setUpStaticFileAuth, withJwt } from './jwt';


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
// import '@asicupv/paella-ai-plugins/paella-ai-plugins.css';
import '@asicupv/paella-opencast-core/paella-opencast-core.css';
import defaultDictionaries from './i18n/all';

const USE_OC_SERVER_FROM_URL = import.meta.env.USE_OC_SERVER_FROM_URL === 'true' || false;
const OC_PRESENTATION_URL = getOcPresentationUrl();
// const OC_PAELLA8_BASE_URL = import.meta.env.OC_PAELLA8_BASE_URL || '/paella8/ui/';
const CONFIG_FOLDER_URL = import.meta.env.CONFIG_FOLDER || '/ui/config/paella8/';
const CONFIG_URL = `${CONFIG_FOLDER_URL}config.json`;


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

// Loads `config.json`, which is used by Paella and contains our own settings
// under `opencast`. It's only fetched once; every caller gets its own copy, as
// Paella modifies the config (e.g. with skin overrides).
let opencastConfig: Promise<OpencastPaellaConfig> | null = null;
async function loadOpencastConfig(configUrl: string, player: Paella): Promise<OpencastPaellaConfig> {
    if (!opencastConfig) {
        opencastConfig = defaultLoadConfigFunc(configUrl, player);
    }
    return structuredClone(await opencastConfig);
}

// Like `defaultLoadVideoManifestFunc`, but attaches the JWT given to this page
// (if any) to the request. In that case, there is also no redirect to the
// login page if the episode can't be loaded, as the user can't log in anyway.
async function loadVideoManifest(_manifestUrl: string, _config: OpencastPaellaConfig, player: Paella): Promise<Manifest> {
    const ocPlayer = player as OpencastPaellaPlayer;
    if (ocPlayer.videoId === null) {
        throw new Error('video-id not defined.');
    }
    const url = ocPlayer.getUrlFromOpencastServer(`/search/episode.json?id=${ocPlayer.videoId}&limit=1`);
    if (url === null) {
        throw new Error('opencast-presentation-url not defined.');
    }

    let episode;
    try {
        episode = await fetch(withJwt(url)).then(async (response) => await response.json());
    } catch {
        throw new Error('Error fetching opencast episode');
    }

    if (episode?.total !== 1 || !episode.result?.[0]) {
        if (!jwt && await ocPlayer.opencastAuth.getLoggedUserName() === null) {
            ocPlayer.log.info('User not authenticated. Redirecting to authentication...', 'engage-paella-player');
            await ocPlayer.opencastAuth.auth(window.location.href);
        }
        throw new Error('Error loading video manifest.');
    }
    return await opencastSearchResultToPaellaManifest(ocPlayer, episode.result[0]);
}



window.addEventListener("load", async () => {  
  const opencastInitParams: OpencastInitParams = {
    configResourcesUrl: CONFIG_FOLDER_URL,
    configUrl: CONFIG_URL,

    loadConfig: loadOpencastConfig,
    getVideoId: defaultGetVideoIdFunc,
    loadVideoManifest,
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
  // Start setting up the service worker for JWT static file auth right away, as
  // it needs the config and may take a moment. If loading the config fails,
  // Paella fails to load as well and logs that.
  const swReady = loadOpencastConfig(CONFIG_URL, ocPlayer).catch(() => null).then(setUpStaticFileAuth);
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
  // Requests to static files are only made after loading the manifest.
  await swReady;
  await ocPlayer.loadManifest();
  ocPlayer.log.info('Paella player loaded successfully!', 'opencast-engage-player');
  // window.ocPlayer = ocPlayer;
});
