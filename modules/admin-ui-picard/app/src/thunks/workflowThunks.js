import {loadWorkflowDefFailure, loadWorkflowDefInProgress, loadWorkflowDefSuccess} from "../actions/workflowActions";

// fetch workflow definitions from server
export const fetchWorkflowDef = (type) => async (dispatch, getState) => {
    try {
        dispatch(loadWorkflowDefInProgress());

        let urlParams

        switch (type) {
            case 'tasks': {
                urlParams = new URLSearchParams({
                    tags : 'archive'
                });
                break;
            }
            case 'delete-event': {
                urlParams = new URLSearchParams({
                    tags: 'delete'
                });
                break;
            }
            default: {
                urlParams = new URLSearchParams({
                    tags: ['upload','schedule']
                });
            }
        }

        let data = await fetch('/admin-ng/event/new/processing?' + urlParams);

        const response = await  data.json();

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
