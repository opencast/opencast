import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import cn from 'classnames';
import {Formik} from "formik";
import {NewThemeSchema} from "../../../shared/wizard/validate";
import GeneralPage from "./GeneralPage";
import BumperPage from "./BumperPage";
import TitleSlidePage from "./TitleSlidePage";
import WatermarkPage from "./WatermarkPage";
import {getThemeDetails, getThemeUsage} from "../../../../selectors/themeDetailsSelectors";
import UsagePage from "./UsagePage";
import {updateThemeDetails} from "../../../../thunks/themeDetailsThunks";

/**
 * This component manages the pages of the theme details
 */
const ThemeDetails = ({ close, themeDetails, themeUsage, updateTheme }) => {
    const { t } = useTranslation();

    const [page, setPage] = useState(0);

    // set initial values for formik form
    const initialValues = {
        ...themeDetails,
        titleSlideMode: themeDetails.titleSlideActive && !!themeDetails.titleSlideBackgroundName
            ? 'upload'
            : 'extract'
    };

    // information about tabs
    const tabs = [
        {
            name: 'generalForm',
            tabTranslation: 'CONFIGURATION.THEMES.DETAILS.GENERAL.CAPTION',
            translation: 'CONFIGURATION.THEMES.DETAILS.GENERAL.CAPTION'
        },
        {
            name: 'bumperForm',
            tabTranslation: 'CONFIGURATION.THEMES.DETAILS.BUMPER.CAPTION',
            translation: 'CONFIGURATION.THEMES.DETAILS.BUMPER.CAPTION'
        },
        {
            name: 'trailerForm',
            tabTranslation: 'CONFIGURATION.THEMES.DETAILS.TRAILER.CAPTION',
            translation: 'CONFIGURATION.THEMES.DETAILS.TRAILER.CAPTION'
        },
        {
            name: 'titleSlideForm',
            tabTranslation: 'CONFIGURATION.THEMES.DETAILS.TITLE.CAPTION',
            translation: 'CONFIGURATION.THEMES.DETAILS.TITLE.CAPTION'
        },
        {
            name: 'watermarkForm',
            tabTranslation: 'CONFIGURATION.THEMES.DETAILS.WATERMARK.CAPTION',
            translation: 'CONFIGURATION.THEMES.DETAILS.WATERMARK.CAPTION'
        },
        {
            name: 'usage',
            tabTranslation: 'CONFIGURATION.THEMES.DETAILS.USAGE.CAPTION',
            translation: 'CONFIGURATION.THEMES.DETAILS.USAGE.CAPTION'
        }
    ];

    // Validation schema of current page
    const currentValidationSchema = NewThemeSchema[page];

    // update theme
    const handleSubmit = values => {
        updateTheme(themeDetails.id, values);
        close();
    };

    const openTab = tabNr => {
        setPage(tabNr);
    };

    return (
        <>
            <nav className="modal-nav" id="modal-nav">
                <a className={cn({active: page === 0})}
                   onClick={() => openTab(0)}>
                    {t(tabs[0].tabTranslation)}
                </a>
                <a className={cn({active: page === 1})}
                   onClick={() => openTab(1)}>
                    {t(tabs[1].tabTranslation)}
                </a>
                <a className={cn({active: page === 2})}
                   onClick={() => openTab(2)}>
                    {t(tabs[2].tabTranslation)}
                </a>
                <a className={cn({active: page === 3})}
                   onClick={() => openTab(3)}>
                    {t(tabs[3].tabTranslation)}
                </a>
                <a className={cn({active: page === 4})}
                   onClick={() => openTab(4)}>
                    {t(tabs[4].tabTranslation)}
                </a>
                <a className={cn({active: page === 5})}
                   onClick={() => openTab(5)}>
                    {t(tabs[5].tabTranslation)}
                </a>
            </nav>

            {/* initialize overall form */}
            <Formik initialValues={initialValues}
                    validationSchema={currentValidationSchema}
                    onSubmit={values => handleSubmit(values)}>
                {/* render modal pages depending on current value of page variable */}
                {formik => (
                    <div>
                        {page === 0 && (
                            <GeneralPage formik={formik}
                                         isEdit/>
                        )}
                        {page === 1 && (
                            <BumperPage formik={formik}
                                        isEdit/>
                        )}
                        {page === 2 && (
                            <BumperPage formik={formik}
                                        isTrailer
                                        isEdit/>
                        )}
                        {page === 3 && (
                            <TitleSlidePage formik={formik}
                                            isEdit/>
                        )}
                        {page === 4 && (
                            <WatermarkPage formik={formik}
                                           isEdit/>
                        )}
                        {page === 5 && (
                            <UsagePage themeUsage={themeUsage} />
                        )}
                        {/* submit and cancel button */}
                        <footer>
                            <button className={cn("submit", {
                                active: (formik.dirty && formik.isValid),
                                inactive: !(formik.dirty && formik.isValid)
                            })} disabled={!(formik.dirty && formik.isValid)}
                                    onClick={() => formik.handleSubmit()}>{t('SUBMIT')}</button>
                            <button className="cancel"
                                    onClick={() => close()}>
                                {t('CANCEL')}
                            </button>
                        </footer>

                        <div className="btm-spacer"/>
                    </div>
                )}
            </Formik>
        </>
    )
};

// get current state out of redux store
const mapStateToProps = state => ({
    themeDetails: getThemeDetails(state),
    themeUsage: getThemeUsage(state)
});

// map actions to dispatch
const mapDispatchToProps = dispatch => ({
    updateTheme: (id, values) => dispatch(updateThemeDetails(id, values))
});

export default connect(mapStateToProps, mapDispatchToProps)(ThemeDetails);
