// Todo: Read true values from ./etc/listproviders/event.asset.options.properties
// Todo: Filter type track out of options when using in new Event wizard
export const uploadAssetOptions = [
    {
        "id": "track_parts",
        "translate":"EVENTS.EVENTS.NEW.SOURCE.UPLOAD.MULTIPLE_PARTS",
        "type": "track",
        "flavorType": "multipart",
        "flavorSubType": "part+source",
        "multiple": true,
        "displayOrder": 11,
        "accept": ".avi,.flv,.m4v,.mkv,.mov,.mp4,.mpeg,.mpg,.ogv,.webm,.wmv,.flac,.m4a,.mp3,.ogg,.wav,.wma"
    },{
        "id": "track_audio",
        "translate": "EVENTS.EVENTS.NEW.SOURCE.UPLOAD.AUDIO_ONLY",
        "type": "track",
        "flavorType": "presenter-audio",
        "flavorSubType": "source",
        "multiple": false,
        "displayOrder": 12,
        "accept": ".flac,.m4a,.mp3,.ogg,.wav,.wma"
    },{
        "id": "track_presenter",
        "translate": "EVENTS.EVENTS.NEW.SOURCE.UPLOAD.NON_SEGMENTABLE",
        "type": "track",
        "flavorType": "presenter",
        "flavorSubType": "source",
        "multiple": false,
        "displayOrder": 13,
        "accept": ".avi,.flv,.m4v,.mkv,.mov,.mp4,.mpeg,.mpg,.ogv,.webm,.wmv,.flac,.m4a,.mp3,.ogg,.wav,.wma"
    },{
        "id": "track_presentation",
        "translate": "EVENTS.EVENTS.NEW.SOURCE.UPLOAD.SEGMENTABLE",
        "type": "track",
        "flavorType": "presentation",
        "flavorSubType": "source",
        "multiple": false,
        "displayOrder": 14,
        "accept": ".avi,.flv,.m4v,.mkv,.mov,.mp4,.mpeg,.mpg,.ogv,.webm,.wmv,.flac,.m4a,.mp3,.ogg,.wav,.wma"
    }, {
        'id': 'attachment_attachment_notes',
        'title': 'class handout notes',
        'flavorType': 'attachment',
        'flavorSubType': 'notes',
        'type': 'attachment'
    }, {
        'id':'catalog_captions_dfxp',
        'title': 'captions DFXP',
        'flavorType': 'captions',
        'flavorSubType': 'timedtext',
        'type': 'catalog'
    },{
        'id': 'attachment_text_webvtt',
        'title': 'Captions WebVTT',
        'flavorType': 'text',
        'flavorSubType': 'webvtt',
        'type': 'attachment'
    },{
        'id':'attachment_presenter_search_preview',
        'title': 'video list thumbnail',
        'flavorType': 'presenter',
        'flavorSubType': 'search+preview',
        'type': 'attachment'
    }
];

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

