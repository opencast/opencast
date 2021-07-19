import React from "react";
import {useTranslation} from "react-i18next";
import {Field, Formik} from "formik";
import Notifications from "../../../shared/Notifications";
import _ from "lodash";
import cn from "classnames";
import {updateSeriesTheme} from "../../../../thunks/seriesDetailsThunks";
import {connect} from "react-redux";

const SeriesDetailsThemeTab = ({ theme, seriesId, themeNames, updateTheme }) => {
    const { t } = useTranslation();

    const handleSubmit = values => {
        updateTheme(seriesId, values);
    }

    const checkValidity = formik => {
        if (formik.dirty && formik.isValid) {
            // check if user provided values differ from initial ones
            return !_.isEqual(formik.values, formik.initialValues);
        } else {
            return false;
        }
    }

    return (
        <Formik enableReinitialize
                initialValues={{theme: theme}}
                onSubmit={values => handleSubmit(values)}>
            {formik => (
                <>
                    <div className="modal-content">
                        <div className="modal-body">
                            <Notifications context="not-corner"/>
                            <div className="full-col">
                                <div className="obj quick-actions">
                                    <header>{t('CONFIGURATION.NAVIGATION.THEMES')}</header>
                                    <div className="obj-container padded">
                                        <ul>
                                            <li>
                                                <p>{t('EVENTS.SERIES.NEW.THEME.DESCRIPTION.TEXT')}</p>
                                                {themeNames.length > 0 && (
                                                    <p>
                                                        <Field name="theme"
                                                               as="select"
                                                               style={{width: '100%'}}>
                                                            <option value={theme}>{theme}</option>
                                                            {themeNames.map((theme, key) => (
                                                                <option value={theme.value}
                                                                        key={key}>{theme.value}</option>
                                                            ))}
                                                        </Field>
                                                    </p>
                                                )}
                                            </li>
                                        </ul>
                                    </div>
                                    {formik.dirty && (
                                        <>
                                            {/* Render buttons for updating theme */}
                                            <footer style={{padding: '15px'}}>
                                                <button type="submit"
                                                        onClick={() => formik.handleSubmit()}
                                                        disabled={!checkValidity(formik)}
                                                        className={cn("submit",
                                                            {
                                                                active: checkValidity(formik),
                                                                inactive: !checkValidity(formik)
                                                            }
                                                        )}>
                                                    {t('EVENTS.SERIES.DETAILS.METADATA.REPLACE_SERIES_THEME')}
                                                </button>
                                                <button onClick={() => formik.resetForm({values: ''})}
                                                        className="cancel">
                                                    {t('CANCEL')}
                                                </button>
                                            </footer>

                                            <div className="btm-spacer"/>
                                        </>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </Formik>
    );
};

const mapDispatchToProps = dispatch => ({
    updateTheme: (id, values) => dispatch(updateSeriesTheme(id, values))
});

export default connect(null, mapDispatchToProps)(SeriesDetailsThemeTab);
