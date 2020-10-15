/**
 * This file contains all redux actions that can be executed on services
 */

// Constants of action types for fetching services from server
export const LOAD_SERVICES_IN_PROGRESS = 'LOAD_SERVICES_IN_PROGRESS';
export const LOAD_SERVICES_SUCCESS = 'LOAD_SERVICES_SUCCESS';
export const LOAD_SERVICES_FAILURE = 'LOAD_SERVICES_FAILURE';

// Actions affecting fetching jobs from server

export const loadServicesInProgress = () => ({
    type: LOAD_SERVICES_IN_PROGRESS
});

export const loadServicesSuccess = services => ({
    type: LOAD_SERVICES_SUCCESS,
    payload: { services }
});

export const loadServicesFailure = () => ({
    type: LOAD_SERVICES_FAILURE
});
