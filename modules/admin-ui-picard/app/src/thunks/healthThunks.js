import {
    loadHealthStatus,
    loadStatusFailure,
    loadStatusInProgress,
    setError,
    setNumError
} from "../actions/healthActions";

/**
 * This file contains methods/thunks used to query the REST-API of Opencast to get information about the health status of OC.
 * This information is shown in the bell in the header.
 * */

export const AMQ_NAME = 'ActiveMQ';
export const STATES_NAMES = 'Service States';
export const BACKEND_NAMES = 'Backend Services';

const OK = 'OK';
const MALFORMED_DATA = 'Malformed Data';

// Fetch health status and transform it to further use
export const fetchHealthStatus = () => async (dispatch, getState) => {
    try {
        dispatch(loadStatusInProgress());

        let healthStatus = {
            name: STATES_NAMES,
            status: '',
            error: false
        }
        dispatch(loadHealthStatus(healthStatus));

        let numError = 0;
        let isError = false;

        fetch('/broker/status').then(
            function (response){
                let healthStatus;
                if (response.status === 204) {
                    healthStatus = {
                        name: AMQ_NAME,
                        status: OK,
                        error: false
                    }
                } else {
                    healthStatus = {
                        name: AMQ_NAME,
                        status: response.statusText,
                        error: true
                    }
                }
                dispatch(loadHealthStatus(healthStatus));
            }
        ).catch(function (err) {
            let healthStatus = {
                name: AMQ_NAME,
                status: err.statusText,
                error: true
            };
            dispatch(loadHealthStatus(healthStatus));

            isError = true;
            numError++;
        });

        fetch('/services/health.json').then(
          function(response) {
              response.json().then(function (data) {
                  let healthStatus;
                  if (undefined === data || undefined === data.health) {
                      healthStatus = {
                          name: STATES_NAMES,
                          status: MALFORMED_DATA,
                          error: true
                      };
                      dispatch(loadHealthStatus(healthStatus));

                      isError = true;
                      numError++;
                      return;
                  }
                  let abnormal = 0;
                  abnormal = data.health['warning'] + data.health['error'];
                  if (abnormal === 0) {
                      healthStatus = {
                          name: BACKEND_NAMES,
                          status: OK,
                          error: false
                      }
                      dispatch(loadHealthStatus(healthStatus));
                  } else {
                      healthStatus = {
                          name: BACKEND_NAMES,
                          status: response.statusText,
                          error: true
                      }
                      dispatch(loadHealthStatus(healthStatus));

                      isError = true;
                      numError = numError + abnormal;
                  }
              }).catch(function (err) {
                  let healthStatus = {
                      name: STATES_NAMES,
                      status: err.statusText,
                      error: true
                  }
                  dispatch(loadHealthStatus(healthStatus));

                  isError = false;
                  numError++;
              })
          }
        );

        dispatch(setError(isError));
        dispatch(setNumError(numError));


    } catch (e) {
        dispatch(loadStatusFailure());
        console.log(e);
    }
}
