import React, {useState} from "react";
import {Formik} from "formik";
import NewThemePage from "./NewThemePage";
import NewSeriesSummary from "./NewSeriesSummary";
import {getSeriesMetadata} from "../../../../selectors/seriesSeletctor";
import {connect} from "react-redux";
import NewMetadataPage from "./NewMetadataPage";
import NewMetadataExtendedPage from "./NewMetadataExtendedPage";
import NewAccessPage from "./NewAccessPage";
import {postNewSeries} from "../../../../thunks/seriesThunks";
import {NewSeriesSchema} from "../../../shared/wizard/validate";
import WizardStepper from "../../../shared/wizard/WizardStepper";
import {initialFormValuesNewSeries} from "../../../../configs/wizardConfig";


/**
 * This component manages the pages of the new series wizard and the submission of values
 */
const NewSeriesWizard = ({ metadataFields, close}) => {

    const initialValues = getInitialValues(metadataFields);

    const [page, setPage] = useState(0);
    const [snapshot, setSnapshot] = useState(initialValues);

    // Caption of steps used by Stepper
    const steps = [{
        translation: 'EVENTS.SERIES.NEW.METADATA.CAPTION',
        name: 'metadata'
    }, {
        translation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
        name: 'metadata-extended',
        hidden: true
    }, {
        translation: 'EVENTS.SERIES.NEW.ACCESS.CAPTION',
        name: 'access'
    }, {
        translation: 'EVENTS.SERIES.NEW.THEME.CAPTION',
        name: 'theme'
    }, {
            translation: 'EVENTS.SERIES.NEW.SUMMARY.CAPTION',
            name: 'summary'
    }];

    // Validation schema of current page
    const currentValidationSchema = NewSeriesSchema[page];

    const nextPage = values => {
        setSnapshot(values);
        if (steps[page + 1].hidden) {
            setPage(page + 2);
        } else {
            setPage(page + 1);
        }
    }

    const previousPage = (values, twoPagesBack) => {
        setSnapshot(values);
        // if previous page is hidden or not always shown, than go back two pages
        if (steps[page - 1].hidden || twoPagesBack) {
            setPage(page - 2);
        } else {
            setPage(page - 1);
        }
    }

    const handleSubmit = (values) => {
        const response = postNewSeries(values, metadataFields);
        close();
    }

    return (
        <>
            {/* Stepper that shows each step of wizard as header */}
            <WizardStepper steps={steps} page={page}/>

            {/* Initialize overall form */}
            <Formik initialValues={snapshot}
                    validationSchema={currentValidationSchema}
                    onSubmit={values => handleSubmit(values)}>
                {/* Render wizard pages depending on current value of page variable */}
                {formik => (
                    <div>
                        {page === 0 && (
                            <NewMetadataPage nextPage={nextPage}
                                             formik={formik}
                                             metadataFields={metadataFields}
                                             header={steps[page].translation}/>
                        )}
                        {page === 1 && (
                            <NewMetadataExtendedPage nextPage={nextPage}
                                                     previousPage={previousPage}
                                                     formik={formik} />
                        )}
                        {page === 2 && (
                            <NewAccessPage nextPage={nextPage}
                                           previousPage={previousPage}
                                           formik={formik}/>
                        )}
                        {page === 3 && (
                            <NewThemePage nextPage={nextPage}
                                          previousPage={previousPage}
                                          formik={formik}/>
                        )}
                        {page === 4 && (
                            <NewSeriesSummary previousPage={previousPage}
                                              formik={formik}
                                              metaDataExtendedHidden={steps[1].hidden}/>
                        )}
                    </div>
                )}
            </Formik>
        </>
    );
};

const getInitialValues = metadataFields => {
    // Transform metadata fields provided by backend (saved in redux)
    let initialValues = {};
    metadataFields.fields.forEach(field => {
        initialValues[field.id] = field.value;
    });

    // Add all initial form values known upfront listed in newSeriesConfig
    for (const [key, value] of Object.entries(initialFormValuesNewSeries)) {
        initialValues[key] = value;
    }

    return initialValues;
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    metadataFields: getSeriesMetadata(state)
});

export default connect(mapStateToProps)(NewSeriesWizard);
