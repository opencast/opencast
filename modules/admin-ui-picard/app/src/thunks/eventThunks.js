import moment from "moment-timezone";
import axios from "axios";
import {
    loadEventMetadataFailure,
    loadEventMetadataInProgress,
    loadEventMetadataSuccess,
    loadEventsFailure,
    loadEventsInProgress,
    loadEventsSuccess
} from "../actions/eventActions";
import {
    getURLParams,
    prepareAccessPolicyRulesForPost,
    prepareMetadataFieldsForPost,
    transformMetadataCollection
} from "../utils/resourceUtils";
import {getTimezoneOffset, makeTwoDigits} from "../utils/utils";
import {sourceMetadata} from "../configs/sourceConfig";
import {NOTIFICATION_CONTEXT, weekdays, WORKFLOW_UPLOAD_ASSETS_NON_TRACK} from "../configs/modalConfig";
import {logger} from "../utils/logger";
import {addNotification} from "./notificationThunks";
import {getAssetUploadOptions} from "../selectors/eventSelectors";



// fetch events from server
export const fetchEvents = () => async (dispatch, getState) => {
    try {
        dispatch(loadEventsInProgress());

        const state = getState();
        let params = getURLParams(state);

        //admin-ng/event/events.json?filter={filter}&sort={sort}&limit=0&offset=0
        let data = await axios.get('/admin-ng/event/events.json', { params: params });

        const response =  await data.data;

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
        logger.error(e);
    }
}

// fetch event metadata from server
export const fetchEventMetadata = () => async dispatch => {
    try {
        dispatch(loadEventMetadataInProgress());

        let data = await axios.get('/admin-ng/event/new/metadata');
        const response = await data.data;

        const metadata = transformMetadataCollection(response[0]);

        dispatch(loadEventMetadataSuccess(metadata));
    } catch (e) {
        dispatch(loadEventMetadataFailure());
        logger.error(e);
    }
};

// get merged metadata for provided event ids
export const postEditMetadata = async ids => {
    let formData = new URLSearchParams();
    formData.append('eventIds', JSON.stringify(ids));

    try {
        let data = await axios.post('/admin-ng/event/events/metadata.json', formData,
            {
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded'
                }
            }
        );
        let response = await data.data;

        // transform response
        const metadata = transformMetadataCollection(response.metadata, true);
        return {
            mergedMetadata: metadata,
            notFound: response.notFound,
            merged: response.merged,
            runningWorkflow: response.runningWorkflow,
        };
    } catch (e) {
        // return error
        return {
            fatalError: e.message
        };
    }
};

export const updateBulkMetadata = (metadataFields, values) => async dispatch => {
    let formData = new URLSearchParams();
    formData.append('eventIds', JSON.stringify(metadataFields.merged));
    let metadata = [{
        flavor: 'dublincore/episode',
        title: 'EVENTS.EVENTS.DETAILS.CATALOG.EPISODE',
        fields: []
    }];

    metadataFields.mergedMetadata.forEach(field => {
        if (field.selected) {
            let value = values[field.id];
            metadata[0].fields.push({
                ...field,
                value: value
            });
        }
    });

    formData.append('metadata', JSON.stringify(metadata));

    axios.put('/admin-ng/event/events/metadata', formData,
        {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    })
        .then(res => {
            logger.info(res);
            dispatch(addNotification('success', 'BULK_METADATA_UPDATE.ALL_EVENTS_UPDATED'));
        })
        .catch(err => {
            logger.error(err);
            // if an internal server error occurred, then backend sends further information
            if (err.status === 500) {
                // backend should send data containing further information about occurred internal error
                // if this error data is undefined then an unexpected error occurred
                if (!err.data) {
                    dispatch(addNotification('error', 'BULK_METADATA_UPDATE.UNEXPECTED_ERROR'));
                } else {
                    if (err.data.updated && err.data.updated.length === 0) {
                        dispatch(addNotification('error', 'BULK_METADATA_UPDATE.NO_EVENTS_UPDATED'));
                    }
                    if (err.data.updateFailures && err.data.updateFailures.length > 0) {
                        dispatch(addNotification('warning', 'BULK_METADATA_UPDATE.SOME_EVENTS_NOT_UPDATED'));
                    }
                    if (err.data.notFound && err.data.notFound.length > 0) {
                        dispatch(addNotification('warning', 'BULK_ACTIONS.EDIT_EVENTS_METADATA.REQUEST_ERRORS.NOT_FOUND'));
                    }
                }
            } else {
                dispatch(addNotification('error', 'BULK_METADATA_UPDATE.UNEXPECTED_ERROR'));
            }
        });
}

