import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import RenderWorkflowConfig from "./RenderWorkflowConfig";
import {fetchWorkflowDef} from "../../../../thunks/workflowThunks";
import {getWorkflowDef} from "../../../../selectors/workflowSelectors";
import {connect} from "react-redux";
import cn from 'classnames';

const StartTaskWorkflowPage = ({ formik, previousPage, nextPage, loadingWorkflowDef, workflowDef }) => {
    const { t } = useTranslation();

    const [selectedWorkflow, setSelectedWorkflow] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Load workflow definitions for selecting
        loadingWorkflowDef();
        setLoading(false);
        console.log('loading workflows');
        console.log('current workflowDef');
        console.log(workflowDef);
    }, []);

    const handleChange = e => {
        const workflowId = e.target.value;
        formik.setFieldValue('workflow', workflowId);
        const chosenWorkflow = workflowDef.find(workflow => workflow.id === workflowId);
        setSelectedWorkflow(chosenWorkflow);
    }

    const descriptionBoxStyle = {
        margin: '15px 0 0 0',
        position: 'relative',
        border: 'solid #c9d0d3',
        borderWidth: '1px',
        backgroundColor: '#fafafa',
        overflow: 'hidden',
        borderRadius: '4px'
    };

    const descriptionTextStyle = {
        margin: '10px',
        fontFamily: '"Open sans", Helvetica,sans-serif',
        fontSize: '12px',
        whiteSpace: 'pre-line'
    };
    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">

                        <div className="obj list-obj">
                            <header>{t('BULK_ACTIONS.SCHEDULE_TASK.TASKS.SELECT')}</header>
                            <div className="obj-container">
                                {!loading && (
                                    workflowDef.length > 0 && (
                                        <select tabIndex="99"
                                                name="workflow"
                                                onChange={e => handleChange(e)}
                                                placeholder={t('EVENTS.EVENTS.DETAILS.PUBLICATIONS.SELECT_WORKFLOW')}
                                                style={{width: '100%'}}>
                                            <option value="" />
                                            {workflowDef.map((workflow, key)=> (
                                                <option key={key} value={workflow.id}>{workflow.title}</option>
                                            ))}
                                        </select>
                                    )
                                )}


                                {formik.values.workflow  && (
                                    <>
                                        {selectedWorkflow.description.length > 0 && (
                                            <div className="collapsible-box" style={descriptionBoxStyle}>
                                                <div style={descriptionTextStyle}>{selectedWorkflow.description}</div>
                                            </div>
                                        )}
                                        {/* Configuration panel of selected workflow */}
                                        {/*Todo: Needs to be implemented after adjustments in definition files done*/}
                                        <div id="new-event-workflow-configuration"
                                             className="checkbox-container obj-container">
                                            <RenderWorkflowConfig workflowId={formik.values.workflow} />
                                        </div>
                                    </>
                                )}
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
                                active: (formik.values.workflow && formik.isValid),
                                inactive: !(formik.values.workflow && formik.isValid)
                            })}
                        disabled={!(formik.values.workflow && formik.isValid)}
                        onClick={() => {
                            nextPage(formik.values);
                        }}
                        tabIndex="100">{t('WIZARD.NEXT_STEP')}</button>
                <button className="cancel"
                        onClick={() => previousPage()}
                        tabIndex="101">{t('WIZARD.BACK')}</button>
            </footer>

            <div className="btm-spacer"/>
        </>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    workflowDef: getWorkflowDef(state)
});

const mapDispatchToProps = dispatch => ({
    loadingWorkflowDef: () => dispatch(fetchWorkflowDef('tasks'))
});

export default connect(mapStateToProps, mapDispatchToProps)(StartTaskWorkflowPage);
