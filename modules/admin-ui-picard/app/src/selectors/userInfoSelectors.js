/**
 * This file contains selectors regarding information about the current user
 */
export const getUserInformation = state => state.userInfo;
export const getUserBasicInfo = state => state.userInfo.user;
export const getUserRoles = state => state.userInfo.roles;
