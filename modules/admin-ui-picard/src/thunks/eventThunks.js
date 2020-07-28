import {loadEventsFailure, loadEventsInProgress, loadEventsSuccess} from "../actions/eventActions";
import * as tfs from '../selectors/tableFilterSelectors';
import * as ts from '../selectors/tableSelectors';

const data ={
    "total": 12000,
    "offset": 0,
    "count": 12,
    "limit": 0,
    "results": [
    {
        "end_date": "2018-08-31T12:55:00Z",
        "agent_id": "F300.1",
        "needs_cutting": false,
        "source": "SCHEDULE",
        "title": "Mock Event Planned",
        "has_open_comments": false,
        "has_preview": false,
        "technical_presenters": [],
        "has_comments": false,
        "technical_end": "2018-08-31T12:55:00Z",
        "presenters": [
            "Peter Planner",
            "Harry Potter",
            "Peter Parker"
        ],
        "technical_start": "2018-08-31T12:00:00Z",
        "location": "F300.1",
        "managedAcl": "",
        "workflow_state": "",
        "id": "9cc888e8-cdf6-4974-bf18-effecdadfa94",
        "start_date": "2018-08-31T12:00:00Z",
        "event_status": "EVENTS.EVENTS.STATUS.SCHEDULED",
        "displayable_status": "EVENTS.EVENTS.STATUS.SCHEDULED",
        "publications": []
    },
    {
        "end_date": "2018-08-10T11:22:51Z",
        "agent_id": "",
        "needs_cutting": false,
        "source": "ARCHIVE",
        "title": "Mock Event Processing",
        "has_open_comments": false,
        "has_preview": true,
        "technical_presenters": [],
        "has_comments": false,
        "technical_end": "2018-08-10T11:22:51Z",
        "presenters": [
            "Peter Processor"
        ],
        "technical_start": "2018-08-10T11:22:51Z",
        "location": "",
        "managedAcl": "",
        "workflow_state": "PROCESSING",
        "id": "c3a4f68d-14d4-47e2-8981-8eb2fb300d3a",
        "start_date": "2018-08-10T11:22:51Z",
        "event_status": "EVENTS.EVENTS.STATUS.PROCESSING",
        "displayable_status": "EVENTS.EVENTS.STATUS.PROCESSING",
        "publications": [],
        "series": {
            "id": "32aefb11-526c-4d62-9bd1-42a2cb85233f",
            "title": "Mock Series"
        }
    },
    {
        "end_date": "2018-08-10T12:39:02Z",
        "agent_id": "",
        "needs_cutting": true,
        "source": "ARCHIVE",
        "title": "Mock Event Unpublished",
        "has_open_comments": true,
        "has_preview": true,
        "technical_presenters": [],
        "has_comments": true,
        "technical_end": "2018-08-10T12:39:02Z",
        "presenters": [
            "Usain Unpublished",
            "Sam Winchester"
        ],
        "technical_start": "2018-08-10T12:39:02Z",
        "location": "",
        "managedAcl": "",
        "workflow_state": "PAUSED",
        "id": "1a2a040b-ef73-4323-93dd-052b86036b75",
        "start_date": "2018-08-10T12:39:02Z",
        "event_status": "EVENTS.EVENTS.STATUS.PROCESSED",
        "displayable_status": "EVENTS.EVENTS.STATUS.PROCESSED",
        "publications": []
    },
    {
        "end_date": "2018-08-10T12:36:57Z",
        "agent_id": "",
        "needs_cutting": false,
        "source": "ARCHIVE",
        "title": "Mock Event Published",
        "has_open_comments": false,
        "has_preview": true,
        "technical_presenters": [],
        "has_comments": false,
        "technical_end": "2018-08-10T12:36:57Z",
        "presenters": [
            "Pamela Published"
        ],
        "technical_start": "2018-08-10T12:36:57Z",
        "location": "",
        "managedAcl": "",
        "workflow_state": "SUCCEEDED",
        "id": "c990ea15-e5ed-4fcf-bc17-cb070091c343",
        "start_date": "2018-08-10T12:36:57Z",
        "event_status": "EVENTS.EVENTS.STATUS.PROCESSED",
        "displayable_status": "EVENTS.EVENTS.STATUS.PROCESSED",
        "publications": [
            {
                "name": "EVENTS.EVENTS.DETAILS.PUBLICATIONS.ENGAGE",
                "id": "engage-player",
                "url": "http://localhost:8080/engage/theodul/ui/core.html?id=c990ea15-e5ed-4fcf-bc17-cb070091c343"
            },
            {
                "name": "EVENTS.EVENTS.DETAILS.PUBLICATIONS.CUSTOM",
                "id": "api",
                "url": "http://localhost:8080/api/events/c990ea15-e5ed-4fcf-bc17-cb070091c343"
            },
            {
                "name": "EVENTS.EVENTS.DETAILS.PUBLICATIONS.CUSTOM",
                "id": "oaipmh-default",
                "url": "/oaipmh/default?verb=ListMetadataFormats&identifier=c990ea15-e5ed-4fcf-bc17-cb070091c343"
            }
        ]
    },
    {
            "end_date": "2018-08-31T12:55:00Z",
            "agent_id": "F300.1",
            "needs_cutting": false,
            "source": "SCHEDULE",
            "title": "Mock Event Planned",
            "has_open_comments": false,
            "has_preview": false,
            "technical_presenters": [],
            "has_comments": false,
            "technical_end": "2018-08-31T12:55:00Z",
            "presenters": [
                "Peter Planner"
            ],
            "technical_start": "2018-08-31T12:00:00Z",
            "location": "F300.1",
            "managedAcl": "",
            "workflow_state": "",
            "id": "9cc888e8-cdf6-4974-bf18-effecdadfa95",
            "start_date": "2018-08-31T12:00:00Z",
            "event_status": "EVENTS.EVENTS.STATUS.SCHEDULED",
            "displayable_status": "EVENTS.EVENTS.STATUS.SCHEDULED",
            "publications": []
        },
    {
            "end_date": "2018-08-10T11:22:51Z",
            "agent_id": "",
            "needs_cutting": false,
            "source": "ARCHIVE",
            "title": "Mock Event Processing",
            "has_open_comments": false,
            "has_preview": true,
            "technical_presenters": [],
            "has_comments": false,
            "technical_end": "2018-08-10T11:22:51Z",
            "presenters": [
                "Peter Processor"
            ],
            "technical_start": "2018-08-10T11:22:51Z",
            "location": "",
            "managedAcl": "",
            "workflow_state": "PROCESSING",
            "id": "c3a4f68d-14d4-47e2-8981-8eb2fb300d3b",
            "start_date": "2018-08-10T11:22:51Z",
            "event_status": "EVENTS.EVENTS.STATUS.PROCESSING",
            "displayable_status": "EVENTS.EVENTS.STATUS.PROCESSING",
            "publications": []
        },
    {
            "end_date": "2018-08-10T12:39:02Z",
            "agent_id": "",
            "needs_cutting": true,
            "source": "ARCHIVE",
            "title": "Mock Event Unpublished",
            "has_open_comments": true,
            "has_preview": true,
            "technical_presenters": [],
            "has_comments": true,
            "technical_end": "2018-08-10T12:39:02Z",
            "presenters": [
                "Usain Unpublished"
            ],
            "technical_start": "2018-08-10T12:39:02Z",
            "location": "",
            "managedAcl": "",
            "workflow_state": "PAUSED",
            "id": "1a2a040b-ef73-4323-93dd-052b86036b76",
            "start_date": "2018-08-10T12:39:02Z",
            "event_status": "EVENTS.EVENTS.STATUS.PROCESSED",
            "displayable_status": "EVENTS.EVENTS.STATUS.PROCESSED",
            "publications": []
        },
    {
            "end_date": "2018-08-10T12:36:57Z",
            "agent_id": "",
            "needs_cutting": false,
            "source": "ARCHIVE",
            "title": "Mock Event Published",
            "has_open_comments": false,
            "has_preview": true,
            "technical_presenters": [],
            "has_comments": false,
            "technical_end": "2018-08-10T12:36:57Z",
            "presenters": [
                "Pamela Published",
                "Geralt of Riva"
            ],
            "technical_start": "2018-08-10T12:36:57Z",
            "location": "",
            "managedAcl": "",
            "workflow_state": "SUCCEEDED",
            "id": "c990ea15-e5ed-4fcf-bc17-cb070091c344",
            "start_date": "2018-08-10T12:36:57Z",
            "event_status": "EVENTS.EVENTS.STATUS.PROCESSED",
            "displayable_status": "EVENTS.EVENTS.STATUS.PROCESSED",
            "publications": [
                {
                    "name": "EVENTS.EVENTS.DETAILS.PUBLICATIONS.ENGAGE",
                    "id": "engage-player",
                    "url": "http://localhost:8080/engage/theodul/ui/core.html?id=c990ea15-e5ed-4fcf-bc17-cb070091c343"
                },
                {
                    "name": "EVENTS.EVENTS.DETAILS.PUBLICATIONS.CUSTOM",
                    "id": "api",
                    "url": "http://localhost:8080/api/events/c990ea15-e5ed-4fcf-bc17-cb070091c343"
                },
                {
                    "name": "EVENTS.EVENTS.DETAILS.PUBLICATIONS.CUSTOM",
                    "id": "oaipmh-default",
                    "url": "/oaipmh/default?verb=ListMetadataFormats&identifier=c990ea15-e5ed-4fcf-bc17-cb070091c343"
                }
            ]
        },
    {
            "end_date": "2018-08-31T12:55:00Z",
            "agent_id": "F300.1",
            "needs_cutting": false,
            "source": "SCHEDULE",
            "title": "Mock Event Planned",
            "has_open_comments": false,
            "has_preview": false,
            "technical_presenters": [],
            "has_comments": false,
            "technical_end": "2018-08-31T12:55:00Z",
            "presenters": [
                "Peter Planner"
            ],
            "technical_start": "2018-08-31T12:00:00Z",
            "location": "F300.1",
            "managedAcl": "",
            "workflow_state": "",
            "id": "9cc888e8-cdf6-4974-bf18-effecdadfa96",
            "start_date": "2018-08-31T12:00:00Z",
            "event_status": "EVENTS.EVENTS.STATUS.SCHEDULED",
            "displayable_status": "EVENTS.EVENTS.STATUS.SCHEDULED",
            "publications": []
        },
    {
            "end_date": "2018-08-10T11:22:51Z",
            "agent_id": "",
            "needs_cutting": false,
            "source": "ARCHIVE",
            "title": "Mock Event Processing",
            "has_open_comments": false,
            "has_preview": true,
            "technical_presenters": [],
            "has_comments": false,
            "technical_end": "2018-08-10T11:22:51Z",
            "presenters": [
                "Peter Processor"
            ],
            "technical_start": "2018-08-10T11:22:51Z",
            "location": "",
            "managedAcl": "",
            "workflow_state": "PROCESSING",
            "id": "c3a4f68d-14d4-47e2-8981-8eb2fb300d3c",
            "start_date": "2018-08-10T11:22:51Z",
            "event_status": "EVENTS.EVENTS.STATUS.PROCESSING",
            "displayable_status": "EVENTS.EVENTS.STATUS.PROCESSING",
            "publications": []
        },
    {
            "end_date": "2018-08-10T12:39:02Z",
            "agent_id": "",
            "needs_cutting": true,
            "source": "ARCHIVE",
            "title": "Mock Event Unpublished",
            "has_open_comments": true,
            "has_preview": true,
            "technical_presenters": [],
            "has_comments": true,
            "technical_end": "2018-08-10T12:39:02Z",
            "presenters": [
                "Usain Unpublished",
                "Harley Quinn",
                "Bruce Wayne",
                "Selina Kyle"
            ],
            "technical_start": "2018-08-10T12:39:02Z",
            "location": "",
            "managedAcl": "",
            "workflow_state": "PAUSED",
            "id": "1a2a040b-ef73-4323-93dd-052b86036b77",
            "start_date": "2018-08-10T12:39:02Z",
            "event_status": "EVENTS.EVENTS.STATUS.PROCESSED",
            "displayable_status": "EVENTS.EVENTS.STATUS.PROCESSED",
            "publications": []
        },
    {
            "end_date": "2018-08-10T12:36:57Z",
            "agent_id": "",
            "needs_cutting": false,
            "source": "ARCHIVE",
            "title": "Mock Event Published",
            "has_open_comments": false,
            "has_preview": true,
            "technical_presenters": [],
            "has_comments": false,
            "technical_end": "2018-08-10T12:36:57Z",
            "presenters": [
                "Pamela Published"
            ],
            "technical_start": "2018-08-10T12:36:57Z",
            "location": "",
            "managedAcl": "",
            "workflow_state": "SUCCEEDED",
            "id": "c990ea15-e5ed-4fcf-bc17-cb070091c345",
            "start_date": "2018-08-10T12:36:57Z",
            "event_status": "EVENTS.EVENTS.STATUS.PROCESSED",
            "displayable_status": "EVENTS.EVENTS.STATUS.PROCESSED",
            "publications": [
                {
                    "name": "EVENTS.EVENTS.DETAILS.PUBLICATIONS.ENGAGE",
                    "id": "engage-player",
                    "url": "http://localhost:8080/engage/theodul/ui/core.html?id=c990ea15-e5ed-4fcf-bc17-cb070091c343"
                },
                {
                    "name": "EVENTS.EVENTS.DETAILS.PUBLICATIONS.CUSTOM",
                    "id": "api",
                    "url": "http://localhost:8080/api/events/c990ea15-e5ed-4fcf-bc17-cb070091c343"
                },
                {
                    "name": "EVENTS.EVENTS.DETAILS.PUBLICATIONS.CUSTOM",
                    "id": "oaipmh-default",
                    "url": "/oaipmh/default?verb=ListMetadataFormats&identifier=c990ea15-e5ed-4fcf-bc17-cb070091c343"
                }
            ]
        }
]
};

