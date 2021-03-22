import React from "react";
import {useTranslation} from "react-i18next";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";
import FileUpload from "../../../shared/wizard/FileUpload";
import {Field} from "formik";
import Notifications from "../../../shared/Notifications";

/**
 * This component renders the bumper/trailer (depending on isTrailer flag) page for new themes in the new themes wizard.
 */
const BumperPage = ({ formik, nextPage, previousPage, isTrailer }) => {
    const { t } = useTranslation();

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <p className="tab-description">{t(!isTrailer ? 'CONFIGURATION.THEMES.DETAILS.BUMPER.DESCRIPTION' : 'CONFIGURATION.THEMES.DETAILS.TRAILER.DESCRIPTION')}</p>
                        {/* notifications */}
                        <Notifications context="not_corner"/>
                        <div className="obj">
                            <header>{t(!isTrailer ? 'CONFIGURATION.THEMES.DETAILS.BUMPER.ACTIVE' : 'CONFIGURATION.THEMES.DETAILS.TRAILER.ACTIVE')}</header>
                            <div className="obj-container content-list padded">
                                <div className="list-row">
                                    <div className="header-column">
                                        <label className="large">{t(!isTrailer ? 'CONFIGURATION.THEMES.DETAILS.BUMPER.ENABLE' : 'CONFIGURATION.THEMES.DETAILS.TRAILER.ENABLE')}</label>
                                    </div>
                                    {/* Checkbox for activating bumper/trailer */}
                                    <div className="content-column">
                                        <div className="content-container">
                                            <Field id="bumper-toggle"
                                                   type="checkbox"
                                                   name={!isTrailer ? 'bumperActive' : 'trailerActive'}/>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* if checkbox is checked, then render object for uploading files */}
                        {((!isTrailer && formik.values.bumperActive) || (isTrailer && formik.values.trailerActive)) && (
                            <div className="obj">
                                <header>{t(!isTrailer ? 'CONFIGURATION.THEMES.DETAILS.BUMPER.SELECT' : 'CONFIGURATION.THEMES.DETAILS.TRAILER.SELECT')}</header>
                                <div className="obj-container padded">
                                    {/* Upload file for bumper/trailer */}
                                    <FileUpload acceptableTypes="video/*"
                                                formikField={!isTrailer ? 'bumperFile' : 'trailerFile'}
                                                buttonKey="CONFIGURATION.THEMES.DETAILS.BUMPER.UPLOAD_BUTTON"
                                                labelKey="CONFIGURATION.THEMES.DETAILS.BUMPER.UPLOAD_LABEL"/>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* Button for navigation to next page */}
            <WizardNavigationButtons formik={formik}
                                     previousPage={previousPage}
                                     nextPage={nextPage}/>
        </>
    );
};


export default BumperPage;
