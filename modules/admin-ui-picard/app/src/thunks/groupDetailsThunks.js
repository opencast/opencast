import {
	loadGroupDetailsFailure,
	loadGroupDetailsInProgress,
	loadGroupDetailsSuccess,
} from "../actions/groupDetailsActions";
import axios from "axios";
import { addNotification } from "./notificationThunks";
import { buildGroupBody } from "../utils/resourceUtils";
import { logger } from "../utils/logger";

// fetch details about certain group from server
export const fetchGroupDetails = (groupName) => async (dispatch) => {
	try {
		dispatch(loadGroupDetailsInProgress());

		let data = await axios.get(`/admin-ng/groups/${groupName}`);

		const response = await data.data;

		let users = [];
		if (response.users.length > 0) {
			users = response.users.map((user) => {
				return {
					id: user.username,
					name: user.name,
				};
			});
		}

		const groupDetails = {
			role: response.role,
			roles: response.roles,
			name: response.name,
			description: response.description,
			id: response.id,
			users: users,
		};

		dispatch(loadGroupDetailsSuccess(groupDetails));
	} catch (e) {
		dispatch(loadGroupDetailsFailure());
		logger.error(e);
	}
};

// update details of a certain group
export const updateGroupDetails = (values, groupId) => async (dispatch) => {
	// get URL params used for put request
	let data = buildGroupBody(values);

	// PUT request
	axios
		.put(`/admin-ng/groups/${groupId}`, data)
		.then((response) => {
			logger.info(response);
			dispatch(addNotification("success", "GROUP_UPDATED"));
		})
		.catch((response) => {
			logger.error(response);
			if (response.status === 409) {
				dispatch(addNotification("error", "GROUP_CONFLICT"));
			} else {
				dispatch(addNotification("error", "GROUP_NOT_SAVED"));
			}
		});
};
