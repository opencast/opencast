import {
	LOAD_GROUP_DETAILS_FAILURE,
	LOAD_GROUP_DETAILS_IN_PROGRESS,
	LOAD_GROUP_DETAILS_SUCCESS,
} from "../actions/groupDetailsActions";

/**
 * This file contains redux reducer for actions affecting the state of details of a group
 */

// initial redux state
const initialState = {
	isLoading: false,
	role: "",
	roles: [],
	name: "",
	description: "",
	id: "",
	users: [],
};

// group details reducer
const groupDetails = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_GROUP_DETAILS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_GROUP_DETAILS_SUCCESS: {
			const { groupDetails } = payload;
			return {
				...state,
				isLoading: false,
				role: groupDetails.role,
				roles: groupDetails.roles,
				name: groupDetails.name,
				description: groupDetails.description,
				id: groupDetails.id,
				users: groupDetails.users,
			};
		}
		case LOAD_GROUP_DETAILS_FAILURE: {
			return {
				...state,
				isLoading: false,
				role: "",
				roles: [],
				name: "",
				description: "",
				id: "",
				users: [],
			};
		}
		default:
			return state;
	}
};

export default groupDetails;
