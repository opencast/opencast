import {
    loadSeriesFailure,
    loadSeriesInProgress,
    loadSeriesMetadataInProgress, loadSeriesMetadataSuccess,
    loadSeriesSuccess, loadSeriesThemesFailure, loadSeriesThemesInProgress, loadSeriesThemesSuccess
} from "../actions/seriesActions";
import {
    getURLParams, prepareAccessPolicyRulesForPost,
    prepareMetadataFieldsForPost,
    transformMetadataCollection
} from "../utils/resourceUtils";
import {transformToObjectArray} from "../utils/utils";
import axios from "axios";
import {addNotification} from "./notificationThunks";


// fetch series from server
export const fetchSeries = () => async (dispatch, getState) => {
   try {
       dispatch(loadSeriesInProgress());

       const state = getState();
       let params = getURLParams(state);

       // /series.json?sortorganizer={sortorganizer}&sort={sort}&filter={filter}&offset=0&limit=100
       let data = await axios.get('admin-ng/series/series.json', { params: params });


       const series = await data.data;
       dispatch(loadSeriesSuccess(series));

   } catch (e) {
       dispatch(loadSeriesFailure());
       console.log(e);
   }
}

// fetch series metadata from server
export const fetchSeriesMetadata = () => async dispatch => {
    try {
       dispatch(loadSeriesMetadataInProgress());

       let data = await axios.get('admin-ng/series/new/metadata');
       const response = await data.data;

        const metadata = transformMetadataCollection(response[0]);

       dispatch(loadSeriesMetadataSuccess(metadata));
    } catch (e) {
        dispatch(loadSeriesFailure());
        console.log(e);
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
        console.log(e);
    }

};

// post new series to backend
export const postNewSeries = async (values, metadataInfo) => {

    let metadataFields, metadata, access;

    metadataFields = prepareMetadataFieldsForPost(metadataInfo.fields, values);

    // metadata for post request
    metadata = {
        flavor: metadataInfo.flavor,
        title: metadataInfo.title,
        fields: metadataFields
    };

    access = prepareAccessPolicyRulesForPost(values.policies);

    let jsonData = {
            metadata: metadata,
            options: {},
            access: access,
            theme: values.theme,
        };

    let data = new URLSearchParams();
    data.append("metadata", JSON.stringify(jsonData));

    // Todo: process bar notification
    axios.post('/admin-ng/series/new', data.toString(),
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        }
    ).then(response => console.log(response)).catch(response => console.log(response));

};

// delete series with provided id
export const deleteSeries = id => async dispatch => {
    // API call for deleting a series
    axios.delete(`/admin-ng/series/${id}`).then(res => {
        console.log(res);
        // add success notification
        dispatch(addNotification('success', 'SERIES_DELETED'));
    }).catch(res => {
        console.log(res);
        // add error notification
        dispatch(addNotification('error', 'SERIES_NOT_DELETED'));
    });
};
