import {loadAclsFailure, loadAclsInProgress, loadAclsSuccess} from "../actions/aclActions";
import {getURLParams, prepareAccessPolicyRulesForPost} from "../utils/resourceUtils";
import axios from "axios";
import {transformToIdValueArray} from "../utils/utils";

// fetch acls from server
export const fetchAcls = () => async (dispatch, getState) => {
    try {
        dispatch(loadAclsInProgress());

        const state = getState();
        let params = getURLParams(state);

        // /acls.json?limit=0&offset=0&filter={filter}&sort={sort}
        let data = await axios.get('admin-ng/acl/acls.json', {params: params});

        const acls = await data.data;
        dispatch(loadAclsSuccess(acls));

    } catch (e) {
        dispatch(loadAclsFailure());
    }
};


// todo: unite following in one fetch method (maybe also move to own file containing all fetches regarding resources endpoint)
// get acl templates
export const fetchAclTemplates = async () => {
    let data = await axios.get('/admin-ng/resources/ACL.json');

    const response = await data.data;

    return transformToIdValueArray(response);
};

// fetch additional actions that a policy allows user to perform on an event
export const fetchAclActions = async () => {
    let data = await axios.get('/admin-ng/resources/ACL.ACTIONS.json');

    const response = await data.data;

    return transformToIdValueArray(response);

};

// fetch all possible roles a policy can have
export const fetchRoles = async () => {
    let params = {
        filters: 'role_target:ACL',
        limit: '100',
        offset: '0'
    };

    let data = await axios.get('/admin-ng/resources/ROLES.json', {params: params});

    const response = await data.data;

    return transformToIdValueArray(response);
};

// fetch all policies of an certain acl template
export const fetchAclTemplateById = async (id) => {

    let template = [];

    let response = await axios.get(`/acl-manager/acl/${id}`);

    let acl = response.data.acl;

    // transform response data in form that is used in new event wizard for policies (for each role one entry)
    for (let i = 0; acl.ace.length > i; i++) {
        if (template.find(rule => rule.role === acl.ace[i].role)) {
            for (let j = 0; template.length > j; j++) {
                // Only update entry for policy if already added with other action
                if(template[j].role === acl.ace[i].role) {
                    if (acl.ace[i].action === "read") {
                        template[j] = {
                            ...template[j],
                            read: acl.ace[i].allow
                        }
                        break;
                    }
                    if (acl.ace[i].action === "write") {
                        template[j] = {
                            ...template[j],
                            write: acl.ace[i].allow
                        }
                        break;
                    }
                    if (acl.ace[i].action !== "read" && acl.ace[i].action !== "write"
                        && acl.ace[i].allow === true) {
                        template[j] = {
                            ...template[j],
                            actions: template[j].actions.concat(acl.ace[i].action)
                        }
                        break;
                    }
                }
            }
        } else {
            // add policy if role not seen before
            if (acl.ace[i].action === "read") {
                template = template.concat({
                    role: acl.ace[i].role,
                    read: acl.ace[i].allow,
                    write: false,
                    actions: []
                });
            }
            if (acl.ace[i].action === "write") {
                template = template.concat({
                    role: acl.ace[i].role,
                    read: false,
                    write: acl.ace[i].allow,
                    actions: []
                });
            }
            if (acl.ace[i].action !== "read" && acl.ace[i].action !== "write"
                && acl.ace[i].allow === true) {
                template = template.concat({
                    role: acl.ace[i].role,
                    read: false,
                    write: false,
                    actions: [acl.ace[i].action]
                })
            }
        }
    }
    return template;

};

// fetch roles for select dialog in new group and new users wizard
export const fetchRolesTargetingUsers = async () => {

    let params = {
        limit: -1,
        target: 'USER'
    };

    let data = await axios.get('/admin-ng/acl/roles.json', {params: params});

    return await data.data;

};


// post new acl to backend
export const postNewAcl = values => {

    let acls = prepareAccessPolicyRulesForPost(values.acls);

    let data = new FormData();
    data.append('name', values.name);
    data.append('acl', JSON.stringify(acls));

    // todo: notification if error occurs
    axios.post('/admin-ng/acl', data,
        {
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        }
    ).then(response => console.log(response)).catch(response => console.log(response));

};