// Check for conflicts with already scheduled events
export const checkForConflicts =  async (startDate, endDate, duration, device) => {

    let metadata = {
        start: startDate,
        device: device,
        duration: duration,
        end: endDate
    }
    let status = 0;

    axios.post('/admin-ng/event/new/conflicts', metadata,
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        }).then(response => {
            status = response.status;
    }).catch(reason => {
            status = reason.status;
    });

    return status !== 409;

}

// post new event to backend
export const postNewEvent = (values, metadataInfo) => async (dispatch, getState) => {
    // get asset upload options from redux store
    const state = getState();
    const uploadAssetOptions = getAssetUploadOptions(state);

    let formData = new FormData();
    let metadataFields, metadata, source, access, assets;
    let configuration = {};

    // prepare metadata provided by user
    metadataFields = prepareMetadataFieldsForPost(metadataInfo.fields, values);

    // if source mode is UPLOAD than also put metadata fields of that in metadataFields
    if(values.sourceMode === "UPLOAD") {
        // set source type UPLOAD
        source = {
            type: values.sourceMode
        }
        for (let i = 0; sourceMetadata.UPLOAD.metadata.length > i; i++) {
            metadataFields = metadataFields.concat({
                id: sourceMetadata.UPLOAD.metadata[i].id,
                value: values[sourceMetadata.UPLOAD.metadata[i].id],
                type: sourceMetadata.UPLOAD.metadata[i].type,
                tabindex: sourceMetadata.UPLOAD.metadata[i].tabindex
            })
        }
    }

    // metadata for post request
    metadata = [{
        flavor: metadataInfo.flavor,
        title: metadataInfo.title,
        fields: metadataFields
    }];

    // transform date data for post request if source mode is SCHEDULE_*
    if (values.sourceMode === 'SCHEDULE_SINGLE' || values.sourceMode === 'SCHEDULE_MULTIPLE') {
        // Get timezone offset
        let offset = getTimezoneOffset();

        // Prepare start date of event for post
        let startDate = new Date(values.scheduleStartDate);
        startDate.setHours((values.scheduleStartTimeHour - offset), values.scheduleStartTimeMinutes, 0, 0);

        let endDate;

        // Prepare end date of event for post
        if(values.sourceMode === 'SCHEDULE_SINGLE') {
            endDate = new Date(values.scheduleStartDate);
        } else {
            endDate = new Date(values.scheduleEndDate);
        }
        endDate.setHours((values.scheduleEndTimeHour - offset), values.scheduleEndTimeMinutes, 0, 0);

        // transform duration into milliseconds
        let duration = values.scheduleDurationHour * 3600000 + values.scheduleDurationMinutes * 60000;

        // data about source for post request
        source = {
            type: values.sourceMode,
            metadata: {
                start: startDate,
                device: values.location,
                inputs: values.deviceInputs.join(', '),
                end: endDate,
                duration: duration.toString()
            }
        }

        if (values.sourceMode === 'MULTIPLE_SCHEDULE') {

            // assemble an iCalendar RRULE (repetition instruction) for the given user input
            let rRule = 'FREQ=WEEKLY;BYDAY=' + values.repeatOn.join(',')
                + ';BYHOUR='+startDate.getUTCHours() + ';BYMINUTE'+startDate.getUTCMinutes();

            source.metadata = {
                ...source.metadata,
                rrule: rRule
            }
        }
    }

    // information about upload assets options
    // need to provide all possible upload asset options independent of source mode/type
    assets = {
        workflow: WORKFLOW_UPLOAD_ASSETS_NON_TRACK,
        options: []
    };

    // iterate through possible upload asset options and put them in assets
    // if source mode/type is UPLOAD and a file for a asset is uploaded by user than append file to form data
    for (let i = 0; uploadAssetOptions.length > i; i++) {
        if (uploadAssetOptions[i].type === 'track' && values.sourceMode === 'UPLOAD') {
            let asset = values.uploadAssetsTrack.find(asset => asset.id === uploadAssetOptions[i].id);
            if (!!asset.file) {
                formData.append(asset.id + '.0', asset.file);
            }
            assets.options = assets.options.concat(uploadAssetOptions[i]);
        } else {
            if (!!values[uploadAssetOptions[i].id] && values.sourceMode === 'UPLOAD') {
                formData.append(uploadAssetOptions[i].id + '.0', values[uploadAssetOptions[i].id]);
                assets.options = assets.options.concat(uploadAssetOptions[i]);
            }
        }

    }

    // prepare access rules provided by user
   access = prepareAccessPolicyRulesForPost(values.policies);

    // prepare configurations for post
    Object.keys(values.configuration).forEach(config => {
        configuration[config] = String(values.configuration[config])
    });

    formData.append('metadata', JSON.stringify({
        metadata: metadata,
        processing: {
            workflow: values.processingWorkflow,
            configuration: configuration
        },
        access: access,
        source: source,
        assets: assets
    }));

    // Todo: process bar notification
    axios.post('/admin-ng/event/new', formData,
        {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        }
    ).then(response => {
        logger.info(response);
        dispatch(addNotification('success', 'EVENTS_CREATED'));

    }).catch(response => {
        logger.error(response);
        dispatch(addNotification('error', 'EVENTS_NOT_CREATED'));
    });

};

