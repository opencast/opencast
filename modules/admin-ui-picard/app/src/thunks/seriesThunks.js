import axios from "axios";
import {
    loadSeriesFailure,
    loadSeriesInProgress,
    loadSeriesMetadataInProgress,
    loadSeriesMetadataSuccess,
    loadSeriesSuccess,
    loadSeriesThemesFailure,
    loadSeriesThemesInProgress,
    loadSeriesThemesSuccess
} from "../actions/seriesActions";
import {
    getURLParams,
    prepareAccessPolicyRulesForPost,
    prepareSeriesExtendedMetadataFieldsForPost,
    prepareSeriesMetadataFieldsForPost,
    transformMetadataCollection
} from "../utils/resourceUtils";
import {transformToIdValueArray, transformToObjectArray} from "../utils/utils";
import {addNotification} from "./notificationThunks";
import {logger} from "../utils/logger";


// fetch series from server
export const fetchSeries = () => async (dispatch, getState) => {
   try {
       dispatch(loadSeriesInProgress());

       const state = getState();
       let params = getURLParams(state);

       // /series.json?sortorganizer={sortorganizer}&sort={sort}&filter={filter}&offset=0&limit=100
       let data = await axios.get('/admin-ng/series/series.json', { params: params });


       const series = await data.data;
       dispatch(loadSeriesSuccess(series));

   } catch (e) {
       dispatch(loadSeriesFailure());
       logger.error(e);
   }
}

// fetch series metadata from server
export const fetchSeriesMetadata = () => async dispatch => {
    try {
       dispatch(loadSeriesMetadataInProgress());

       let data = await axios.get('/admin-ng/series/new/metadata');
       const response = await data.data;

        const mainCatalog = 'dublincore/series';
        let usualMetadata = {};
        const extendedMetadata = [];

        for(const metadataCatalog of response){
            if(metadataCatalog.flavor === mainCatalog){
                usualMetadata = transformMetadataCollection({...metadataCatalog});
            } else {
                extendedMetadata.push(transformMetadataCollection({...metadataCatalog}));
            }
        }

       dispatch(loadSeriesMetadataSuccess(usualMetadata, extendedMetadata));
    } catch (e) {
        dispatch(loadSeriesFailure());
        logger.error(e);
    }
}

// fetch series themes from server
export const fetchSeriesThemes = () => async dispatch => {
    try {
        dispatch(loadSeriesThemesInProgress());

        let data = await axios.get('/admin-ng/series/new/themes');

        const response = await data.data;

        const themes = transformToObjectArray(response);

        dispatch(loadSeriesThemesSuccess(themes));
    } catch (e) {
        dispatch(loadSeriesThemesFailure());
        logger.error(e);
    }

};

// post new series to backend
export const postNewSeries = (values, metadataInfo, extendedMetadata) => async dispatch => {

    let metadataFields, extendedMetadataFields, metadata, access;

    // prepare metadata provided by user
    metadataFields = prepareSeriesMetadataFieldsForPost(metadataInfo.fields, values);
    extendedMetadataFields = prepareSeriesExtendedMetadataFieldsForPost(extendedMetadata, values);

    // metadata for post request
    metadata = [{
        flavor: metadataInfo.flavor,
        title: metadataInfo.title,
        fields: metadataFields
    }];

    for(const entry of extendedMetadataFields){
        metadata.push(entry);
    }

    access = prepareAccessPolicyRulesForPost(values.policies);

    let jsonData = {
            metadata: metadata,
            options: {},
            access: access
        };

    if (values.theme !== '') {
        jsonData = {
            ...jsonData,
            theme: parseInt(values.theme)
        };
    }

    let data = new URLSearchParams();
    data.append("metadata", JSON.stringify(jsonData));

    // Todo: process bar notification
    axios.post('/admin-ng/series/new', data.toString(),
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        }
    ).then(response => {
        logger.info(response);
        dispatch(addNotification('success', 'SERIES_ADDED'));
    }).catch(response => {
        logger.error(response);
        dispatch(addNotification('error', 'SERIES_NOT_SAVED'));
    });

};

// delete series with provided id
export const deleteSeries = id => async dispatch => {
    // API call for deleting a series
    axios.delete(`/admin-ng/series/${id}`).then(res => {
        logger.info(res);
        // add success notification
        dispatch(addNotification('success', 'SERIES_DELETED'));
    }).catch(res => {
        logger.error(res);
        // add error notification
        dispatch(addNotification('error', 'SERIES_NOT_DELETED'));
    });
};

// delete series with provided ids
export const deleteMultipleSeries = series => async dispatch => {

    let data = [];

    for (let i = 0; i < series.length; i++) {
        if (series[i].selected) {
            data.push(series[i].id)
        }
    }

    axios.post('/admin-ng/series/deleteSeries', data).then(res => {
        logger.info(res);
        //add success notification
        dispatch(addNotification('success', 'SERIES_DELETED'));
    }).catch(res => {
        logger.error(res);
        //add error notification
        dispatch(addNotification('error', 'SERIES_NOT_DELETED'));
    });
};

// Get names and ids of selectable series
export const fetchSeriesOptions = async () => {
    let data = await axios.get('/admin-ng/resources/SERIES.json');

    const response = await data.data;

    return transformToIdValueArray(response);
};

// Check if a series has events
export const hasEvents = async seriesId => {
    let data = await axios.get(`/admin-ng/series/${seriesId}/hasEvents.json`);

    return (await data.data).hasEvents;
};

// Get series configuration and flag indicating if series with events is allowed to delete
export const getSeriesConfig = async () => {
    let data = await axios.get('/admin-ng/series/configuration.json');

    const response = await data.data;

    return !!response.deleteSeriesWithEventsAllowed;
};
