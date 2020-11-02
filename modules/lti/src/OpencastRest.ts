import axios from "axios";

export interface Attachment {
    readonly type: string;
    readonly url: string;
}

export interface JobResult {
    readonly title: string;
    readonly status: string;
}

export interface MediaPackage {
    readonly attachments: Attachment[];
    readonly creators: string[];
}

export interface SearchEpisodeResult {
    readonly dcCreator: string;
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
    return v.length !== undefined;
}

export function isSingleValue(v: string | string[]): v is string {
    return v.length === undefined;
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

const debug = window.location.search.includes("&debug=true");

function hostAndPort() {
    return debug ? "http://localhost:7878" : "";
}

export async function getEventMetadata(eventId?: string): Promise<EventMetadataContainer[]> {
    const useEventId = eventId === undefined ? "new" : eventId;
    const response = await axios.get(hostAndPort() + "/lti-service-gui/" + useEventId + "/metadata");
    return response.data;
}

export async function copyEventToSeries(eventId: string, targetSeries: string): Promise<{}> {
    return axios.post(hostAndPort() + "/lti-service-gui/" + eventId + "/copy?target_series=" + targetSeries);
}

export async function searchEpisode(
    limit: number,
    offset: number,
    episodeId?: string,
    seriesId?: string,
    seriesName?: string): Promise<SearchEpisodeResults> {
    let urlSuffix = "";
    if (seriesId !== undefined)
        urlSuffix += "&sid=" + seriesId;
    if (seriesName !== undefined)
        urlSuffix += "&sname=" + seriesName;
    if (episodeId !== undefined)
        urlSuffix += "&id=" + episodeId;
    const response = await axios.get(`${hostAndPort()}/search/episode.json?limit=${limit}&offset=${offset}${urlSuffix}`);
    const resultsRaw = response.data["search-results"]["result"];
    const results = Array.isArray(resultsRaw) ? resultsRaw : resultsRaw !== undefined ? [resultsRaw] : [];
    return {
        results: results.map((result: any) => ({
            dcCreator: result.dcCreator,
            id: result.id,
            dcTitle: result.dcTitle,
            dcCreated: result.dcCreated,
            languageShortCode: result.dcLanguage,
            licenseKey: result.dcLicense,
            mediapackage: {
                creators: result.mediapackage.creators !== undefined ? result.mediapackage.creators.creator : [],
                attachments: result.mediapackage.attachments.attachment.map((attachment: any) => ({
                    type: attachment.type,
                    url: attachment.url
                }))
            }
        })),
        total: response.data["search-results"].total,
        limit: response.data["search-results"].limit,
        offset: response.data["search-results"].offset
    }
}

export async function deleteEvent(eventId: string): Promise<void> {
    return axios.delete(hostAndPort() + "/lti-service-gui/" + eventId);
}

export async function getLti(): Promise<LtiData> {
    const response = await axios.get(hostAndPort() + "/lti");
    return {
        roles: response.data.roles !== undefined ? response.data.roles.split(",") : [],
    }
}

export async function getJobs(seriesId: string): Promise<JobResult[]> {
    const response = await axios.get(hostAndPort() + "/lti-service-gui/jobs?seriesId=" + seriesId);
    return response.data.map((r: any) => ({ title: r.title, status: r.status }));
}

export async function uploadFile(
    metadata: EventMetadataContainer,
    seriesId: string,
    eventId?: string,
    presenterFile?: Blob,
    captionFile?: Blob): Promise<{}> {
    const data = new FormData();
    data.append("metadata", JSON.stringify([metadata]));
    if (eventId !== undefined)
        data.append("eventId", eventId);
    data.append("seriesId", seriesId);
    if (captionFile !== undefined)
        data.append("captions", captionFile);
    if (presenterFile !== undefined)
        data.append("presenter", presenterFile);
    return axios.post(hostAndPort() + "/lti-service-gui", data);
}
