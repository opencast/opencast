import {type VideoIdToUrlCallback} from '@asicupv/paella-embedapi';

declare global {  
  interface Window {    
    paellaVideoIdToUrl?: VideoIdToUrlCallback;
  }
}

const currentScript: any = document.currentScript;


// Convert a video ID into a full URL to load in Paella
function paellaVideoIdToUrl (id: string) : string {
  try {
      const scriptUrl = new URL(currentScript?.src);
      const videoUrl = `${scriptUrl.protocol}//${scriptUrl.host}/paella8/ui/watch.html?id=${id}`;

      return videoUrl;
  }
  catch (e) {
      throw new Error(`Error constructing video URL: ${e}`);
  }
}



window.paellaVideoIdToUrl = paellaVideoIdToUrl;