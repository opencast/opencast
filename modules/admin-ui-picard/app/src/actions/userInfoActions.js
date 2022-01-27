/**
 * This file contains all redux actions that can be executed on information about the current user
 */

// Constants of action types for fetching information about the current user from server
export const LOAD_USER_INFO_IN_PROGRESS = 'LOAD_USER_INFO_IN_PROGRESS';
export const LOAD_USER_INFO_SUCCESS = 'LOAD_USER_INFO_SUCCESS';
export const LOAD_USER_INFO_FAILURE = 'LOAD_USER_INFO_FAILURE';

// Actions affecting fetching information about the current user from server

export const loadUserInfoInProgress = () => ({
    type: LOAD_USER_INFO_IN_PROGRESS
});

export const loadUserInfoSuccess = userInfo => ({
    type: LOAD_USER_INFO_SUCCESS,
    payload: { userInfo }
});

export const loadUserInfoFailure = () => ({
    type: LOAD_USER_INFO_FAILURE
});
