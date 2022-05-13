import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import {connect} from "react-redux";
import {Field, FieldArray} from "formik";
import Notifications from "../../../shared/Notifications";
import RenderMultiField from "../../../shared/wizard/RenderMultiField";
import {
    checkAcls,
    fetchAclActions,
    fetchAclTemplateById,
    fetchAclTemplates,
    fetchRolesWithTarget
} from '../../../../thunks/aclThunks';
import {getUserInformation} from "../../../../selectors/userInfoSelectors";
import {hasAccess} from "../../../../utils/utils";

/**
 * This component renders the access policy page in the new ACL wizard and in the ACL details modal
 */
const AclAccessPage = ({ previousPage, nextPage, formik, isEdit, user, checkAcls }) => {
    const { t } = useTranslation();

    const [aclTemplates, setAclTemplates] = useState([]);
    const [aclActions, setAclActions] = useState([]);
    const [roles, setRoles] = useState([]);
    const [loading, setLoading] = useState(false);

    const isAccess = (hasAccess("ROLE_UI_SERIES_DETAILS_ACL_EDIT", user)) || !isEdit;

    useEffect(() => {
        // fetch data about roles, acl templates and actions from backend
        async function fetchData() {
            setLoading(true);
            const responseTemplates = await fetchAclTemplates();
            setAclTemplates(responseTemplates);
            const responseActions = await fetchAclActions();
            setAclActions(responseActions);
            const responseRoles = await fetchRolesWithTarget('ACL');
            setRoles(responseRoles);
            setLoading(false);
        }

        fetchData();
    }, []);

    const handleTemplateChange = async (e) => {
        // fetch information about chosen template from backend
        const template =  await fetchAclTemplateById(e.target.value);

        formik.setFieldValue('acls', template);
        await checkAcls(formik.values.acls);
    }

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <Notifications context="not_corner"/>
                        {!loading && (
                            <ul>
                                <li>
                                    <div className="obj list-obj">
                                        <header className="no-expand">
                                            {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.TITLE')}
                                        </header>
                                        <div className="obj-container">

                                            {/* Template selection */}
                                            <div className="obj tbl-list">
                                                {isAccess && (
                                                    <table className="main-tbl">
                                                        <thead>
                                                        <tr>
                                                            <th>{t('USERS.ACLS.NEW.ACCESS.TEMPLATES.TITLE')}</th>
                                                        </tr>
                                                        </thead>
                                                        <tbody>
                                                        <tr>
                                                            {aclTemplates.length > 0 ? (
                                                                <td>
                                                                    <div className="obj-container padded">
                                                                        <p>
                                                                            {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.DESCRIPTION')}
                                                                        </p>
                                                                        <select tabIndex="1"
                                                                                autoFocus
                                                                                style={{width: '200px'}}
                                                                                onChange={e => handleTemplateChange(e)}
                                                                                placeholder={t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.LABEL')}>
                                                                            <option value=""/>
                                                                            {aclTemplates.map((template, key) => (
                                                                                <option value={template.id}
                                                                                        key={key}>
                                                                                    {template.value}
                                                                                </option>
                                                                            ))}
                                                                        </select>
                                                                    </div>
                                                                </td>
                                                            ) : (
                                                                // Show if no option is available
                                                                <td>
                                                                    <div className="obj-container padded">
                                                                        <p>
                                                                            {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.DESCRIPTION')}
                                                                        </p>
                                                                        {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.EMPTY')}
                                                                    </div>
                                                                </td>
                                                            )}
                                                        </tr>
                                                        </tbody>
                                                    </table>
                                                )}
                                            </div>

                                            <div className="obj-container">
                                                <div className="obj tbl-list">
                                                    <header>
                                                        {t('')}
                                                    </header>
                                                    <div className="obj-container">
                                                        <table className="main-tbl">
                                                            <thead>
                                                                <tr>
                                                                    <th>
                                                                        {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.ROLE')}
                                                                    </th>
                                                                    <th className="fit">
                                                                        {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.READ')}
                                                                    </th>
                                                                    <th className="fit">
                                                                        {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.WRITE')}
                                                                    </th>
                                                                    {aclActions.length > 0 && (
                                                                        <th className="fit">
                                                                            {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.ADDITIONAL_ACTIONS')}
                                                                        </th>
                                                                    )}
                                                                    <th className="fit">
                                                                        {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.ACTION')}
                                                                    </th>
                                                                </tr>
                                                            </thead>
                                                            <tbody>
                                                                <FieldArray name="acls">
                                                                    {({ insert, remove, push }) => (
                                                                        <>
                                                                            {roles.length > 0 ? (
                                                                                formik.values.acls.length > 0 &&
                                                                                    formik.values.acls.map((acl, index) => (
                                                                                        <tr key={index}>
                                                                                            <td>
                                                                                                <Field style={{width: '360px'}}
                                                                                                       name={`acls.${index}.role`}
                                                                                                       as="select"
                                                                                                       disabled={!isAccess}
                                                                                                       placeholder={t('USERS.ACLS.NEW.ACCESS.ROLES.LABEL')}>
                                                                                                    {roles.length > 0 && (
                                                                                                        <>
                                                                                                            <option  value="" />
                                                                                                            {roles.map((role, key) => (
                                                                                                                <option value={role.name}
                                                                                                                        key={key}>{role.name}</option>
                                                                                                            ))}
                                                                                                        </>
                                                                                                    )}
                                                                                                </Field>
                                                                                            </td>
                                                                                            <td className="fit text-center">
                                                                                                <Field type="checkbox" name={`acls.${index}.read`}/>
                                                                                            </td>
                                                                                            <td className="fit text-center">
                                                                                                <Field type="checkbox" name={`acls.${index}.write`}/>
                                                                                            </td>
                                                                                            {aclActions.length > 0 && (
                                                                                                isAccess ? (
                                                                                                    <td className="fit editable">
                                                                                                        <div>
                                                                                                            <Field
                                                                                                                name={`acls.${index}.actions`}
                                                                                                                fieldInfo={
                                                                                                                    {
                                                                                                                        id: `acls.${index}.actions`,
                                                                                                                        type: 'mixed_text',
                                                                                                                        collection: aclActions
                                                                                                                    }
                                                                                                                }
                                                                                                                onlyCollectionValues
                                                                                                                component={RenderMultiField}/>
                                                                                                        </div>
                                                                                                    </td>
                                                                                                    ) : (
                                                                                                        <td className="fit">
                                                                                                            {/*repeat for each additional action*/}
                                                                                                            {formik.values.acls[index].actions.map((action, key) => (
                                                                                                                <div key={key}>{action.value}</div>
                                                                                                            ))}
                                                                                                        </td>
                                                                                                        )
                                                                                            )}
                                                                                            {/*Remove policy*/}
                                                                                            {isAccess && (
                                                                                                <td>
                                                                                                    <a onClick={() => remove(index)}
                                                                                                       className="remove"/>
                                                                                                </td>
                                                                                            )}
                                                                                        </tr>
                                                                                    ))
                                                                            ) : (
                                                                                <tr>
                                                                                    <td>{t('USERS.ACLS.NEW.ACCESS.ROLES.EMPTY')}</td>
                                                                                </tr>
                                                                            )}

                                                                            {isAccess && (
                                                                                <tr>
                                                                                    {/*Add additional policy row*/}
                                                                                    <td colSpan="5">
                                                                                        <a onClick={() => {
                                                                                            push({
                                                                                                role: '',
                                                                                                read: false,
                                                                                                write: false,
                                                                                                actions: []
                                                                                            });
                                                                                            checkAcls(formik.values.acls);
                                                                                        }}> + {t('USERS.ACLS.NEW.ACCESS.ACCESS_POLICY.NEW')}</a>
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
                                        </div>
                                    </div>
                                </li>
                            </ul>
                        )}
                    </div>
                </div>
            </div>
            {/* Button for navigation to next page and previous page */}
            {!isEdit && (
                <>
                    <footer>
                        <button type="submit"
                                className={cn("submit",
                                    {
                                        active: (formik.dirty && formik.isValid),
                                        inactive: !(formik.dirty && formik.isValid)
                                    })}
                                disabled={!(formik.dirty && formik.isValid)}
                                onClick={async () => {
                                    if(await checkAcls(formik.values.acls)) {
                                        nextPage(formik.values);
                                    }
                                }}
                                tabIndex="100">{t('WIZARD.NEXT_STEP')}</button>
                        <button className="cancel"
                                onClick={() => previousPage(formik.values, false)}
                                tabIndex="101">{t('WIZARD.BACK')}</button>
                    </footer>

                    <div className="btm-spacer"/>
                </>
            )}

        </>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    user: getUserInformation(state)
});

const mapDispatchToProps = dispatch => ({
    checkAcls: acls => dispatch(checkAcls(acls))
});

export default connect(mapStateToProps, mapDispatchToProps)(AclAccessPage);
