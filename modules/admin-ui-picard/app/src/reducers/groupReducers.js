import { groupsTableConfig } from "../configs/tableConfigs/groupsTableConfig";
import {
	LOAD_GROUPS_FAILURE,
	LOAD_GROUPS_IN_PROGRESS,
	LOAD_GROUPS_SUCCESS,
	SET_GROUP_COLUMNS,
} from "../actions/groupActions";

/**
 * This file contains redux reducer for actions affecting the state of groups
 */

// Fill columns initially with columns defined in groupsTableConfig
const initialColumns = groupsTableConfig.columns.map((column) => ({
	...column,
	deactivated: false,
}));

// Initial state of groups in redux store
const initialState = {
	isLoading: false,
	results: [],
	columns: initialColumns,
	total: 0,
	count: 0,
	offset: 0,
	limit: 0,
};

// Reducer for groups
const groups = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_GROUPS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_GROUPS_SUCCESS: {
			const { groups } = payload;
			return {
				...state,
				isLoading: false,
				total: groups.total,
				count: groups.count,
				limit: groups.limit,
				offset: groups.offset,
				results: groups.results,
			};
		}
		case LOAD_GROUPS_FAILURE: {
			return {
				...state,
				isLoading: false,
			};
		}
		case SET_GROUP_COLUMNS: {
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

export default groups;
