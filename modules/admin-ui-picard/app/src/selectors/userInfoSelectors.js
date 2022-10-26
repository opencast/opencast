/**
 * This file contains selectors regarding information about the current user
 */
export const getUserInformation = (state) => state.userInfo;
export const getUserBasicInfo = (state) => state.userInfo.user;
export const getUserRoles = (state) => state.userInfo.roles;
export const getOrgProperties = (state) => state.userInfo.org.properties;
export const getOrgId = (state) => state.userInfo.org.id;
