import {
	LOAD_ACL_DETAILS_FAILURE,
	LOAD_ACL_DETAILS_IN_PROGRESS,
	LOAD_ACL_DETAILS_SUCCESS,
} from "../actions/aclDetailsActions";

/**
 * This file contains redux reducer for actions affecting the state of details of an ACL
 */

// initial redux state
const initialState = {
	isLoading: false,
	organizationId: "",
	id: 0,
	name: "",
	acl: {},
};

// reducer
const aclDetails = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_ACL_DETAILS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_ACL_DETAILS_SUCCESS: {
			const { aclDetails } = payload;
			return {
				...state,
				isLoading: false,
				organizationId: aclDetails.organizationId,
				id: aclDetails.id,
				name: aclDetails.name,
				acl: aclDetails.acl,
			};
		}
		case LOAD_ACL_DETAILS_FAILURE: {
			return {
				...state,
				isLoading: false,
				organizationId: "",
				id: 0,
				name: "",
				acl: {},
			};
		}
		default:
			return state;
	}
};

export default aclDetails;
