import {loadEventsFailure, loadEventsInProgress, loadEventsSuccess} from "../actions/eventActions";
import {getURLParams} from "../utils/resourceUtils";

// fetch events from server
export const fetchEvents = () => async (dispatch, getState) => {
    try {
        dispatch(loadEventsInProgress());

        const state = getState();
        let params = getURLParams(state);

        //admin-ng/event/events.json?filter={filter}&sort={sort}&limit=0&offset=0
        let data = await fetch('admin-ng/event/events.json?' + params);

        const response =  await data.json();

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

