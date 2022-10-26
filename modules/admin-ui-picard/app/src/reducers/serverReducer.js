import { serversTableConfig } from "../configs/tableConfigs/serversTableConfig";
import {
	LOAD_SERVERS_FAILURE,
	LOAD_SERVERS_IN_PROGRESS,
	LOAD_SERVERS_SUCCESS,
	SET_SERVER_COLUMNS,
} from "../actions/serverActions";

/**
 * This file contains redux reducer for actions affecting the state of servers
 */

// Fill columns initially with columns defined in serversTableConfig
const initialColumns = serversTableConfig.columns.map((column) => ({
	...column,
	deactivated: false,
}));

// Initial state of servers in redux store
const initialState = {
	isLoading: false,
	results: [],
	columns: initialColumns,
	total: 0,
	count: 0,
	offset: 0,
	limit: 0,
};

// Reducer for servers
const servers = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_SERVERS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_SERVERS_SUCCESS: {
			const { servers } = payload;
			return {
				...state,
				isLoading: false,
				total: servers.total,
				count: servers.count,
				limit: servers.limit,
				offset: servers.offset,
				results: servers.results,
			};
		}
		case LOAD_SERVERS_FAILURE: {
			return {
				...state,
				isLoading: false,
			};
		}
		case SET_SERVER_COLUMNS: {
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

export default servers;
