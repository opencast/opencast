import { getHashParameter, getUrlParameter } from "@asicupv/paella-opencast-core";
import type { Paella, TrimmingParams } from '@asicupv/paella-core';


/**
 * Converts a human-readable time string (e.g., "1h2m3s") into seconds.
 */
function humanTimeToSeconds(humanTime: string): number {
    let hours = 0;
    let minutes = 0;
    let seconds = 0;
    const hoursRE = /([0-9]+)h/i.exec(humanTime);
    const minRE = /([0-9]+)m/i.exec(humanTime);
    const secRE = /([0-9]+)s/i.exec(humanTime);
    if (hoursRE) {
        hours = parseInt(hoursRE[1]) * 60 * 60;
    }
    if (minRE) {
        minutes = parseInt(minRE[1]) * 60;
    }
    if (secRE) {
        seconds = parseInt(secRE[1]);
    }
    const totalTime = hours + minutes + seconds;
    return totalTime;
}

/**
 * Applies trimming settings to the Paella player.
 */
async function setTrimming(player: Paella, trimmingData: Required<TrimmingParams>): Promise<void> {
    const { start, end, enabled } = trimmingData;
    if (enabled && start > 0 && start < end ) {
        await player.videoContainer.setTrimming(trimmingData);
    }
};


async function applyTrimmingFromQueryParams(paella: Paella): Promise<void> {
    // Enable trimming
    // Retrieve video duration in case a default trim end time is needed
    const videoDuration = await paella.duration();    
    let trimmingData = { start: 0, end: videoDuration, enabled: false };

    // Retrieve trimming data in URL param: ?trimming=1m2s;2m
    // Retrieve trimming data in URL start-end params in seconds: ?start=12&end=345
    // Allow the 'end' param to overrule the end in trimming data,
    // Allow a 'start' or an 'end' URL parameter to be passed alone
    const trimming = getHashParameter('trimming') ?? getUrlParameter('trimming');
    const startTrimVal = getHashParameter('start') ?? getUrlParameter('start');
    const endTrimVal = getHashParameter('end') ?? getUrlParameter('end');

    if (trimming != null || startTrimVal != null || endTrimVal != null) {
        let startTrimming = 0; // default start time
        let endTrimming = videoDuration; // raw video duration;
        if (trimming) {
            const trimmingSplit = trimming.split(';');
            if (trimmingSplit.length == 2) {
                startTrimming = trimmingData.start + humanTimeToSeconds(trimmingSplit[0]);
                endTrimming = (trimmingData.end == 0)
                    ? trimmingData.start + humanTimeToSeconds(trimmingSplit[1])
                    : Math.min(trimmingData.start + humanTimeToSeconds(trimmingSplit[1]), trimmingData.end);
            }
        }
        else {
            if (startTrimVal) {
                startTrimming = trimmingData.start + Math.floor(parseInt(startTrimVal));
            }
            if (endTrimVal) {
                endTrimming = Math.min(trimmingData.start + Math.floor(parseInt(endTrimVal)), videoDuration);
            }
        }
        if (startTrimming < endTrimming && endTrimming > 0 && startTrimming >= 0) {
            trimmingData = {
                start: startTrimming,
                end: endTrimming,
                enabled: true
            };
        }
        paella.log.debug(`Setting trim to ${JSON.stringify(trimmingData)}`, 'engage-paella-player');
        await setTrimming(paella, trimmingData);
    }
}


async function applyInitialSeekFromQueryParams(paella: Paella): Promise<void> {
    // Check time param in URL and seek:  ?time=1m2s
    const timeString = getHashParameter('time') ?? getUrlParameter('time');
    // Check t param, which is seek time in seconds, to be passed as a query or hash: #t=12002
    const timeStringInSecs = getHashParameter('t') ?? getUrlParameter('t');

    if ((timeString != null) || (timeStringInSecs != null)) {
        let totalTime = 0;
        if (timeString) {
            totalTime = humanTimeToSeconds(timeString);
        }
        else if (timeStringInSecs) {
            totalTime = Math.floor(parseInt(timeStringInSecs));
        }
        paella.log.debug(`Setting initial seek to '${totalTime}' seconds`, 'engage-paella-player');
        await paella.setCurrentTime(totalTime);
    }
}

async function applyCaptionsFromQueryParams(paella: Paella): Promise<void> {
    // Check captions param in URL:  ?captions  / ?captions=<lang>
    const captions = getHashParameter('captions') ?? getUrlParameter('captions');
    if (captions != null) {
        let captionsIndex = 0;
        if (captions !== '') {
            paella.captions.some((c, idx) => {
                if (c.language == captions) {
                    captionsIndex = idx;
                    return true;
                }
                return false;
            });
        }
        const captionSelected = paella.captions[captionsIndex];
        if (captionSelected) {
            paella.log.info(`Enabling captions: ${captionSelected?.label} (${captionSelected?.language})`, 'engage-paella-player');
            paella.captionsCanvas.enableCaptions({ index: captionsIndex });
        }
    }
}

/**
 * Applies query parameters from the URL to configure the Paella player.
 * Handles trimming, seek time, and captions settings.
 */
export async function applyQueryParams(paella: Paella): Promise<void> {    
    await applyTrimmingFromQueryParams(paella);
    await applyInitialSeekFromQueryParams(paella);    
    await applyCaptionsFromQueryParams(paella);    
};
