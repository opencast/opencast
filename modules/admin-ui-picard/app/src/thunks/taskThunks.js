import axios from "axios";
import { addNotification } from "./notificationThunks";
import { logger } from "../utils/logger";

export const postTasks = (values) => async (dispatch) => {
	let configuration = {};
	Object.keys(values.configuration).forEach((config) => {
		configuration[config] = String(values.configuration[config]);
	});

	let workflowConfig = {};
	for (let i = 0; i < values.events.length; i++) {
		if (values.events[i].selected) {
			let eventId = values.events[i].id;
			workflowConfig[eventId] = configuration;
		}
	}

	let metadataJson = {
		workflow: values.workflow,
		configuration: workflowConfig,
	};

	let data = new URLSearchParams();
	data.append("metadata", JSON.stringify(metadataJson));

	axios
		.post("/admin-ng/tasks/new", data, {
			headers: {
				"Content-Type": "application/x-www-form-urlencoded",
			},
		})
		.then((response) => {
			logger.info(response);
			dispatch(addNotification("success", "TASK_CREATED"));
		})
		.catch((response) => {
			logger.error(response);
			dispatch(addNotification("error", "TASK_NOT_CREATED"));
		});
};
