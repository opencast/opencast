import {loadRecordingsInProgress, loadRecordingsFailure, loadRecordingsSuccess} from "../actions/recordingActions";
import {getFilters} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from "../selectors/tableSelectors";

export const fetchRecordings = () => async (dispatch, getState) => {
    try {
        dispatch(loadRecordingsInProgress());

        const state = getState();

        // Todo: Check if empty values problem when using proxy backend
        // Get filter map from state
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

        // Get sorting from state
        let sort = getTableSorting(state) + ':' + getTableDirection(state);

        // Get page info needed for fetching recordings from state
        let pageLimit = getPageLimit(state);
        let offset = getPageOffset(state);

        let data;

        if (typeof filters == "undefined") {
            // /agents.json?filter={filter}&limit=100&offset=0&inputs=false&sort={sort}
            data = await fetch('admin-ng/capture-agents/agents.json?' + new URLSearchParams({
                sort:sort,
                limit: pageLimit,
                offset: offset
            }));
        } else {
            // /agents.json?filter={filter}&limit=100&offset=0&inputs=false&sort={sort}
            data = await  fetch('admin-ng/capture-agents/agents.json?' + new URLSearchParams({
                filter: filters,
                sort: sort,
                limit: pageLimit,
                offset: offset
            }));
        }

        const recordings = await data.json();
        dispatch(loadRecordingsSuccess(recordings));

    } catch (e) {
        dispatch(loadRecordingsFailure());
        console.log(e);
    }
}
