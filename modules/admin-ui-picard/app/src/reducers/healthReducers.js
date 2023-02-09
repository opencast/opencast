import {
	ADD_NUM_ERROR,
	LOAD_HEALTH_STATUS,
	LOAD_STATUS_FAILURE,
	LOAD_STATUS_IN_PROGRESS,
	RESET_NUM_ERROR,
	SET_ERROR,
} from "../actions/healthActions";
import { BACKEND_NAMES, STATES_NAMES } from "../thunks/healthThunks";

/**
 * This file contains redux reducer for actions affecting the state of information about health status
 */

// Initial state of health status in redux store
const initialState = {
	loading: false,
	service: [
		{
			name: STATES_NAMES,
			error: false,
			status: "",
		},
		{
			name: BACKEND_NAMES,
			error: false,
			status: "",
		},
	],
	error: false,
	numErr: 0,
};

// Reducer for health status
export const health = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_HEALTH_STATUS: {
			const { healthStatus: updatedHealthStatus } = payload;
			return {
				...state,
				loading: false,
				service: state.service.map((healthStatus) => {
					if (healthStatus.name === updatedHealthStatus.name) {
						return updatedHealthStatus;
					}
					return healthStatus;
				}),
			};
		}
		case LOAD_STATUS_IN_PROGRESS: {
			return {
				...state,
				loading: true,
			};
		}
		case LOAD_STATUS_FAILURE: {
			return {
				...state,
				loading: false,
			};
		}
		case SET_ERROR: {
			const { isError } = payload;
			return {
				...state,
				error: isError,
			};
		}
		case ADD_NUM_ERROR: {
			const { numError } = payload;
			return {
				...state,
				numErr: state.numErr + numError,
			};
		}
		case RESET_NUM_ERROR: {
			return {
				...state,
				numErr: 0,
			};
		}
		default:
			return state;
	}
};

export default health;
