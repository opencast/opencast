import React, {useEffect} from "react";
import {connect} from "react-redux";
import cn from "classnames";
import _ from 'lodash';
import {DatePicker} from "@material-ui/pickers";
import {createTheme, ThemeProvider} from "@material-ui/core";
import {MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import {Field, Formik} from "formik";
import Notifications from "../../../shared/Notifications";
import {removeNotificationWizardForm} from "../../../../actions/notificationActions";
import {
    checkConflicts,
    saveSchedulingInfo
} from "../../../../thunks/eventDetailsThunks";
import {addNotification} from "../../../../thunks/notificationThunks";
import {
    getSchedulingConflicts,
    getSchedulingProperties,
    getSchedulingSource,
    isCheckingConflicts
} from "../../../../selectors/eventDetailsSelectors";
import {getUserInformation} from "../../../../selectors/userInfoSelectors";
import {getRecordings} from "../../../../selectors/recordingSelectors";
import {
    getCurrentLanguageInformation,
    getTimezoneOffset,
    getTimezoneString,
    hasAccess,
    makeTwoDigits
} from "../../../../utils/utils";
import {
    changeDurationHour,
    changeDurationMinute,
    changeEndHour,
    changeEndMinute,
    changeStartDate,
    changeStartHour,
    changeStartMinute,
    makeDate
} from "../../../../utils/dateUtils";
import {hours, minutes} from "../../../../configs/modalConfig";
import {filterDevicesForAccess, hasDeviceAccess} from "../../../../utils/resourceUtils";
import {NOTIFICATION_CONTEXT} from "../../../../configs/modalConfig";
import DropDown from "../../../shared/DropDown";

/**
 * This component manages the main assets tab of event details modal
 */
const EventDetailsSchedulingTab = ({ eventId, t,
                                   source, conflicts, hasSchedulingProperties, captureAgents, checkingConflicts, user,
                                   checkConflicts, saveSchedulingInfo, removeNotificationWizardForm, addNotification}) => {

    useEffect(() => {
        removeNotificationWizardForm();
        checkConflicts(eventId, source.start.date, source.end.date, source.device.id).then(r => {});
    }, []);

    // Get info about the current language and its date locale
    const currentLanguage = getCurrentLanguageInformation();

    // Get timezone offset; Checks should be performed on UTC times
    const offset = getTimezoneOffset();

    // Set timezone
    const tz = getTimezoneString(offset);

    // Style to bring date picker pop up to front
    const theme = createTheme({
        props: {
            MuiDialog: {
                style: {
                    zIndex: '2147483550',
                }
            }
        }
    });

    // Variable and function for checking access rights
    const hasAccessRole = hasAccess("ROLE_UI_EVENTS_DETAILS_SCHEDULING_EDIT", user);
    const accessAllowed = (agentId) => {return (!checkingConflicts)  && hasDeviceAccess(user, agentId)};

    // finds the inputs to be displayed in the formik
    const getInputs = deviceId => {
        if(deviceId === source.device.id) {
            return !!source.device.inputs ? source.device.inputs : [];
        } else {
            for(const agent of filterDevicesForAccess(user, captureAgents)){
                if(agent.id === deviceId){
                    return !!agent.inputs ? agent.inputs : [];
                }
            }
            return [];
        }
    }

    // changes the inputs in the formik
    const changeInputs = (deviceId, setFieldValue) => {
        setFieldValue('captureAgent', deviceId);
        setFieldValue('inputs', []);
    }
    const filterCaptureAgents = (agent) => {
        return agent.id === source.agentId || hasDeviceAccess(user, agent.id);
    }

    // checks validity of the formik form
    const checkValidity = (formik) => {
        if (formik.dirty && formik.isValid && hasAccessRole && accessAllowed(formik.values.captureAgent) && !(conflicts.length > 0)) {
            // check if user provided values differ from initial ones
            if (!_.isEqual(formik.values, formik.initialValues)){
                if(!_.isEqual(formik.values.inputs, formik.initialValues.inputs)){
                    return !_.isEqual(formik.values.inputs.sort(), formik.initialValues.inputs.sort());
                } else {
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    // submits the formik form
    const submitForm = async (values) => {
        const startDate = makeDate(values.scheduleStartDate, values.scheduleStartHour, values.scheduleStartMinute);
        const endDate = makeDate(values.scheduleEndDate, values.scheduleEndHour, values.scheduleEndMinute);
        checkConflicts(eventId, startDate, endDate, values.captureAgent).then(r => {
            if(r && !(conflicts.length > 0)){
                saveSchedulingInfo(eventId, values, startDate, endDate).then(r => {});
            } else {
                addNotification('error', 'EVENTS_NOT_UPDATED', -1, null, NOTIFICATION_CONTEXT);
            }
        });
    }

    // initial values of the formik form
    const getInitialValues = () =>{
        const startDate = new Date(source.start.date);
        const endDate = new Date(source.end.date);

        const inputs = !!source.device.inputMethods ? Array.from(source.device.inputMethods) : []

        return {
            scheduleStartDate: startDate.setHours(0,0,0),
            scheduleStartHour: makeTwoDigits(source.start.hour),
            scheduleStartMinute: makeTwoDigits(source.start.minute),
            scheduleDurationHours: makeTwoDigits(source.duration.hour),
            scheduleDurationMinutes: makeTwoDigits(source.duration.minute),
            scheduleEndDate: endDate.setHours(0,0,0),
            scheduleEndHour: makeTwoDigits(source.end.hour),
            scheduleEndMinute: makeTwoDigits(source.end.minute),
            captureAgent: source.device.name,
            inputs: inputs.filter(input => input !== '')
        };
    }

    return (
        <div className="modal-content">
            <div className="modal-body">
                {/* Notifications */}
                <Notifications context="not_corner"/>

                <div className="full-col">

                    {/*list of scheduling conflicts*/
                        conflicts.length > 0 && (
                        <table className="main-tbl scheduling-conflict">
                            <tbody>
                                {conflicts.map((conflict, key) => (
                                    <tr key={key}>
                                        <td>{conflict.title}</td>
                                        <td>{t('dateFormats.dateTime.medium', {dateTime: new Date(conflict.start)})}</td>
                                        <td>{t('dateFormats.dateTime.medium', {dateTime: new Date(conflict.end)})}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}

                    {/* Scheduling configuration */
                    hasSchedulingProperties && (

                        /* Initialize form */
                        <MuiPickersUtilsProvider  utils={DateFnsUtils} locale={currentLanguage.dateLocale}>
                            <Formik
                                enableReinitialize
                                initialValues={getInitialValues()}
                                onSubmit={(values) =>
                                    submitForm(values).then(r => {})
                                }
                            >
                                {formik => (
                                    <div className="obj tbl-details">
                                        <header>
                                            <span>
                                                {t("EVENTS.EVENTS.DETAILS.SCHEDULING.CAPTION") /* Scheduling configuration */}
                                            </span>
                                        </header>
                                        <div className="obj-container">
                                            <table className="main-tbl">
                                                <tbody>
                                                    {/* time zone */}
                                                    <tr>
                                                        <td>{t('EVENTS.EVENTS.DETAILS.SOURCE.DATE_TIME.TIMEZONE')}</td>
                                                        <td>{tz}</td>
                                                    </tr>

                                                    {/* start date */}
                                                    <tr>
                                                        <td>{t('EVENTS.EVENTS.DETAILS.SOURCE.DATE_TIME.START_DATE')}</td>
                                                        <td>
                                                            {(hasAccessRole && accessAllowed(formik.values.captureAgent))? (
                                                                /* date picker for start date */
                                                                <ThemeProvider theme={theme}>
                                                                    <DatePicker name="scheduleStartDate"
                                                                                tabIndex={"1"}
                                                                                value={formik.values.scheduleStartDate}
                                                                                onChange={value =>
                                                                                    changeStartDate(value, formik.values, formik.setFieldValue, eventId, checkConflicts)
                                                                                }
                                                                    />
                                                                </ThemeProvider>
                                                            ):(
                                                                <>{source.start.date.toLocaleDateString(currentLanguage.dateLocale.code)}</>
                                                            )}
                                                        </td>
                                                    </tr>

                                                    {/* start time */}
                                                    <tr>
                                                        <td>{t('EVENTS.EVENTS.DETAILS.SOURCE.DATE_TIME.START_TIME')}</td>
                                                        {hasAccessRole && (

                                                            <td className="editable">
                                                                {/* drop-down for hour
                                                                  *
                                                                  * This is the second input field.
                                                                  */}
                                                                <DropDown value={formik.values.scheduleStartHour}
                                                                          text={formik.values.scheduleStartHour}
                                                                          options={hours}
                                                                          type={'time'}
                                                                          required={true}
                                                                          handleChange={element =>
                                                                              changeStartHour(element.value, formik.values, formik.setFieldValue, eventId, checkConflicts)
                                                                          }
                                                                          placeholder={t('EVENTS.EVENTS.DETAILS.SOURCE.PLACEHOLDER.HOUR')}
                                                                          tabIndex={"2"}
                                                                          disabled={!accessAllowed(formik.values.captureAgent)}
                                                                />

                                                                {/* drop-down for minute
                                                                  *
                                                                  * This is the third input field.
                                                                  */}
                                                                <DropDown value={formik.values.scheduleStartMinute}
                                                                          text={formik.values.scheduleStartMinute}
                                                                          options={minutes}
                                                                          type={'time'}
                                                                          required={true}
                                                                          handleChange={element =>
                                                                              changeStartMinute(element.value, formik.values, formik.setFieldValue, eventId, checkConflicts)
                                                                          }
                                                                          placeholder={t('EVENTS.EVENTS.DETAILS.SOURCE.PLACEHOLDER.MINUTE')}
                                                                          tabIndex={"3"}
                                                                          disabled={!accessAllowed(formik.values.captureAgent)}
                                                                />
                                                            </td>
                                                        )}
                                                        {!hasAccessRole && (
                                                            <td>
                                                                {makeTwoDigits(source.start.hour)}:
                                                                {makeTwoDigits(source.start.minute)}
                                                            </td>
                                                        )}
                                                    </tr>

                                                    {/* duration */}
                                                    <tr>
                                                        <td>{t('EVENTS.EVENTS.DETAILS.SOURCE.DATE_TIME.DURATION')}</td>
                                                        {hasAccessRole && (
                                                            <td className="editable">
                                                                {/* drop-down for hour
                                                                  *
                                                                  * This is the fourth input field.
                                                                  */}
                                                                <DropDown value={formik.values.scheduleDurationHours}
                                                                          text={formik.values.scheduleDurationHours}
                                                                          options={hours}
                                                                          type={'time'}
                                                                          required={true}
                                                                          handleChange={element =>
                                                                              changeDurationHour(element.value, formik.values, formik.setFieldValue, eventId, checkConflicts)
                                                                }
                                                                          placeholder={t('WIZARD.DURATION.HOURS')}
                                                                          tabIndex={"4"}
                                                                          disabled={!accessAllowed(formik.values.captureAgent)}
                                                                />

                                                                {/* drop-down for minute
                                                                  *
                                                                  * This is the fifth input field.
                                                                  */}
                                                                <DropDown value={formik.values.scheduleDurationMinutes}
                                                                          text={formik.values.scheduleDurationMinutes}
                                                                          options={minutes}
                                                                          type={'time'}
                                                                          required={true}
                                                                          handleChange={element =>
                                                                              changeDurationMinute(element.value, formik.values, formik.setFieldValue, eventId, checkConflicts)
                                                                          }
                                                                          placeholder={t('WIZARD.DURATION.MINUTES')}
                                                                          tabIndex={"5"}
                                                                          disabled={!accessAllowed(formik.values.captureAgent)}
                                                                />
                                                            </td>
                                                        )}
                                                        {!hasAccessRole && (
                                                            <td>
                                                                {makeTwoDigits(source.duration.hour)}:
                                                                {makeTwoDigits(source.duration.minute)}
                                                            </td>
                                                        )}
                                                    </tr>

                                                    {/* end time */}
                                                    <tr>
                                                        <td>{t('EVENTS.EVENTS.DETAILS.SOURCE.DATE_TIME.END_TIME')}</td>
                                                        {hasAccessRole && (
                                                            <td className="editable">
                                                                {/* drop-down for hour
                                                                  *
                                                                  * This is the sixth input field.
                                                                  */}
                                                                <DropDown value={formik.values.scheduleEndHour}
                                                                          text={formik.values.scheduleEndHour}
                                                                          options={hours}
                                                                          type={'time'}
                                                                          required={true}
                                                                          handleChange={element =>
                                                                              changeEndHour(element.value, formik.values, formik.setFieldValue, eventId, checkConflicts)
                                                                          }
                                                                          placeholder={t('EVENTS.EVENTS.DETAILS.SOURCE.PLACEHOLDER.HOUR')}
                                                                          tabIndex={"6"}
                                                                          disabled={!accessAllowed(formik.values.captureAgent)}
                                                                />

                                                                {/* drop-down for minute
                                                                  *
                                                                  * This is the seventh input field.
                                                                  */}
                                                                <DropDown value={formik.values.scheduleEndMinute}
                                                                          text={formik.values.scheduleEndMinute}
                                                                          options={minutes}
                                                                          type={'time'}
                                                                          required={true}
                                                                          handleChange={element =>
                                                                              changeEndMinute(element.value, formik.values, formik.setFieldValue, eventId, checkConflicts)
                                                                          }
                                                                          placeholder={t('EVENTS.EVENTS.DETAILS.SOURCE.PLACEHOLDER.MINUTE')}
                                                                          tabIndex={"7"}
                                                                          disabled={!accessAllowed(formik.values.captureAgent)}
                                                                />

                                                                {/* display end date if on different day to start date */}
                                                                {(formik.values.scheduleEndDate.toString() !== formik.values.scheduleStartDate.toString()) && (
                                                                    <span style={{marginLeft: '10px'}}>{(new Date(formik.values.scheduleEndDate)).toLocaleDateString(currentLanguage.dateLocale.code)}</span>
                                                                )}
                                                            </td>
                                                        )}
                                                        {!hasAccessRole && (
                                                            <td>
                                                                {makeTwoDigits(source.end.hour)}:
                                                                {makeTwoDigits(source.end.minute)}
                                                                {(formik.values.scheduleEndDate.toString() !== formik.values.scheduleStartDate.toString()) && (
                                                                    <span>{(new Date(formik.values.scheduleEndDate)).toLocaleDateString(currentLanguage.dateLocale.code)}</span>
                                                                )}
                                                            </td>
                                                        )}
                                                    </tr>

                                                    {/* capture agent (aka. room or location) */}
                                                    <tr>
                                                        <td>{t('EVENTS.EVENTS.DETAILS.SOURCE.PLACEHOLDER.LOCATION')}</td>
                                                        {hasAccessRole && (
                                                            <td className="editable">
                                                                {/* drop-down for capture agents (aka. rooms or locations)
                                                                  *
                                                                  * This is the eighth input field.
                                                                  */}
                                                                <DropDown value={formik.values.captureAgent}
                                                                          text={formik.values.captureAgent}
                                                                          options={filterDevicesForAccess(user, captureAgents).filter(a => filterCaptureAgents(a))}
                                                                          type={'captureAgent'}
                                                                          required={true}
                                                                          handleChange={element => changeInputs(element.value, formik.setFieldValue)}
                                                                          placeholder={t('EVENTS.EVENTS.DETAILS.SOURCE.PLACEHOLDER.LOCATION')}
                                                                          tabIndex={"8"}
                                                                          disabled={!accessAllowed(formik.values.captureAgent)}
                                                                />
                                                            </td>
                                                        )}
                                                        {!hasAccessRole && (
                                                            <td>
                                                                {source.device.name}
                                                            </td>
                                                        )}
                                                    </tr>

                                                    {/* inputs */}
                                                    <tr>
                                                        <td>{t('EVENTS.EVENTS.DETAILS.SOURCE.PLACEHOLDER.INPUTS')}</td>
                                                        <td>
                                                            {!!formik.values.captureAgent && !!getInputs(formik.values.captureAgent) && getInputs(formik.values.captureAgent).length > 0 && (
                                                                (hasAccessRole && accessAllowed(formik.values.captureAgent)) ? (
                                                                    /* checkboxes for available inputs
                                                                     *
                                                                     * These are the input fields starting at 8.
                                                                     */
                                                                    getInputs(formik.values.captureAgent).map((inputMethod, key) => (
                                                                        <label key={key}>
                                                                            <Field name="inputs"
                                                                                   type="checkbox"
                                                                                   tabIndex={8 + key}
                                                                                   value={inputMethod.id}
                                                                            />
                                                                            {t(inputMethod.value)}
                                                                        </label>
                                                                    ))
                                                                ) : (
                                                                    formik.values.inputs.map((input, key) => (
                                                                        <span key={key}>
                                                                            {t(getInputs(formik.values.captureAgent).find(agent => (agent.id === input)).value)}
                                                                            <br/>
                                                                        </span>
                                                                    ))
                                                                )
                                                            )}
                                                        </td>
                                                    </tr>
                                                </tbody>
                                            </table>
                                        </div>

                                        {/* Save and cancel buttons */}
                                        {formik.dirty && (
                                            <>
                                                {/* Render buttons for updating scheduling */}
                                                <footer style={{padding: '15px'}}>
                                                    <button type="submit"
                                                            onClick={() => formik.handleSubmit()}
                                                            disabled={!checkValidity(formik)}
                                                            className={cn("submit",
                                                                {
                                                                    active: checkValidity(formik),
                                                                    inactive: !checkValidity(formik)
                                                                }
                                                            )}>
                                                        {t('SAVE') /* Save */}
                                                    </button>
                                                    <button className="cancel"
                                                            onClick={() => {
                                                                formik.resetForm({values: getInitialValues()});
                                                            }}>
                                                        {t('CANCEL') /* Cancel */}
                                                    </button>
                                                </footer>

                                                <div className="btm-spacer"/>
                                            </>
                                        )}
                                    </div>
                                )}
                            </Formik>
                        </MuiPickersUtilsProvider>
                    )}
                </div>
            </div>
        </div>
    );
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    user: getUserInformation(state),
    hasSchedulingProperties: getSchedulingProperties(state),
    source: getSchedulingSource(state),
    conflicts: getSchedulingConflicts(state),
    checkingConflicts: isCheckingConflicts(state),
    captureAgents: getRecordings(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    checkConflicts: (eventId, startDate, endDate, deviceId) => dispatch(checkConflicts(eventId, startDate, endDate, deviceId)),
    saveSchedulingInfo: (eventId, values, startDate, endDate) => dispatch(saveSchedulingInfo(eventId, values, startDate, endDate)),
    removeNotificationWizardForm: () => dispatch(removeNotificationWizardForm()),
    addNotification: (type, key, duration, parameter, context) => dispatch(addNotification(type, key, duration, parameter, context)),
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsSchedulingTab);