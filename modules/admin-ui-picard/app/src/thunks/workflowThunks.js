import axios from "axios";
import {
	loadWorkflowDefFailure,
	loadWorkflowDefInProgress,
	loadWorkflowDefSuccess,
} from "../actions/workflowActions";
import { logger } from "../utils/logger";

// fetch workflow definitions from server
export const fetchWorkflowDef = (type) => async (dispatch) => {
	try {
		dispatch(loadWorkflowDefInProgress());

		let urlParams;

		switch (type) {
			case "tasks": {
				urlParams = {
					tags: "archive",
				};
				break;
			}
			case "delete-event": {
				urlParams = {
					tags: "delete",
				};
				break;
			}
			case "event-details":
				urlParams = {
					tags: "schedule",
				};
				break;
			default: {
				urlParams = {
					tags: "upload,schedule",
				};
			}
		}

		let data = await axios.get("/admin-ng/event/new/processing?", {
			params: urlParams,
		});

		const response = await data.data;

		let workflows = response.workflows;

		workflows = workflows.map((workflow) => {
			if (workflow.configuration_panel_json.length > 0) {
				return {
					...workflow,
					configuration_panel_json: JSON.parse(
						workflow.configuration_panel_json
					),
				};
			} else {
				return workflow;
			}
		});

		const workflowDef = {
			defaultWorkflowId: response.default_workflow_id,
			workflows: workflows,
		};
		dispatch(loadWorkflowDefSuccess(workflowDef));
	} catch (e) {
		dispatch(loadWorkflowDefFailure());
		logger.error(e);
	}
};
