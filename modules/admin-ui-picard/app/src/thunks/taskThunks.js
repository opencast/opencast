import axios from "axios";

export const postTasks = async values => {
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

    let data = new FormData();
    data.append('metadata', JSON.stringify(metadataJson));

    axios.post('/admin-ng/tasks/new', data, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    }).then(response => console.log(response)).catch(response => console.log(response));
}
