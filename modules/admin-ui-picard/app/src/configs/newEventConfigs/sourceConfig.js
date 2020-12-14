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
    }
];

export const sourceMetadata = {
    UPLOAD: {
        metadata: [
            {
                'id': 'startDate',
                'label': 'EVENTS.EVENTS.DETAILS.METADATA.START_DATE',
                'value': new Date(Date.now()).toISOString(),
                'type': 'date',
                'readOnly': false,
                'required': false,
                'tabindex': 7
            }
        ]
    }
};

export const weekdays = {
    'MO': 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.MO',
    'TU': 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.TU',
    'WE': 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.WE',
    'TH': 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.TH',
    'FR': 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.FR',
    'SA': 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.SA',
    'SU': 'EVENTS.EVENTS.NEW.WEEKDAYSLONG.SU',
};

