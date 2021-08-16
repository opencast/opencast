import axios from "axios";
import {loadWorkflowDefFailure, loadWorkflowDefInProgress, loadWorkflowDefSuccess} from "../actions/workflowActions";

// fetch workflow definitions from server
export const fetchWorkflowDef = (type) => async (dispatch) => {
    try {
        dispatch(loadWorkflowDefInProgress());

        let urlParams

        switch (type) {
            case 'tasks': {
                urlParams = {
                    tags : 'archive'
                };
                break;
            }
            case 'delete-event': {
                urlParams = {
                    tags: 'delete'
                };
                break;
            }
            case 'event-details':
                urlParams = {
                    tags: 'schedule'
                };
            default: {
                urlParams = {
                    tags: 'upload,schedule'
                };
            }
        }

        let data = await axios.get('/admin-ng/event/new/processing?', { params: urlParams });

        const response = await  data.data;

        const workflowDef = {
            defaultWorkflowId: response.default_workflow_id,
            workflows: response.workflows
        }
        dispatch(loadWorkflowDefSuccess(workflowDef));
    } catch (e) {
        dispatch(loadWorkflowDefFailure());
        console.log(e);
    }
}
