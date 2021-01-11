import React, {useEffect} from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import Notifications from "../../../shared/Notifications";
import {DatePicker} from "@material-ui/pickers";
import {getTimezoneOffset} from "../../../../utils/utils";
import {createMuiTheme, ThemeProvider} from "@material-ui/core";
import {Field} from "formik";
import {sourceMetadata, uploadAssetOptions} from "../../../../configs/newEventConfigs/sourceConfig";
import RenderField from "./RenderField";
import {
    hours,
    minutes,
    newEventWizardStates,
    NOTIFICATION_CONTEXT,
    weekdays
} from "../../../../configs/newEventConfigs/newEventWizardStates";
import {getRecordings} from "../../../../selectors/recordingSelectors";
import {fetchRecordings} from "../../../../thunks/recordingThunks";
import {connect} from "react-redux";
import {addNotification} from "../../../../thunks/notificationThunks";
import {getNotifications} from "../../../../selectors/notificationSelector";
import {removeNotificationEventsForm} from "../../../../actions/notificationActions";
import {checkForConflicts} from "../../../../thunks/eventThunks";


// Style to bring date picker pop up to front
const theme = createMuiTheme({
    props: {
        MuiDialog: {
            style: {
                zIndex: '2147483550',
            }
        }
    }
});

/**
 * This component renders the source page for new events in the new event wizard.
 */
