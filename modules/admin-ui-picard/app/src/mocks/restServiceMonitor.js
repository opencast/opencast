/*
* This Service is used to query the REST-API of Opencast to get the current status of the background services.
* On one hand this information is necessary for the notification about the status of these services in the header
* (Bell-icon) and on the other hand for the service section in the systems menu pointy.
*
* Currently only a mock json is returned.
*
* */
const services = {
    error: true,
    numErr: 1,
    service: {
        'ActiveMQ': {
            error: true,
            status: 'Not Found'
        },
        'Backend Service': {
            status: 'OK'
        }
    }
};

export default services;
