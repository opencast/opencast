import React from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import {Field} from "formik";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";
import FileUpload from "../../../shared/wizard/FileUpload";

/**
 * This component renders the watermark page for new themes in the new themes wizard.
 */
const WatermarkPage = ({ formik, nextPage, previousPage }) => {
    const { t } = useTranslation();

    const handleButtonClick = position => {
        formik.setFieldValue('watermarkPosition', position);
    }

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <p>{t('CONFIGURATION.THEMES.DETAILS.WATERMARK.DESCRIPTION')}</p>
                        {/*todo: Notification*/}
                        <div className="obj">
                           <header>{t('CONFIGURATION.THEMES.DETAILS.WATERMARK.CAPTION')}</header>
                            <div className="obj-container content-list padded">
                                <div className="list-row">
                                    <div className="header-column">
                                        <label className="large">{t('CONFIGURATION.THEMES.DETAILS.WATERMARK.ENABLE')}</label>
                                    </div>
                                    {/* Checkbox for activating watermark */}
                                    <div className="content-column">
                                        <div className="content-container">
                                            <Field id="watermark-toggle"
                                                   type="checkbox"
                                                   name="watermarkActive"/>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* if checkbox is checked, then render object for uploading files */}
                        {formik.values.watermarkActive && (
                            <>
                                <div className="obj">
                                    <header>{t('CONFIGURATION.THEMES.DETAILS.WATERMARK.UPLOAD')}</header>
                                    <div className="obj-container padded">
                                        <FileUpload acceptableTypes="image/*"
                                                    formikField="watermarkFile"
                                                    buttonKey="CONFIGURATION.THEMES.DETAILS.WATERMARK.UPLOAD_BUTTON"
                                                    labelKey="CONFIGURATION.THEMES.DETAILS.WATERMARK.UPLOAD_LABEL"
                                                    descriptionKey="CONFIGURATION.THEMES.DETAILS.WATERMARK.FILEUPLOAD_DESCRIPTION" />
                                    </div>
                                </div>

                                {/*if file uploaded, then render buttons for choice of position*/}
                                {formik.values.watermarkFile.id && (
                                    <div className="obj">
                                        <header>{t('CONFIGURATION.THEMES.DETAILS.WATERMARK.POSITION')}</header>
                                        <div className="obj-container padded">
                                            <div className="video-container">
                                                <div className="watermark-config">
                                                    <div className="position-selection">
                                                        <button className={cn("position-button position-top-left",
                                                            {active: formik.values.watermarkPosition === 'topLeft'})}
                                                                onClick={() => handleButtonClick('topLeft')}>
                                                            {t('CONFIGURATION.THEMES.DETAILS.WATERMARK.TOP_LEFT')}
                                                        </button>
                                                        <button className={cn("position-button position-top-right",
                                                            {active: formik.values.watermarkPosition === 'topRight'})}
                                                                onClick={() => handleButtonClick('topRight')}>
                                                            {t('CONFIGURATION.THEMES.DETAILS.WATERMARK.TOP_RIGHT')}
                                                        </button>
                                                        <button className={cn("position-button position-bottom-left",
                                                            {active: formik.values.watermarkPosition === 'bottomLeft'})}
                                                                onClick={() => handleButtonClick('bottomLeft')}>
                                                            {t('CONFIGURATION.THEMES.DETAILS.WATERMARK.BOTTOM_LEFT')}
                                                        </button>
                                                        <button className={cn("position-button position-bottom-right",
                                                            {active: formik.values.watermarkPosition === 'bottomRight'})}
                                                                onClick={() => handleButtonClick('bottomRight')}>
                                                            {t('CONFIGURATION.THEMES.DETAILS.WATERMARK.BOTTOM_RIGHT')}
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                )}

                            </>
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

export default WatermarkPage;
