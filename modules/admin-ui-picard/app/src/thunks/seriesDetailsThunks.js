import axios from "axios";
import _ from 'lodash';
import {logger} from "../utils/logger";
import {
    loadSeriesDetailsAclsSuccess,
    loadSeriesDetailsFailure,
    loadSeriesDetailsFeedsSuccess,
    loadSeriesDetailsInProgress,
    loadSeriesDetailsMetadataSuccess,
    loadSeriesDetailsThemeNamesFailure,
    loadSeriesDetailsThemeNamesInProgress,
    loadSeriesDetailsThemeNamesSuccess,
    loadSeriesDetailsThemeSuccess,
    setSeriesDetailsExtendedMetadata,
    setSeriesDetailsMetadata,
    setSeriesDetailsTheme,
    loadSeriesStatisticsInProgress,
    loadSeriesStatisticsSuccess,
    loadSeriesStatisticsFailure,
    updateSeriesStatisticsSuccess,
    updateSeriesStatisticsFailure
} from "../actions/seriesDetailsActions";
import {
    getSeriesDetailsExtendedMetadata,
    getSeriesDetailsMetadata,
    getSeriesDetailsThemeNames,
    getStatistics
} from "../selectors/seriesDetailsSelectors";
import {addNotification} from "./notificationThunks";
import {
    createPolicy,
    transformMetadataCollection,
    transformMetadataForUpdate
} from "../utils/resourceUtils";
import {transformToIdValueArray} from "../utils/utils";
import {NOTIFICATION_CONTEXT} from "../configs/modalConfig";
import {fetchStatistics, fetchStatisticsValueUpdate} from "./statisticsThunks";

// fetch metadata of certain series from server
export const fetchSeriesDetailsMetadata = id => async dispatch => {
    try {
        dispatch(loadSeriesDetailsInProgress());

        // fetch metadata
        let data = await axios.get(`/admin-ng/series/${id}/metadata.json`);

        const metadataResponse = await data.data;

        const mainCatalog = 'dublincore/series';
        let seriesMetadata = {};
        let extendedMetadata = [];

        for(const catalog of metadataResponse) {
            if(catalog.flavor === mainCatalog){
                seriesMetadata = transformMetadataCollection({...catalog});
            } else {
                extendedMetadata.push(transformMetadataCollection({...catalog}));
            }
        }

        dispatch(loadSeriesDetailsMetadataSuccess(seriesMetadata, extendedMetadata));

    } catch (e) {
        dispatch(loadSeriesDetailsFailure());
    }
}

// fetch acls of certain series from server
export const fetchSeriesDetailsAcls = id => async dispatch => {
    try {
        dispatch(loadSeriesDetailsInProgress());

        // fetch acl
        let data = await axios.get(`/admin-ng/series/${id}/access.json`);

        const response = await data.data;

        let seriesAcls = [];
        if (!!response.series_access) {
            const json = JSON.parse(response.series_access.acl).acl.ace;
            let policies = {};
            let policyRoles = [];
            json.forEach(policy => {
                if (!policies[policy.role]) {
                    policies[policy.role] = createPolicy(policy.role);
                    policyRoles.push(policy.role);
                }
                if (policy.action === 'read' || policy.action === 'write') {
                    policies[policy.role][policy.action] = policy.allow;
                } else if (policy.allow === true || policy.allow === 'true') {
                    policies[policy.role].actions.push(policy.action);
                }
            });
            seriesAcls = policyRoles.map(role => policies[role]);
        }

        dispatch(loadSeriesDetailsAclsSuccess(seriesAcls));

    } catch (e) {
        dispatch(loadSeriesDetailsFailure());
        logger.error(e);
    }
}

// fetch feeds of certain series from server
export const fetchSeriesDetailsFeeds = id => async dispatch => {
    try {
        dispatch(loadSeriesDetailsInProgress());

        // fetch feeds
        let data = await axios.get('/admin-ng/feeds/feeds');

        const feedsResponse = await data.data;

        logger.info(feedsResponse);

        let seriesFeeds = [];
        for (let i = 0; i < feedsResponse.length; i++) {
            if (feedsResponse[i].name === 'Series') {
                let pattern = feedsResponse[i].identifier.split('/series')[0] + feedsResponse[i].pattern;
                let uidLink = pattern.split('<series_id>')[0] + id;
                let typeLink = uidLink.split('<type>');
                let versionLink = typeLink[1].split('<version>');
                seriesFeeds = [
                    {
                        type: 'atom',
                        version: '0.3',
                        link: typeLink[0] + 'atom' + versionLink[0] + '0.3' + versionLink[1]
                    },
                    {
                        type: 'atom',
                        version: '1.0',
                        link: typeLink[0] + 'atom' + versionLink[0] + '1.0' + versionLink[1]
                    },
                    {
                        type: 'rss',
                        version: '2.0',
                        link: typeLink[0] + 'rss' + versionLink[0] + '2.0' + versionLink[1]
                    }
                ]
            }
        }

        dispatch(loadSeriesDetailsFeedsSuccess(seriesFeeds));

    } catch (e) {
        logger.error(e);
        dispatch(loadSeriesDetailsFailure());
    }
}

