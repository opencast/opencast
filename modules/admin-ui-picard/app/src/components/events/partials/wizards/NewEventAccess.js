import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import Notifications from "../../../shared/Notifications";
import {fetchAclActions, fetchAclTemplateById, fetchAclTemplates, fetchRoles} from "../../../../thunks/aclThunks";
import {FieldArray, Field} from "formik";
import RenderMultiField from "./RenderMultiField";

const NewEventAccess = ({ onSubmit, previousPage, nextPage, formik }) => {
    const { t } = useTranslation();

    // States containing response from server concerning acl templates, actions and roles
    const [aclTemplates, setAclTemplates] = useState([]);
    const [aclActions, setAclActions] = useState([]);
    const [roles, setRoles] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        async function fetchData() {
            setLoading(true);
            const responseTemplates = await fetchAclTemplates();
            setAclTemplates(responseTemplates);
            const responseActions = await fetchAclActions();
            setAclActions(responseActions);
            const responseRoles = await fetchRoles();
            setRoles(responseRoles);
            setLoading(false);
        }

        fetchData();
    }, []);

    const handleTemplateChange = async (e) => {
        const template =  await fetchAclTemplateById(e.target.value);

        formik.setFieldValue('policies', template);
    }

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        {/*TODO: implement notifications events-access (similar to event-form)*/}
                        <Notifications />
                        {!loading && (
                            <ul>
                                <li>
                                    <div className="obj list-obj">
                                        <header className="no-expand">
                                            {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.TITLE')}
                                        </header>
                                        <div className="obj-container">
                                            <p>
                                                {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.DESCRIPTION')}
                                            </p>
                                            <div className="obj tbl-list">
                                                <table className="main-tbl">
                                                    <thead>
                                                    <tr>
                                                        <th>{t('EVENTS.SERIES.NEW.ACCESS.TEMPLATES.TITLE')}</th>
                                                    </tr>
                                                    </thead>
                                                    <tbody>
                                                    <tr>
                                                        {aclTemplates.length > 0 ? (
                                                            <td>
                                                                <div className="obj-container padded">
                                                                    <select tabIndex="1"
                                                                            autoFocus
                                                                            style={{width: '200px'}}
                                                                            onChange={e => handleTemplateChange(e)}
                                                                            placeholder={t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.LABEL')}>
                                                                        <option value=""/>
                                                                        {aclTemplates.map((template, key) => (
                                                                            <option value={template.id}
                                                                                    key={key}>{template.label}</option>
                                                                        ))}
                                                                    </select>
                                                                </div>
                                                            </td>
                                                        ) : (
                                                            //Show if no option is available
                                                            <td>
                                                                <div className="obj-container padded">
                                                                    {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.EMPTY')}
                                                                </div>
                                                            </td>
                                                        )}

                                                    </tr>
                                                    </tbody>
                                                </table>
                                            </div>
                                        </div>

                                        <div className="obj-container">
                                            <div className="obj tbl-list">
                                                <header>
                                                    {t('EVENTS.SERIES.DETAILS.ACCESS.ACCESS_POLICY.DETAILS')}
                                                </header>
                                                <div className="obj-container">
                                                    <table className="main-tbl">
                                                        <thead>
                                                        <tr>
                                                            <th>
                                                                {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.ROLE')}
                                                            </th>
                                                            <th className="fit">
                                                                {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.READ')}
                                                            </th>
                                                            <th className="fit">
                                                                {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.WRITE')}
                                                            </th>
                                                            <th className="fit">
                                                                {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.ADDITIONAL_ACTIONS')}
                                                            </th>
                                                            <th className="fit">
                                                                {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.ACTION')}
                                                            </th>
                                                        </tr>
                                                        </thead>
                                                        <tbody>
                                                        {/*Add fieldArray/row for each policy in policies field*/}
                                                        <FieldArray name="policies">
                                                            {({ insert, remove, push }) => (
                                                                <>
                                                                    {roles.length > 0 ? (
                                                                        formik.values.policies.length > 0 &&
                                                                                formik.values.policies.map((policy, index) => (
                                                                                    <tr key={index}>

                                                                                        <td>
                                                                                            {/*Todo: make no options available if no (see translation)*/}
                                                                                            <Field style={{width: '360px'}}
                                                                                                   name={`policies.${index}.role`}
                                                                                                   as="select"
                                                                                                   placeholder={t('EVENTS.SERIES.NEW.ACCESS.ROLES.LABEL')}>
                                                                                                {roles.length > 0 && (
                                                                                                    <>
                                                                                                        <option value="" />
                                                                                                        {roles.map((role, key) => (
                                                                                                            <option value={role.id}
                                                                                                                    key={key}>{role.id}</option>
                                                                                                        ))}
                                                                                                    </>
                                                                                                )}

                                                                                            </Field>
                                                                                        </td>
                                                                                        {/* Checkboxes for  policy.read and policy.write*/}
                                                                                        <td className="fit text-center"><Field type="checkbox" name={`policies.${index}.read`}/></td>
                                                                                        <td className="fit text-center"><Field type="checkbox" name={`policies.${index}.write`}/></td>
                                                                                        {/* Show only if policy has actions*/}
                                                                                        {aclActions.length > 0 && (
                                                                                            <td className="fit editable">
                                                                                                <div>
                                                                                                    <RenderMultiField fieldInformation={
                                                                                                        {
                                                                                                            id: `policies.${index}.actions`,
                                                                                                            type: 'mixed_text',
                                                                                                            collection: aclActions
                                                                                                        }
                                                                                                    } onlyCollectionValues/>
                                                                                                </div>
                                                                                            </td>
                                                                                        )}
                                                                                        {/*Remove policy*/}
                                                                                        <td>
                                                                                            <a onClick={() => remove(index)}
                                                                                               className="remove" />
                                                                                        </td>
                                                                                    </tr>
                                                                                ))
                                                                    ) : (
                                                                        <tr>
                                                                            <td>{t('EVENTS.SERIES.NEW.ACCESS.ROLES.EMPTY')}</td>
                                                                        </tr>
                                                                    )}

                                                                    {/*Todo: show only if user has role ROLE_UI_SERIES_DETAILS_ACL_EDIT */}
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
                                                                                console.log(formik.values)
                                                                            }}>
                                                                                + {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.NEW')}
                                                                            </a>
                                                                        </td>
                                                                    </tr>
                                                                </>
                                                            )}
                                                        </FieldArray>
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
                </div>
            </div>
            {/* Button for navigation to next page and previous page */}
            <footer>
                <button type="submit"
                        className={cn("submit",
                            {
                                active: (formik.dirty && formik.isValid),
                                inactive: !(formik.dirty && formik.isValid)
                            })}
                        disabled={!(formik.dirty && formik.isValid)}
                        onClick={() => {
                            nextPage(formik.values);
                            onSubmit();
                        }}
                        tabIndex="100">{t('WIZARD.NEXT_STEP')}</button>
                <button className="cancel"
                        onClick={() => previousPage(formik.values, false)}
                        tabIndex="101">{t('WIZARD.BACK')}</button>
            </footer>

            <div className="btm-spacer"/>
        </>
    )
}

export default NewEventAccess;
