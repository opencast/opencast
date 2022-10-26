/**
 * This file contains all redux actions that can be executed on the statistics page
 */

// Constants of actions types
export const LOAD_STATISTICS_IN_PROGRESS = "LOAD_STATISTICS_IN_PROGRESS";
export const LOAD_STATISTICS_SUCCESS = "LOAD_STATISTICS_SUCCESS";
export const LOAD_STATISTICS_FAILURE = "LOAD_STATISTICS_FAILURE";
export const UPDATE_STATISTICS_SUCCESS = "LOAD_STATISTICS_SUCCESS";
export const UPDATE_STATISTICS_FAILURE = "LOAD_STATISTICS_FAILURE";

// Actions affecting fetching statistics from server
export const loadStatisticsInProgress = () => ({
	type: LOAD_STATISTICS_IN_PROGRESS,
});

export const loadStatisticsSuccess = (statistics, hasError) => ({
	type: LOAD_STATISTICS_SUCCESS,
	payload: {
		statistics,
		hasError,
	},
});

export const loadStatisticsFailure = (hasError) => ({
	type: LOAD_STATISTICS_FAILURE,
	payload: {
		hasError,
	},
});

export const updateStatisticsSuccess = (statistics) => ({
	type: UPDATE_STATISTICS_SUCCESS,
	payload: {
		statistics,
	},
});

export const updateStatisticsFailure = () => ({
	type: UPDATE_STATISTICS_FAILURE,
});
