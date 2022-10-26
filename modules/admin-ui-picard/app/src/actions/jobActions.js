/**
 * This file contains all redux actions that can be executed on jobs
 */

// Constants of action types for fetching jobs from server
export const LOAD_JOBS_IN_PROGRESS = "LOAD_JOBS_IN_PROGRESS";
export const LOAD_JOBS_SUCCESS = "LOAD_JOBS_SUCCESS";
export const LOAD_JOBS_FAILURE = "LOAD_JOBS_FAILURE";

// Constants of action types affecting UI
export const SET_JOB_COLUMNS = "SET_JOB_COLUMNS";

// Actions affecting fetching jobs from server

export const loadJobsInProgress = () => ({
	type: LOAD_JOBS_IN_PROGRESS,
});

export const loadJobsSuccess = (jobs) => ({
	type: LOAD_JOBS_SUCCESS,
	payload: { jobs },
});

export const loadJobsFailure = () => ({
	type: LOAD_JOBS_FAILURE,
});

// Actions affecting UI

export const setJobColumns = (updatedColumns) => ({
	type: SET_JOB_COLUMNS,
	payload: { updatedColumns },
});
