import React, {useState} from "react";
import {Formik} from "formik";
import {useTranslation} from "react-i18next";
import {initialFormValuesStartTask} from "../../../configs/wizardConfig";
import {StartTaskSchema} from "../../shared/wizard/validate";
import WizardStepper from "../../shared/wizard/WizardStepper";
import StartTaskGeneralPage from "./wizards/StartTaskGeneralPage";
import StartTaskWorkflowPage from "./wizards/StartTaskWorkflowPage";
import StartTaskSummaryPage from "./wizards/StartTaskSummaryPage";

const StartTaskModal = ({ close }) => {
    const { t } = useTranslation();

    const initialValues = initialFormValuesStartTask;

    const [page, setPage] = useState(0);
    const [snapshot, setSnapshot] = useState(initialValues);

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

    const currentValidationSchema = StartTaskSchema[page];

    const handleSubmit = values => {
        console.log('to be implemented');
        close();
    }

    return (
        <section className="modal wizard modal-animation">
            <header>
                <a className="fa fa-times close-modal" onClick={() => close()}/>
                <h2>{t('BULK_ACTIONS.SCHEDULE_TASK.CAPTION')}</h2>
            </header>

            {/* Stepper that shows each step of wizard as header */}
            <WizardStepper steps={steps} page={page}/>

            {/* Initialize overall form */}
            <Formik initialValues={snapshot}
                    //validationSchema={currentValidationSchema}
                    onSubmit={values => handleSubmit(values)}>
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
    );
};

export default StartTaskModal;
