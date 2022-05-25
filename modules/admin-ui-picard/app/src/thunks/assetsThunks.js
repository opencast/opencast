import axios from "axios";
import {getAssetUploadOptions} from "../selectors/eventSelectors";
import {
    loadAssetUploadOptionsFailure,
    loadAssetUploadOptionsInProgress,
    loadAssetUploadOptionsSuccess, setAssetUploadWorkflow
} from "../actions/assetActions";
import {logger} from "../utils/logger";

// thunks for assets, especially for getting asset options

export const fetchAssetUploadOptions = () => async (dispatch, getState) => {
    // get
    const state = getState();
    const assetUploadOptions = getAssetUploadOptions(state);

    const sourcePrefix = 'EVENTS.EVENTS.NEW.SOURCE.UPLOAD';
    const assetPrefix = 'EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION';
    const workflowPrefix = 'EVENTS.EVENTS.NEW.UPLOAD_ASSET.WORKFLOWDEFID';

    if(!(assetUploadOptions.length !== 0 && assetUploadOptions.length !== 0)){
        dispatch(loadAssetUploadOptionsInProgress());

        // request  from API
        axios.get('/admin-ng/resources/eventUploadAssetOptions.json')
            .then( dataResponse => {
                const assetUploadOptions = [];

                for(const [optionKey, optionJson] of Object.entries(dataResponse.data)){
                    if (optionKey.charAt(0) !== '$') {
                        const isSourceOption = (optionKey.indexOf(sourcePrefix) >= 0);
                        const isAssetOption = (optionKey.indexOf(assetPrefix) >= 0);

                        if(isSourceOption || isAssetOption){
                            let option = JSON.parse(optionJson);

                            option = {
                                ...option,
                                title: !option.title ? optionKey : option.title,
                                showAs: isSourceOption ? 'source' : 'uploadAsset'
                            };

                            assetUploadOptions.push(option);
                        } else if(optionKey.indexOf(workflowPrefix) >= 0) {
                            const workflow = optionJson;
                            dispatch(setAssetUploadWorkflow(workflow));
                        }
                    }
                }

                dispatch(loadAssetUploadOptionsSuccess(assetUploadOptions));
            })
            .catch( response => {
                // getting  from API failed
                dispatch(loadAssetUploadOptionsFailure());
                logger.error(response);
            });
    }
}