const NewEventSource = ({ onSubmit, previousPage, nextPage, formik, loadingInputDevices, inputDevices, addNotification,
                            removeNotificationEventsForm }) => {
    const { t } = useTranslation();

    useEffect(() => {
        // Load recordings that can be used for input
        loadingInputDevices();
    }, []);

    // check user input for conflicts
    const checkConflicts = async () =>  {
        const values = formik.values
        let check = true;
        // Only perform checks if source mode is SCHEDULE_SINGLE or SCHEDULE_MULTIPLE
        if (values.sourceMode === 'SCHEDULE_SINGLE' ||
            values.sourceMode === 'SCHEDULE_MULTIPLE') {
            // Get timezone offset; Checks should be performed on UTC times
            let offset = getTimezoneOffset();

            // Prepare start date of event for check
            let startDate = new Date(values.scheduleStartDate);
            startDate.setHours((values.scheduleStartTimeHour - offset), values.scheduleStartTimeMinutes, 0, 0);

            // If start date of event is smaller than today --> Event is in past
            if (startDate < new Date()) {
                addNotification('error', 'CONFLICT_ALREADY_ENDED', -1, null, NOTIFICATION_CONTEXT);
                check = false;
            }

            let endDate;

            // Prepare end date of event for check
            if(values.sourceMode === 'SCHEDULE_SINGLE') {
                endDate = new Date(values.scheduleStartDate);
            } else {
                endDate = new Date(values.scheduleEndDate);
            }
            endDate.setHours((values.scheduleEndTimeHour - offset), values.scheduleEndTimeMinutes, 0, 0);

            // if start date is higher than end date --> end date is before start date
            if (startDate > endDate) {
                addNotification('error', 'CONFLICT_END_BEFORE_START', -1, null, NOTIFICATION_CONTEXT);
                check = false;
            }

            // transform duration into milliseconds (needed for API request)
            let duration = values.scheduleDurationHour * 3600000 + values.scheduleDurationMinutes * 60000;

            // Check for conflicts with other already scheduled events
            let conflicts = await checkForConflicts(startDate, endDate, duration, values.location);

            // If conflicts with already scheduled events detected --> need to change times/date
            if(!conflicts) {
                addNotification('error', 'CONFLICT_DETECTED', -1, null, NOTIFICATION_CONTEXT);
                check = false;
            }

        }
        return check;
    }

    // Remove old notifications of context event-form
    // Helps to prevent multiple notifications for same problem
    const removeOldNotifications = () => {
        removeNotificationEventsForm();
    }

    return(
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        {/*Show notifications with context events-form*/}
                        <Notifications context={NOTIFICATION_CONTEXT}/>
                        <div className="obj list-obj">
                            <header className="no-expand">{t('EVENTS.EVENTS.NEW.SOURCE.SELECT_SOURCE')}</header>
                            {/* Radio buttons for choosing source mode */}
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

                        {/* Render rest of page depending on which source mode is chosen */}
                        {formik.values.sourceMode === 'UPLOAD' && (
                            <Upload setFieldValue={formik.setFieldValue} />
                        )}
                        {(formik.values.sourceMode === 'SCHEDULE_SINGLE' ||
                            formik.values.sourceMode === 'SCHEDULE_MULTIPLE') && (
                            <Schedule formik={formik}
                                      inputDevices={inputDevices}/>
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
                        onClick={async () => {
                            removeOldNotifications();
                            if(await checkConflicts()) {
                                nextPage(formik.values);
                                onSubmit();
                            }
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

/*
 * Renders buttons for uploading files and fields for additional metadata
 */
const Upload = ({ setFieldValue }) => {
    const { t } = useTranslation();

    return (
        <>
            <div className="obj list-obj">
                <header>{t('EVENTS.EVENTS.NEW.SOURCE.UPLOAD.RECORDING_ELEMENTS')}</header>
                <div className="obj-container">
                    <ul>
                        {/*File upload button for each upload asset*/}
                        {uploadAssetOptions.map((asset, key) => (
                            <li key={key}>
                                <div className="file-upload">
                                    <input id={asset.id}
                                           className="blue-btn file-select-btn"
                                           accept={asset.accept}
                                           onChange={e => setFieldValue(asset.id, e.target.files[0])}
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
                        {/* One row for each metadata field*/}
                        {sourceMetadata.UPLOAD.metadata.map((field, key) => (
                            <tr key={key}>
                                <td>
                                    <span>{t(field.label)}</span>
                                    {field.required && (
                                        <i className="required">*</i>
                                    )}
                                </td>
                                <td className="editable">
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

/*
 * Renders fields for providing information for schedule of event
 */
const Schedule = ({ formik, inputDevices }) => {
    const { t } = useTranslation();

    const renderInputDeviceOptions = () => {
        if (!!formik.values.location) {
            let inputDevice = inputDevices.find(({ Name }) => Name === formik.values.location);
            return (
                inputDevice.inputs.map((input, key) => (
                        <label key={key}>
                            <Field type="checkbox" name="deviceInputs" value={input.id} tabIndex="12"/>
                            {t(input.value)}
                        </label>
                    )
                ));
        }
    }

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
                            <ThemeProvider theme={theme}>
                                <DatePicker name="scheduleStartDate"
                                            value={formik.values.scheduleStartDate}
                                            onChange={value => {
                                                formik.setFieldValue("scheduleStartDate", value);
                                                console.log(formik);
                                            }}
                                            tabIndex="4"/>
                            </ThemeProvider>
                        </td>
                    </tr>
                    {/* Render fields specific for multiple schedule (Only if this is current source mode)*/}
                    {formik.values.sourceMode === 'SCHEDULE_MULTIPLE' && (
                        <>
                            <tr>
                                <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.END_DATE')} <i className="required">*</i></td>
                                <td>
                                    <ThemeProvider theme={theme}>
                                        <DatePicker name="scheduleEndDate"
                                                    value={formik.values.scheduleEndDate}
                                                    onChange={value => formik.setFieldValue("scheduleEndDate", value)}
                                                    tabIndex="4"/>
                                    </ThemeProvider>
                                </td>
                            </tr>
                            <tr>
                                <td>{t('EVENTS.EVENTS.NEW.SOURCE.SCHEDULE_MULTIPLE.REPEAT_ON')} <i className="required">*</i>
                                </td>
                                <td>
                                    {/* Repeat checkbox for each week day*/}
                                    {weekdays.map((day, key) => (
                                        <div key={key} className="day-check-container">
                                            {t(day.label)}
                                            <br/>
                                            <Field type="checkbox" name="repeatOn" value={day.name}/>
                                        </div>
                                    ))}
                                </td>
                            </tr>
                        </>
                    )}
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.START_TIME')} <i className="required">*</i></td>
                        <td>
                            {/* one options for each entry in hours*/}
                            {/* todo: check for conflicts*/}
                            <Field tabIndex="5"
                                   as="select"
                                   name="scheduleStartTimeHour"
                                   placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.HOUR')}>
                                <option value="" />
                                {hours.map(i => (
                                    <option value={i.value}>{i.value}</option>
                                ))}
                            </Field>
                            {/* one options for each entry in minutes*/}
                            {/* todo: check for conflicts*/}
                            <Field tabIndex="6"
                                   as="select"
                                   name="scheduleStartTimeMinutes"
                                   placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.MINUTE')}>
                                <option value=""/>
                                {minutes.map((i) => (
                                    <option value={i.value}>{i.value}</option>
                                ))}
                            </Field>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.DURATION')} <i className="required">*</i></td>
                        <td>
                            {/* one options for each entry in hours*/}
                            {/* todo: check for conflicts*/}
                            <Field tabIndex="7"
                                   as="select"
                                   name="scheduleDurationHour"
                                   placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.HOUR')}>
                                <option value="" />
                                {hours.map((i) => (
                                    <option value={i.value}>{i.value}</option>
                                ))}
                            </Field>
                            {/* one options for each entry in minutes*/}
                            {/* todo: check for conflicts*/}
                            <Field tabIndex="8"
                                   as="select"
                                   name="scheduleDurationMinutes"
                                   placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.MINUTE')}>
                                <option value=""/>
                                {minutes.map((i) => (
                                    <option value={i.value}>{i.value}</option>
                                ))}
                            </Field>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.DATE_TIME.END_TIME')} <i className="required">*</i></td>
                        <td>
                            {/* one options for each entry in hours*/}
                            {/* todo: check for conflicts*/}
                            <Field tabIndex="9"
                                   as="select"
                                   name="scheduleEndTimeHour"
                                   placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.HOUR')}>
                                <option value="" />
                                {hours.map((i) => (
                                    <option value={i.value}>{i.value}</option>
                                ))}
                            </Field>
                            {/* one options for each entry in minutes*/}
                            {/* todo: check for conflicts*/}
                            <Field tabIndex="10"
                                   as="select"
                                   name="scheduleEndTimeMinutes"
                                   placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.MINUTE')}>
                                <option value=""/>
                                {minutes.map((i) => (
                                    <option value={i.value}>{i.value}</option>
                                ))}
                            </Field>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.LOCATION')} <i className="required">*</i></td>
                        {/* one options for each capture agents that has input options */}
                        {/* todo: check for conflicts */}
                        <td>
                            <select placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.LOCATION')}
                                    tabIndex="11"
                                    onChange={e => {
                                        formik.setFieldValue("location", e.target.value);
                                        formik.setFieldValue("deviceInputs", []);
                                    }}
                                    name="location">
                                <option value=""/>
                                {inputDevices.map((inputDevice, key) => (
                                    <option key={key} value={inputDevice.Name}>{inputDevice.Name}</option>
                                ))}
                            </select>
                        </td>
                    </tr>
                    <tr>
                        <td>{t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.INPUTS')} <i className="required">*</i></td>
                        <td>
                            {/* Render checkbox for each input option of the selected input device*/}
                            {renderInputDeviceOptions()}
                        </td>
                    </tr>
                </table>
            </div>
        </div>
    );
};


// Getting state data out of redux store
const mapStateToProps = state => ({
    inputDevices: getRecordings(state),
    notifications: getNotifications(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadingInputDevices: () => dispatch(fetchRecordings("inputs")),
    addNotification: (type, key, duration, parameter, context) => dispatch(addNotification(type, key, duration, parameter, context)),
    removeNotificationEventsForm: () => dispatch(removeNotificationEventsForm())
});


export default connect(mapStateToProps, mapDispatchToProps)(NewEventSource);
