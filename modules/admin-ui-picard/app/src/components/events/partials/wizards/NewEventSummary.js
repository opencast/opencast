import React from "react";
import cn from "classnames";
import {useTranslation} from "react-i18next";
import Notifications from "../../../shared/Notifications";

const NewEventSummary = ({ onSubmit, previousPage, formik }) => {
    const { t } = useTranslation();

    return(
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        {/*TODO: include notifications the right way event-form*/}
                        <Notifications />

                        {/*Summary metadata*/}
                        <div className="obj tbl-list">
                            <header className="no-expand">{t('EVENTS.EVENTS.NEW.METADATA.CAPTION')}</header>
                            <div className="obj-container">
                                <table className="main-tbl">
                                    {/*todo: get a constant containing all metadata entries and make tr for each*/}
                                    <tr>
                                        <td>Title</td>
                                        <td>Testing</td>
                                    </tr>
                                </table>
                            </div>
                        </div>

                        {/*Summary metadata extended*/}
                        {/*todo: get a constant containing all metadata extended entries and make tr for each or show not at all*/}
                        <div className="obj tbl-list">
                            <header className="no-expand">{t('EVENTS.EVENTS.NEW.METADATA_EXTENDED.CAPTION')}</header>
                            <div className="obj-container">
                                <table className="main-tbl">
                                    <tr>
                                        <td>Neues Feld</td>
                                        <td>Inhalt</td>
                                    </tr>
                                </table>
                            </div>
                        </div>

                        {/*Summary upload assets*/}
                        {/*todo: get a constant containing all asset uploads and make tr for each or show not at all*/}
                        <div className="obj tbl-list">
                            <header className="no-expand">{t('EVENTS.EVENTS.NEW.UPLOAD_ASSET.CAPTION')}</header>
                            <div className="obj-container">
                                <table className="main-tbl">
                                    <tr>
                                        <td>Title <span className="ui-helper-hidden"> (asset.type "asset.flavorType/asset.flavorSubType"</span></td>
                                        <td>FileName</td>
                                    </tr>
                                </table>
                            </div>
                        </div>

                        {/*Summary source mode UPLOAD*/}
                        {/*todo: get a constant containing all source entries and make tr for each*/}
                        <div className="obj tbl-list">
                            <header className="no-expand">{t('EVENTS.EVENTS.NEW.SOURCE.CAPTION')}</header>
                            <div className="obj-container">
                                <table className="main-tbl">
                                    <tr>
                                        <td>title
                                            <span className="ui-helper-hidden">(asset.type "asset.flavorType/asset.subFlavorType")</span>
                                        </td>
                                        <td>filename</td>
                                    </tr>
                                </table>
                                {/*Summary source mode SCHEDULE-SINGLE/SCHEDULE-MULTIPLE*/}
                                <table className="main-tbl">
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_DATE')}</td>
                                        <td>start-UPLOAD</td>
                                    </tr>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_DATE')}</td>
                                        <td>start-SCHEDULE</td>
                                    </tr>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_TIME')}</td>
                                        <td>start-Time-SCHEDULE</td>
                                    </tr>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.END_DATE')}</td>
                                        <td>end-SCHEDULE</td>
                                    </tr>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.END_TIME')}</td>
                                        <td>end-Time-SCHEDULE</td>
                                    </tr>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.SCHEDULE_MULTIPLE.REPEAT_ON')}</td>
                                        <td>Repeat on</td>
                                    </tr>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.SCHEDULE_MULTIPLE.WEEKDAYS')}</td>
                                        <td>weekday</td>
                                    </tr>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.DURATION')}</td>
                                        <td>duration</td>
                                    </tr>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.LOCATION')}</td>
                                        <td>location</td>
                                    </tr>
                                    <tr>
                                        <td>{t('EVENTS.EVENTS.NEW.SUMMARY.SOURCE.INPUT')}</td>
                                        <td>input</td>
                                    </tr>
                                </table>
                            </div>
                        </div>

                        {/*Summary processing configuration*/}
                        <div className="obj tbl-list">
                            <header className="no-expand">
                                {t('EVENTS.EVENTS.NEW.PROCESSING.CAPTION')}
                            </header>
                            <table className="main-tbl">
                                <tr>
                                    <td>{t('EVENTS.EVENTS.NEW.PROCESSING.WORKFLOW')}</td>
                                    <td>Workflow title</td>
                                </tr>
                                {/*todo: repeat entry for each configuration key/value pair*/}
                                <tr>
                                    <td>Configuration key</td>
                                    <td>Configuration value</td>
                                </tr>
                            </table>
                        </div>

                        {/*Summary access configuration*/}
                        <div className="obj tbl-list">
                            <header className="no-expand">
                                {t('EVENTS.EVENTS.NEW.ACCESS.CAPTION')}
                            </header>
                            <table className="main-tbl">
                                <thead>
                                    <tr>
                                        <th className="fit">
                                            {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.ROLE')}
                                        </th>
                                        <th className="fit">
                                            {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.READ')}
                                        </th>
                                        <th className="fit">
                                            {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.WRITE')}
                                        </th>
                                        {/*todo: show only if has additional actions*/}
                                        <th className="fit">
                                            {t('EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.ADDITIONAL_ACTIONS')}
                                        </th>
                                    </tr>
                                </thead>
                                <tr>
                                    <td>Policy role</td>
                                    {/*todo: is checked whenever has read/write rights or not (ng-model)*/}
                                    <td className="fit"><input type="checkbox" disabled /></td>
                                    <td className="fit"><input type="checkbox" disabled /></td>
                                    {/*todo: show only if has additional actions*/}
                                    <td className="fit">
                                        {/*todo: repeat for each additional actions*/}
                                        <div>custom Action</div>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
            {/* Button for navigation to next page and previous page */}
            <footer>
                <button type="submit"
                        className={cn("submit",
                            {
                                active: (formik.dirty && formik.isValid),
                                inactive: !(formik.dirty && formik.isValid)
                            })}
                        disabled={!(formik.dirty && formik.isValid)}
                        onClick={() => {
                            onSubmit();
                        }}
                        tabIndex="100">{t('WIZARD.NEXT_STEP')}</button>
                <button className="cancel"
                        onClick={() => previousPage(formik.values, false)}
                        tabIndex="101">{t('WIZARD.BACK')}</button>
            </footer>

            <div className="btm-spacer"/>
        </>
    )
}

export default NewEventSummary;
