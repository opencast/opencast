import React from "react";
import {useTranslation} from "react-i18next";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";

const StartTaskSummaryPage = ({ formik, previousPage }) => {
    const { t } = useTranslation();

    return (
        <>
            <div className="modal-content active">
                <div className="modal-body">
                    <div className="full-col">
                        <div className="obj list-obj">
                            <header>{t('BULK_ACTIONS.SCHEDULE_TASK.SUMMARY.CAPTION')}</header>
                            <div className="obj-container">
                                <ul>
                                    <li>
                                        <span>{t('BULK_ACTIONS.SCHEDULE_TASK.SUMMARY.EVENTS')}</span>
                                        <p>{t('BULK_ACTIONS.SCHEDULE_TASK.SUMMARY.EVENTS_SUMMARY',
                                            {numberOfEvents: formik.values.events.filter(e => e.selected === true).length})}</p>
                                    </li>
                                    <li>
                                        <span>{t('BULK_ACTIONS.SCHEDULE_TASK.SUMMARY.WORKFLOW')}</span>
                                        <p>{formik.values.workflow}</p>
                                    </li>
                                    {/* todo: implement when backend is updated*/}
                                    <li>
                                        <span>{t('BULK_ACTIONS.SCHEDULE_TASK.SUMMARY.CONFIGURATION')}</span>
                                        <p>To be implemented</p>
                                    </li>
                                </ul>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <WizardNavigationButtons isLast
                                     previousPage={previousPage}
                                     formik={formik}/>
        </>
    );
};

export default StartTaskSummaryPage;
