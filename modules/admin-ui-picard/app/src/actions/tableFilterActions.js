/**
 * This file contains all redux actions that can be executed on table filters
 */

// Constants of of actions types for fetching filters from server
export const LOAD_FILTERS_IN_PROGRESS = "LOAD_FILTERS_IN_PROGRESS";
export const LOAD_FILTERS_SUCCESS = "LOAD_FILTERS_SUCCESS";
export const LOAD_FILTERS_FAILURE = "LOAD_FILTERS_FAILURE";

// Constants of of actions types for fetching stats (events) from server
export const LOAD_STATS = "LOAD_STATS";

// Constants of action types concerning filter values
export const RESET_FILTER_VALUES = "RESET_FILTER_VALUES";
export const EDIT_FILTER_VALUE = "EDIT_FILTER_VALUE";

// Constants of action types concerning text filter
export const EDIT_TEXT_FILTER = "EDIT_TEXT_FILTER";
export const REMOVE_TEXT_FILTER = "REMOVE_TEXT_FILTER";

export const LOAD_FILTER_PROFILE = "LOAD_FILTER_PROFILE";

// Constants of action types concerning dates and filter selection;
export const EDIT_SELECTED_FILTER = "EDIT_SELECTED_FILTER";
export const REMOVE_SELECTED_FILTER = "REMOVE_SELECTED_FILTER";
export const EDIT_SECOND_FILTER = "EDIT_SECOND_FILTER";
export const REMOVE_SECOND_FILTER = "REMOVE_SECOND_FILTER";

// Actions affecting fetching of filters from server

export const loadFiltersInProgress = () => ({
	type: LOAD_FILTERS_IN_PROGRESS,
});

export const loadFiltersSuccess = (filters, resource) => ({
	type: LOAD_FILTERS_SUCCESS,
	payload: { filters, resource },
});

export const loadFiltersFailure = () => ({
	type: LOAD_FILTERS_FAILURE,
});

// Actions affecting fetching of stats from server

export const loadStats = (stats) => ({
	type: LOAD_STATS,
	payload: { stats },
});

// Actions affecting filter values

export const resetFilterValues = () => ({
	type: RESET_FILTER_VALUES,
});

export const editFilterValue = (filterName, value) => ({
	type: EDIT_FILTER_VALUE,
	payload: { filterName, value },
});

// Actions affecting text filter

export const editTextFilter = (textFilter) => ({
	type: EDIT_TEXT_FILTER,
	payload: { textFilter },
});

export const removeTextFilter = () => ({
	type: REMOVE_TEXT_FILTER,
});

// Actions affecting loading filters profiles saved

export const loadFilterProfile = (filterMap) => ({
	type: LOAD_FILTER_PROFILE,
	payload: { filterMap },
});

// Actions affecting dates and filter selection

export const editSelectedFilter = (filter) => ({
	type: EDIT_SELECTED_FILTER,
	payload: { filter },
});

export const removeSelectedFilter = () => ({
	type: REMOVE_SELECTED_FILTER,
});

export const editSecondFilter = (filter) => ({
	type: EDIT_SECOND_FILTER,
	payload: { filter },
});

export const removeSecondFilter = () => ({
	type: REMOVE_SECOND_FILTER,
});
