import { servicesTableConfig } from "../configs/tableConfigs/servicesTableConfig";
import {
	LOAD_SERVICES_FAILURE,
	LOAD_SERVICES_IN_PROGRESS,
	LOAD_SERVICES_SUCCESS,
	SET_SERVICES_COLUMNS,
} from "../actions/serviceActions";

/**
 * This file contains redux reducer for actions affecting the state of services
 */

// Fill columns initially with columns defined in servicesTableConfig
const initialColumns = servicesTableConfig.columns.map((column) => ({
	...column,
	deactivated: false,
}));

// Initial state of services in redux store
const initialState = {
	isLoading: false,
	results: [],
	columns: initialColumns,
	total: 0,
	count: 0,
	offset: 0,
	limit: 0,
};

// Reducer for services
const services = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_SERVICES_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_SERVICES_SUCCESS: {
			const { services } = payload;
			return {
				...state,
				isLoading: false,
				total: services.total,
				count: services.count,
				limit: services.limit,
				offset: services.offset,
				results: services.results,
			};
		}
		case LOAD_SERVICES_FAILURE: {
			return {
				...state,
				isLoading: false,
			};
		}
		case SET_SERVICES_COLUMNS: {
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

export default services;