export const fetchEvents = (filter, sort) => async (dispatch, getState) => {
    try {
        dispatch(loadEventsInProgress());

        console.log('Filters in event thunk: ');
        console.log(filter);

        const state = getState();

        // Get filter map from state if filter flag is true
        let filterMap = null;
        if (filter) {
            filterMap = tfs.getFilters(state);
        }

        // Get sorting from state if sort flag is true
        let sortBy, direction = null;
        if (sort) {
            sortBy = ts.getTableSorting(state);
            direction = ts.getTableDirection(state);
        }

        // Get page info needed for fetching events from state
        let pageLimit = ts.getPageLimit(state);
        let offset = ts.getPageOffset(state);


        //TODO: Fetch actual data from server
        //Todo: maybe some Transfromations for publication needed
        //const response = JSON.parse(data);

        const response = data;
        for (let i = 0; response.results.length > i; i++) {
            // insert date property
            response.results[i] = {
                ...response.results[i],
                date: response.results[i].start_date
            }
            // insert enabled and hiding property of publications, if result has publications
            let result = response.results[i]
            if(!!result.publications && result.publications.length > 0) {
                let transformedPublications = [];
                for(let j = 0; result.publications.length > j; j++) {
                    transformedPublications.push({
                        ...result.publications[j],
                        enabled: true,
                        hiding: false});
                }
                response.results[i] = {
                    ...response.results[i],
                    publications: transformedPublications,
                };
            }
        }
        const events = response;
        dispatch(loadEventsSuccess(events));
    } catch (e) {
        dispatch(loadEventsFailure());
        console.log(e);
    }
}

