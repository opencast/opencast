import React from "react";
import {useTranslation} from "react-i18next";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";

/**
 * This component renders the summary page for new themes in the new theme wizard.
 */
const ThemeSummaryPage = ({ formik, previousPage }) => {
    const { t } = useTranslation();

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <div className="obj">
                            <header>{t('CONFIGURATION.THEMES.DETAILS.SUMMARY.CAPTION')}</header>
                            <div className="obj-container summary-list padded">
                                <ul>
                                    {/* show only when file is uploaded for a list item */}
                                    {formik.values.bumperFile.id && (
                                        <li>
                                            <h4>{t('CONFIGURATION.THEMES.DETAILS.BUMPER.CAPTION')}</h4>
                                            <p>
                                                <span>{t('CONFIGURATION.THEMES.DETAILS.BUMPER.FILE_UPLOADED')}</span>
                                                {formik.values.bumperFile.name}
                                            </p>
                                        </li>
                                    )}
                                    {formik.values.trailerFile.id && (
                                        <li>
                                            <h4>{t('CONFIGURATION.THEMES.DETAILS.TRAILER.CAPTION')}</h4>
                                            <p>
                                                <span>{t('CONFIGURATION.THEMES.DETAILS.TRAILER.FILE_UPLOADED')}</span>
                                                {formik.values.trailerFile.name}
                                            </p>
                                        </li>
                                    )}
                                    {(formik.values.titleSlideMode === 'upload' && formik.values.titleSlideBackground.id) && (
                                        <li>
                                            <h4>{t('CONFIGURATION.THEMES.DETAILS.TITLE.CAPTION')}</h4>
                                            <p>
                                                <span>{t('CONFIGURATION.THEMES.DETAILS.TITLE.FILE_UPLOADED')}</span>
                                                {formik.values.titleSlideBackground.name}
                                            </p>
                                        </li>
                                    )}
                                    {formik.values.watermarkFile.id && (
                                        <li>
                                            <h4>{t('CONFIGURATION.THEMES.DETAILS.WATERMARK.CAPTION')}</h4>
                                            <p>
                                                <span>{t('CONFIGURATION.THEMES.DETAILS.WATERMARK.FILE_UPLOADED')}</span>
                                                {formik.values.watermarkFile.name}
                                            </p>
                                        </li>
                                    )}
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Button for navigation to next page */}
            <WizardNavigationButtons isLast
                                     formik={formik}
                                     previousPage={previousPage}/>
        </>
    );
};

export default ThemeSummaryPage;
