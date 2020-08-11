import { loadFiltersSuccess, loadFiltersFailure, loadFiltersInProgress} from '../actions/tableFilterActions';
/**
* This file contains methods/thunks used to query the REST-API of Opencast to get the filters of a certain resource type.
* This information is used to filter the entries of the table in the main view.
*
* Currently only a mock json containing filters of events is returned.
*
* */
let eventsData = {
    'presentersBibliographic':{
        'translatable':false,
        'options':{
            'Opencast Project Administrator':'Opencast Project Administrator',
            'System User':'System User'
        },
        'label':'FILTERS.EVENTS.PRESENTERS_BIBLIOGRAPHIC.LABEL',
        'type':'select'
    },
    'presentersTechnical':{
        'translatable':false,
        'options':{
            'opencast_system_account':'System User',
            'admin':'Opencast Project Administrator'
        },
        'label':'FILTERS.EVENTS.PRESENTERS_TECHNICAL.LABEL',
        'type':'select'
    },
    'agent':{
        'translatable':false,
        'label':'FILTERS.EVENTS.AGENT_ID.LABEL',
        'type':'select'
    },
    'comments':{
        'translatable':false,
        'options':{
            'RESOLVED':'RESOLVED',
            'NONE':'NONE',
            'OPEN':'OPEN'
        },
        'label':'FILTERS.EVENTS.COMMENTS.LABEL',
        'type':'select'
    },
    'series':{
        'translatable':false,
        'options':{
            '2c06b745-2c5b-4f9c-b856-ad0cd5b575ca':'< & >'
        },
        'label':'FILTERS.EVENTS.SERIES.LABEL',
        'type':'select'
    },
    'location':{
        'translatable':false,
        'label':'FILTERS.EVENTS.LOCATION.LABEL',
        'type':'select'
    },
    'contributors':{
        'translatable':false,
        'options':{
            'Opencast Project Administrator':'Opencast Project Administrator',
            'System User':'System User'
        },
        'label':'FILTERS.EVENTS.CONTRIBUTORS.LABEL',
        'type':'select'
    },
    'startDate':{
        'translatable':false,
        'label':'FILTERS.EVENTS.START_DATE.LABEL',
        'type':'period'
    },
    'status':{
        'translatable':true,
        'options':{
            'EVENTS.EVENTS.STATUS.RECORDING_FAILURE':'EVENTS.EVENTS.STATUS.RECORDING_FAILURE',
            'EVENTS.EVENTS.STATUS.PENDING':'EVENTS.EVENTS.STATUS.PENDING',
            'EVENTS.EVENTS.STATUS.RECORDING':'EVENTS.EVENTS.STATUS.RECORDING',
            'EVENTS.EVENTS.STATUS.PAUSED':'EVENTS.EVENTS.STATUS.PAUSED',
            'EVENTS.EVENTS.STATUS.INGESTING':'EVENTS.EVENTS.STATUS.INGESTING',
            'EVENTS.EVENTS.STATUS.PROCESSING_FAILURE':'EVENTS.EVENTS.STATUS.PROCESSING_FAILURE',
            'EVENTS.EVENTS.STATUS.SCHEDULED':'EVENTS.EVENTS.STATUS.SCHEDULED',
            'EVENTS.EVENTS.STATUS.PROCESSING':'EVENTS.EVENTS.STATUS.PROCESSING',
            'EVENTS.EVENTS.STATUS.PROCESSING_CANCELED':'EVENTS.EVENTS.STATUS.PROCESSING_CANCELED',
            'EVENTS.EVENTS.STATUS.PROCESSED':'EVENTS.EVENTS.STATUS.PROCESSED'
        },
        'label':'FILTERS.EVENTS.STATUS.LABEL',
        'type':'select'
    }
};

let seriesData = {
    "creationDate":{
        "translatable":false,
        "label":"FILTERS.SERIES.CREATION_DATE.LABEL",
        "type":"period"
    },
    "organizers":{
        "translatable":false,
        "options":{
            "Opencast Project Administrator":"Opencast Project Administrator",
            "System User":"System User"
        },
        "label":"FILTERS.SERIES.ORGANIZERS.LABEL",
        "type":"select"
    },
    "contributors":{
        "translatable":false,
        "options":{
            "Opencast Project Administrator":"Opencast Project Administrator",
            "System User":"System User"
        },
        "label":"FILTERS.SERIES.CONTRIBUTORS.LABEL",
        "type":"select"
    }
};


// Fetch table filters from opencast instance and transform them for further use
export const fetchFilters = resource => async dispatch => {
    try {
        dispatch(loadFiltersInProgress());
        //TODO: Fetch the actual data from server
        let response;
        if (resource === 'events') {
            response = transformResponse(eventsData);
        }
        if (resource === 'series') {
            response = transformResponse(seriesData);
        }
        const filters = response
        const filtersList = Object.keys(filters.filters).map(key => {
            let filter = filters.filters[key];
            filter.name = key;
            return filter;
        });
        dispatch(loadFiltersSuccess(filtersList));
    } catch (e) {
        dispatch(loadFiltersFailure());
        console.log(e);
    }
}

// Transform received filter.json to a structure that can be used for filtering
function transformResponse(data) {
    let filters = {};
    try {
        filters = data;
        // todo: comment in when data is actually recieved from Opencast-API
        // filters = JSON.parse(data);
        for (let key in filters) {
            filters[key].value = "";
            if (!filters[key].options) {
                continue;
            }
            let filterArr = [];
            let options = filters[key].options;
            for (let subKey in options) {
                filterArr.push({value: subKey, label: options[subKey]});
            }
            filterArr = filterArr.sort(function(a,b) {
                if (a.label.toLowerCase() < b.label.toLowerCase()) {
                    return -1;
                }
                if (a.label.toLowerCase() > b.label.toLowerCase()) {
                    return 1;
                }
                return 0;
            });
            filters[key].options = filterArr;
        }
    } catch (e) {  console.log(e.message);}
    console.log(filters);
    return {filters: filters};
}

