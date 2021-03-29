import React from "react";
import {useTranslation} from "react-i18next";
import {Field} from "formik";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";

const NewAclMetadataPage = ({ nextPage, formik }) => {
    const { t } = useTranslation();
    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <ul>
                            <li>
                                <div className="obj tbl-details">
                                    <header>{t('USERS.ACLS.NEW.METADATA.TITLE')}</header>
                                    <div className="obj-container">
                                        <table className="main-tbl">
                                            <tr>
                                                <td>
                                                    {t('USERS.ACLS.NEW.METADATA.NAME.CAPTION')}
                                                    <i className="required">*</i>
                                                </td>
                                                <td>
                                                    <Field className="hidden-input"
                                                           name="name"
                                                           tabindex="1"
                                                           focushere
                                                           placeholder={t('USERS.ACLS.NEW.METADATA.NAME.PLACEHOLDER')}/>
                                                </td>
                                            </tr>
                                        </table>
                                    </div>
                                </div>
                            </li>
                        </ul>
                    </div>
                </div>
            </div>

            {/* Button for navigation to next page */}
            <WizardNavigationButtons isFirst
                                     formik={formik}
                                     nextPage={nextPage}/>
        </>
    );
};

export default NewAclMetadataPage;
