import React from "react";
import {Formik} from "formik";
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import {initialFormValuesStartTask} from "../../../../configs/modalConfig";
import WizardStepper from "../../../shared/wizard/WizardStepper";
import StartTaskGeneralPage from "../ModalTabsAndPages/StartTaskGeneralPage";
import StartTaskWorkflowPage from "../ModalTabsAndPages/StartTaskWorkflowPage";
import StartTaskSummaryPage from "../ModalTabsAndPages/StartTaskSummaryPage";
import {postTasks} from "../../../../thunks/taskThunks";
import {usePageFunctions} from "../../../../hooks/wizardHooks";

/**
 * This component manages the pages of the task start bulk action
 */
const StartTaskModal = ({ close, postTasks }) => {
    const { t } = useTranslation();

    const initialValues = initialFormValuesStartTask;

    const [snapshot, page, nextPage, previousPage] = usePageFunctions(0, initialValues);

    const steps = [
        {
            translation: 'BULK_ACTIONS.SCHEDULE_TASK.GENERAL.CAPTION',
            name: 'general'
        },
        {
            translation: 'BULK_ACTIONS.SCHEDULE_TASK.TASKS.CAPTION',
            name: 'tasks'
        },
        {
            translation: 'BULK_ACTIONS.SCHEDULE_TASK.SUMMARY.CAPTION',
            name: 'summary'
        }
    ];

    const handleSubmit = values => {
        postTasks(values);
        close();
    };

    return (
        <>
            <div className="modal-animation modal-overlay"/>
            <section className="modal wizard modal-animation">
                <header>
                    <a className="fa fa-times close-modal" onClick={() => close()}/>
                    <h2>{t('BULK_ACTIONS.SCHEDULE_TASK.CAPTION')}</h2>
                </header>

                {/* Stepper that shows each step of wizard as header */}
                <WizardStepper steps={steps}
                               page={page}
                               setPage={nextPage}
                               completed={false}
                               setCompleted={() => {}}
                               formik={{
                                   values: {acls: []},
                                   isValid: false,
                                   dirty: false
                               }}/>

                {/* Initialize overall form */}
                <Formik initialValues={snapshot}
                        onSubmit={values => handleSubmit(values)}>
                    {/* Render wizard pages depending on current value of page variable */}
                    {formik => (
                        <div>
                            {page === 0 && (
                                <StartTaskGeneralPage formik={formik}
                                                      nextPage={nextPage}/>
                            )}
                            {page === 1 && (
                                <StartTaskWorkflowPage formik={formik}
                                                       nextPage={nextPage}
                                                       previousPage={previousPage}/>
                            )}
                            {page === 2 && (
                                <StartTaskSummaryPage formik={formik}
                                                      previousPage={previousPage}/>
                            )}
                        </div>
                    )}
                </Formik>

            </section>
        </>
    );
};

const mapDispatchToState = dispatch => ({
    postTasks: values => dispatch(postTasks(values))
});

export default connect(null, mapDispatchToState)(StartTaskModal);
