import { aclsTableConfig } from "../configs/tableConfigs/aclsTableConfig";
import {
	LOAD_ACLS_FAILURE,
	LOAD_ACLS_IN_PROGRESS,
	LOAD_ACLS_SUCCESS,
	SET_ACL_COLUMNS,
} from "../actions/aclActions";

/**
 * This file contains redux reducer for actions affecting the state of acls
 */

// Fill columns initially with columns defined in aclsTableConfig
const initialColumns = aclsTableConfig.columns.map((column) => ({
	...column,
	deactivated: false,
}));

// Initial state of acls in redux store
const initialState = {
	isLoading: false,
	results: [],
	columns: initialColumns,
	total: 0,
	count: 0,
	offset: 0,
	limit: 0,
};

// Reducer for acls
const acls = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_ACLS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_ACLS_SUCCESS: {
			const { acls } = payload;
			return {
				...state,
				isLoading: false,
				total: acls.total,
				count: acls.count,
				limit: acls.limit,
				offset: acls.offset,
				results: acls.results,
			};
		}
		case LOAD_ACLS_FAILURE: {
			return {
				...state,
				isLoading: false,
			};
		}
		case SET_ACL_COLUMNS: {
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

export default acls;
