/**
 * This file contains all redux actions that can be executed on a series
 */

// Constants of action types for fetching details of a certain series from server
export const LOAD_SERIES_DETAILS_IN_PROGRESS = 'LOAD_SERIES_METADATA_IN_PROGRESS';
export const LOAD_SERIES_DETAILS_SUCCESS = 'LOAD_SERIES_METADATA_SUCCESS';
export const LOAD_SERIES_DETAILS_FAILURE = 'LOAD_SERIES_METADATA_FAILURE';


// Actions affecting fetching details of a certain series from server

export const loadSeriesMetadataInProgress = () => ({
    type: LOAD_SERIES_DETAILS_IN_PROGRESS
});

export const loadSeriesMetadataSuccess = seriesMetadata => ({
    type: LOAD_SERIES_DETAILS_SUCCESS,
    payload: { seriesMetadata }
});

export const loadSeriesMetadataFailure = () => ({
    type: LOAD_SERIES_DETAILS_FAILURE
});
