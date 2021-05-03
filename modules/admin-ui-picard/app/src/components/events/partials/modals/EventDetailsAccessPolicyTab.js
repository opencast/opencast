import React, {useState, useEffect} from "react";
import {connect} from "react-redux";
import {
} from "../../../../thunks/eventDetailsThunks";
import {
} from "../../../../selectors/eventDetailsSelectors";
import RenderMultiField from "../../../shared/wizard/RenderMultiField";
import {fetchAclActions, fetchAclTemplates} from "../../../../thunks/aclThunks";
import Notifications from "../../../shared/Notifications";
import {Formik} from "formik";

/**
 * This component manages the access policy tab of the event details modal
 */
const EventDetailsAccessPolicyTab = ({ eventId, header, t

                                      }) => {

    {/* todo: get real values */}
    const transactions = {
        read_only: false
    };

    const baseAclId = "No clue!";
    const roles = [{
        name: "ROLE_USER_ADMIN"
    }];
    const policies = [{
        "episode_access": {
            "privileges": {
                "ROLE_USER_ADMIN": {
                    "read": true,
                    "write": true
                }
            },
            "acl": "{\"acl\":{\"ace\":[{\"allow\":true,\"role\":\"ROLE_USER_ADMIN\",\"action\":\"read\"},{\"allow\":true,\"role\":\"ROLE_USER_ADMIN\",\"action\":\"write\"}]}}",
            "current_acl": 0
        },
        "system_acls": [
            {
                "id": 451,
                "name": "authenticated"
            },
            {
                "id": 452,
                "name": "private"
            },
            {
                "id": 453,
                "name": "public"
            }
        ],
        role: roles[0].name,
        read: false,
        write: true,
        actions: {value: ["None!"]}
    }];

    const hasActions = false;





    const [aclTemplates, setAclTemplates] = useState([]);
    const [aclActions, setAclActions] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect( () => {
        async function fetchData() {
            setLoading(true);
            const responseTemplates = await fetchAclTemplates();
            setAclTemplates(responseTemplates);
            const responseActions = await fetchAclActions();
            setAclActions(responseActions);
            setLoading(false);
        }

        fetchData().then(r => {});
    }, []);

    const exampleFunction = (exampleValue, exampleValue2) => {
        /*saveNewComment(eventId, exampleValue, exampleValue2).then((successful) => {
            if(successful){
                loadComments(eventId);
                setNewCommentText("");
                setCommentReason("");
            }
        });*/
    }


    const deletePolicy = async (e) => {

    };

    const addPolicy = async (e) => {

    };

    const saveAccess = async (e) => {

    };

    const changeAccess = async (e) => {

    };

    const handleTemplateChange = async (e) => {
        // fetch information about chosen template from backend
        /*const template =  await fetchAclTemplateById(e.target.value);

        formik.setFieldValue('policies', template);
        checkPolicies();*/
    };

    // todo: add user and role management
    return (
        <div className="modal-content">
            <div className="modal-body">
                { /*
                <div data-admin-ng-notification="" type="warning" show="access.episode_access.locked"
                     message="{{ metadata.locked }}"></div>
                <div data-admin-ng-notifications="" type="error" context="event-acl"></div>
                <div data-admin-ng-notifications="" context="events-access"></div>*/}

                <div className="full-col">
                    {/* Notifications */}
                    <Notifications context="not_corner"/>
                    {!loading && (
                        <ul>
                            <li>
                                <div className="obj list-obj">
                                    <header>{t(header)/* Access Policy */}</header>
                                    <div className="obj-container">
                                        <div className="obj tbl-list">
                                            <table className="main-tbl">{/*todo: ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')" */}
                                                <thead>
                                                    <tr>
                                                        <th>
                                                            {t("EVENTS.EVENTS.DETAILS.ACCESS.TEMPLATES.TITLE") /* Templates */}
                                                        </th>
                                                    </tr>
                                                </thead>
                                                <tbody>
                                                    <tr>
                                                        <td>
                                                            <div className="obj-container padded chosen-container chosen-container-single">
                                                                <p>
                                                                    {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.DESCRIPTION") /* Description */}
                                                                </p>
                                                                {/*
                                                                <div ng-show="!transactions.read_only">
                                                                    <select chosen
                                                                            pre-select-from="acls"
                                                                            data-width="'200px'"
                                                                            ng-disabled="((tab == 'access') && !$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT'))"
                                                                            ng-change="changeBaseAcl()"
                                                                            ng-model="baseAclId"
                                                                            ng-options="id as name for (id, name) in acls"
                                                                            placeholder-text-single="'{{ '' | translate }}'"
                                                                            no-results-text="'{{ '' | translate }}'"
                                                                    />
                                                                </div>
                                                                <p ng-show="transactions.read_only">{{baseAclId}}</p>
                                                                */}
                                                                {!transactions.read_only ? (
                                                                    <select className="chosen-single chosen-default"
                                                                            chosen
                                                                            style={{width: '200px'}}
                                                                            onChange={e => handleTemplateChange(e)}>
                                                                        {(aclTemplates && aclTemplates.length > 0) ?
                                                                            (
                                                                                <>
                                                                                    <option value="" disabled selected hidden>
                                                                                        {t('EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.LABEL')}
                                                                                    </option>
                                                                                    {
                                                                                        aclTemplates.map((template, key) => (
                                                                                            <option value={template.id}
                                                                                                    key={key}>
                                                                                                {template.value}
                                                                                            </option>
                                                                                        ))
                                                                                    }
                                                                                </>
                                                                            ) : (
                                                                                <option value="" disabled selected hidden>
                                                                                    {t('EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.EMPTY')}
                                                                                </option>
                                                                            )
                                                                        }
                                                                    </select>
                                                                ) : (
                                                                    baseAclId
                                                                )}
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                    <div className="obj-container">
                                        <div className="obj tbl-list">
                                            <header>{t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.DETAILS") /*Details*/}</header>
                                            <div className="obj-container">
                                                <table className="main-tbl">
                                                    <thead>
                                                    <tr>
                                                        <th>
                                                            {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.ROLE") /* <!-- Role --> */}
                                                        </th>
                                                        <th className="fit">
                                                            {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.READ") /* <!-- Read --> */}
                                                        </th>
                                                        <th className="fit">
                                                            {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.WRITE") /* <!-- Write --> */}
                                                        </th>
                                                        { hasActions && (<th className="fit">
                                                            {t("EVENTS.SERIES.DETAILS.ACCESS.ACCESS_POLICY.ADDITIONAL_ACTIONS") /* <!-- Additional Actions --> */}
                                                        </th>) }
                                                        {true /* todo: ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')" */ && (<th className="fit">
                                                            {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.ACTION") /* <!-- Action --> */}
                                                        </th>) }
                                                    </tr>
                                                    </thead>
                                                    <tbody>
                                                    {
                                                        /*
                                                    <tr ng-repeat="policy in policies track by $index">
                                                        <td ng-show="!transactions.read_only">
                                                            <select chosen
                                                                    pre-select-from="roles"
                                                                    ng-disabled="((tab == 'access') && !$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT'))"
                                                                    data-width="'360px'"
                                                                    ng-model="policy.role"
                                                                    ng-change="accessChanged(policy.role)"
                                                                    ng-options="role as role for role in roles"
                                                                    call-on-search="getMatchingRoles"
                                                                    placeholder-text-single="'{{ 'EVENTS.EVENTS.DETAILS.ACCESS.ROLES.LABEL' | translate }}'"
                                                                    no-results-text="'{{ 'EVENTS.EVENTS.DETAILS.ACCESS.ROLES.EMPTY' | translate }}'"
                                                            />
                                                        </td>
                                                        <td ng-show="transactions.read_only">
                                                            <p>{{policy.role}}</p>
                                                        </td>
                                                        <td className="fit text-center"><input type="checkbox"
                                                                                               ng-model="policy.read"
                                                                                               ng-change="accessSave(policy)"
                                                                                               ng-disabled="(transactions.read_only) || !$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')"/>
                                                        </td>
                                                        <td className="fit text-center"><input type="checkbox"
                                                                                               ng-model="policy.write"
                                                                                               ng-change="accessSave(policy)"
                                                                                               ng-disabled="(transactions.read_only) || !$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')"/>
                                                        </td>
                                                        <td className="fit editable" ng-if="hasActions">
                                                            <div
                                                                ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT') && !transactions.read_only"
                                                                save="accessSave" admin-ng-editable-multi-select
                                                                mixed="false" params="policy.actions"
                                                                collection="actions"></div>
                                                            <div
                                                                ng-if="((!$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')) || transactions.read_only)"
                                                                ng-repeat="customAction in policy.actions.value">{{customAction}}</div>
                                                        </td>
                                                        <td ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')"
                                                            className="fit"><a ng-show="!transactions.read_only"
                                                                               ng-click="deletePolicy(policy)"
                                                                               className="remove"></a>
                                                        </td>
                                                    </tr>
                                                    */
                                                        policies.map( (policy, key) => (
                                                            <tr>
                                                                <td>
                                                                    {!transactions.read_only ? (
                                                                        <select key={key}
                                                                                className="chosen-single chosen-default"
                                                                                chosen
                                                                                style={{width: '360px'}}
                                                                                onChange={role => changeAccess(role)}>
                                                                            {/*pre-select-from="roles"
                                                                                ng-disabled="((tab == 'access') && !$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT'))"
                                                                                call-on-search="getMatchingRoles"*/}
                                                                            {(roles && roles.length > 0) ?
                                                                                (
                                                                                    <>
                                                                                        <option value="" disabled selected
                                                                                                hidden>
                                                                                            {t('EVENTS.EVENTS.DETAILS.ACCESS.ROLES.LABEL')}
                                                                                        </option>
                                                                                        {
                                                                                            roles.map((role, roleKey) => (
                                                                                                <option value={role.name}
                                                                                                        key={roleKey}>
                                                                                                    {role.name}
                                                                                                </option>
                                                                                            ))
                                                                                        }
                                                                                    </>
                                                                                ) : (
                                                                                    <option value="" disabled selected hidden>
                                                                                        {t('EVENTS.EVENTS.DETAILS.ACCESS.ROLES.EMPTY')}
                                                                                    </option>
                                                                                )
                                                                            }
                                                                        </select>
                                                                    ) : (
                                                                        <p>{policy.role}</p>
                                                                    )
                                                                    }
                                                                </td>

                                                                 <td className="fit text-center">
                                                                        <input type="checkbox"
                                                                               disabled={ transactions.read_only }
                                                                               className={`${transactions.read_only ? 
                                                                                   "disabled" : "false"}`}
                                                                               defaultChecked={policy.read}
                                                                               onChange={ (policy) => saveAccess(policy)}

                                                                        /> {/*ng-disabled="!$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')"*/}
                                                                </td>
                                                                <td className="fit text-center">
                                                                        <input type="checkbox"
                                                                               disabled={ transactions.read_only }
                                                                               className={`${transactions.read_only ? 
                                                                                   "disabled" : "false"}`}
                                                                               defaultChecked={policy.write}
                                                                               onChange={ (policy) => saveAccess(policy)}

                                                                        /> {/*ng-disabled="!$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')"*/}
                                                                </td>
                                                                { hasActions && (
                                                                    <td className="fit editable">
                                                                        <Formik
                                                                            initialValues={{policy: {actions: ''}}}
                                                                            onSubmit={(values, actions) => {
                                                                                /*alert(JSON.stringify(values, null, 2));
                                                                                actions.setSubmitting(false);*/
                                                                                saveAccess(values).then(r => {})
                                                                            }}
                                                                        >
                                                                            { !transactions.read_only /*ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')*/ && (
                                                                                <div>
                                                                                    <RenderMultiField fieldInformation={
                                                                                        {
                                                                                            id: `policy.actions`,
                                                                                            type: 'mixed_text',
                                                                                            collection: aclActions
                                                                                        }
                                                                                    } onlyCollectionValues/>
                                                                                </div>
                                                                            )}
                                                                        </Formik>
                                                                        {transactions.read_only /*ng-if="((!$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT'))"*/ && (
                                                                            policy.actions.value.map((customAction, actionKey) => (
                                                                                <div key={actionKey}>
                                                                                    {customAction}
                                                                                </div>
                                                                            ))
                                                                        )}
                                                                    </td>
                                                                )}
                                                                { true /*ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')"*/ && (
                                                                    <td className="fit">
                                                                        {transactions.read_only || (
                                                                            <a onClick={(policy) => deletePolicy()}
                                                                               className="remove"/>
                                                                            )}
                                                                    </td>
                                                                )}
                                                            </tr>
                                                            )
                                                        )
                                                    }
                                                    { !transactions.read_only /* ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')" */ && (
                                                        <tr>
                                                            <td colSpan="5">

                                                                <a onClick={() => addPolicy()}>
                                                                    + {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.NEW")}
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    )}
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </li>
                        </ul>
                    )}
                </div>
                <div className="full-col">
                </div>
            </div>
        </div>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsAccessPolicyTab);