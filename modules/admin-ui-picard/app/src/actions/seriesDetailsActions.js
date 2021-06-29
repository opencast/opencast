/**
 * This file contains all redux actions that can be executed on a series
 */

// Constants of action types for fetching details of a certain series from server
export const LOAD_SERIES_DETAILS_IN_PROGRESS = 'LOAD_SERIES_DETAILS_IN_PROGRESS';
export const LOAD_SERIES_DETAILS_SUCCESS = 'LOAD_SERIES_DETAILS_SUCCESS';
export const LOAD_SERIES_DETAILS_FAILURE = 'LOAD_SERIES_DETAILS_FAILURE';


// Actions affecting fetching details of a certain series from server

export const loadSeriesDetailsInProgress = () => ({
    type: LOAD_SERIES_DETAILS_IN_PROGRESS
});

export const loadSeriesDetailsSuccess = seriesDetails => ({
    type: LOAD_SERIES_DETAILS_SUCCESS,
    payload: { seriesDetails }
});

export const loadSeriesDetailsFailure = () => ({
    type: LOAD_SERIES_DETAILS_FAILURE
});
