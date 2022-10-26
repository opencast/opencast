/**
 * This file contains selectors regarding information about the health status
 */

export const getHealthStatus = (state) => state.health.service;
export const getErrorStatus = (state) => state.health.error;
export const getErrorCount = (state) => state.health.numErr;
