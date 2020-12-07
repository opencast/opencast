// Todo: Read true values from ./etc/listproviders/event.asset.options.properties
// Todo: Filter type track out of options when using in new Event wizard
export const uploadAssetOptions = [
    {'id': 'attachment_attachment_notes',
        'title': 'class handout notes',
        'flavorType': 'attachment',
        'flavorSubType': 'notes',
        'type': 'attachment'
    }, {'id':'catalog_captions_dfxp',
        'title': 'captions DFXP',
        'flavorType': 'captions',
        'flavorSubType': 'timedtext',
        'type': 'catalog'
    },{'id': 'attachment_text_webvtt',
        'title': 'Captions WebVTT',
        'flavorType': 'text',
        'flavorSubType': 'webvtt',
        'type': 'attachment'
    },{'id':'attachment_presenter_search_preview',
        'title': 'video list thumbnail',
        'flavorType': 'presenter',
        'flavorSubType': 'search+preview',
        'type': 'attachment'
    }
];

export const sourceMetadata = {
    UPLOAD: {
        metadata: {
            start: {
                'id': 'startDate',
                'label': 'EVENTS.EVENTS.DETAILS.METADATA.START_DATE',
                'value': new Date(Date.now()).toISOString(),
                'type': 'date',
                'readOnly': false,
                'required': false,
                'tabindex': 7
            }
        }
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

