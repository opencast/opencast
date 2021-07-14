import React, {useState, useEffect} from "react";
import {connect} from "react-redux";
import RenderMultiField from "../wizard/RenderMultiField";
import {
    fetchAclActions,
    fetchAclTemplateById,
    fetchAclTemplates,
    fetchRolesWithTarget
} from "../../../thunks/aclThunks";
import Notifications from "../Notifications";
import {Formik, Field, FieldArray} from "formik";
import {addNotification} from "../../../thunks/notificationThunks";
import {NOTIFICATION_CONTEXT} from "../../../configs/wizardConfig";

/**
 * This component manages the access policy tab of resource details modals
 */
const ResourceDetailsAccessPolicyTab = ({ resourceId, header, t, policies, fetchHasActiveTransactions, fetchAccessPolicies, saveNewAccessPolicies,
                                          addNotification, fetchAclTemplates, fetchRoles}) => {

    const baseAclId = "";

    // list of policy templates
    const [aclTemplates, setAclTemplates] = useState([]);

    // list of possible additional actions
    const [aclActions, setAclActions] = useState([]);

    // shows, whether a resource has additional actions on top of normal read and write rights
    const [hasActions, setHasActions] = useState(false);

    // list of possible roles
    const [roles, setRoles] = useState(false);

    // tracks, whether the policies are different to the initial value
    const [policyChanged, setPolicyChanged] = useState(false);

    // this state is used, because the policies should be read-only, if a transaction is currently being performed on a resource
    const [transactions, setTransactions] = useState({read_only: false});

    // this state tracks, whether data is currently being fetched
    const [loading, setLoading] = useState(false);

    /* creates a new policy with the role from the argument and no rights or actions*/
    const createPolicy = (role) => {
        return {
            role: role,
            read: false,
            write: false,
            actions: []
        };
    };

    /* fetch initial values from backend */
    useEffect( () => {
        async function fetchData() {
            setLoading(true);
            const responseTemplates = await fetchAclTemplates();
            setAclTemplates(responseTemplates);
            const responseActions = await fetchAclActions();
            setAclActions(responseActions);
            setHasActions(responseActions.length > 0);
            await fetchAccessPolicies(resourceId);
            fetchRoles().then(roles => setRoles(roles));
            if(fetchHasActiveTransactions) {
                const fetchTransactionResult = await fetchHasActiveTransactions(resourceId);
                fetchTransactionResult.active !== undefined ?
                    setTransactions({read_only: fetchTransactionResult.active})
                    : setTransactions({read_only: true});
                if (fetchTransactionResult.active == undefined || fetchTransactionResult.active) {
                    addNotification('warning', 'ACTIVE_TRANSACTION', 5, null, NOTIFICATION_CONTEXT);
                }
            }
            setLoading(false);
        }

        fetchData().then(r => {});
    }, []);

    /* resets the formik form and hides the save and cancel buttons */
    const resetPolicies = (resetFormik) => {
        setPolicyChanged(false);
        resetFormik();
    }

    /* transforms rules into proper format for saving and checks validity
    * if the policies are valid, the new policies are saved in the backend */
    const saveAccess = (values) => {
        const {ace, roleWithFullRightsExists, allRulesValid} = validatePolicies(values);

        if(!allRulesValid){
            addNotification('warning','INVALID_ACL_RULES', 3, null, NOTIFICATION_CONTEXT);
        }

        if(!roleWithFullRightsExists){
            addNotification('warning','MISSING_ACL_RULES', 3, null, NOTIFICATION_CONTEXT);
        }

        if(allRulesValid && roleWithFullRightsExists){
            saveNewAccessPolicies(resourceId, ace).then(success => {
                // fetch new policies from the backend, if save successful
                if(success){
                    setPolicyChanged(false);
                    fetchAccessPolicies(resourceId);
                }
            })
        }
    }

    /* validates the policies in the formik form */
    const validateFormik = (values) => {
        const errors = {};
        setPolicyChanged(isPolicyChanged(values.policies));

        // each policy needs a role
        if(values.policies.find(policy => ((!policy.role) || (policy.role === '')))){
            errors.emptyRole = "Empty role!";
        }

        return errors;
    }

    /* transforms rules into proper format for saving and checks validity
    * of the policies
    * each policy needs a role and at least one of: read-rights, write-rights, additional action
    * there needs to be at least one role, which has both read and write rights*/
    const validatePolicies = (values) => {

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

        return {ace, roleWithFullRightsExists, allRulesValid};
    }

    /* checks whether the current state of the policies from the formik is different form the
    * initial policies or equal to them */
    const isPolicyChanged = (newPolicies) => {
        if (newPolicies.length !== policies.length) {
            return true;
        }
        const sortSchema = (pol1, pol2) => {return (pol1.role > pol2.role)? 1 : -1};
        const sortedNewPolicies = [...newPolicies].sort(sortSchema);
        const sortedInitialPolicies = [...policies].sort(sortSchema);
        for (let i = 0; i < sortedNewPolicies.length; i++){
            if (sortedNewPolicies[i].role !== sortedInitialPolicies[i].role ||
                sortedNewPolicies[i].read !== sortedInitialPolicies[i].read ||
                sortedNewPolicies[i].write !== sortedInitialPolicies[i].write ||
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

    /* fetches the policies for the chosen template and sets the policies in the formik form to those policies */
    const handleTemplateChange = async (templateId, setFormikFieldValue) => {
        // fetch information about chosen template from backend
        const template =  await fetchAclTemplateById(templateId);

        setFormikFieldValue('policies', template);
        setFormikFieldValue('template', templateId);
    };

    // todo: add user and role management
    return (
        <div className="modal-content">
            <div className="modal-body">
                <div className="full-col">
                    {/* Notifications */}
                    <Notifications context="not_corner"/>

                    {!loading && (
                        <ul>
                            <li>
                                <Formik
                                    initialValues={{policies: [...policies], template: ""}}
                                    enableReinitialize
                                    validate={values => validateFormik(values)}
                                    onSubmit={(values, actions) =>
                                        saveAccess(values).then(r => {})
                                    }
                                >
                                    {formik => (
                                        <div className="obj list-obj">
                                            <header>{t(header)/* Access Policy */}</header>

                                            {/* policy templates */}
                                            <div className="obj-container">
                                                <div className="obj tbl-list">
                                                    <table className="main-tbl">{/*todo: show only if: $root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT') */}
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
                                                                        {t("EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.DESCRIPTION") /* Description */}
                                                                    </p>
                                                                    {!transactions.read_only ? (

                                                                        /* dropdown for selecting a policy template */
                                                                        <Field className="chosen-single chosen-default"
                                                                               style={{width: '200px'}}
                                                                               name={"template"}
                                                                               as="select"
                                                                               onChange={event => handleTemplateChange(event.target.value, formik.setFieldValue)}
                                                                        >
                                                                            {(aclTemplates && aclTemplates.length > 0) ? (
                                                                                <>
                                                                                    <option value="" defaultValue hidden>
                                                                                        {t('EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.LABEL')}
                                                                                    </option>
                                                                                    {
                                                                                        aclTemplates.map((template, key) => (
                                                                                            <option value={template.id}
                                                                                                    key={key}
                                                                                            >
                                                                                                {template.value}
                                                                                            </option>
                                                                                        ))
                                                                                    }
                                                                                </>
                                                                            ) : (
                                                                                <option value="" defaultValue hidden>
                                                                                    {t('EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.EMPTY')}
                                                                                </option>
                                                                            )}
                                                                        </Field>

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

                                            {/* list of policy details and interface for changing them */}
                                            <div className="obj-container">
                                                <div className="obj tbl-list">
                                                    <header>{t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.DETAILS") /*Details*/}</header>

                                                    <div className="obj-container">
                                                        <table className="main-tbl">

                                                            {/* column headers */}
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
                                                                { hasActions && (
                                                                    <th className="fit">
                                                                        {t("EVENTS.SERIES.DETAILS.ACCESS.ACCESS_POLICY.ADDITIONAL_ACTIONS") /* <!-- Additional Actions --> */}
                                                                    </th>
                                                                )}
                                                                { true /* todo: show only if: $root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT') */ && (
                                                                    <th className="fit">
                                                                        {t("EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.ACTION") /* <!-- Action --> */}
                                                                    </th>
                                                                )}
                                                            </tr>
                                                            </thead>

                                                            <tbody>
                                                            {/* list of policies */}
                                                            <FieldArray name="policies">
                                                                { ({ replace, remove, push }) => (
                                                                    <>
                                                                        { (formik.values.policies.length > 0) &&
                                                                        formik.values.policies.map((policy, index) => (
                                                                            <tr key={index}>

                                                                                {/* dropdown for policy.role */}
                                                                                <td>
                                                                                    { !transactions.read_only ? (
                                                                                        <Field className="chosen-single chosen-default"
                                                                                               style={{width: '360px'}}
                                                                                               name={`policies.${index}.role`}
                                                                                               as="select"
                                                                                               onChange={role => replace(index, {...policy, role:role.target.value})}
                                                                                        >
                                                                                            { (roles && roles.length > 0) && (
                                                                                                <>
                                                                                                    { !policy.role &&
                                                                                                    <option value="" defaultValue hidden>
                                                                                                        {t('EVENTS.EVENTS.DETAILS.ACCESS.ROLES.LABEL')}
                                                                                                    </option>
                                                                                                    }
                                                                                                    { !!policy.role &&
                                                                                                    <option value={policy.role} defaultValue>
                                                                                                        {policy.role}
                                                                                                    </option>
                                                                                                    }
                                                                                                    { roles.filter(role => !formik.values.policies.find(policy => policy.role === role.name)).map((role, key) => (
                                                                                                        <option value={role.name}
                                                                                                                key={key}
                                                                                                        >
                                                                                                            {role.name}
                                                                                                        </option>
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

                                                                                {/* Checkboxes for policy.read and policy.write */}
                                                                                <td className="fit text-center">
                                                                                    <Field type="checkbox"
                                                                                           name={`policies.${index}.read`}
                                                                                           disabled={ transactions.read_only }
                                                                                           className={`${transactions.read_only ?
                                                                                               "disabled" : "false"}`}
                                                                                           onChange={ (read) => replace(index, {...policy, read: read.target.checked})}
                                                                                    /> {/*Todo: show only if: !$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')*/}
                                                                                </td>
                                                                                <td className="fit text-center">
                                                                                    <Field type="checkbox"
                                                                                           name={`policies.${index}.write`}
                                                                                           disabled={ transactions.read_only }
                                                                                           className={`${transactions.read_only ?
                                                                                               "disabled" : "false"}`}
                                                                                           onChange={ (write) => replace(index, {...policy, write: write.target.checked})}
                                                                                    /> {/*Todo: show only if: !$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')*/}
                                                                                </td>

                                                                                {/* Multi value field for policy.actions (additional actions) */}
                                                                                { hasActions && (
                                                                                    <td className="fit editable">
                                                                                        { !transactions.read_only /*Todo:  && $root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')*/ && (
                                                                                            <div>
                                                                                                <Field
                                                                                                    fieldInfo={
                                                                                                        {
                                                                                                            id: `policies.${index}.actions`,
                                                                                                            type: 'mixed_text',
                                                                                                            collection: aclActions
                                                                                                        }
                                                                                                    }
                                                                                                    onlyCollectionValues
                                                                                                    name={`policies.${index}.actions`}
                                                                                                    component={RenderMultiField}
                                                                                                />
                                                                                            </div>
                                                                                        )}
                                                                                        {transactions.read_only /*Todo:  || ((!$root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')*/ && (
                                                                                            policy.actions.map((customAction, actionKey) => (
                                                                                                <div key={actionKey}>
                                                                                                    {customAction}
                                                                                                </div>
                                                                                            ))
                                                                                        )}
                                                                                    </td>
                                                                                )}

                                                                                {/* Remove policy */}
                                                                                { true /*Todo: show only if: $root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT')*/ && (
                                                                                    <td>
                                                                                        { !transactions.read_only && (
                                                                                            <a onClick={() => remove(index)} className="remove" />
                                                                                        )}
                                                                                    </td>
                                                                                )}
                                                                            </tr>
                                                                        ))
                                                                        }

                                                                        {/* create additional policy */}
                                                                        { !transactions.read_only /*Todo:   && $root.userIs('ROLE_UI_EVENTS_DETAILS_ACL_EDIT') */ && (
                                                                            <tr>
                                                                                <td colSpan="5">
                                                                                    <a onClick={() => push(createPolicy(""))}>
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

                                            {/* Save and cancel buttons */}
                                            { (!transactions.read_only && policyChanged && formik.dirty) && (
                                                <footer style={{padding: '15px'}}>
                                                    <div className="pull-left">
                                                        <button type="reset"
                                                                onClick={() => resetPolicies(formik.resetForm)}
                                                                className="cancel"
                                                        >
                                                            {t('CANCEL')/* Cancel */}
                                                        </button>
                                                    </div>
                                                    <div className="pull-right">
                                                        <button onClick={() => saveAccess(formik.values)}
                                                                disabled={ !formik.isValid}
                                                                className={`save green  ${(!formik.isValid) ? "disabled" : ""}`}
                                                        >
                                                            {t('EVENTS.SERIES.DETAILS.ACCESS.ACCESS_POLICY.REPLACE_EVENT_ACLS')/* Save */}
                                                        </button>
                                                    </div>
                                                </footer>
                                            )}

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


// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    addNotification: (type, key, duration, parameter, context) => dispatch(addNotification(type, key, duration, parameter, context)),
    fetchRoles: () => fetchRolesWithTarget("ACL"),
    fetchAclTemplates: () => fetchAclTemplates(),
});

export default connect(null, mapDispatchToProps)(ResourceDetailsAccessPolicyTab);