import {loadAclsFailure, loadAclsInProgress, loadAclsSuccess} from "../actions/aclActions";
import {getURLParams} from "../utils/resourceUtils";
import axios from "axios";

// fetch acls from server
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
};


// todo: unite following in one fetch method (maybe also move to own file containing all fetches regarding resources endpoint)
export const fetchAclTemplates = async () => {
    let data = await fetch('/admin-ng/resources/ACL.json');

    const response = await data.json();

    return Object.keys(response).map(key => {
        return {
            id: key,
            label: response[key]
        }
    });
};

export const fetchAclActions = async () => {
    let data = await fetch('/admin-ng/resources/ACL.ACTIONS.json');

    const response = await data.json();

    return Object.keys(response).map(key => {
        return {
            id: key,
            value: response[key]
        }
    });

};

export const fetchRoles = async () => {
    let params = new URLSearchParams({
        filters: 'role_target:ACL',
        limit: '100',
        offset: '0'
    })

    let data = await fetch ('/admin-ng/resources/ROLES.json?' + params);

    const response = await data.json();

    return Object.keys(response).map(key => {
        return {
            id: key,
            label: response[key]
        }
    });
};

export const fetchAclTemplateById = async (id) => {

    let template = [];

    let response = await axios.get(`/acl-manager/acl/${id}`);

    let acl = response.data.acl;
    for (let i = 0; acl.ace.length > i; i++) {
        if (template.find(rule => rule.role === acl.ace[i].role)) {
            for (let j = 0; template.length > j; j++) {
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
