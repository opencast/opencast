import {loadJobsFailure, loadJobsInProgress, loadJobsSuccess} from "../actions/jobActions";
import {getFilters} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableSorting} from "../selectors/tableSelectors";

export const fetchJobs = () => async (dispatch, getState) => {
    try {
        dispatch(loadJobsInProgress());

        const state = getState();

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
        let sort = getTableSorting(state) + ':' + getTableSorting(state);

        // Get page info needed for fetching jobs from state
        let pageLimit = getPageLimit(state);
        let offset = getPageOffset(state);

        let data;

        if (typeof filters == "undefined") {
            // /jobs.json?limit=0&offset=0&filter={filter}&sort={sort}
            data = await fetch('admin-ng/job/jobs.json?' + new URLSearchParams({
                sort: sort,
                limit: pageLimit,
                offset: offset
            }));
        } else {
            // /jobs.json?limit=0&offset=0&filter={filter}&sort={sort}
            data = await fetch('admin-ng/job/jobs.json?' + new URLSearchParams({
                filter: filters,
                sort: sort,
                limit: pageLimit,
                offset: offset
            }));
        }

        const jobs = await data.json();
        dispatch(loadJobsSuccess(jobs));

    } catch (e) {
        dispatch(loadJobsFailure());
    }
}
