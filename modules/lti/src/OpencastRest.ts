import { parsedQueryString } from "./utils";

export interface Attachment {
    readonly type: string;
    readonly url: string;
}

export interface Track {
    readonly type: string;
    readonly url: string;
    readonly resolution: { width: number, height: number} | undefined;
}

export interface JobResult {
    readonly title: string;
    readonly status: string;
}

export interface MediaPackage {
    readonly attachments: Attachment[];
    readonly creators: string[];
    readonly tracks: Track[] | undefined;
}

export interface SearchEpisodeResult {
    readonly dcCreator?: string;
    readonly id: string;
    readonly dcTitle: string;
    readonly dcCreated: string;
    readonly mediapackage: MediaPackage;
    readonly languageShortCode: string;
    readonly licenseKey: string;
}

export interface SearchEpisodeResults {
    readonly results: SearchEpisodeResult[];
    readonly total: number;
    readonly limit: number;
    readonly offset: number;
}

export type EventMetadataCollection = {
    [name: string]: string
};

export interface EventMetadataField {
    readonly readOnly: boolean;
    readonly translatable: boolean;
    readonly id: string;
    readonly label: string;
    readonly type: "text" | "text_long" | "ordered_text" | "mixed_text" | "date";
    readonly value: string | string[];
    readonly required: boolean;
    readonly collection?: EventMetadataCollection;
}

export interface EventMetadataContainer {
    readonly flavor: string;
    readonly locked?: string;
    readonly title: string;
    readonly fields: EventMetadataField[];
}

export interface LtiData {
    readonly roles: string[];
}

interface Config {
    readonly excludeLiveStreams: boolean;
    readonly onlyLiveStreams: boolean;
}

export function findField(
    id: string,
    metadata: EventMetadataContainer): EventMetadataField | undefined {
    return metadata.fields.find((f: EventMetadataField) => f.id === id);
}

export function findFieldValue(
    id: string,
    metadata: EventMetadataContainer): string | string[] | undefined {
    const result = findField(id, metadata);
    if (result === undefined)
        return undefined;
    return result.value;
}

export function findFieldSingleValue(
    id: string,
    metadata: EventMetadataContainer): string | undefined {
    const result = findFieldValue(id, metadata);
    if (result === undefined)
        return;
    return typeof result === "string" ? result : undefined;
}

export function isMultiValue(v: string | string[]): v is string[] {
    return typeof v === 'object';
}

export function isSingleValue(v: string | string[]): v is string {
    return typeof v === 'string';
}

export function findFieldMultiValue(
    id: string,
    metadata: EventMetadataContainer): string[] | undefined {
    const result = findFieldValue(id, metadata);
    if (result === undefined)
        return;
    if (!isMultiValue(result))
        return;
    return result;
}

export function findFieldCollection(
    id: string,
    metadata: EventMetadataContainer): EventMetadataCollection | undefined {
    const field = findField(id, metadata);
    return field === undefined ? undefined : field.collection;
}

export function collectionToPairs(c: EventMetadataCollection): [string, string][] {
    return Object.keys(c).map((k) => [k, c[k]]);
}

const debug = parsedQueryString().debug === "true";

function hostAndPort() {
    return debug ? "http://localhost:7878" : "";
}

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
    const response = await fetch(url, init);
    if (!response.ok) {
        throw new Error(`Request to ${url} failed: ${response.status} ${response.statusText}`);
    }
    return response.json();
}

async function fetchNoContent(url: string, init?: RequestInit): Promise<void> {
    const response = await fetch(url, init);
    if (!response.ok) {
        throw new Error(`Request to ${url} failed: ${response.status} ${response.statusText}`);
    }
}

export async function getEventMetadata(eventId?: string): Promise<EventMetadataContainer[]> {
    const useEventId = eventId === undefined ? "new" : eventId;
    return fetchJson<EventMetadataContainer[]>(hostAndPort() + "/lti-service-gui/" + useEventId + "/metadata");
}

export async function copyEventToSeries(eventId: string, targetSeries: string): Promise<void> {
    return fetchNoContent(
        hostAndPort() + "/lti-service-gui/" + eventId + "/copy?target_series=" + targetSeries,
        { method: "POST" }
    );
}

async function getConfig(): Promise<Config> {
    try {
        const config = await fetchJson<any>(hostAndPort() + '/ui/config/ltitools/config.json');
        return {
            excludeLiveStreams: config.series.excludeLiveStreams !== undefined ? config.series.excludeLiveStreams : false,
            onlyLiveStreams: config.series.onlyLiveStreams !== undefined ? config.series.onlyLiveStreams : false
        }
    } catch (_) {
        return {
            excludeLiveStreams: false,
            onlyLiveStreams: false
        }
    }
}


/**
 * Parse resolution from string to object if possible
 * A resolution is expected to have the format {width}x{height}
 * @param resolution resolution to be parsed
 */
const parseResolutionFromString = (resolution: any) => {
  if (typeof resolution === "string" && /[0-9]+x[0-9]+/.test(resolution)) {
    return {
      width: parseInt(resolution.split('x')[0]),
      height: parseInt(resolution.split('x')[1]),
    }
  }
  return undefined;
}

/**
 * Track is not guaranteed to be an array or even exist at all, so we need to handle different cases
 * @param result a result from the search query
 */
