// Todo: Read true values from ./etc/listproviders/event.asset.options.properties
// Todo: Filter type track out of options when using in new Event wizard

/*export const uploadAssetOptions = [
    {
        "id":"catalog_captions_dfxp",
        "type":"catalog",
        "flavorType":"captions",
        "flavorSubType":"timedtext",
        "multiple":false,
        "displayOrder":7,
        "title":"EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION.CAPTIONS_DFXP",
        "showAs":"uploadAsset"
    },
    {
        "id":"attachment_captions_webvtt",
        "type":"attachment",
        "flavorType":"text",
        "flavorSubType":"webvtt",
        "multiple":false,
        "displayOrder":3,
        "title":"EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION.CAPTIONS_WEBVTT",
        "showAs":"uploadAsset"
    },
    {
        "id":"attachment_class_handout_notes",
        "type":"attachment",
        "flavorType":"attachment",
        "flavorSubType":"notes",
        "multiple":false,
        "displayOrder":5,
        "displayOverride":"Overriding Handout Translation",
        "title":"EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION.CLASS_HANDOUT_NOTES",
        "showAs":"uploadAsset"
    },
    {
        "id":"attachment_preview_image",
        "type":"attachment",
        "flavorType":"presenter",
        "flavorSubType":"search+preview",
        "multiple":false,
        "displayOrder":2,
        "title":"EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION.PREVIEW_IMAGE",
        "showAs":"uploadAsset"
    },
    {
        "id":"catalog_smil",
        "type":"catalog",
        "flavorType":"smil",
        "flavorSubType":"smil",
        "displayOrder":4,
        "title":"EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION.SMIL",
        "showAs":"uploadAsset"
    },
    {
        "id":"track_trackpart",
        "type":"track",
        "flavorType":"trackpart",
        "flavorSubType":"*",
        "multiple":true,
        "displayOrder":12,
        "title":"EVENTS.EVENTS.NEW.SOURCE.UPLOAD.MULTIPLE_PARTS",
        "showAs":"source"
    },
    {
        "id":"track_presenter",
        "type":"track",
        "flavorType":"presenter",
        "flavorSubType":"source",
        "multiple":false,
        "displayOrder":10,
        "title":"EVENTS.EVENTS.NEW.SOURCE.UPLOAD.NON_SEGMENTABLE",
        "showAs":"source"
    },
    {
        "id":"track_presentation",
        "type":"track",
        "flavorType":"presentation",
        "flavorSubType":"source",
        "multiple":false,
        "displayOrder":11,
        "title":"EVENTS.EVENTS.NEW.SOURCE.UPLOAD.SEGMENTABLE",
        "showAs":"source"
    },
    {
        "id":"track_unknown",
        "type":"track",
        "flavorType":"presentation",
        "flavorSubType":"unknown",
        "multiple":false,
        "displayOrder":11,
        "displayOverride.SHORT":"Override Text",
        "displayFallback.DETAIL":"Fallback detail text",
        "title":"EVENTS.EVENTS.NEW.SOURCE.UPLOAD.UNKNOWN",
        "showAs":"source"
    },
    {
        "id":"track_audio",
        "type":"track",
        "flavorType":"audio",
        "flavorSubType":"source",
        "multiple":false,
        "displayOrder":6,
        "displayOverride.SHORT":"Hi I'm the translate text short override!",
        "displayOverride.DETAIL":"Hi I'm the translate text detail override!",
        "title":"EVENTS.EVENTS.NEW.SOURCE.UPLOAD.AUDIO_ONLY",
        "showAs":"source"
    }
];*/

/* additional metadata that user should provide for new events
 * UPLOAD, SCHEDULE_SINGLE, SCHEDULE_MULTIPLE signal in which case the additional metadata is required/should be provided
 * A metadata field has following keys:
 * - id: identifies the metadata field
 * - label: translation key for the label of the metadata field
 * - value: indicates the kind of value that the field should have (e.g. [] for multiple Values)
 * - type: indicates the type of metadata field (see metadata field provided by backend)
 * - readOnly: flag indicating if metadata field can be changed
 * - required: flag indicating if metadata field is required
 * - tabindex: tabindex of the metadata field
 */
export const sourceMetadata = {
    UPLOAD: {
        metadata: [
            {
                id: 'startDate',
                label: 'EVENTS.EVENTS.DETAILS.METADATA.START_DATE',
                value: new Date(Date.now()).toISOString(),
                type: 'date',
                readOnly: false,
                required: false,
                tabindex: 7
            }
        ]
    }
};

