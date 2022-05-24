/**
 * This file contains all redux actions that can be executed on asset options
 */

// Constants of action types for fetching asset upload options from server
export const LOAD_ASSET_UPLOAD_OPTIONS_IN_PROGRESS = 'LOAD_ASSET_UPLOAD_OPTIONS_IN_PROGRESS';
export const LOAD_ASSET_UPLOAD_OPTIONS_SUCCESS = 'LOAD_ASSET_UPLOAD_OPTIONS_SUCCESS';
export const LOAD_ASSET_UPLOAD_OPTIONS_FAILURE = 'LOAD_ASSET_UPLOAD_OPTIONS_FAILURE';

// Actions affecting fetching asset upload options from server

export const loadAssetUploadOptionsInProgress = () => ({
    type: LOAD_ASSET_UPLOAD_OPTIONS_IN_PROGRESS
});

export const loadAssetUploadOptionsSuccess = (assetUploadOptions) => ({
    type: LOAD_ASSET_UPLOAD_OPTIONS_SUCCESS,
    payload: { assetUploadOptions }
});

export const loadAssetUploadOptionsFailure = () => ({
    type: LOAD_ASSET_UPLOAD_OPTIONS_FAILURE
});