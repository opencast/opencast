import React, {useState, useEffect} from "react";
import {connect} from "react-redux";
import {
    saveAccessPolicies
} from "../../../../thunks/eventDetailsThunks";
import {
} from "../../../../selectors/eventDetailsSelectors";
import RenderMultiField from "../../../shared/wizard/RenderMultiField";
import {
    fetchAclActions,
    fetchAclTemplateById,
    fetchAclTemplates,
    fetchRolesWithTarget
} from "../../../../thunks/aclThunks";
import {
    fetchAccessPolicies,
    fetchHasActiveTransactions
} from "../../../../thunks/eventDetailsThunks";
import Notifications from "../../../shared/Notifications";
import {Formik, Field, FieldArray} from "formik";
import {addNotification} from "../../../../thunks/notificationThunks";
import {NOTIFICATION_CONTEXT} from "../../../../configs/wizardConfig";

/**
 * This component manages the access policy tab of the event details modal
 */
const EventDetailsAccessPolicyTab = ({ eventId, header, t,

                                      addNotification, fetchAclTemplates, fetchHasActiveTransactions, fetchAccessPolicies, fetchRoles, saveNewAccessPolicies}) => {

    const baseAclId = "";

    const [aclTemplates, setAclTemplates] = useState([]);
    const [aclActions, setAclActions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [hasActions, setHasActions] = useState(false);
    const [roles, setRoles] = useState(false);
    const [policyChanged, setPolicyChanged] = useState(false);
    const [policies, setPolicies] = useState([]);
    const [initialPolicies, setInitialPolicies] = useState([]);
    const [transactions, setTransactions] = useState({read_only: true});

    const createPolicy = (role) => {
        return {
            role: role,
            read: false,
            write: false,
            actions: []
        };
    };

    useEffect( () => {

        async function fetchData() {
            setLoading(true);
            const responseTemplates = await fetchAclTemplates();
            setAclTemplates(responseTemplates);
            const responseActions = await fetchAclActions();
            setAclActions(responseActions);
            setHasActions(responseActions.length > 0);
            fetchAccessPolicies(eventId).then(accessPolicies => {
                if(!!accessPolicies.episode_access){
                    const json = JSON.parse(accessPolicies.episode_access.acl).acl.ace;
                    let newPolicies = {};
                    let policyRoles = [];
                    for(let i = 0; i < json.length; i++){
                        const policy = json[i];
                        if(!newPolicies[policy.role]){
                            newPolicies[policy.role] = createPolicy(policy.role);
                            policyRoles.push(policy.role);
                        }
                        if (policy.action === 'read' || policy.action === 'write') {
                            newPolicies[policy.role][policy.action] = policy.allow;
                        } else if (policy.allow === true || policy.allow === 'true'){
                            newPolicies[policy.role].actions.push(policy.action);
                        }
                    }
                    setPolicies(policyRoles.map(role => newPolicies[role]));
                    setInitialPolicies(policyRoles.map(role => newPolicies[role]));
                }
            });
            fetchRoles().then(roles => setRoles(roles));
            const fetchTransactionResult = await fetchHasActiveTransactions(eventId);
            fetchTransactionResult.active !== undefined?
                setTransactions({read_only: fetchTransactionResult.active})
                : setTransactions({read_only: true});
            setLoading(false);
        }

        fetchData().then(r => {});
    }, []);

    const resetPolicies = (resetFormik) => {
        setPolicyChanged(false);
        resetFormik();
    }

    const saveAccess = (values, resetFormik) => {
        // TODO: remove old notifications

        let ace = [];
        let roleWithFullRightsExists = false;
        let allRulesValid = true;

        values.policies.map(policy => {
            if (policy.read && policy.write) {
                roleWithFullRightsExists = true;
            }

            if ((policy.read || policy.write || policy.actions.length > 0) && !!policy.role) {
                if (policy.read) {
                    ace.push({
                        'action' : 'read',
                        'allow'  : policy.read,
                        'role'   : policy.role
                    });
                }

                if (policy.write) {
                    ace.push({
                        'action' : 'write',
                        'allow'  : policy.write,
                        'role'   : policy.role
                    });
                }

                policy.actions.map( customAction => {
                    ace.push({
                        'action' : customAction,
                        'allow'  : true,
                        'role'   : policy.role
                    });
                });
            } else {
                allRulesValid = false;
            }
        })

        if(!allRulesValid){
            addNotification('warning','INVALID_ACL_RULES', -1, null, NOTIFICATION_CONTEXT);
        }

        if(!roleWithFullRightsExists){
            addNotification('warning','MISSING_ACL_RULES', -1, null, NOTIFICATION_CONTEXT);
        }

        if(allRulesValid && roleWithFullRightsExists){
            //TODO: save, if successful, reload
            saveNewAccessPolicies(eventId, ace);
        }
    }

    const isPolicyChanged = (newPolicies) => {
        const sortSchema = (pol1, pol2) => {return (pol1.role > pol2.role)? 1 : -1};
        const sortedNewPolicies = [...newPolicies].sort(sortSchema);
        const sortedInitialPolicies = [...initialPolicies].sort(sortSchema);
        if (newPolicies.length !== initialPolicies.length) {
            return true;
        }
        for (let i = 0; i < sortedNewPolicies.length; i++){
            console.log(sortedNewPolicies[i].role);
            console.log(sortedNewPolicies[i].actions);
            console.log(sortedInitialPolicies[i].actions);
            console.log();
            if (sortedNewPolicies[i].role !== sortedInitialPolicies[i].role ||
                sortedNewPolicies[i].read !== sortedInitialPolicies[i].read ||
                sortedNewPolicies[i].write !== sortedInitialPolicies[i].write ||
                sortedNewPolicies[i].actions.name !== sortedInitialPolicies[i].actions.name ||
                sortedNewPolicies[i].actions.length !== sortedInitialPolicies[i].actions.length){
                return true;
            }
            if(sortedNewPolicies[i].actions.length > 0 && sortedNewPolicies[i].actions.length === sortedInitialPolicies[i].actions.length){
                for(let j = 0; j < sortedNewPolicies[i].actions.length; j++){
                    if(sortedNewPolicies[i].actions[j] !== sortedInitialPolicies[i].actions[j]){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    const deletePolicy = async (policy, remove, index, policies) => {
        const newPolicies = policies.filter(pol => pol !== policy);
        remove(index);
        setPolicyChanged(isPolicyChanged(newPolicies));
    };

    const addPolicy = async (push, policies) => {
        const newPolicy = createPolicy("");
        const newPolicies = policies.concat(newPolicy);
        push(newPolicy);
        setPolicyChanged(isPolicyChanged(newPolicies));
    };

    const changeReadAccess = async (policy, read, replace, index, policies) => {
        const newPolicies = policies.map(pol => (pol === policy)?
            {
                ...pol,
                read: read
            } : pol);
        replace(index, {...policy, read: read})
        setPolicyChanged(isPolicyChanged(newPolicies));
    };

    const changeWriteAccess = async (policy, write, replace, index, policies) => {
        const newPolicies = policies.map(pol => (pol === policy)?
            {
                ...pol,
                write: write
            } : pol);
        replace(index, {...policy, write: write})
        setPolicyChanged(isPolicyChanged(newPolicies));
    };

    const changeAccessAction = async (e) => {
        setPolicyChanged(isPolicyChanged());
    };
    const changeAccessRole = async (policy, role, replace, index, policies) => {
        const newPolicies = policies.map(pol => (pol === policy)?
            {
                ...pol,
                role: role
            } : pol);
        replace(index, {...policy, role:role});
        setPolicyChanged(isPolicyChanged(newPolicies));
    };

    const handleTemplateChange = async (templateId, setFormikFieldValue) => {
        // fetch information about chosen template from backend
        const template =  await fetchAclTemplateById(templateId);

        console.log(templateId);
        console.log(template);

        setFormikFieldValue('policies', template);
        setPolicyChanged(isPolicyChanged(template));
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
                                <Formik
                                    initialValues={{policies: [...policies]}}
                                    onSubmit={(values, actions) => {
                                        /*alert(JSON.stringify(values, null, 2));
                                        actions.setSubmitting(false);*/
                                        saveAccess(values).then(r => {})
                                    }}
                                >
                                    {formik => (
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
                                                                {!transactions.read_only ? (
                                                                    <select className="chosen-single chosen-default"
                                                                            chosen
                                                                            style={{width: '200px'}}
                                                                            onChange={event => handleTemplateChange(event.target.value, formik.setFieldValue)}>
                                                                        {(aclTemplates && aclTemplates.length > 0) ?
                                                                            (
                                                                                <>
                                                                                    <option value="" selected hidden disabled>
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
                                                    <FieldArray name="policies">
                                                                {({ replace, remove, push }) => (
                                                                    <>
                                                                        { (formik.values.policies.length > 0) &&
                                                                                    formik.values.policies.map((policy, index) => (
                                                                                        <tr key={index}>

                                                                                            <td>
                                                                    {!transactions.read_only ? (
                                                                                                <Field className="chosen-single chosen-default"
                                                                                                       style={{width: '360px'}}
                                                                                                       name={`policies.${index}.role`}
                                                                                                       as="select"
                                                                                                       onChange={role => changeAccessRole(policy, role.target.value, replace, index, formik.values.policies)}>
                                                                                                    {(roles && roles.length > 0) && (
                                                                                                        <>
                                                                                                            {!policy.role &&
                                                                                                                <option value="" defaultValue hidden>
                                                                                                                    {t('EVENTS.EVENTS.DETAILS.ACCESS.ROLES.LABEL')}
                                                                                                                </option>
                                                                                                            }
                                                                                                            {!!policy.role &&
                                                                                                                <option value={policy.role} defaultValue>
                                                                                                                    {policy.role}
                                                                                                                </option>
                                                                                                            }
                                                                                                            {roles.filter(role => !formik.values.policies.find(policy => policy.role === role.name)).map((role, key) => (
                                                                                                                <option value={role.name}
                                                                                                                        key={key}>{role.name}</option>
                                                                                                            ))}
                                                                                                        </>
                                                                                                    )}
                                                                                                    {(roles && roles.length > 0) || (
                                                                                        <option value="" defaultValue hidden>
                                                                                            {t('EVENTS.EVENTS.DETAILS.ACCESS.ROLES.EMPTY')}
                                                                                        </option>
                                                                                                    )}
                                                                                                </Field>
                                                            ) : (
                                                                        <p>{policy.role}</p>
                                                                    )}
                                                                                            </td>
                                                                                            {/* Checkboxes for  policy.read and policy.write*/}
                                                                                            <td className="fit text-center">
                                                                                                <Field type="checkbox"
                                                                                                       name={`policies.${index}.read`}
                                                                                                       disabled={ transactions.read_only }
                                                                                                       className={`${transactions.read_only ? 
                                                                                                           "disabled" : "false"}`}
                                                                                                       onChange={ (read) => changeReadAccess(policy, read.target.checked, replace, index, formik.values.policies)}
                                                                                                /> {/*Todo:  ng-disabled="!$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')"*/}
                                                                                            </td>
                                                                                            <td className="fit text-center">
                                                                                                <Field type="checkbox"
                                                                                                       name={`policies.${index}.write`}
                                                                                                       disabled={ transactions.read_only }
                                                                                                       className={`${transactions.read_only ?
                                                                                                           "disabled" : "false"}`}
                                                                                                       onChange={ (write) => changeWriteAccess(policy, write.target.checked, replace, index, formik.values.policies)}

                                                                                                /> {/*Todo:  ng-disabled="!$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')"*/}
                                                                                            </td>
                                                                                            {hasActions && (
                                                                                                <td className="fit editable">
                                                                                                    { !transactions.read_only /*Todo:  && ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')*/ && (
                                                                                                        <div>
                                                                                                            <RenderMultiField
                                                                                                                fieldInformation={
                                                                                                                    {
                                                                                                                        id: `policies.${index}.actions`,
                                                                                                                        type: 'mixed_text',
                                                                                                                        collection: aclActions
                                                                                                                    }
                                                                                                                }
                                                                                                                onlyCollectionValues
                                                                                                            />
                                                                                                        </div>
                                                                                                    )}
                                                                                                    {transactions.read_only /*Todo:  || ng-if="((!$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT'))"*/ && (
                                                                                                        policy.actions.value.map((customAction, actionKey) => (
                                                                                                            <div key={actionKey}>
                                                                                                                {customAction}
                                                                                                            </div>
                                                                                                        ))
                                                                                                    )}
                                                                                                </td>
                                                                                            )}
                                                                                            { true /*Todo: ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')"*/ && (
                                                                                                /*Remove policy*/
                                                                                                <td>
                                                                                                    <a onClick={() => deletePolicy(policy, remove, index, formik.values.policies)}
                                                                                                       className="remove" />
                                                                                                </td>
                                                                                            )}
                                                                                        </tr>
                                                                                    ))}
                                                    { !transactions.read_only /*Todo:   && ng-if="$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')" */ && (
                                                        <tr>
                                                            <td colSpan="5">

                                                                <a onClick={() => addPolicy(push, formik.values.policies)}>
                                                                    + {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.NEW")}
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    )}

                                                                    </>
                                                                )}
                                                            </FieldArray>
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>
                                    </div>

                                                        { (!transactions.read_only && policyChanged) && (
                                                            <footer style={{padding: '15px'}}>
                                                        <div className="pull-left">
                                                        <button type="reset" onClick={() => {
                                                            resetPolicies(formik.resetForm);
                                                            //resetPolicies(formik.values.policies, formik.);
                                                        }} className="cancel">{t('CANCEL')/* Cancel */}</button>
                                                        </div>
                                                        <div className="pull-right">
                                                        <button onClick={() => {
                                                            saveAccess(formik.values);
                                                        }} className="save green">{t('SAVE')/* Save */}</button>
                                                        </div>
                                                            </footer>) }
                                </div>
                                )}
                            </Formik>
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
    addNotification: (type, key, duration, parameter, context) => dispatch(addNotification(type, key, duration, parameter, context)),
    fetchAccessPolicies: (eventId) => dispatch(fetchAccessPolicies(eventId)),
    fetchHasActiveTransactions: (eventId) => dispatch(fetchHasActiveTransactions(eventId)),
    fetchRoles: () => fetchRolesWithTarget("ACL"),
    fetchAclTemplates: () => fetchAclTemplates(),
    saveNewAccessPolicies: (eventId, policies) => dispatch(saveAccessPolicies(eventId, policies)),
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsAccessPolicyTab);