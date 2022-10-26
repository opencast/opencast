import {
	EDIT_FILTER_VALUE,
	EDIT_SECOND_FILTER,
	EDIT_SELECTED_FILTER,
	EDIT_TEXT_FILTER,
	LOAD_FILTER_PROFILE,
	LOAD_FILTERS_FAILURE,
	LOAD_FILTERS_IN_PROGRESS,
	LOAD_FILTERS_SUCCESS,
	LOAD_STATS,
	REMOVE_SECOND_FILTER,
	REMOVE_SELECTED_FILTER,
	REMOVE_TEXT_FILTER,
	RESET_FILTER_VALUES,
} from "../actions/tableFilterActions";

/**
 * This file contains redux reducer for actions affecting the state of table filters
 */

// Initial state of table filters in redux store
const initialState = {
	isLoading: false,
	currentResource: "",
	data: [],
	filterProfiles: [],
	textFilter: "",
	selectedFilter: "",
	secondFilter: "",
	stats: [],
};

// Reducer for table filters
const tableFilters = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_FILTERS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_FILTERS_SUCCESS: {
			const { filters, resource } = payload;
			return {
				...state,
				isLoading: false,
				currentResource: resource,
				data: filters,
			};
		}
		case LOAD_FILTERS_FAILURE: {
			return {
				...state,
				isLoading: false,
			};
		}
		case LOAD_STATS: {
			const { stats } = payload;
			return {
				...state,
				stats: stats,
			};
		}
		case EDIT_FILTER_VALUE: {
			const { filterName, value } = payload;
			return {
				...state,
				data: state.data.map((filter) => {
					return filter.name === filterName
						? { ...filter, value: value }
						: filter;
				}),
			};
		}
		case RESET_FILTER_VALUES: {
			return {
				...state,
				data: state.data.map((filter) => {
					return { ...filter, value: "" };
				}),
			};
		}
		case EDIT_TEXT_FILTER: {
			const { textFilter } = payload;
			return {
				...state,
				textFilter: textFilter,
			};
		}
		case REMOVE_TEXT_FILTER: {
			return {
				...state,
				textFilter: "",
			};
		}
		case LOAD_FILTER_PROFILE: {
			const { filterMap } = payload;
			return {
				...state,
				data: filterMap,
			};
		}
		case EDIT_SELECTED_FILTER: {
			const { filter } = payload;
			return {
				...state,
				selectedFilter: filter,
			};
		}
		case REMOVE_SELECTED_FILTER: {
			return {
				...state,
				selectedFilter: "",
			};
		}
		case EDIT_SECOND_FILTER: {
			const { filter } = payload;
			return {
				...state,
				secondFilter: filter,
			};
		}
		case REMOVE_SECOND_FILTER: {
			return {
				...state,
				secondFilter: "",
			};
		}
		default:
			return state;
	}
};

export default tableFilters;
