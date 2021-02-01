import React, {useState} from "react";
import PropTypes from 'prop-types';
import {Formik} from "formik";
import NewEventMetadata from "./NewEventMetadata";
import NewEventSource from "./NewEventSource";
import NewEventProcessing from "./NewEventProcessing";
import NewEventAccess from "./NewEventAccess";
import NewEventSummary from "./NewEventSummary";
import {getEventMetadata} from "../../../../selectors/eventSelectors";
import {connect} from "react-redux";
import cn from 'classnames';
import {NewEventSchema} from "./validate";
import {makeStyles, Step, StepLabel, Stepper,} from '@material-ui/core';
import {FaCircle, FaDotCircle} from "react-icons/all";
import {useTranslation} from "react-i18next";
import {sourceMetadata, uploadAssetOptions} from "../../../../configs/newEventConfigs/sourceConfig";
import {initialFormValuesNewEvents} from "../../../../configs/newEventConfigs/newEventWizardConfig";
import {MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import {getCurrentLanguageInformation} from "../../../../utils/utils";
import NewEventAssetUpload from "./NewEventAssetUpload";
import NewEventMetadataExtended from "./NewEventMetadataExtended";

// Base style for Stepper component
const useStepperStyle = makeStyles((theme) => ({
    root: {
        background: '#eeeff0',
        height: '100px'
    },
}));

// Style of icons used in Stepper
const useStepIconStyles = makeStyles({
    root: {
        height: 22,
        alignItems: 'center',
    },
    circle: {
        color: '#92a0ab',
        width: '20px',
        height: '20px'
    },
});

// Get info about the current language and its date locale
const currentLanguage = getCurrentLanguageInformation();

/**
 * This component manages the pages of the new event wizard and the submission of values
 */
const NewEventWizard = ({onSubmit, metadataFields}) => {
    const {t} = useTranslation();

    const classes = useStepperStyle();

    const initialValues = getInitialValues(metadataFields);


    const [page, setPage] = useState(0);
    const [snapshot, setSnapshot] = useState(initialValues);

    // Caption of steps used by Stepper
    const steps = [
        {
            translation: 'EVENTS.EVENTS.NEW.METADATA.CAPTION',
            name: 'metadata'
        },
        {
            translation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
            name: 'metadata-extended',
            hidden: true
        },
        {
            translation: 'EVENTS.EVENTS.NEW.SOURCE.CAPTION',
            name: 'source'
        },
        {
            translation: 'EVENTS.EVENTS.NEW.UPLOAD_ASSET.CAPTION',
            name: 'upload-asset',
            hidden: false
        },
        {
            translation: 'EVENTS.EVENTS.NEW.PROCESSING.CAPTION',
            name: 'processing'
        },
        {
            translation: 'EVENTS.EVENTS.NEW.ACCESS.CAPTION',
            name: 'access'
        },
        {
            translation: 'EVENTS.EVENTS.NEW.SUMMARY.CAPTION',
            name: 'summary'
        }
    ];

    // Validation schema of current page
    const currentValidationSchema = NewEventSchema[page];

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

    //todo: implement
    const handleSubmit = values => {
        console.log("To be implemented!!!");
        console.log(values);
    }

    return (
        <>
            {/* Stepper that shows each step of wizard as header */}
            <Stepper activeStep={page} alternativeLabel connector={false} className={cn("step-by-step", classes.root)}>
                {steps.map(label => (
                    !label.hidden ? (
                        <Step key={label.translation}>
                            <StepLabel StepIconComponent={CustomStepIcon}>{t(label.translation)}</StepLabel>
                        </Step>
                    ) : null
                ))}
            </Stepper>
            {/* Initialize overall form */}
            <MuiPickersUtilsProvider  utils={DateFnsUtils} locale={currentLanguage.dateLocale}>
                <Formik initialValues={snapshot}
                        validationSchema={currentValidationSchema}
                        onSubmit={handleSubmit}>
                    {/* Render wizard pages depending on current value of page variable */}
                    {formik => (
                        <div>
                            {page === 0 && <NewEventMetadata nextPage={nextPage}
                                                             formik={formik}
                                                             onSubmit={() => console.log(formik.values)}/>}
                            {page === 1 && (
                                <NewEventMetadataExtended previousPage={previousPage}
                                                          nextPage={nextPage}
                                                          formik={formik}
                                                          onSubmit={() => console.log("Step Metadata Extended onSubmit")}/>
                            )}
                            {page === 2 && (
                                <NewEventSource previousPage={previousPage}
                                                nextPage={nextPage}
                                                formik={formik}
                                                onSubmit={() => console.log(formik.values)}/>
                            )}
                            {page === 3  && (
                                <NewEventAssetUpload previousPage={previousPage}
                                                     nextPage={nextPage}
                                                     formik={formik}
                                                     onSubmit={() => console.log("Step Asset onSubmit")} />
                            )}
                            {page === 4 && (
                                <NewEventProcessing previousPage={previousPage}
                                                    nextPage={nextPage}
                                                    formik={formik}
                                                    onSubmit={() => console.log("Step Processing onSubmit")}/>
                            )}
                            {page === 5 && (
                                <NewEventAccess previousPage={previousPage}
                                                nextPage={nextPage}
                                                formik={formik}
                                                onSubmit={() => console.log("Step Access onSubmit")}/>
                            )}
                            {page === 6 && (
                                <NewEventSummary previousPage={previousPage}
                                                 nextPage={handleSubmit}
                                                 formik={formik}
                                                 metaDataExtendedHidden={steps[1].hidden}
                                                 assetUploadHidden={steps[3].hidden}
                                                 onSubmit={() => console.log("Step Summary onSubmit")}/>
                            )}
                        </div>
                    )}
                </Formik>
            </MuiPickersUtilsProvider>
        </>

    )
}

// Component that renders icons of Stepper depending on completeness of steps
const CustomStepIcon = (props) => {
    const classes = useStepIconStyles();
    const { completed } = props;

    return (
        <div className={cn(classes.root)}>
            {completed ? <FaCircle className={classes.circle}/> : <FaDotCircle className={classes.circle}/>}
        </div>
    )
}

// Transform all initial values needed from information provided by backend
const getInitialValues = metadataFields => {
    // Transform metadata fields provided by backend (saved in redux)
    let initialValues = {};
    metadataFields.fields.forEach(field => {
        initialValues[field.id] = field.value;
    });

    // Transform additional metadata for source (provided by constant in newEventConfig)
    if (!!sourceMetadata.UPLOAD) {
        sourceMetadata.UPLOAD.metadata.forEach(field => {
            initialValues[field.id] = field.value;
        });
    }
    if (!!sourceMetadata.SINGLE_SCHEDULE) {
        sourceMetadata.SINGLE_SCHEDULE.metadata.forEach(field => {
            initialValues[field.id] = field.value;
        });
    }
    if (!!sourceMetadata.MULTIPLE_SCHEDULE) {
        sourceMetadata.MULTIPLE_SCHEDULE.metadata.forEach(field => {
            initialValues[field.id] = field.value;
        });
    }

    // Add possible files that can be uploaded in source step
    // Todo: exchange uploadAssetOptions with Function for getting these options
    if (!!uploadAssetOptions) {
        uploadAssetOptions.forEach(option => {
            // needs to be null, because it will save a object (file) not string or int
            initialValues[option.id] = null;
        });
    }

    // Add all initial form values known upfront listed in newEventsConfig
    for (const [key, value] of Object.entries(initialFormValuesNewEvents)) {
        initialValues[key] = value;
    }
    console.log("INITIAL VALUES NEW EVENTS:");
    console.log(initialValues);

    return initialValues;
}

// Prop types of CustomStepIcon component
CustomStepIcon.propTypes = {
    active: PropTypes.bool,
    completed: PropTypes.bool
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    metadataFields: getEventMetadata(state)
});

export default connect(mapStateToProps)(NewEventWizard);
