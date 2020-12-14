import React from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import Notifications from "../../../shared/Notifications";
import {DatePicker, MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import {getTimezoneOffset} from "../../../../utils/utils";
import {createMuiTheme, ThemeProvider} from "@material-ui/core";
import {Field} from "formik";
import {sourceMetadata, uploadAssetOptions} from "../../../../configs/newEventConfigs/sourceConfig";
import RenderField from "./RenderField";


const theme = createMuiTheme({
    props: {
        MuiDialog: {
            style: {
                zIndex: '2147483550',
            }
        }
    }
});

const NewEventSource = ({ onSubmit, previousPage, nextPage, formik }) => {
    const { t } = useTranslation();

    console.log("FORMIK");
    console.log(formik);

    return(
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        {/*todo: Implement context event-form and add notification if conflict*/}
                        <Notifications />
                        {/*Todo: Show Table only if there are conflicts*/}
                        <table>
                            {/*Todo: Repeat row for each conflict that occurs*/}
                            <tr>
                                <td>Conflict Title</td>
                                <td>Conflict Start</td>
                                <td>Conflict End</td>
                            </tr>
                        </table>
                        <div className="obj list-obj">
                            <header className="no-expand">{t('EVENTS.EVENTS.NEW.SOURCE.SELECT_SOURCE')}</header>
                            <div className="obj-container">
                                <ul>
                                    <li>
                                        <label>
                                            <Field type="radio"
                                                   name="sourceMode"
                                                   className="source-toggle"
                                                   value="UPLOAD"/>
                                            <span>{t('EVENTS.EVENTS.NEW.SOURCE.UPLOAD.CAPTION')}</span>
                                        </label>
                                    </li>
                                    <li>
                                        <label>
                                            <Field type="radio"
                                                   name="sourceMode"
                                                   className="source-toggle"
                                                   value="SCHEDULE_SINGLE"/>
                                            <span>{t('EVENTS.EVENTS.NEW.SOURCE.SCHEDULE_SINGLE.CAPTION')}</span>
                                        </label>
                                    </li>
                                    <li>
                                        <label>
                                            <Field type="radio"
                                                   name="sourceMode"
                                                   className="source-toggle"
                                                   value="SCHEDULE_MULTIPLE"/>
                                            <span>{t('EVENTS.EVENTS.NEW.SOURCE.SCHEDULE_MULTIPLE.CAPTION')}</span>
                                        </label>
                                    </li>
                                </ul>
                            </div>
                        </div>
                        {formik.values.sourceMode === 'UPLOAD' && (
                            <Upload />
                        )}
                        {formik.values.sourceMode === 'SCHEDULE_SINGLE' && (
                            <ScheduleSingle />
                        )}
                        {formik.values.sourceMode === 'SCHEDULE_MULTIPLE' && (
                            <ScheduleMultiple />
                        )}
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
                            nextPage(formik.values);
                            onSubmit();
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

const Upload = ({ }) => {
    const { t } = useTranslation();

    return (
        <>
            <div className="obj list-obj">
                <header>{t('EVENTS.EVENTS.NEW.SOURCE.UPLOAD.RECORDING_ELEMENTS')}</header>
                <div className="obj-container">
                    <ul>
                        {/*Todo: Repeat for Assets*/}
                        {uploadAssetOptions.map((asset, key) => (
                            <li key={key}>
                                <div className="file-upload">
                                    <input id={asset.id}
                                           className="blue-btn file-select-btn"
                                           accept={asset.accept}
                                           type="file"
                                           tabIndex=""/>
                                </div>
                                <span>{t(asset.translate + ".SHORT")}</span>
                                <span className="ui-helper-hidden">({asset.type} "{asset.flavorType}/{asset.flavorSubType}")</span>
                                <p>{t(asset.translate + ".DETAIL")}</p>
                            </li>
                        ))
                        }
                    </ul>
                </div>
            </div>
            <div className="obj list-obj">
                <header className="no-expand">{t('EVENTS.EVENTS.NEW.SOURCE.UPLOAD.RECORDING_METADATA')}</header>
                <div className="obj-container">
                    <table className="main-tbl">
                        {/* todo: one row for each metadata field*/}
                        {sourceMetadata.UPLOAD.metadata.map((field, key) => (
                            <tr key={key}>
                                <td>
                                    <span>{t(field.label)}</span>
                                    <i className="required">*</i>
                                </td>
                                <td className="editable">
                                    {/* todo: Field input stuff*/}
                                    <Field name={field.id}
                                           metadataField={field}
                                           component={RenderField}/>
                                </td>
                            </tr>
                        ))}
                    </table>
                </div>
            </div>
        </>
    );
};

const ScheduleSingle = ({ }) => {
    const { t } = useTranslation();

    return (
        <div className="obj">
            <header>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.CAPTION')}</header>
            <div className="obj-container">
                <table className="main-tbl">
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.TIMEZONE')}</td>
                        <td>{'UTC' + getTimezoneOffset()}</td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_DATE')} <i className="required">*</i></td>
                        <td>
                            {/*todo: Set value and onChange according formik field*/}
                            <ThemeProvider theme={theme}>
                                <MuiPickersUtilsProvider utils={DateFnsUtils}>
                                    <DatePicker tabIndex="4"
                                                disableToolbar
                                                format="dd/MM/yyyy"/>
                                </MuiPickersUtilsProvider>
                            </ThemeProvider>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_TIME')} <i className="required">*</i></td>
                        <td>
                            {/* todo: Options von wizard.step.hours*/}
                            <select tabIndex="5"
                                    placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.HOUR')}>
                                <option value="" />
                            </select>
                            {/* todo: Options von wizard.step.minute*/}
                            <select tabIndex="6"
                                    placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.MINUTE')}>
                                <option value=""/>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.DURATION')} <i className="required">*</i></td>
                        <td>
                            {/* todo: Options von wizard.step.hours and check for conflicts*/}
                            <select tabIndex="7"
                                    placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.HOUR')}>
                                <option value="" />
                            </select>
                            {/* todo: Options von wizard.step.minute and check for conflicts*/}
                            <select tabIndex="8"
                                    placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.MINUTE')}>
                                <option value=""/>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.END_TIME')} <i className="required">*</i></td>
                        <td>
                            {/* todo: Options von wizard.step.hours and check for conflicts*/}
                            <select tabIndex="9"
                                    placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.HOUR')}>
                                <option value="" />
                            </select>
                            {/* todo: Options von wizard.step.minute and check for conflicts*/}
                            <select tabIndex="10"
                                    placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.MINUTE')}>
                                <option value=""/>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.LOCATION')} <i className="required">*</i></td>
                        {/* todo: Options available capture agents  and check for conflicts*/}
                        <td>
                            <select placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.LOCATION')}
                                    tabIndex="11">
                                <option value=""/>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.INPUTS')} <i className="required">*</i></td>
                        <td>
                            {/* todo: repeat for each input method of chosen device */}
                            <label>
                                <input type="checkbox" tabIndex="12"/>
                                Translate inputMethod.id
                            </label>
                        </td>
                    </tr>
                </table>
            </div>
        </div>
    );
};

const ScheduleMultiple = ({ }) => {
    const { t } = useTranslation();

    return (
        <div className="obj">
            <header>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.CAPTION')}</header>
            <div className="obj-container">
                <table className="main-tbl">
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.TIMEZONE')}</td>
                        <td>{'UTC' + getTimezoneOffset()}</td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_DATE')} <i className="required">*</i></td>
                        <td>
                            {/*todo: Set value and onChange according formik field*/}
                            <ThemeProvider theme={theme}>
                                <MuiPickersUtilsProvider utils={DateFnsUtils}>
                                    <DatePicker tabIndex="4"
                                                disableToolbar
                                                format="dd/MM/yyyy"/>
                                </MuiPickersUtilsProvider>
                            </ThemeProvider>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.END_DATE')} <i className="required">*</i></td>
                        <td>
                            {/*todo: Set value and onChange according formik field*/}
                            <ThemeProvider theme={theme}>
                                <MuiPickersUtilsProvider utils={DateFnsUtils}>
                                    <DatePicker tabIndex="5"
                                                disableToolbar
                                                format="dd/MM/yyyy"/>
                                </MuiPickersUtilsProvider>
                            </ThemeProvider>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.SCHEDULE_MULTIPLE.REPEAT_ON')} <i className="required">*</i></td>
                        <td>
                            {/* todo: repeat for each week day*/}
                            <div className="day-check-container">Weekday Translation
                                <br />
                                <input type="checkbox"/>
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_TIME')} <i className="required">*</i></td>
                        <td>
                            {/* todo: Options von wizard.step.hours and check for conflicts*/}
                            <select placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.HOUR')}
                                    tabIndex="7">
                                <option value=""/>
                            </select>
                            {/* todo: Options von wizard.step.minute and check for conflicts*/}
                            <select placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.MINUTE')}
                                    tabIndex="8">
                                <option value=""/>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.DURATION')} <i className="required">*</i></td>
                        <td>
                            {/* todo: Options von wizard.step.hours and check for conflicts*/}
                            <select placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.HOUR')}
                                    tabIndex="9">
                                <option value=""/>
                            </select>
                            {/* todo: Options von wizard.step.minute and check for conflicts*/}
                            <select placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.MINUTE')}
                                    tabIndex="10">
                                <option value=""/>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.END_TIME')} <i className="required">*</i></td>
                        <td>
                            {/* todo: Options von wizard.step.hours and check for conflicts*/}
                            <select placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.HOUR')}
                                    tabIndex="15">
                                <option value=""/>
                            </select>
                            {/* todo: Options von wizard.step.minute and check for conflicts*/}
                            <select placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.MINUTE')}
                                    tabIndex="16">
                                <option value=""/>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.LOCATION')} <i className="required">*</i></td>
                        {/* todo: Options available capture agents  and check for conflicts*/}
                        <td>
                            <select placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.LOCATION')}
                                    tabIndex="19">
                                <option value=""/>
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.INPUTS')} <i className="required">*</i></td>
                        <td>
                            {/* todo: repeat for each input method of chosen device */}
                            <label>
                                <input type="checkbox" tabIndex="20"/>
                                Translate inputMethod.id
                            </label>
                        </td>
                    </tr>
                </table>
            </div>
        </div>
    );
}

export default NewEventSource;
