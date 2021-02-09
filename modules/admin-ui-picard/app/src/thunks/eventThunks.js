import {
    loadEventMetadataFailure,
    loadEventMetadataInProgress,
    loadEventMetadataSuccess,
    loadEventsFailure,
    loadEventsInProgress,
    loadEventsSuccess
} from "../actions/eventActions";
import {getURLParams} from "../utils/resourceUtils";
import axios from "axios";
import {sourceMetadata, uploadAssetOptions} from "../configs/newEventConfigs/sourceConfig";
import {WORKFLOW_UPLOAD_ASSETS_NON_TRACK} from "../configs/newEventConfigs/newEventWizardConfig";
import {getTimezoneOffset} from "../utils/utils";

// fetch events from server
export const fetchEvents = () => async (dispatch, getState) => {
    try {
        dispatch(loadEventsInProgress());

        const state = getState();
        let params = getURLParams(state);

        //admin-ng/event/events.json?filter={filter}&sort={sort}&limit=0&offset=0
        let data = await axios.get('admin-ng/event/events.json', { params: params });

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
        console.log(e);
    }
}

// fetch event metadata from server
export const fetchEventMetadata = () => async (dispatch, getState) => {
    try {
        dispatch(loadEventMetadataInProgress());

        let data = await axios.get('admin-ng/event/new/metadata');
        const response = await data.data;

        const metadata = response[0];

        for (let i = 0; metadata.fields.length > i; i++) {
            if (!!metadata.fields[i].collection) {
                metadata.fields[i].collection = Object.keys(metadata.fields[i].collection).map(key => {
                    return {
                        name: key,
                        value: metadata.fields[i].collection[key]
                    }
                })
            }
        }

        dispatch(loadEventMetadataSuccess(metadata));
    } catch (e) {
        dispatch(loadEventMetadataFailure());
        console.log(e);
    }
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

export const postNewEvent = async (values, metadataInfo) => {
    let formData = new FormData();
    let metadataFields = [], metadata, source, access, assets;

    // fill metadataField with field information send by server previously and values provided by user
    // Todo: What is hashkey?
    for (let i = 0; metadataInfo.fields.length > i; i++) {
        let fieldValue = {
            id: metadataInfo.fields[i].id,
            type: metadataInfo.fields[i].type,
            value: values[metadataInfo.fields[i].id],
            tabindex: i + 1,
            $$hashKey: "object:123"
        };
        if (!!metadataInfo.fields[i].translatable) {
            fieldValue = {
                ...fieldValue,
                translatable: metadataInfo.fields[i].translatable
            }
        }
        metadataFields = metadataFields.concat(fieldValue);
    }

    // if source mode is UPLOAD than also put metadata fields of that in metadataFields
    if(values.sourceMode === "UPLOAD") {
        // set source type UPLOAD
        source = {
            type: values.sourceMode
        }
        for (let i = 0; sourceMetadata.UPLOAD.metadata.length > i; i++) {
            console.log(sourceMetadata.UPLOAD.metadata[i].id);
            metadataFields = metadataFields.concat({
                id: sourceMetadata.UPLOAD.metadata[i].id,
                value: values[sourceMetadata.UPLOAD.metadata[i].id],
                type: sourceMetadata.UPLOAD.metadata[i].type,
                tabindex: sourceMetadata.UPLOAD.metadata[i].tabindex
            })
        }
    }

    // metadata for post request
    metadata = {
        flavor: metadataInfo.flavor,
        title: metadataInfo.title,
        fields: metadataFields
    };

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
                formData.append(asset.id, asset.file);
            }
        } else {
            if (!!values[uploadAssetOptions[i].id] && values.sourceMode === 'UPLOAD') {
                formData.append(uploadAssetOptions[i].id, values[uploadAssetOptions[i].id])
            }
        }
        assets.options = assets.options.concat(uploadAssetOptions[i]);
    }

    // access policies for post request
    access = {
        acl: {
            ace: []
        }
    };

    // iterate through all policies provided by user and transform them into form required for request
    for (let i = 0; values.policies.length > i; i++) {
        access.acl.ace = access.acl.ace.concat({
            action: 'read',
            allow: values.policies[i].read,
            role: values.policies[i].role
        },{
            action: 'write',
            allow: values.policies[i].write,
            role: values.policies[i].role
        });
        if (values.policies[i].actions.length > 0) {
            for (let j = 0; values.policies[i].actions.length > j; j++) {
                access.acl.ace = access.acl.ace.concat({
                    action: values.policies[i].actions[j],
                    allow: true,
                    role: values.policies[i].role
                })
            }
        }
    }

    // todo: change placeholder in configuration
    formData.append('metadata', JSON.stringify({
        metadata: metadata,
        processing: {
            workflow: values.processingWorkflow,
            configuration: {
                'placeholderKey': 'placeholderValue'
            }
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
    ).then(response => console.log(response)).catch(response => console.log(response));

}

