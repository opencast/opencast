/**
 * This file contains all redux actions that can be executed on users
 */

// Constants of action types for fetching users from server
export const LOAD_USERS_IN_PROGRESS = 'LOAD_USERS_IN_PROGRESS';
export const LOAD_USERS_SUCCESS = 'LOAD_USERS_SUCCESS';
export const LOAD_USERS_FAILURE = 'LOAD_USERS_FAILURE';

// Actions affecting fetching users from server

export const loadUsersInProgress = () => ({
    type: LOAD_USERS_IN_PROGRESS
});

export const loadUsersSuccess = users => ({
    type: LOAD_USERS_SUCCESS,
    payload: { users }
});

export const loadUsersFailure = () => ({
    type: LOAD_USERS_FAILURE
});
