/**
 * This file contains selectors regarding details of a certain series
 */
export const getSeriesDetailsMetadata = state => state.seriesDetails.metadata;
export const getSeriesDetailsAcl = state => state.seriesDetails.acl;
export const getSeriesDetailsFeeds = state => state.seriesDetails.feeds;
export const getSeriesDetailsTheme = state => state.seriesDetails.theme;
export const getSeriesDetailsThemeNames = state => state.seriesDetails.themeNames;
