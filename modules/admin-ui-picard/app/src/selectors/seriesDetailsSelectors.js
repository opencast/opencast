/**
 * This file contains selectors regarding details of a certain series
 */
export const getSeriesMetadata = state => state.seriesDetails.metadata;
export const getSeriesAcl = state => state.seriesDetails.acl;
export const getSeriesFeeds = state => state.seriesDetails.feeds;
