/**
 * This file contains all redux actions that can be executed on series
 */

// Constants of actions types for fetching series from server
export const LOAD_SERIES_IN_PROGRESS = 'LOAD_SERIES_IN_PROGRESS';
export const LOAD_SERIES_SUCCESS = 'LOAD_SERIES_SUCCESS';
export const LOAD_SERIES_FAILURE = 'LOAD_SERIES_FAILURE';

// Constant of actions types affecting UI
export const SHOW_ACTIONS = 'SHOW_ACTIONS';

// Actions affecting fetching series from server

export const loadSeriesInProgress = () => ({
    type: LOAD_SERIES_IN_PROGRESS
});

export const loadSeriesSuccess = series => ({
    type: LOAD_SERIES_SUCCESS,
    payload: { series }
});


export const loadSeriesFailure = () => ({
    type: LOAD_SERIES_FAILURE
});

// Actions affecting UI

export const showActions = isShowing => ({
    type: SHOW_ACTIONS,
    payload: { isShowing }
});
