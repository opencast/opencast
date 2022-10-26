/**
 * This file contains all redux actions that can be executed on table filter profiles
 */

// Constants of action types concerning filter profile
export const CREATE_FILTER_PROFILE = "CREATE_FILTER_PROFILE";
export const EDIT_FILTER_PROFILE = "EDIT_FILTER_PROFILE";
export const REMOVE_FILTER_PROFILE = "REMOVE_FILTER_PROFILE";
export const CANCEL_EDITING_FILTER_PROFILE = "CANCEL_EDITING_FILTER_PROFILE";

// Actions affecting filter profiles

export const createFilterProfile = (filterProfile) => ({
	type: CREATE_FILTER_PROFILE,
	payload: { filterProfile },
});

export const editFilterProfile = (filterProfile) => ({
	type: EDIT_FILTER_PROFILE,
	payload: { filterProfile },
});

export const removeFilterProfile = (filterProfile) => ({
	type: REMOVE_FILTER_PROFILE,
	payload: { filterProfile },
});

export const cancelEditFilterProfile = () => ({
	type: CANCEL_EDITING_FILTER_PROFILE,
});