const parseTracksFromResult = (result: any) => {
  if (Array.isArray(result.mediapackage.media.track)) {
    return (
      result.mediapackage.media.track.reduce((res: Track[], track: any) => {
        // Avoid tracks that belong to an adaptive streaming publication
        if(!('logicalname' in track) && 'video' in track && 'resolution' in track.video) {
          res.push({
            type: track.type,
            url: track.url,
            resolution: parseResolutionFromString(track.video.resolution)
          })
        }
        return res;
      }, [])
    )
  } else if (result.mediapackage.media.track !== null) {
    // Avoid tracks that belong to an adaptive streaming publication
    if ('logicalname' in result.mediapackage.media.track ||
        !('video' in result.mediapackage.media.track && 'resolution' in result.mediapackage.media.track.video)) {
      return undefined;
    }
    return {
      type: result.mediapackage.media.track.type,
      url: result.mediapackage.media.track.url,
      resolution: parseResolutionFromString(result.mediapackage.media.track.video.resolution),
    }
  }
  return undefined;
}

async function parseLiveFromConfig() : Promise<String> {
    try {
        const config = await getConfig();
        if (config.excludeLiveStreams && !config.onlyLiveStreams) {
            return "false";
        }
        if (!config.excludeLiveStreams && config.onlyLiveStreams) {
            return "true";
        }
        return "";
    } catch (_) {
        return "";
    }
}

export async function searchEpisode(
    limit: number,
    offset: number,
    episodeId?: string,
    seriesId?: string,
    seriesName?: string,
    sort?: string,
    live?: string): Promise<SearchEpisodeResults> {
    let urlSuffix = "";
    if (seriesId !== undefined)
        urlSuffix += "&sid=" + seriesId;
    if (seriesName !== undefined)
        urlSuffix += "&sname=" + seriesName;
    if (episodeId !== undefined)
        urlSuffix += "&id=" + episodeId;
    if (sort !== undefined)
        urlSuffix += "&sort=" + sort;
    if (live !== undefined) {
        urlSuffix += "&live=" + live;
    } else {
        const config_live = await parseLiveFromConfig();
        if (typeof config_live === "string" && config_live !== "")
            urlSuffix += "&live=" + config_live;
    }
    const url = `${hostAndPort()}/search/episode.json?limit=${limit}&offset=${offset}${urlSuffix}`;
    const searchResult = await fetchJson<any>(url);
    const resultsRaw = searchResult["result"];
    const results = Array.isArray(resultsRaw) ? resultsRaw : resultsRaw !== undefined ? [resultsRaw] : [];
    return {
        results: results.map((result: any) => ({
            dcCreator: result.dc.creator?.[0],
            id: result.mediapackage.id,
            dcTitle: result.dc.title[0],
            dcCreated: result.dc.created?.[0],
            languageShortCode: result.dc?.language?.[0],
            licenseKey: result.dc?.license?.[0],
            mediapackage: {
                creators: result.mediapackage.creators !== undefined
                    ? Array.isArray(result.mediapackage.creators.creator)
                        ? result.mediapackage.creators.creator
                        : [result.mediapackage.creators.creator]
                    : [],
                attachments: Array.from(
                    Array.isArray(result.mediapackage.attachments.attachment)
                        ? result.mediapackage.attachments.attachment
                        : Array.of(result.mediapackage.attachments.attachment),
                    (attachment: any) => ({
                        type: attachment.type,
                        url: attachment.url
                    })),
                tracks: parseTracksFromResult(result)
            }
        })),
        total: searchResult.total,
        limit: searchResult.limit,
        offset: searchResult.offset
    }
}

export async function deleteEvent(eventId: string): Promise<void> {
    return fetchNoContent(hostAndPort() + "/lti-service-gui/" + eventId, { method: "DELETE" });
}

export async function getLti(): Promise<LtiData> {
    const lti = await fetchJson<any>(hostAndPort() + "/lti");
    return {
        roles: lti.roles !== undefined ? lti.roles.split(",") : [],
    }
}

export async function getJobs(seriesId: string): Promise<JobResult[]> {
    const jobs = await fetchJson<any>(hostAndPort() + "/lti-service-gui/jobs?seriesId=" + seriesId);
    return jobs.map((r: any) => ({ title: r.title, status: r.status }));
}

export async function uploadFile(
    metadata: EventMetadataContainer,
    seriesId: string,
    eventId?: string,
    presenterFile?: Blob,
    captionFile?: Blob,
    captionFormat?: string,
    captionLanguage?: string,
    setUploadPogress?: (progress: number) => void): Promise<{}> {
    const percentage = 100;
    const data = new FormData();
    data.append("metadata", JSON.stringify([metadata]));
    if (eventId !== undefined)
        data.append("eventId", eventId);
    data.append("seriesId", seriesId);
    if (captionFormat !== undefined)
        data.append("captionFormat", captionFormat);
    if (captionLanguage !== undefined)
        data.append("captionLanguage", captionLanguage);
    if (captionFile !== undefined)
        data.append("captions", captionFile);
    if (presenterFile !== undefined)
        data.append("presenter", presenterFile);
    return new Promise((resolve, reject) => {
        const request = new XMLHttpRequest();
        request.open("POST", hostAndPort() + "/lti-service-gui");
        if (setUploadPogress !== undefined) {
            request.upload.onprogress = (event) =>
                setUploadPogress(Math.round(event.loaded * percentage / (event.total || 1)));
        }
        request.onload = () => {
            if (request.status >= 200 && request.status < 300) {
                resolve({});
            } else {
                reject(new Error(`Upload failed: ${request.status} ${request.statusText}`));
            }
        };
        request.onerror = () => reject(new Error("Upload failed"));
        request.send(data);
    });
}
