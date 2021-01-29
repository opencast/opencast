import {
    loadEventMetadataFailure,
    loadEventMetadataInProgress, loadEventMetadataSuccess,
    loadEventsFailure,
    loadEventsInProgress,
    loadEventsSuccess
} from "../actions/eventActions";
import {getURLParams} from "../utils/resourceUtils";
import axios from "axios";

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
export const fetchEventMetadata = () => async (dispatch, getState)=> {
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

