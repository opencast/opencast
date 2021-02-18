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
import axios from "axios";
import {sourceMetadata, uploadAssetOptions} from "../configs/wizard/sourceConfig";
import {WORKFLOW_UPLOAD_ASSETS_NON_TRACK} from "../configs/wizard/newEventWizardConfig";
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
export const fetchEventMetadata = () => async dispatch => {
    try {
        dispatch(loadEventMetadataInProgress());

        let data = await axios.get('admin-ng/event/new/metadata');
        const response = await data.data;

        const metadata = transformMetadataCollection(response[0]);

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

// post new event to backend
export const postNewEvent = async (values, metadataInfo) => {

    let formData = new FormData();
    let metadataFields, metadata, source, access, assets;

    // prepare metadata provided by user
    metadataFields = prepareMetadataFieldsForPost(metadataInfo.fields, values);

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

    // prepare access rules provided by user
   access = prepareAccessPolicyRulesForPost(values.policies);

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

