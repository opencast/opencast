import React, {useState} from "react";
import {Formik} from "formik";
import NewEventSummary from "./NewEventSummary";
import {getEventMetadata} from "../../../../selectors/eventSelectors";
import {connect} from "react-redux";
import {MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import {getCurrentLanguageInformation} from "../../../../utils/utils";
import NewAssetUploadPage from "../ModalTabsAndPages/NewAssetUploadPage";
import NewMetadataExtendedPage from "../ModalTabsAndPages/NewMetadataExtendedPage";
import {postNewEvent} from "../../../../thunks/eventThunks";
import NewMetadataPage from "../ModalTabsAndPages/NewMetadataPage";
import NewAccessPage from "../ModalTabsAndPages/NewAccessPage";
import NewProcessingPage from "../ModalTabsAndPages/NewProcessingPage";
import NewSourcePage from "../ModalTabsAndPages/NewSourcePage";
import {sourceMetadata, uploadAssetOptions} from "../../../../configs/sourceConfig";
import {initialFormValuesNewEvents} from "../../../../configs/modalConfig";
import {NewEventSchema} from "../../../../utils/validate";
import {logger} from "../../../../utils/logger";
import WizardStepperEvent from '../../../shared/wizard/WizardStepperEvent';


// Get info about the current language and its date locale
const currentLanguage = getCurrentLanguageInformation();

/**
 * This component manages the pages of the new event wizard and the submission of values
 */
const NewEventWizard = ({ metadataFields, close, postNewEvent }) => {

    const initialValues = getInitialValues(metadataFields);
    let workflowPanelRef = React.useRef();


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

    const handleSubmit = (values) => {
        workflowPanelRef.current?.submitForm();
        const response = postNewEvent(values, metadataFields);
        logger.info(response);
        close();
    }

    return (
        <>
            {/* Initialize overall form */}
            <MuiPickersUtilsProvider  utils={DateFnsUtils} locale={currentLanguage.dateLocale}>
                <Formik initialValues={snapshot}
                        validationSchema={currentValidationSchema}
                        onSubmit={values => handleSubmit(values)}>
                    {/* Render wizard pages depending on current value of page variable */}
                    {formik => (
                      <>
                          {/* Stepper that shows each step of wizard as header */}
                          <WizardStepperEvent steps={steps}
                                              page={page}
                                              setPage={setPage}
                                              formik={formik}/>
                          <div>
                              {page === 0 && (
                                <NewMetadataPage nextPage={nextPage}
                                                 formik={formik}
                                                 metadataFields={metadataFields}
                                                 header={steps[page].translation} />
                              )}
                              {page === 1 && (
                                // todo: finish implementation when information about endpoints and structure are gathered
                                <NewMetadataExtendedPage previousPage={previousPage}
                                                         nextPage={nextPage}
                                                         formik={formik}
                                                         header={steps[page].translation} />
                              )}
                              {page === 2 && (
                                <NewSourcePage previousPage={previousPage}
                                               nextPage={nextPage}
                                               formik={formik} />
                              )}
                              {page === 3  && (
                                <NewAssetUploadPage previousPage={previousPage}
                                                    nextPage={nextPage}
                                                    formik={formik} />
                              )}
                              {page === 4 && (
                                <NewProcessingPage previousPage={previousPage}
                                                   nextPage={nextPage}
                                                   workflowPanelRef={workflowPanelRef}
                                                   formik={formik} />
                              )}
                              {page === 5 && (
                                <NewAccessPage previousPage={previousPage}
                                               nextPage={nextPage}
                                               formik={formik}
                                               editAccessRole='ROLE_UI_SERIES_DETAILS_ACL_EDIT' />
                              )}
                              {page === 6 && (
                                <NewEventSummary previousPage={previousPage}
                                                 formik={formik}
                                                 metaDataExtendedHidden={steps[1].hidden}
                                                 assetUploadHidden={steps[3].hidden} />
                              )}
                          </div>
                      </>
                    )}
                </Formik>
            </MuiPickersUtilsProvider>
        </>

    );
};

// Transform all initial values needed from information provided by backend
const getInitialValues = metadataFields => {
    // Transform metadata fields provided by backend (saved in redux)
    let initialValues = {};

    if (!!metadataFields.fields && metadataFields.fields.length > 0) {
        metadataFields.fields.forEach(field => {
            initialValues[field.id] = field.value;
        });
    }

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
        initialValues.uploadAssetsTrack = [];
        // initial value of upload asset needs to be null, because object (file) is saved there
        uploadAssetOptions.forEach(option => {
            if (option.type === 'track') {
                initialValues.uploadAssetsTrack.push({
                    ...option,
                    file: null
                });
            } else {
                initialValues[option.id] = null;
            }

        });
    }

    // Add all initial form values known upfront listed in newEventsConfig
    for (const [key, value] of Object.entries(initialFormValuesNewEvents)) {
        initialValues[key] = value;
    }

    const defaultDate = new Date();

    // fill times with some default values
    initialValues['scheduleStartTimeHour'] = (defaultDate.getHours() + 1).toString();
    initialValues['scheduleStartTimeMinutes'] = '00';
    initialValues['scheduleDurationHour'] = '00';
    initialValues['scheduleDurationMinutes'] = '55';
    initialValues['scheduleEndTimeHour'] = (defaultDate.getHours() + 1).toString();
    initialValues['scheduleEndTimeMinutes'] = '55';

    return initialValues;
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    metadataFields: getEventMetadata(state)
});

const mapDispatchToProps = dispatch => ({
    postNewEvent: (values, metadataFields) => dispatch(postNewEvent(values, metadataFields))
});


export default connect(mapStateToProps, mapDispatchToProps)(NewEventWizard);
