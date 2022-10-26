import axios from "axios";
import { getAssetUploadOptions } from "../selectors/eventSelectors";
import {
	loadAssetUploadOptionsFailure,
	loadAssetUploadOptionsInProgress,
	loadAssetUploadOptionsSuccess,
	setAssetUploadWorkflow,
} from "../actions/assetActions";
import { logger } from "../utils/logger";

// thunks for assets, especially for getting asset options

export const fetchAssetUploadOptions = () => async (dispatch, getState) => {
	// get old asset upload options
	const state = getState();
	const assetUploadOptions = getAssetUploadOptions(state);

	const sourcePrefix = "EVENTS.EVENTS.NEW.SOURCE.UPLOAD";
	const assetPrefix = "EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION";
	const workflowPrefix = "EVENTS.EVENTS.NEW.UPLOAD_ASSET.WORKFLOWDEFID";

	// only fetch asset upload options, if they haven't been fetched yet
	if (!(assetUploadOptions.length !== 0 && assetUploadOptions.length !== 0)) {
		dispatch(loadAssetUploadOptionsInProgress());

		// request asset upload options from API
		axios
			.get("/admin-ng/resources/eventUploadAssetOptions.json")
			.then((dataResponse) => {
				const assetUploadOptions = [];

				// iterate over response and only use non-comment lines
				for (const [optionKey, optionJson] of Object.entries(
					dataResponse.data
				)) {
					if (optionKey.charAt(0) !== "$") {
						const isSourceOption = optionKey.indexOf(sourcePrefix) >= 0;
						const isAssetOption = optionKey.indexOf(assetPrefix) >= 0;

						// if the line is a source upload option or additional asset upload option,
						// format it and add to upload options list
						if (isSourceOption || isAssetOption) {
							let option = JSON.parse(optionJson);

							option = {
								...option,
								title: !option.title ? optionKey : option.title,
								showAs: isSourceOption ? "source" : "uploadAsset",
							};

							assetUploadOptions.push(option);
						} else if (optionKey.indexOf(workflowPrefix) >= 0) {
							// if the line is the upload asset workflow id, set the asset upload workflow
							dispatch(setAssetUploadWorkflow(optionJson));
						}
					}
				}

				dispatch(loadAssetUploadOptionsSuccess(assetUploadOptions));
			})
			.catch((response) => {
				// getting asset upload options from API failed
				dispatch(loadAssetUploadOptionsFailure());
				logger.error(response);
			});
	}
};
