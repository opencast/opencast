import React from "react";
import {useTranslation} from "react-i18next";
import Notifications from "../../../shared/Notifications";
import {Field} from "formik";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";

const GeneralPage = ({ formik, nextPage }) => {
    const { t } = useTranslation();

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <div className="form-container">
                            <div className="row">
                                <Notifications />
                                <label className="required">
                                    {t('CONFIGURATION.THEMES.DETAILS.GENERAL.NAME')}
                                </label>
                                <Field name="name"
                                       type="text"
                                       placeholder={t('CONFIGURATION.THEMES.DETAILS.GENERAL.NAME') + '...'} />
                            </div>
                            <div className="row">
                                <label>{t('CONFIGURATION.THEMES.DETAILS.GENERAL.DESCRIPTION')}</label>
                                <Field name="description"
                                       as="textarea"
                                       placeholder={t('CONFIGURATION.THEMES.DETAILS.GENERAL.DESCRIPTION') + '...'}/>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Button for navigation to next page */}
            <WizardNavigationButtons isFirst
                                     formik={formik}
                                     nextPage={nextPage}/>
        </>
    );
}

export default GeneralPage;
