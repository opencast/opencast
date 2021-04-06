import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import Notifications from "../../../shared/Notifications";
import {connect} from "react-redux";
import cn from 'classnames';
import {Field} from "formik";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";
import {getSelectedRows} from "../../../../selectors/tableSelectors";
import {addNotification} from "../../../../thunks/notificationThunks";
import {getNotifications} from "../../../../selectors/notificationSelector";

const StartTaskGeneralPage = ({ formik, nextPage, selectedRows }) => {
    const { t } = useTranslation();

    const [allChecked, setAllChecked] = useState(false);
    const [selectedEvents, setSelectedEvents] = useState(selectedRows);

    const isStartable = event => {
        if (event.event_status.toUpperCase().indexOf('PROCESSED') > -1
            || event.event_status.toUpperCase().indexOf('PROCESSING_FAILURE') > -1
            || event.event_status.toUpperCase().indexOf('PROCESSING_CANCELED') > -1 || !event.selected) {
            return true;
        } else {
            return false;
        }
    };

    const isTaskStartable = events => {
        for (let i = 0; i < events.length; i++) {
            if (!isStartable(events[i])) {
                return false;
            }
        }
        return true;
    }

    // Select or deselect all rows in table
    const onChangeAllSelected = e => {
        const selected = e.target.checked;
        setAllChecked(selected);
        if (selected) {
            formik.setFieldValue('eventIds', selectedEvents);
        } else {
            formik.setFieldValue('eventIds',  []);
        }
    };

    const onChangeSelected = (e, id) => {
        const selected = e.target.checked;
        let changedEvents = selectedEvents.map(event => {
            if (event.id === id) {
                return {
                    ...event,
                    selected: selected
                }
            } else {
                return event
            }
        });
        setSelectedEvents(changedEvents);

        if (!selected) {
            setAllChecked(false);
        }
    };

    return (
        <>
            <div className="modal-content active">
                <div className="modal-body">
                    <div className="row">
                        {/*todo: only show if task not startable*/}
                        {!isTaskStartable(selectedEvents) && (
                            <div className="alert sticky warning">
                                <p>{t('BULK_ACTIONS.SCHEDULE_TASK.GENERAL.CANNOTSTART')}</p>
                            </div>
                        )}
                        <Notifications context="not_corner"/>
                    </div>
                    <div className="full-col">
                        <div className="obj tbl-list">
                            <header>
                                {t('BULK_ACTIONS.SCHEDULE_TASK.GENERAL.CAPTION')}
                                {/*todo: add translation parameter rows.length*/}
                                <span className="header-value">
                                    {t('BULK_ACTIONS.SCHEDULE_TASK.GENERAL.SUMMARY')}
                                </span>
                            </header>
                            <div className="obj-container">
                                <table className="main-tbl">
                                    <thead>
                                        <tr>
                                            <th className="small">
                                                <input className="select-all-cbox"
                                                       type="checkbox"
                                                       checked={allChecked}
                                                       onChange={e => onChangeAllSelected(e)}/>
                                            </th>
                                            <th className="full-width">{t('EVENTS.EVENTS.TABLE.TITLE')}</th>
                                            <th className="nowrap">{t('EVENTS.EVENTS.TABLE.SERIES')}</th>
                                            <th className="nowrap">{t('EVENTS.EVENTS.TABLE.STATUS')}</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {/*todo: repeat for each event chosen (error: ...)*/}
                                        {selectedEvents.map((event, key) => (
                                            <tr key={key} className={cn({error: !isStartable(event)})}>
                                                <td>
                                                    <Field name="eventIds"
                                                           type="checkbox"
                                                           value={event} />
                                                           {/*checked={event.selected} />*/}
                                                </td>
                                                <td>{event.title}</td>
                                                <td className="nowrap">{event.series ? event.series.title : ''}</td>
                                                <td className="nowrap">{t(event.event_status)}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <WizardNavigationButtons isFirst
                                     formik={formik}
                                     nextPage={nextPage}/>
        </>
    );
};

const mapStateToProps = state => ({
    selectedRows: getSelectedRows(state),
    notifications: getNotifications(state)
});

const mapDispatchToProps = dispatch => ({
    addNotification: (type, key, duration, parameter, context) => dispatch(addNotification(type, key, duration, parameter, context))
})

export default connect(mapStateToProps, mapDispatchToProps)(StartTaskGeneralPage);
