/**
 * This file contains selectors regarding events
 */

export const getEvents = (state) => state.events.results;
export const getVisibilityEventColumns = (state) => state.events.columns;
export const isShowActions = (state) => state.events.showActions;
export const isLoading = (state) => state.events.isLoading;
export const getEventMetadata = (state) => state.events.metadata;
export const getExtendedEventMetadata = (state) =>
	state.events.extendedMetadata;
export const isLoadingScheduling = (state) =>
	state.events.schedulingInfo.isLoading;
export const getSchedulingEditedEvents = (state) =>
	state.events.schedulingInfo.editedEvents;
export const getSchedulingSeriesOptions = (state) =>
	state.events.schedulingInfo.seriesOptions;
export const getTotalEvents = (state) => state.events.total;
export const getAssetUploadOptions = (state) => state.events.uploadAssetOptions;
export const isFetchingAssetUploadOptions = (state) =>
	state.events.isFetchingAssetUploadOptions;
export const getAssetUploadWorkflow = (state) =>
	state.events.uploadAssetWorkflow;