// delete event with provided id
export const deleteEvent = id => async dispatch => {
    // API call for deleting an event
    axios.delete(`/admin-ng/event/${id}`).then(res => {
        // add success notification depending on status code
        if (res.status === 200) {
            dispatch(addNotification('success', 'EVENT_DELETED'));
        } else {
            dispatch(addNotification('success', 'EVENT_WILL_BE_DELETED'));
        }
    }).catch(res => {
        // add error notification depending on status code
        if (res.status === 401) {
            dispatch(addNotification('error', 'EVENTS_NOT_DELETED_NOT_AUTHORIZED'));
        } else {
            dispatch(addNotification('error', 'EVENTS_NOT_DELETED'));
        }
    });
};

// delete multiple events
export const deleteMultipleEvent = events => async dispatch => {

    let data = [];

    for (let i = 0; i < events.length; i++) {
        if (events[i].selected) {
            data.push(events[i].id);
        }
    }

    axios.post('/admin-ng/event/deleteEvents', data)
        .then(res => {
            logger.info(res);
            //add success notification
            dispatch(addNotification('success', 'EVENTS_DELETED'));
        }).catch(res => {
        logger.error(res);
        //add error notification
        dispatch(addNotification('error', 'EVENTS_NOT_DELETED'));
    });

};

// fetch scheduling info for events
export const fetchScheduling = async events => {
    let formData = new FormData();

    for (let i = 0; i < events.length; i++) {
        if (events[i].selected) {
            formData.append('eventIds', events[i].id);
        }
    }

    formData.append('ignoreNonScheduled', true);

    const response = await axios.post('/admin-ng/event/scheduling.json', formData);

    let data = await response.data;

    // transform data for further use
    let editedEvents = [];
    for (let i = 0; i < data.length; i++) {
        let startDate = new Date(data[i].start);
        let endDate = new Date(data[i].end);
        let event = {
            eventId: data[i].eventId,
            title: data[i].agentConfiguration['event.title'],
            changedTitle: data[i].agentConfiguration['event.title'],
            series: data[i].agentConfiguration['event.series'],
            changedSeries: data[i].agentConfiguration['event.series'],
            location: data[i].agentConfiguration['event.location'],
            changedLocation: data[i].agentConfiguration['event.location'],
            deviceInputs: data[i].agentConfiguration['capture.device.names'],
            changedDeviceInputs: [],
            startTimeHour: makeTwoDigits(startDate.getHours()),
            changedStartTimeHour: makeTwoDigits(startDate.getHours()),
            startTimeMinutes: makeTwoDigits(startDate.getMinutes()),
            changedStartTimeMinutes: makeTwoDigits(startDate.getMinutes()),
            endTimeHour: makeTwoDigits(endDate.getHours()),
            changedEndTimeHour: makeTwoDigits(endDate.getHours()),
            endTimeMinutes: makeTwoDigits(endDate.getMinutes()),
            changedEndTimeMinutes: makeTwoDigits(endDate.getMinutes()),
            weekday: weekdays[startDate.getDay()].name,
            changedWeekday: weekdays[startDate.getDay()].name
        }
        editedEvents.push(event);
    }

    return editedEvents;
};

