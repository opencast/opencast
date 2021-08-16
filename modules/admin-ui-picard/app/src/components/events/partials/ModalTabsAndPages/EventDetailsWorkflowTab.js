import React, {useEffect} from "react";
import {connect} from "react-redux";
import {Field, Formik} from "formik";
import {
    fetchWorkflows

} from "../../../../thunks/eventDetailsThunks";
import {
    getWorkflowConfiguration,
    getWorkflows,
    isFetchingWorkflows,
    getWorkflowDefinitions
} from "../../../../selectors/eventDetailsSelectors";
import Notifications from "../../../shared/Notifications";
import RenderWorkflowConfig from "../wizards/RenderWorkflowConfig";

/**
 * This component manages the workflows tab of the event details modal
 */
const EventDetailsWorkflowTab = ({ eventId, header, t,
                                   workflows, isLoading, workflowDefinitions, workflowConfiguration,
                                   loadWorkflows}) => {
    const isRoleWorkflowEdit = true; /*todo: if: "$root.userIs('ROLE_UI_EVENTS_DETAILS_WORKFLOWS_EDIT')"*/
    const isRoleWorkflowDelete = true; /*todo: if: "$root.userIs('ROLE_UI_EVENTS_DETAILS_WORKFLOWS_DELETE')"*/


    useEffect(() => {
        loadWorkflows(eventId).then(r => {});
    }, [])

    const hasCurrentAgentAccess= () => {
        return true;
    }
    
    const isCurrentWorkflow = (workflowId) => {
        return false;
    }

    const workflowAction = (workflowId, action) => {
        console.log(`Perform action ${action} on workflow ${workflowId}!`);
    }

    const changeWorkflow = (someBoolean, value, changeFormikValue) => {
        console.log(`Changing Workflow ${someBoolean}!`);
        changeFormikValue('workflowDefinition', value);
    }

    const deleteWorkflow = (workflowId) => {
        console.log(`Deleting workflow ${workflowId}!`);
    }

    const openSubTab = (tabType, resourceType, id, someBool=false) => {
        console.log(`Open Sub Tab ${tabType}, res: ${resourceType}, id: ${id}, ${someBool}`);
    }

    return (
        <div className="modal-content" data-modal-tab-content="workflows">
            <div className="modal-body">
                <div className="full-col">
                    {/* Notifications */}
                    <Notifications context="not_corner"/>

                    <ul>
                        <li>
                            {workflows.scheduling || (
                                <div className="obj tbl-container">
                                    <header>
                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOW_INSTANCES.TITLE") /* Workflow instances */}
                                    </header>
                                    <div className="obj-container">
                                        <table className="main-tbl">
                                            <thead>
                                                <tr>
                                                    <th>
                                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.ID") /* ID */}
                                                    </th>
                                                    <th>
                                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.TITLE") /* Title */}
                                                    </th>
                                                    <th>
                                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.SUBMITTER") /* Submitter */}
                                                    </th>
                                                    <th>
                                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.SUBMITTED") /* Submitted */}
                                                    </th>
                                                    <th>
                                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.STATUS") /* Status */}
                                                    </th>
                                                    {isRoleWorkflowEdit && (
                                                        <th className="fit">
                                                            {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.ACTIONS") /* Actions */}
                                                        </th>
                                                    )}
                                                    <th className="medium"></th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {isLoading || workflows.entries.map((item, key) => ( /*orderBy:'submitted':true track by $index"*/
                                                    <tr key={key}>
                                                        <td>{item.id}</td>
                                                        <td>{item.title}</td>
                                                        <td>{item.submitter}</td>
                                                        <td>{t('dateFormats.dateTime.medium', {dateTime: new Date(item.submitted)})}</td>
                                                        <td>{t(item.status)}</td>
                                                        {isRoleWorkflowEdit  && (
                                                            <td >
                                                                {(item.status === 'EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.RUNNING') && (
                                                                    <a
                                                                       onClick={() => workflowAction(item.id, 'STOP')}
                                                                       className="stop fa-fw"
                                                                       title={t('EVENTS.EVENTS.DETAILS.WORKFLOWS.TOOLTIP.STOP')}
                                                                    >
                                                                        {/* STOP */}
                                                                    </a>
                                                                )}
                                                                {(item.status === 'EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.PAUSED') && (
                                                                    <a
                                                                       onClick={() => workflowAction(item.id, 'NONE')}
                                                                       className="fa fa-hand-stop-o fa-fw" style={{color:"red"}}
                                                                       title={t('EVENTS.EVENTS.DETAILS.WORKFLOWS.TOOLTIP.ABORT')}
                                                                    >
                                                                        {/* Abort */}
                                                                    </a>
                                                                )}
                                                                {(item.status === 'EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.PAUSED') && (
                                                                    <a
                                                                       onClick={() => workflowAction(item.id, 'RETRY')}
                                                                       className="fa fa-refresh fa-fw"
                                                                       title={t('EVENTS.EVENTS.DETAILS.WORKFLOWS.TOOLTIP.RETRY')}
                                                                    >
                                                                        {/* Retry */}
                                                                    </a>
                                                                )}
                                                               {( (item.status === 'EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.SUCCEEDED'
                                                                   || item.status === 'EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.FAILED'
                                                                   || item.status === 'EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.STOPPED')
                                                                 && !isCurrentWorkflow(item.id)
                                                                 && isRoleWorkflowDelete ) && (
                                                                   <a
                                                                      onClick={() => deleteWorkflow(item.id)}
                                                                      className="remove fa-fw"
                                                                      title={t('EVENTS.EVENTS.DETAILS.WORKFLOWS.TOOLTIP.DELETE')}
                                                                   >
                                                                        {/* DELETE */}
                                                                    </a>
                                                               )}
                                                            </td>
                                                        )}
                                                        <td>
                                                            <a className="details-link"
                                                               onClick={() => openSubTab('workflow-details', 'EventWorkflowDetailsResource', item.id)}
                                                            >
                                                                {t("EVENTS.EVENTS.DETAILS.MEDIA.DETAILS") /* Details */}
                                                            </a>
                                                        </td>
                                                    </tr>
                                                ))}
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            )}

                            {workflows.scheduling && (
                                <div className="obj list-obj">
                                    <header>
                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.CONFIGURATION") /* Workflow configuration */}
                                    </header>
                                    <div className="obj-container">
                                        <div className="obj list-obj quick-actions">

                                            <table className="main-tbl">
                                                <thead>
                                                    <tr>
                                                        <th>
                                                            {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.WORKFLOW") /*Select Workflow*/}
                                                        </th>
                                                    </tr>
                                                </thead>

                                                <tbody>
                                                    <tr>
                                                        <td>
                                                            <div className="obj-container padded">
                                                                {isLoading || (
                                                                    <>
                                                                        <Formik
                                                                            initialValues={{workflowDefinition: ""}}
                                                                            enableReinitialize
                                                                            onSubmit={(values, actions) =>
                                                                                {}
                                                                            }
                                                                        >
                                                                            {formik => (
                                                                                <Field className="chosen-single chosen-default"
                                                                                       data-width="'100%'"
                                                                                       style={{width: '360px'}}
                                                                                       name={"workflowDefinition"}
                                                                                       as="select"
                                                                                       onChange={r => changeWorkflow(false, r.target.value, formik.setFieldValue)}
                                                                                       disabled={!hasCurrentAgentAccess() || !isRoleWorkflowEdit}
                                                                                > {/*pre-select-from="workflowDefinitionIds"*/}
                                                                                    { (workflowDefinitions && workflowDefinitions.length > 0) && (
                                                                                        <>
                                                                                            <option value="" defaultValue hidden>
                                                                                                {t('EVENTS.EVENTS.NEW.PROCESSING.SELECT_WORKFLOW')}
                                                                                            </option>
                                                                                            { workflowDefinitions.map((workflowDef, key) => ( /*w.id as w.title for w in workflowDefinitions | orderBy: 'displayOrder':true*/
                                                                                                <option value={workflowDef.id}
                                                                                                        key={key}
                                                                                                >
                                                                                                    {workflowDef.id}
                                                                                                </option>
                                                                                            ))}
                                                                                        </>
                                                                                    )}
                                                                                    {(workflowDefinitions && workflowDefinitions.length > 0) || (
                                                                                        <option value="" defaultValue hidden>
                                                                                            {t('EVENTS.EVENTS.NEW.PROCESSING.SELECT_WORKFLOW_EMPTY')}
                                                                                        </option>
                                                                                    )}
                                                                                </Field>
                                                                            )}
                                                                        </Formik>
                                                                        <div className="obj-container padded">
                                                                        {workflows.workflow.description}
                                                                        </div>
                                                                    </>
                                                                )}
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>

                                        <div className="obj list-obj quick-actions">

                                            <table className="main-tbl">
                                                <thead>
                                                    <tr>
                                                        <th>
                                                            {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.CONFIGURATION") /* Configuration */}
                                                        </th>
                                                    </tr>
                                                </thead>

                                                <tbody>
                                                    <tr>
                                                        <td>
                                                            <div className="obj-container padded">
                                                                {(hasCurrentAgentAccess() && isRoleWorkflowEdit) && (
                                                                    <div id="event-workflow-configuration"
                                                                         className="checkbox-container obj-container"
                                                                    >{/* ng-bind-html="workflowConfiguration"*/}
                                                                        {/*<RenderWorkflowConfig workflowId={workflowConfiguration.id} />*/}
                                                                    </div>
                                                                )}
                                                                {workflowConfiguration.id || (
                                                                    <div>
                                                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.NO_CONFIGURATION") /* No config */}
                                                                    </div>
                                                                )}
                                                            </div>
                                                        </td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>
                                </div>
                            )}
                        </li>
                    </ul>
                </div>
            </div>
        </div>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    workflows: getWorkflows(state),
    isLoading: isFetchingWorkflows(state),
    workflowDefinitions: getWorkflowDefinitions(state),
    workflowConfiguration: getWorkflowConfiguration(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadWorkflows: (eventId) => dispatch(fetchWorkflows(eventId))
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsWorkflowTab);
