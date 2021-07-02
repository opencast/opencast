import axios from "axios";
import {addNotification} from "./notificationThunks";

export const postTasks = values => async dispatch => {
    let configuration = {};

    // todo: implement config when backend is updated
    for (let i = 0; i < values.events.length; i++) {
        if (values.events[i].selected) {
            let eventId = values.events[i].id;
            configuration[eventId] = {
                configuration: 'to be Implemented'
            }
        }
    }

    let metadataJson = {
        workflow: values.workflow,
        configuration: configuration
    }

    let data = new URLSearchParams();
    data.append('metadata', JSON.stringify(metadataJson));

    axios.post('/admin-ng/tasks/new', data, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    }).then(response => {
        console.log(response);
        dispatch(addNotification('success', 'TASK_CREATED'));
    }).catch(response => {
        console.log(response);
        dispatch(addNotification('error', 'TASK_NOT_CREATED'));
    });
}