// check if there are any scheduling conflicts with other events
export const checkForSchedulingConflicts = events =>  async dispatch => {
    const formData = new FormData();
    let update = [];
    let timezone = moment.tz.guess();
    for (let i = 0; i < events.length; i++) {
        update.push({
            events: [events[i].eventId],
            scheduling: {
                timezone: timezone,
                start: {
                    hour: parseInt(events[i].changedStartTimeHour),
                    minute: parseInt(events[i].changedStartTimeMinutes)
                },
                end: {
                    hour: parseInt(events[i].changedEndTimeHour),
                    minutes: parseInt(events[i].changedEndTimeMinutes)
                },
                weekday: events[i].changedWeekday,
                agentId: events[i].changedLocation
            }
        });
    }

    formData.append('update', JSON.stringify(update));

    let response = [];

    axios.post('/admin-ng/event/bulk/conflicts', formData)
        .then(res => logger.info(res))
        .catch(res => {
            if (res.status === 409) {
                dispatch(addNotification('error', 'CONFLICT_BULK_DETECTED', -1, null, NOTIFICATION_CONTEXT));
                response = res.data;
            }
            logger.error(res);
        });

    return response;
};

// update multiple scheduled events at once
export const updateScheduledEventsBulk = values => async dispatch => {
    let formData = new FormData();
    let update = [];
    let timezone = moment.tz.guess();

    for (let i = 0; i < values.changedEvents.length; i++) {
        let eventChanges = values.editedEvents.find(event => event.eventId === values.changedEvents[i]);
        let originalEvent = values.events.find(event => event.id === values.changedEvents[i]);

        if (!eventChanges || !originalEvent) {
            dispatch(addNotification('error', 'EVENTS_NOT_UPDATED'));
            return;
        }

        update.push({
            events: [eventChanges.eventId],
            metadata: {
                flavor: originalEvent.flavor,
                title: originalEvent.title,
                fields: [
                    {
                        id: 'isPartOf',
                        // todo: maybe change this; dunno if considered in backend
                        collection: {},
                        label: 'EVENTS.EVENTS.DETAILS.METADATA.SERIES',
                        readOnly: false,
                        required: false,
                        translatable: false,
                        type: 'text',
                        value: eventChanges.changedSeries,
                        // todo: what is hashkey?
                        $$hashKey: 'object:1589'
                    }
                ]
            },
            scheduling: {
                timezone: timezone,
                start: {
                    hour: parseInt(eventChanges.changedStartTimeHour),
                    minute: parseInt(eventChanges.changedStartTimeMinutes)
                },
                end: {
                    hour: parseInt(eventChanges.changedEndTimeHour),
                    minute: parseInt(eventChanges.changedEndTimeMinutes)
                },
                weekday: eventChanges.changedWeekday,
                agentId: eventChanges.changedLocation
            }
        });
    }

    formData.append('update', JSON.stringify(update))

    axios.put('/admin-ng/event/bulk/update', formData)
        .then(res => {
            logger.info(res);
            dispatch(addNotification('success', 'EVENTS_UPDATED_ALL'));
        })
        .catch(res => {
            logger.error(res);
            dispatch(addNotification('error', 'EVENTS_NOT_UPDATED'));
        });
};
