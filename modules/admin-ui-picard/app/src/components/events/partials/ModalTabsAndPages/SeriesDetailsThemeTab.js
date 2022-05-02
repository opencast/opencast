import React from "react";
import {useTranslation} from "react-i18next";
import {Field, Formik} from "formik";
import _ from "lodash";
import cn from "classnames";
import {connect} from "react-redux";
import Notifications from "../../../shared/Notifications";
import {updateSeriesTheme} from "../../../../thunks/seriesDetailsThunks";
import {getUserInformation} from "../../../../selectors/userInfoSelectors";
import {hasAccess} from "../../../../utils/utils";

/**
 * This component renders the tab for editing the theme of a certain series
 */
const SeriesDetailsThemeTab = ({ theme, seriesId, themeNames, updateTheme, user }) => {
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
                                                               disabled={!hasAccess("ROLE_UI_SERIES_DETAILS_THEMES_EDIT", user)}
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
                                                    {t('SAVE')}
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

// Getting state data out of redux store
const mapStateToProps = state => ({
    user: getUserInformation(state)
});

const mapDispatchToProps = dispatch => ({
    updateTheme: (id, values) => dispatch(updateSeriesTheme(id, values))
});

export default connect(mapStateToProps, mapDispatchToProps)(SeriesDetailsThemeTab);
