import {loadEventsFailure, loadEventsInProgress, loadEventsSuccess} from "../actions/eventActions";
import {getFilters} from '../selectors/tableFilterSelectors';
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from '../selectors/tableSelectors';

export const fetchEvents = (filter, sort) => async (dispatch, getState) => {
    try {
        dispatch(loadEventsInProgress());

        console.log('Filters in event thunk: ');
        console.log(filter);

        const state = getState();

        // Get filter map from state if filter flag is true
        let filterMap = null;
        if (filter) {
            filterMap = getFilters(state);
        }

        // Get sorting from state if sort flag is true
        let sortBy, direction = null;
        if (sort) {
            sortBy = getTableSorting(state);
            direction = getTableDirection(state);
        }

        // Get page info needed for fetching events from state
        let pageLimit = getPageLimit(state);
        let offset = getPageOffset(state);


        //TODO: Fetch actual data from server
        //Todo: maybe some Transfromations for publication needed
        const data = await fetch('admin-ng/event/events.json');

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

