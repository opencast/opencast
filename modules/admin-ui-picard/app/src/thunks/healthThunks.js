import axios from "axios";
import {
	addNumError,
	loadHealthStatus,
	loadStatusFailure,
	loadStatusInProgress,
	resetNumError,
	setError,
} from "../actions/healthActions";
import { logger } from "../utils/logger";

/**
 * This file contains methods/thunks used to query the REST-API of Opencast to get information about the health status of OC.
 * This information is shown in the bell in the header.
 * */

export const STATES_NAMES = "Service States";
export const BACKEND_NAMES = "Backend Services";

const OK = "OK";
const MALFORMED_DATA = "Malformed Data";
const ERROR = "error";

// Fetch health status and transform it to further use
export const fetchHealthStatus = () => async (dispatch) => {
	try {
		dispatch(loadStatusInProgress());

		// Reset state of health status
		let healthStatus = {
			name: STATES_NAMES,
			status: "",
			error: false,
		};
		await dispatch(loadHealthStatus(healthStatus));
		await dispatch(resetNumError());
		await dispatch(setError(false));

		// Get current state of services
		axios
			.get("/services/health.json")
			.then(function (response) {
				let healthStatus;
				if (undefined === response.data || undefined === response.data.health) {
					healthStatus = {
						name: STATES_NAMES,
						status: MALFORMED_DATA,
						error: true,
					};
					dispatch(loadHealthStatus(healthStatus));

					dispatch(setError(true));
					dispatch(addNumError(1));
					return;
				}
				let abnormal = 0;
				abnormal =
					response.data.health["warning"] + response.data.health["error"];
				if (abnormal === 0) {
					healthStatus = {
						name: BACKEND_NAMES,
						status: OK,
						error: false,
					};
					dispatch(loadHealthStatus(healthStatus));
				} else {
					healthStatus = {
						name: BACKEND_NAMES,
						status: ERROR,
						error: true,
					};
					dispatch(loadHealthStatus(healthStatus));

					dispatch(setError(true));
					dispatch(addNumError(abnormal));
				}
			})
			.catch(function (err) {
				let healthStatus = {
					name: STATES_NAMES,
					status: err.message,
					error: true,
				};
				dispatch(loadHealthStatus(healthStatus));

				dispatch(setError(true));
				dispatch(addNumError(1));
			});
	} catch (e) {
		dispatch(loadStatusFailure());
		logger.error(e);
	}
};
