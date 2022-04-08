import React, {useEffect} from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import {Field} from "formik";
import {connect} from "react-redux";
import {fetchWorkflowDef} from "../../../../thunks/workflowThunks";
import {getWorkflowDef} from "../../../../selectors/workflowSelectors";
import RenderWorkflowConfig from "../wizards/RenderWorkflowConfig";

/**
 * This component renders the processing page for new events in the new event wizard.
 */
const NewProcessingPage = ({ previousPage, nextPage, formik, workflowPanelRef, loadingWorkflowDef, workflowDef }) => {
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

    const setDefaultValues = e => {
      let workflowId = e.target.value;
      let defaultConfiguration = {};
      // find configuration panel information about chosen workflow
      let configPanel = workflowDef.find(workflow => workflow.id === workflowId).configuration_panel;

      // only set default values if there is an configuration panel
      if (configPanel.length > 0) {
        // iterate through all config options and set their defaults
        configPanel.forEach(configOption => {
          if (configOption.fieldset) {
            defaultConfiguration = fillDefaultConfig(configOption.fieldset, defaultConfiguration);
          }
        });
      }

      // set default configuration in formik
      formik.setFieldValue("configuration", defaultConfiguration);
      // set chosen workflow in formik
      formik.setFieldValue("processingWorkflow", workflowId);
    }

    // fills default configuration with values
    const fillDefaultConfig = (fieldset, defaultConfiguration) => {
      // iteration through each input field
      fieldset.forEach(field => {
          // set value in default configuration
          if (field.type !== "radio") {
            defaultConfiguration[field.name] = field.value;
          } else {
            // set only the checked input of radio button as default value
            if (field.type === "radio" && field.checked) {
              defaultConfiguration[field.name] = field.value;
            }
          }

          // if an input has further configuration than go through fillDefaultConfig again
          if (field.fieldset) {
            defaultConfiguration = fillDefaultConfig(field.fieldset, defaultConfiguration);
          }
      });

      return defaultConfiguration;

    }

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        {/* Workflow definition Selection*/}
                        <div className="obj quick-actions">
                            <header className="no-expand">
                                {t('EVENTS.EVENTS.NEW.PROCESSING.SELECT_WORKFLOW')}
                            </header>
                            <div className="obj-container padded">
                                {workflowDef.length > 0 ? (
                                    <Field tabIndex="99"
                                           as="select"
                                           name="processingWorkflow"
                                           onChange={e => setDefaultValues(e)}
                                           placeholder={t('EVENTS.EVENTS.NEW.PROCESSING.SELECT_WORKFLOW')}
                                           style={{width: '100%'}}>
                                        <option value="" />
                                        {workflowDef.map((workflow, key)=> (
                                            <option key={key} value={workflow.id}>{workflow.title}</option>
                                        ))}
                                    </Field>
                                ) : (
                                    <span>{t('EVENTS.EVENTS.NEW.PROCESSING.SELECT_WORKFLOW_EMPTY')}</span>
                                )}

                                {/* Configuration panel of selected workflow */}
                                {/*Todo: Needs to be implemented after adjustments in definition files done*/}
                                <div className="collapsible-box">
                                    <div id="new-event-workflow-configuration"
                                         className="checkbox-container obj-container">
                                        {formik.values.processingWorkflow ? (
                                            <RenderWorkflowConfig displayDescription={true}
                                                                  workflowId={formik.values.processingWorkflow}
                                                                  workflowPanelRef={workflowPanelRef}
                                                                  formik={formik} />
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
                                active: (formik.values.processingWorkflow && formik.isValid),
                                inactive: !(formik.values.processingWorkflow && formik.isValid)
                            })}
                        disabled={!(formik.values.processingWorkflow && formik.isValid)}
                        onClick={() => {
                            nextPage(formik.values);
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

// Getting state data out of redux store
const mapStateToProps = state => ({
    workflowDef: getWorkflowDef(state),
});

const mapDispatchToProps = dispatch => ({
    loadingWorkflowDef: () => dispatch(fetchWorkflowDef())
})

export default connect(mapStateToProps, mapDispatchToProps)(NewProcessingPage);
