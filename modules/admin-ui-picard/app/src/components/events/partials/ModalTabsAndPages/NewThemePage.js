import React from "react";
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import {Field} from "formik";
import {getSeriesThemes} from "../../../../selectors/seriesSeletctor";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";

/**
 * This component renders the theme page for new series in the new series wizard.
 */
const NewThemePage = ({ formik, nextPage, previousPage, seriesThemes }) => {
    const { t } = useTranslation();

    const getDescription = id => {
        const theme = seriesThemes.find(theme => theme.id === id);

        return theme.description;
    }

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <div className="obj quick-actions">
                            <header className="no-expand">{t('EVENTS.SERIES.NEW.THEME.TITLE')}</header>
                            <div className="obj-container padded">
                                <ul>
                                    <li>
                                        <p>{t('EVENTS.SERIES.NEW.THEME.DESCRIPTION.TEXT')}</p>
                                        {seriesThemes.length > 0 ? (
                                            <>
                                                <p>
                                                    <Field tabIndex="1"
                                                           name="theme"
                                                           as="select"
                                                           placeholder={t('EVENTS.SERIES.NEW.THEME.LABEL')}
                                                           style={{width: '100%'}}>
                                                        <option value=''>{t('EVENTS.SERIES.NEW.THEME.LABEL')}</option>
                                                        {seriesThemes.map((theme, key) => (
                                                            <option value={theme.id}
                                                                    key={key}>{theme.name}</option>
                                                        ))}
                                                    </Field>
                                                </p>
                                                {!!formik.values.theme && (
                                                    <p>{getDescription(formik.values.theme)}</p>
                                                )}
                                            </>
                                        ) : (
                                            <p>{t('EVENTS.SERIES.NEW.THEME.EMPTY')}</p>
                                        )}

                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Button for navigation to next page and previous page */}
            <WizardNavigationButtons formik={formik}
                                     nextPage={nextPage}
                                     previousPage={previousPage}/>

        </>
    )
};

const mapStateToProps = state => ({
    seriesThemes: getSeriesThemes(state)
});

export default connect(mapStateToProps)(NewThemePage);
