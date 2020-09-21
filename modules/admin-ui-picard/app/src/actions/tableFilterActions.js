/**
 * This file contains all redux actions that can be executed on table filters
 */

// Constants of of actions types for fetching filters from server
export const LOAD_FILTERS_IN_PROGRESS = 'LOAD_FILTERS_IN_PROGRESS';
export const LOAD_FILTERS_SUCCESS = 'LOAD_FILTERS_SUCCESS';
export const LOAD_FILTERS_FAILURE = 'LOAD_FILTERS_FAILURE';

// Constants of action types concerning filter values
export const RESET_FILTER_VALUES = 'RESET_FILTER_VALUES';
export const EDIT_FILTER_VALUE = 'EDIT_FILTER_VALUE';

// Constants of action types concerning text filter
export const EDIT_TEXT_FILTER = 'EDIT_TEXT_FILTER';
export const REMOVE_TEXT_FILTER = 'REMOVE_TEXT_FILTER';

export const LOAD_FILTER_PROFILE = 'LOAD_FILTER_PROFILE';

// Constants of action types concerning dates and filter selection;
export const EDIT_SELECTED_FILTER = 'EDIT_SELECTED_FILTER';
export const REMOVE_SELECTED_FILTER = 'REMOVE_SELECTED_FILTER';
export const EDIT_SECOND_FILTER = 'EDIT_SECOND_FILTER';
export const REMOVE_SECOND_FILTER = 'REMOVE_SECOND_FILTER';
export const SET_START_DATE = 'SET_START_DATE';
export const SET_END_DATE = 'SET_END_DATE';
export const RESET_START_DATE = 'RESET_START_DATE';
export const RESET_END_DATE = 'RESET_END_DATE';


// Actions affecting fetching of filters from server

export const loadFiltersInProgress = () => ({
    type: LOAD_FILTERS_IN_PROGRESS
});

export const loadFiltersSuccess = filters => ({
    type: LOAD_FILTERS_SUCCESS,
    payload: { filters }
});

export const loadFiltersFailure = () => ({
    type: LOAD_FILTERS_FAILURE
});

// Actions affecting filter values

export const resetFilterValues = () => ({
    type: RESET_FILTER_VALUES
});

export const editFilterValue = (filterName, value) => ({
    type: EDIT_FILTER_VALUE,
    payload: { filterName, value }
});


// Actions affecting text filter

export const editTextFilter = textFilter => ({
    type: EDIT_TEXT_FILTER,
    payload: { textFilter }
});

export const removeTextFilter = () => ({
    type: REMOVE_TEXT_FILTER
});

// Actions affecting loading filters profiles saved

export const loadFilterProfile = filterMap => ({
    type: LOAD_FILTER_PROFILE,
    payload: { filterMap }
});


// Actions affecting dates and filter selection

export const editSelectedFilter = filter => ({
    type: EDIT_SELECTED_FILTER,
    payload: { filter }
});

export const removeSelectedFilter = () => ({
    type: REMOVE_SELECTED_FILTER
});

export const editSecondFilter = filter => ({
    type: EDIT_SECOND_FILTER,
    payload: { filter }
});

export const removeSecondFilter = () => ({
   type: REMOVE_SECOND_FILTER
});

export const setStartDate = date => ({
    type: SET_START_DATE,
    payload: { date }
});

export const setEndDate = date => ({
    type: SET_END_DATE,
    payload: { date }
});

export const resetStartDate = () => ({
    type: RESET_START_DATE
});

export const resetEndDate = () => ({
    type: RESET_END_DATE
});


