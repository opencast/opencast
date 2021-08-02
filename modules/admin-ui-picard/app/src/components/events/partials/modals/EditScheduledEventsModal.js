import React, {useState} from "react";
import {Formik} from "formik";
import {useTranslation} from "react-i18next";
import {initialFormValuesEditScheduledEvents} from "../../../../configs/modalConfig";
import WizardStepper from "../../../shared/wizard/WizardStepper";
import EditScheduledEventsGeneralPage from "../ModalTabsAndPages/EditScheduledEventsGeneralPage";
import EditScheduledEventsEditPage from "../ModalTabsAndPages/EditScheduledEventsEditPage";
import EditScheduledEventsSummaryPage from "../ModalTabsAndPages/EditScheduledEventsSummaryPage";
import {updateScheduledEventsBulk} from "../../../../thunks/eventThunks";
import {connect} from "react-redux";
import {usePageFunctions} from "../../../../hooks/wizardHooks";

/**
 * This component manages the pages of the edit scheduled bulk action
 */
const EditScheduledEventsModal = ({ close, updateScheduledEventsBulk }) => {
    const { t } = useTranslation();

    const initialValues = initialFormValuesEditScheduledEvents;

    const [snapshot, page, nextPage, previousPage] = usePageFunctions(0, initialValues);

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

    const handleSubmit = values => {
        // Only update events if there are changes
        if (values.changedEvents.length > 0) {
            const response = updateScheduledEventsBulk(values);
            console.log(response);
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

                {/* Stepper that shows each step of wizard as header */}
                <WizardStepper steps={steps} page={page}/>

                {/* Initialize overall form */}
                <Formik initialValues={snapshot}
                        onSubmit={values => handleSubmit(values)}>
                    {/* Render wizard pages depending on current value of page variable */}
                    {formik => (
                        <div>
                            {page === 0 && (
                                <EditScheduledEventsGeneralPage formik={formik}
                                                                nextPage={nextPage}/>
                            )}
                            {page === 1 && (
                                <EditScheduledEventsEditPage formik={formik}
                                                             nextPage={nextPage}
                                                             previousPage={previousPage}/>
                            )}
                            {page === 2 && (
                                <EditScheduledEventsSummaryPage formik={formik}
                                                                previousPage={previousPage}/>
                            )}
                        </div>
                    )}
                </Formik>
            </section>
        </>
    );
};

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    updateScheduledEventsBulk: values => dispatch(updateScheduledEventsBulk(values))
});

export default connect(null, mapDispatchToProps)(EditScheduledEventsModal);
