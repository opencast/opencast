import {
    loadSeriesDetailsFailure,
    loadSeriesDetailsInProgress,
    loadSeriesDetailsSuccess
} from "../actions/seriesDetailsActions";
import axios from "axios";
import {transformMetadataCollection} from "../utils/resourceUtils";

// fetch details of certain series from server
export const fetchSeriesDetails = id => async dispatch => {
    try {
        dispatch(loadSeriesDetailsInProgress());

        // fetch metadata
        let data = await axios.get(`admin-ng/series/${id}/metadata.json`);

        const metadataResponse = await data.data;

        const metadata = transformMetadataCollection(metadataResponse[0]);


        // fetch acl
        data = await axios.get(`admin-ng/series/${id}/access.json`);

        const aclResponse = await data.data;

        // fetch feeds
        // todo: implement no data case
        data = await axios.get('admin-ng/feeds/feeds');

        const feedsResponse = await data.data;

        let seriesDetails = {
            metadata: metadata,
            acl: aclResponse,
            feeds: feedsResponse
        }

        dispatch(loadSeriesDetailsSuccess(seriesDetails));

    } catch (e) {
        dispatch(loadSeriesDetailsFailure());
    }
}
