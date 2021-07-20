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
import {getThemeDetails} from "../../../../selectors/themeDetailsSelectors";
import UsagePage from "./UsagePage";


const ThemeDetails = ({ close, themeDetails }) => {
    const { t } = useTranslation();

    const initialValues = themeDetails;

    const [page, setPage] = useState(0);
    const [snapshot, setSnapshot] = useState(initialValues);

    // Caption of steps used by Stepper
    const tabs = [
        {
            name: 'generalForm',
            tabTranslation: '',
            translation: 'CONFIGURATION.THEMES.DETAILS.GENERAL.CAPTION'
        },
        {
            name: 'bumperForm',
            tabTranslation: '',
            translation: 'CONFIGURATION.THEMES.DETAILS.BUMPER.CAPTION'
        },
        {
            name: 'trailerForm',
            tabTranslation: '',
            translation: 'CONFIGURATION.THEMES.DETAILS.TRAILER.CAPTION'
        },
        {
            name: 'titleSlideForm',
            tabTranslation: '',
            translation: 'CONFIGURATION.THEMES.DETAILS.TITLE.CAPTION'
        },
        {
            name: 'watermarkForm',
            tabTranslation: '',
            translation: 'CONFIGURATION.THEMES.DETAILS.WATERMARK.CAPTION'
        },
        {
            name: 'usage',
            tabTranslation: '',
            // todo: check for actual translation
            translation: 'CONFIGURATION.THEMES.DETAILS.SUMMARY.CAPTION'
        }
    ];

    // Validation schema of current page
    const currentValidationSchema = NewThemeSchema[page];

    const handleSubmit = values => {
        console.log("to be implemented");
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

            {/* Initialize overall form */}
            <Formik initialValues={snapshot}
                    validationSchema={currentValidationSchema}
                    onSubmit={values => handleSubmit(values)}>
                {/* Render modal pages depending on current value of page variable */}
                {formik => (
                    <div>
                        {page === 0 && (
                            <GeneralPage formik={formik}/>
                        )}
                        {page === 1 && (
                            <BumperPage formik={formik}/>
                        )}
                        {page === 2 && (
                            <BumperPage formik={formik}
                                        isTrailer />
                        )}
                        {page === 3 && (
                            <TitleSlidePage formik={formik}/>
                        )}
                        {page === 4 && (
                            <WatermarkPage formik={formik}/>
                        )}
                        {page === 5 && (
                            <UsagePage />
                        )}
                        {/*todo: add buttons*/}
                    </div>
                )}
            </Formik>
        </>
    )
};

// get current state out of redux store
const mapStateToProps = state => ({
    themeDetails: getThemeDetails(state),
});

const mapDispatchToProps = dispatch => ({
    // todo: update Theme thunk
});

export default connect(mapStateToProps, mapDispatchToProps)(ThemeDetails);
