/**
 * This file contains all redux actions that can be executed on groups
 */

// Constants of action types for fetching groups from server
export const LOAD_GROUPS_IN_PROGRESS = 'LOAD_GROUPS_IN_PROGRESS';
export const LOAD_GROUPS_SUCCESS = 'LOAD_GROUPS_SUCCESS';
export const LOAD_GROUPS_FAILURE = 'LOAD_GROUPS_FAILURE';

// Actions affecting fetching groups from server

export const loadGroupsInProgress = () => ({
    type: LOAD_GROUPS_IN_PROGRESS
});

export const loadGroupsSuccess = groups => ({
    type: LOAD_GROUPS_SUCCESS,
    payload: { groups }
});

export const loadGroupsFailure = () => ({
    type: LOAD_GROUPS_FAILURE
});
