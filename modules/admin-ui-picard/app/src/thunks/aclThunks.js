import {loadAclsFailure, loadAclsInProgress, loadAclsSuccess} from "../actions/aclActions";
import {getURLParams} from "../utils/resourceUtils";

export const fetchAcls = () => async (dispatch, getState) => {
    try {
        dispatch(loadAclsInProgress());

        const state = getState();
        let params = getURLParams(state);

        // /acls.json?limit=0&offset=0&filter={filter}&sort={sort}
        let data = await fetch('admin-ng/acl/acls.json?' + params);

        const acls = await data.json();
        dispatch(loadAclsSuccess(acls));

    } catch (e) {
        dispatch(loadAclsFailure());
    }
}
