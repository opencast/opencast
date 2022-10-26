/**
 * This file contains redux reducer for actions affecting the state of statistics
 */
import {
	LOAD_STATISTICS_IN_PROGRESS,
	LOAD_STATISTICS_SUCCESS,
	LOAD_STATISTICS_FAILURE,
	UPDATE_STATISTICS_SUCCESS,
	UPDATE_STATISTICS_FAILURE,
} from "../actions/statisticsActions";

// Initial state of series details in redux store
const initialState = {
	fetchingStatisticsInProgress: false,
	statistics: [],
	hasStatisticsError: false,
};

// Reducer for statistics
const statistics = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_STATISTICS_IN_PROGRESS: {
			return {
				...state,
				fetchingStatisticsInProgress: true,
			};
		}
		case LOAD_STATISTICS_SUCCESS: {
			const { statistics, hasError } = payload;
			return {
				...state,
				fetchingStatisticsInProgress: false,
				statistics: statistics,
				hasStatisticsError: hasError,
			};
		}
		case LOAD_STATISTICS_FAILURE: {
			const { hasError } = payload;
			return {
				...state,
				fetchingStatisticsInProgress: false,
				statistics: [],
				hasStatisticsError: hasError,
			};
		}
		case UPDATE_STATISTICS_SUCCESS: {
			const { statistics } = payload;
			return {
				...state,
				statistics: statistics,
			};
		}
		case UPDATE_STATISTICS_FAILURE: {
			return {
				...state,
			};
		}
		default:
			return state;
	}
};

export default statistics;
