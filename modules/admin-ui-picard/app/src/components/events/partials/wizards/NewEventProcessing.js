import React, {useEffect} from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import {Field} from "formik";
import {connect} from "react-redux";
import {fetchWorkflowDef} from "../../../../thunks/workflowThunks";
import {getWorkflowDef} from "../../../../selectors/workflowSelectors";
import RenderWorkflowConfig from "./RenderWorkflowConfig";


const NewEventProcessing = ({ onSubmit, previousPage, nextPage, formik, loadingWorkflowDef, workflowDef }) => {
    const { t } = useTranslation();

    useEffect(() => {
        // Load workflow definitions for selecting
        loadingWorkflowDef();
    }, []);

    const previous = () => {
        // if not UPLOAD is chosen as source mode, then back to source page
        if (formik.values.sourceMode !== 'UPLOAD') {
            previousPage(formik.values, true);
        } else {
            previousPage(formik.values, false);
        }

    }

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <div className="obj quick-actions">
                            <header className="no-expand">
                                {t('EVENTS.EVENTS.NEW.PROCESSING.SELECT_WORKFLOW')}
                            </header>
                            <div className="obj-container padded">
                                {/*TODO: Add no-results-text*/}
                                <Field tabIndex="99"
                                       as="select"
                                       name="processingWorkflow"
                                       placeholder={t('EVENTS.EVENTS.NEW.PROCESSING.SELECT_WORKFLOW')}
                                       style={{width: '100%'}}>
                                    <option value="" />
                                    {workflowDef.map((workflow, key)=> (
                                        <option key={key} value={workflow.id}>{workflow.title}</option>
                                    ))}
                                </Field>
                                <div className="collapsible-box">
                                    <div id="new-event-workflow-configuration"
                                         className="checkbox-container obj-container">
                                        {formik.values.processingWorkflow ? (
                                            <RenderWorkflowConfig workflowId={formik.values.processingWorkflow} />
                                        ) : null}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Button for navigation to next page and previous page */}
            <footer>
                <button type="submit"
                        className={cn("submit",
                            {
                                active: (formik.touched.processingWorkflow && formik.isValid),
                                inactive: !(formik.touched.processingWorkflow && formik.isValid)
                            })}
                        disabled={!(formik.touched.processingWorkflow && formik.isValid)}
                        onClick={() => {
                            console.log("IN BUTTON OF PROCESSING");
                            console.log(formik.values);
                            console.log(formik);
                            nextPage(formik.values);
                            onSubmit();
                        }}
                        tabIndex="100">{t('WIZARD.NEXT_STEP')}</button>
                <button className="cancel"
                        onClick={() => previous()}
                        tabIndex="101">{t('WIZARD.BACK')}</button>
            </footer>

            <div className="btm-spacer"/>
        </>
    )
}

const mapStateToProps = state => ({
    workflowDef: getWorkflowDef(state)
});

const mapDispatchToProps = dispatch => ({
    loadingWorkflowDef: () => dispatch(fetchWorkflowDef())
})

export default connect(mapStateToProps, mapDispatchToProps)(NewEventProcessing);
