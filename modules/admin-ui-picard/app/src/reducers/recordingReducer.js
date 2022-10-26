import { recordingsTableConfig } from "../configs/tableConfigs/recordingsTableConfig";
import {
	LOAD_RECORDINGS_FAILURE,
	LOAD_RECORDINGS_IN_PROGRESS,
	LOAD_RECORDINGS_SUCCESS,
	SET_RECORDINGS_COLUMNS,
} from "../actions/recordingActions";

/**
 * This file contains redux reducer for actions affecting the state of recordings
 */

// Fill columns initially with columns defined in recordingsTableConfig
const initialColumns = recordingsTableConfig.columns.map((column) => ({
	...column,
	deactivated: false,
}));

// Initial state of recordings in redux store
const initialState = {
	isLoading: false,
	results: [],
	columns: initialColumns,
	total: 0,
	count: 0,
	offset: 0,
	limit: 0,
};

// Reducer for recordings
const recordings = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_RECORDINGS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_RECORDINGS_SUCCESS: {
			const { recordings } = payload;
			return {
				...state,
				isLoading: false,
				total: recordings.total,
				count: recordings.count,
				limit: recordings.limit,
				offset: recordings.offset,
				results: recordings.results,
			};
		}
		case LOAD_RECORDINGS_FAILURE: {
			return {
				...state,
				isLoading: false,
			};
		}
		case SET_RECORDINGS_COLUMNS: {
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

export default recordings;
