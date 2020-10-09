import {jobsTableConfig} from "../configs/tableConfigs/jobsTableConfig";
import {LOAD_JOBS_FAILURE, LOAD_JOBS_IN_PROGRESS, LOAD_JOBS_SUCCESS} from "../actions/jobActions";

const initialColumns = jobsTableConfig.columns.map(column =>
    ({
        name: column.name,
        deactivated: false
    }));

const initialState = {
    isLoading: false,
    results: [],
    columns: initialColumns,
    total: 0,
    count: 0,
    offset: 0,
    limit: 0
}

const jobs = (state=initialState, action) =>{
    const { type, payload } = action;
    switch (type) {
        case LOAD_JOBS_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            }
        }
        case LOAD_JOBS_SUCCESS: {
            const { jobs } = payload;
            return {
                ...state,
                isLoading: false,
                total: jobs.total,
                count: jobs.count,
                limit: jobs.limit,
                offset: jobs.offset,
                results: jobs.results
            }
        }
        case LOAD_JOBS_FAILURE: {
            return {
                ...state,
                isLoading: false
            }
        }
        default:
            return state;
    }
}

export default jobs;
