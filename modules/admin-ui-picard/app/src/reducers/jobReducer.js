import { jobsTableConfig } from "../configs/tableConfigs/jobsTableConfig";
import {
	LOAD_JOBS_FAILURE,
	LOAD_JOBS_IN_PROGRESS,
	LOAD_JOBS_SUCCESS,
	SET_JOB_COLUMNS,
} from "../actions/jobActions";

/**
 * This file contains redux reducer for actions affecting the state of jobs
 */

// Fill columns initially with columns defined in jobsTableConfig
const initialColumns = jobsTableConfig.columns.map((column) => ({
	...column,
	deactivated: false,
}));

// Initial state of jobs in redux store
const initialState = {
	isLoading: false,
	results: [],
	columns: initialColumns,
	total: 0,
	count: 0,
	offset: 0,
	limit: 0,
};

// Reducer for jobs
const jobs = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_JOBS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
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
				results: jobs.results,
			};
		}
		case LOAD_JOBS_FAILURE: {
			return {
				...state,
				isLoading: false,
			};
		}
		case SET_JOB_COLUMNS: {
			const { updatedColumns } = payload;
			return {
				...state,
				columns: updatedColumns,
			};
		}
		default:
			return state;
	}
};

export default jobs;
