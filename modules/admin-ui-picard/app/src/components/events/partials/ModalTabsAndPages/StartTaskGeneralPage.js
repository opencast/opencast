import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import Notifications from "../../../shared/Notifications";
import {connect} from "react-redux";
import cn from 'classnames';
import {getSelectedRows} from "../../../../selectors/tableSelectors";

/**
 * This component renders the table overview of selected events in start task bulk action
 */
const StartTaskGeneralPage = ({ formik, nextPage, selectedRows }) => {
    const { t } = useTranslation();

    const [allChecked, setAllChecked] = useState(true);
    const [selectedEvents, setSelectedEvents] = useState(selectedRows);

    useEffect(() => {
        // Set field value for formik on mount, because initially all events are selected
        formik.setFieldValue('events', selectedEvents);
    }, []);

    // Check if an event is in a state that a task on it can be started
    const isStartable = event => {
        return event.event_status.toUpperCase().indexOf('PROCESSED') > -1
            || event.event_status.toUpperCase().indexOf('PROCESSING_FAILURE') > -1
            || event.event_status.toUpperCase().indexOf('PROCESSING_CANCELED') > -1 || !event.selected;
    };

    // Check if multiple event is in a state that a task on it can be started
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
        let changedSelection = selectedEvents.map(event => {
            return {
                ...event,
                selected: selected
            }
        });
        setSelectedEvents(changedSelection);
        formik.setFieldValue('events', changedSelection);
    };

    // Handle change of checkboxes indicating which events to consider further
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
        formik.setFieldValue('events', changedEvents);

        if (!selected) {
            setAllChecked(false);
        }
        if (changedEvents.every(event => event.selected === true)) {
            setAllChecked(true);
        }
    };

    // Check validity for activating next button
    const checkValidity = () => {
        if (formik.values.events.length > 0) {
            if (isTaskStartable(formik.values.events) && formik.isValid) {
                return formik.values.events.some(event => event.selected === true);

            } else {
                return false;
            }
        } else {
            return false;
        }
    };

    return (
        <>
            <div className="modal-content active">
                <div className="modal-body">
                    <div className="row">
                        {/* Show only if task not startable */}
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
                                <span className="header-value">
                                    {t('BULK_ACTIONS.SCHEDULE_TASK.GENERAL.SUMMARY',
                                        { count: selectedEvents.filter(e => e.selected === true).length })}
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
                                        {/* Repeat for each event chosen */}
                                        {selectedEvents.map((event, key) => (
                                            <tr key={key} className={cn({error: !isStartable(event)})}>
                                                <td>
                                                    <input name="events"
                                                           type="checkbox"
                                                           onChange={e => onChangeSelected(e, event.id)}
                                                           checked={event.selected} />
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

            {/* Button for navigation to next page and previous page */}
            <footer>
                <button type="submit"
                        className={cn("submit",
                            {
                                active: checkValidity(),
                                inactive: !checkValidity()
                            })}
                        disabled={!checkValidity()}
                        onClick={() => {
                            nextPage(formik.values);
                        }}
                        tabIndex="100">{t('WIZARD.NEXT_STEP')}</button>
            </footer>

            <div className="btm-spacer"/>
        </>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    selectedRows: getSelectedRows(state),
});



export default connect(mapStateToProps)(StartTaskGeneralPage);
