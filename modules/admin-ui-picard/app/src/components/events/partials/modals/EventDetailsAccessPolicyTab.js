import React, {useState, useEffect} from "react";
import {connect} from "react-redux";
import {
} from "../../../../thunks/eventDetailsThunks";
import {
} from "../../../../selectors/eventDetailsSelectors";

/**
 * This component manages the comment tab of the event details modal
 */
const EventDetailsAccessPolicyTab = ({ eventId, header, t

                                      }) => {

    useEffect( () => {
    }, []);

    const [ replyToComment, setReplyToComment ] = useState(false);

    const saveComment = (commentText, commentReason) => {
        /*saveNewComment(eventId, commentText, commentReason).then((successful) => {
            if(successful){
                loadComments(eventId);
                setNewCommentText("");
                setCommentReason("");
            }
        });*/
    }

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
                    <ul>
                        <li>
                            <div className="obj list-obj">
                                <header>{t(header)/* Access Policy */}</header>
                                <div className="obj-container">
                                    <div className="obj tbl-list">
                                        <table className="main-tbl"
                                               >{/*todo: ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')" */}
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
                                                    <div className="obj-container padded">
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
                                                                    placeholder-text-single="'{{ 'EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.LABEL' | translate }}'"
                                                                    no-results-text="'{{ 'EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.EMPTY' | translate }}'"
                                                            />
                                                        </div>
                                                        <p ng-show="transactions.read_only">{{baseAclId}}</p>
                                                        */}
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
                                                    <th className="fit"> {/* todo: ng-if="hasActions" */}
                                                        {t("EVENTS.SERIES.DETAILS.ACCESS.ACCESS_POLICY.ADDITIONAL_ACTIONS") /* <!-- Additional Actions --> */}
                                                    </th>
                                                    <th className="fit"> {/* todo: ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')" */}
                                                        {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.ACTION") /* <!-- Action --> */}
                                                    </th>
                                                </tr>
                                                </thead>
                                                <tbody>
                                                {/*
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
                                                */}
                                                <tr>{/* ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')" */}
                                                    <td colSpan="5">
                                                        <a>{/* ng-show="!transactions.read_only"
                                                                       ng-click="addPolicy()" */}
                                                            + {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.NEW")}
                                                        </a>
                                                    </td>
                                                </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </li>
                    </ul>
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