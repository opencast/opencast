import React, {useEffect, useState} from "react";
import {Formik} from "formik";
import {useTranslation} from "react-i18next";
import {initialFormValuesEditScheduledEvents} from "../../../../configs/modalConfig";
import WizardStepper from "../../../shared/wizard/WizardStepper";
import EditScheduledEventsGeneralPage from "../ModalTabsAndPages/EditScheduledEventsGeneralPage";
import EditScheduledEventsEditPage from "../ModalTabsAndPages/EditScheduledEventsEditPage";
import EditScheduledEventsSummaryPage from "../ModalTabsAndPages/EditScheduledEventsSummaryPage";
import {checkForSchedulingConflicts, updateScheduledEventsBulk} from "../../../../thunks/eventThunks";
import {connect} from "react-redux";
import {usePageFunctions} from "../../../../hooks/wizardHooks";
import {logger} from "../../../../utils/logger";
import {fetchRecordings} from "../../../../thunks/recordingThunks";
import {getRecordings} from "../../../../selectors/recordingSelectors";
import {getUserInformation} from "../../../../selectors/userInfoSelectors";
import {filterDevicesForAccess} from "../../../../utils/resourceUtils";
import {checkSchedulingConflicts, checkValidityUpdateScheduleEventSelection} from "../../../../utils/bulkActionUtils";
import {addNotification} from "../../../../thunks/notificationThunks";

/**
 * This component manages the pages of the edit scheduled bulk action
 */
const EditScheduledEventsModal = ({ close, updateScheduledEventsBulk, loadingInputDevices,
                                         checkForSchedulingConflicts, addNotification, inputDevices, user }) => {
    const { t } = useTranslation();

    const initialValues = initialFormValuesEditScheduledEvents;

    const [snapshot, page, nextPage, previousPage, setPage, pageCompleted, setPageCompleted] = usePageFunctions(0, initialValues);

    // for edit page: conflicts with other events
    const [conflicts, setConflicts] = useState([]);

    useEffect(() => {
        // Load recordings that can be used for input
        loadingInputDevices();
    }, []);

    const steps = [
        {
            translation: 'BULK_ACTIONS.EDIT_EVENTS.GENERAL.CAPTION',
            name: 'general'
        },
        {
            translation: 'BULK_ACTIONS.EDIT_EVENTS.EDIT.CAPTION',
            name: 'edit'
        },
        {
            translation: 'BULK_ACTIONS.EDIT_EVENTS.SUMMARY.CAPTION',
            name: 'summary'
        }
    ];

    const validateFormik = values => {
        const errors = {};
        if (!checkValidityUpdateScheduleEventSelection(values, user)) {
            errors.events = 'Not all events editable!';
        }
        if (steps[page].name !== 'general') {
            return checkSchedulingConflicts(values, setConflicts, checkForSchedulingConflicts, addNotification).then(result => {
                const errors = {};
                if (!result) {
                    errors.editedEvents = 'Scheduling conflicts exist!';
                }
                return errors;
            });
        } else {
            return errors;
        }
    };

    const handleSubmit = values => {
        // Only update events if there are changes
        if (values.changedEvents.length > 0) {
            const response = updateScheduledEventsBulk(values);
            logger.info(response);
        }
        close();
    };

    return (
        <>
            <div className="modal-animation modal-overlay"/>
            <section className="modal wizard modal-animation">
                <header>
                    <a className="fa fa-times close-modal" onClick={() => close()}/>
                    <h2>{t('BULK_ACTIONS.EDIT_EVENTS.CAPTION')}</h2>
                </header>

                {/* Initialize overall form */}
                <Formik initialValues={snapshot}
                        validate={values => validateFormik(values)}
                        onSubmit={values => handleSubmit(values)}>
                    {/* Render wizard pages depending on current value of page variable */}
                    {formik => {

                        // eslint-disable-next-line react-hooks/rules-of-hooks
                        useEffect(() => {
                            formik.validateForm().then();
                        }, [page]);

                        return (
                            <>
                                {/* Stepper that shows each step of wizard as header */}
                                <WizardStepper steps={steps}
                                               page={page}
                                               setPage={setPage}
                                               completed={pageCompleted}
                                               setCompleted={setPageCompleted}
                                               formik={formik}/>
                                <div>
                                    {page === 0 && (
                                        <EditScheduledEventsGeneralPage formik={formik}
                                                                        nextPage={nextPage}/>
                                    )}
                                    {page === 1 && (
                                        <EditScheduledEventsEditPage formik={formik}
                                                                     nextPage={nextPage}
                                                                     previousPage={previousPage}
                                                                     conflictState={{conflicts, setConflicts}}
                                                                     inputDevices={filterDevicesForAccess(user, inputDevices)}
                                                                     setPageCompleted={setPageCompleted}/>
                                    )}
                                    {page === 2 && (
                                        <EditScheduledEventsSummaryPage formik={formik}
                                                                        previousPage={previousPage}/>
                                    )}
                                </div>
                            </>
                        );
                    }}
                </Formik>
            </section>
        </>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    inputDevices: getRecordings(state),
    user: getUserInformation(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    checkForSchedulingConflicts: events => dispatch(checkForSchedulingConflicts(events)),
    addNotification: (type, key, duration, parameter, context) => dispatch(addNotification(type, key, duration, parameter, context)),
    updateScheduledEventsBulk: values => dispatch(updateScheduledEventsBulk(values)),
    loadingInputDevices: () => dispatch(fetchRecordings("inputs")),
});

export default connect(mapStateToProps, mapDispatchToProps)(EditScheduledEventsModal);