// fetch theme of certain series from server
export const fetchSeriesDetailsTheme = id => async dispatch => {
    try {
        dispatch(loadSeriesDetailsInProgress());

        let data = await axios.get(`/admin-ng/series/${id}/theme.json`);

        const themeResponse = await data.data;

        let seriesTheme = '';

        // check if series has a theme
        if (!_.isEmpty(themeResponse)) {
            // transform response for further use
            seriesTheme = (transformToIdValueArray(themeResponse))[0].value;
        }

        dispatch(loadSeriesDetailsThemeSuccess(seriesTheme));
    } catch (e) {
        logger.error(e);
        dispatch(loadSeriesDetailsFailure());
    }
}

// fetch names of possible themes from server
export const fetchNamesOfPossibleThemes = () => async dispatch => {
    try {
        dispatch(loadSeriesDetailsThemeNamesInProgress());

        let data = await axios.get('/admin-ng/resources/THEMES.NAME.json');

        const response = await data.data;

        // transform response for further use
        let themeNames = transformToIdValueArray(response);

        dispatch(loadSeriesDetailsThemeNamesSuccess(themeNames));
    } catch (e) {
        dispatch(loadSeriesDetailsThemeNamesFailure());
    }
}

// update series with new metadata
export const updateSeriesMetadata = (id, values) => async (dispatch, getState) => {
    try {
        let metadataInfos = getSeriesDetailsMetadata(getState());

        const {fields, data, headers} = transformMetadataForUpdate(metadataInfos, values);

        await axios.put(`/admin-ng/series/${id}/metadata`, data, headers);

        // updated metadata in series details redux store
        let seriesMetadata = {
            flavor: metadataInfos.flavor,
            title: metadataInfos.title,
            fields: fields
        };
        dispatch(setSeriesDetailsMetadata(seriesMetadata));
    } catch (e) {
        logger.error(e);
    }
}

// update series with new metadata
export const updateExtendedSeriesMetadata = (id, values, catalog) => async (dispatch, getState) => {
    try {
        const {fields, data, headers} = transformMetadataForUpdate(catalog, values);

        await axios.put(`/admin-ng/series/${id}/metadata`, data, headers);

        // updated metadata in series details redux store
        let seriesMetadata = {
            flavor: catalog.flavor,
            title: catalog.title,
            fields: fields
        };

        const oldExtendedMetadata = getSeriesDetailsExtendedMetadata(getState());
        let newExtendedMetadata = [];

        for(const catalog of oldExtendedMetadata){
            if((catalog.flavor === seriesMetadata.flavor) && (catalog.title === seriesMetadata.title)){
                newExtendedMetadata.push(seriesMetadata);
            } else {
                newExtendedMetadata.push(catalog);
            }
        }

        dispatch(setSeriesDetailsExtendedMetadata(newExtendedMetadata));
    } catch (e) {
        logger.error(e);
    }
}

export const updateSeriesAccess = (id, policies) => async (dispatch) => {
    let data = new URLSearchParams();

    data.append("acl", JSON.stringify(policies));
    data.append("override", true);

    return axios.post(`/admin-ng/series/${id}/access`, data, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    })
        .then(res => {
            logger.info(res);
            dispatch(addNotification('info', 'SAVED_ACL_RULES', -1, null, NOTIFICATION_CONTEXT));
            return true;
        })
        .catch(res => {
            logger.error(res);
            dispatch(addNotification('error', 'ACL_NOT_SAVED', -1, null, NOTIFICATION_CONTEXT));
            return false;
        });
}

export const updateSeriesTheme = (id, values) => async (dispatch, getState) => {
    let themeNames = getSeriesDetailsThemeNames(getState());

    let themeId = themeNames.find(theme => theme.value === values.theme).id;

    let data = new URLSearchParams();
    data.append("themeId", themeId);

    axios.put(`/admin-ng/series/${id}/theme`, data)
        .then(response => {
            let themeResponse = response.data;

            let seriesTheme = (transformToIdValueArray(themeResponse))[0].value;

            dispatch(setSeriesDetailsTheme(seriesTheme));
        })
        .catch(response => {
            logger.error(response);
        });
}



// thunks for statistics

export const fetchSeriesStatistics = seriesId => async (dispatch) => {
    dispatch(fetchStatistics(seriesId, 'series', getStatistics, loadSeriesStatisticsInProgress,
        loadSeriesStatisticsSuccess, loadSeriesStatisticsFailure));
}

export const fetchSeriesStatisticsValueUpdate = (seriesId, providerId, from, to, dataResolution, timeMode) => async (dispatch) => {
    dispatch(fetchStatisticsValueUpdate(seriesId, 'series', providerId, from, to, dataResolution, timeMode,
        getStatistics, updateSeriesStatisticsSuccess, updateSeriesStatisticsFailure));
}
