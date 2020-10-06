import {loadEventsFailure, loadEventsInProgress, loadEventsSuccess} from "../actions/eventActions";
import {getFilters} from '../selectors/tableFilterSelectors';
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from '../selectors/tableSelectors';

export const fetchEvents = () => async (dispatch, getState) => {
    try {
        dispatch(loadEventsInProgress());

        const state = getState();

        // Todo: Check if empty values problem when using proxy backend
        // Get filter map from state if filter flag is true
        let filters;
        let filterArray = [];
        let filterMap = getFilters(state);
        for (let key in filterMap) {
            if (!!filterMap[key].value) {
                filterArray.push(filterMap[key].name + ':' + filterMap[key].value);
            }
        }
        if (filterArray.length) {
            filters = filterArray.join(',');
        }
        console.log(filters);


        // Get sorting from state if sort flag is true
        let sortBy = getTableSorting(state);
        let direction = getTableDirection(state);
        let sort = sortBy + ':' + direction;


        // Get page info needed for fetching events from state
        let pageLimit = getPageLimit(state);
        let offset = getPageOffset(state);

        let data;

        if (typeof filters == "undefined") {
            //admin-ng/event/events.json?filter={filter}&sort={sort}&limit=0&offset=0
            data = await fetch('admin-ng/event/events.json?' + new URLSearchParams({
                sort: sort,
                limit: pageLimit,
                offset: offset
            }));
        } else {
            //admin-ng/event/events.json?filter={filter}&sort={sort}&limit=0&offset=0
            data = await fetch('admin-ng/event/events.json?' + new URLSearchParams({
                filter: filters,
                sort: sort,
                limit: pageLimit,
                offset: offset
            }));
        }

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

