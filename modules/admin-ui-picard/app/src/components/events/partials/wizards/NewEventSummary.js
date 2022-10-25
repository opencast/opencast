import React from "react";
import {useTranslation} from "react-i18next";
import {
    getAssetUploadOptions,
    getEventMetadata,
    getExtendedEventMetadata
} from "../../../../selectors/eventSelectors";
import {connect} from "react-redux";
import {getWorkflowDef} from "../../../../selectors/workflowSelectors";
import MetadataSummaryTable from "./summaryTables/MetadataSummaryTable";
import MetadataExtendedSummaryTable from "./summaryTables/MetadataExtendedSummaryTable";
import AccessSummaryTable from "./summaryTables/AccessSummaryTable";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";

/**
 * This component renders the summary page for new events in the new event wizard.
 */
const NewEventSummary = ({ previousPage, formik, metaDataExtendedHidden, assetUploadHidden,
                             metadataEvents, extendedMetadata, workflowDef, uploadAssetOptions }) => {
    const { t } = useTranslation();

    // Get upload assets that are not of type track
    const uploadAssetsOptionsNonTrack = uploadAssetOptions.filter(asset => asset.type !== 'track');

    // upload asset that user has provided
    let uploadAssetsNonTrack = [];
    for (let i = 0; uploadAssetsOptionsNonTrack.length > i; i++) {
        let fieldValue = formik.values[uploadAssetsOptionsNonTrack[i].id];
        if (!!fieldValue) {
            uploadAssetsNonTrack = uploadAssetsNonTrack.concat({
                name: uploadAssetsOptionsNonTrack[i].id,
                translate: !!uploadAssetsOptionsNonTrack[i].displayOverride ? t(uploadAssetsOptionsNonTrack[i].displayOverride) : t(uploadAssetsOptionsNonTrack[i].title),
                type:uploadAssetsOptionsNonTrack[i].type,
                flavorType: uploadAssetsOptionsNonTrack[i].flavorType,
                flavorSubType: uploadAssetsOptionsNonTrack[i].flavorSubType,
                value: fieldValue
            });
        }
    }

    // Get additional information about chosen workflow definition
    const workflowDefinition = workflowDef.find(workflow => workflow.id === formik.values.processingWorkflow);


    return(
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        {/*Summary metadata*/}
                        <MetadataSummaryTable metadataFields={metadataEvents.fields}
                                              formikValues={formik.values}
                                              header={'EVENTS.EVENTS.NEW.METADATA.CAPTION'}/>

                        {/*Summary metadata extended*/}
                        {!metaDataExtendedHidden ? (
                            <MetadataExtendedSummaryTable extendedMetadata={extendedMetadata}
                                                          formikValues={formik.values}
                                                          formikInitialValues={formik.initialValues}
                                                          header={'EVENTS.EVENTS.NEW.METADATA_EXTENDED.CAPTION'}/>
                        ): null}

                        {/*Summary upload assets*/}
                        {/*Show only if asset upload page is not hidden, the sourceMode is UPLOAD and the there
                        are actually upload assets provided by the user*/}
                        {!assetUploadHidden && formik.values.sourceMode === 'UPLOAD' && uploadAssetsNonTrack.length > 0 ? (
                            <div className="obj tbl-list">
                                <header className="no-expand">{t('EVENTS.EVENTS.NEW.UPLOAD_ASSET.CAPTION')}</header>
                                <div className="obj-container">
                                    <table className="main-tbl">
                                        <tbody>
                                            {/*Insert row for each upload asset user has provided*/}
                                            {uploadAssetsNonTrack.map((asset, key) => (
                                                <tr key={key}>
                                                    <td>
                                                        {asset.translate}
                                                        <span className="ui-helper-hidden">
                                                            ({asset.type} "{asset.flavorType}//{asset.flavorSubType}")
                                                        </span>
                                                    </td>
                                                    <td>{asset.value.name}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        ) : null}


                        {/* Summary source */}
                        <div className="obj tbl-list">
                            <header className="no-expand">{t('EVENTS.EVENTS.NEW.SOURCE.CAPTION')}</header>
                            <div className="obj-container">
                                {/*Summary source mode UPLOAD*/}
                                {formik.values.sourceMode === 'UPLOAD' && (
                                    <table className="main-tbl">
                                        <tbody>
                                            {/*Insert row for each upload asset of type track user has provided*/}
                                            {formik.values.uploadAssetsTrack.map((asset, key) => (
                                                !!asset.file ? (
                                                    <tr key={key}>
                                                        <td>{t(asset.title + ".SHORT", asset['displayOverride.SHORT'])}
                                                            <span className="ui-helper-hidden">
                                                            ({asset.type} "{asset.flavorType}//{asset.flavorSubType}")
                                                        </span>
                                                        </td>
                                                        <td>{asset.file[0].name}</td>
                                                    </tr>
                                                ) : null
                                            ))}
                                        <tr>
                                            <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_DATE')}</td>
                                            <td>{t('dateFormats.date.short', { date: new Date(formik.values.startDate) })}</td>
                                        </tr>
                                        </tbody>
                                    </table>
                                )}
                                {/*Summary source mode SCHEDULE-SINGLE/SCHEDULE-MULTIPLE*/}
                                {(formik.values.sourceMode === 'SCHEDULE_SINGLE' || formik.values.sourceMode === 'SCHEDULE_MULTIPLE') && (
                                    <table className="main-tbl">
                                        <tbody>
                                            <tr>
                                                <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_DATE')}</td>
                                                <td>{t('dateFormats.date.short', { date: formik.values.scheduleStartDate })}</td>
                                            </tr>
                                            <tr>
                                                <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_TIME')}</td>
                                                <td>{formik.values.scheduleStartHour}:{formik.values.scheduleStartMinute}</td>
                                            </tr>
                                            {formik.values.sourceMode === 'SCHEDULE_MULTIPLE' && (
                                                <tr>
                                                    <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.END_DATE')}</td>
                                                    <td>{t('dateFormats.date.short', { date: formik.values.scheduleEndDate })}</td>
                                                </tr>
                                            )}
                                            <tr>
                                                <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.END_TIME')}</td>
                                                <td>{formik.values.scheduleEndHour}:{formik.values.scheduleEndMinute}</td>
                                            </tr>
                                            {formik.values.sourceMode === 'SCHEDULE_MULTIPLE' && (
                                                <tr>
                                                    <td>{t('EVENTS.EVENTS.NEW.SOURCE.SCHEDULE_MULTIPLE.WEEKDAYS')}</td>
                                                    <td>{formik.values.repeatOn.map(day => t('EVENTS.EVENTS.NEW.WEEKDAYSLONG.' + day)).join(', ')}</td>
                                                </tr>
                                            )}
                                            <tr>
                                                <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.DURATION')}</td>
                                                <td>{formik.values.scheduleDurationHours}:{formik.values.scheduleDurationMinutes}</td>
                                            </tr>
                                            <tr>
                                                <td>{t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.LOCATION')}</td>
                                                <td>{formik.values.location}</td>
                                            </tr>
                                            {!!formik.values.deviceInputs && (
                                                <tr>
                                                    <td>{t('EVENTS.EVENTS.NEW.SUMMARY.SOURCE.INPUT')}</td>
                                                    <td>{formik.values.deviceInputs.join(', ')}</td>
                                                </tr>
                                            )}
                                        </tbody>
                                    </table>
                                )}

                            </div>
                        </div>

                        {/* Summary processing configuration */}
                        <div className="obj tbl-list">
                            <header className="no-expand">
                                {t('EVENTS.EVENTS.NEW.PROCESSING.CAPTION')}
                            </header>
                            <table className="main-tbl">
                                <tbody>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.PROCESSING.WORKFLOW')}</td>
                                        <td>{!!workflowDefinition ? workflowDefinition.title : ""}</td>
                                    </tr>
                                    {/* Repeat entry for each configuration key/value pair */}
                                    {Object.keys(formik.values.configuration).map((config, key) => (
                                      <tr key={key}>
                                          <td>{config}</td>
                                          <td>{formik.values.configuration[config].toString()}</td>
                                      </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {/*Summary access configuration*/}
                        <AccessSummaryTable policies={formik.values.acls}
                                            header={'EVENTS.EVENTS.NEW.ACCESS.CAPTION'}/>
                    </div>
                </div>
            </div>

            {/* Button for navigation to next page and previous page */}
            <WizardNavigationButtons isLast
                                     previousPage={previousPage}
                                     formik={formik}/>
        </>
    )
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    metadataEvents: getEventMetadata(state),
    extendedMetadata: getExtendedEventMetadata(state),
    workflowDef: getWorkflowDef(state),
    uploadAssetOptions: getAssetUploadOptions(state),
})

export default connect(mapStateToProps)(NewEventSummary);
