import React, {useState} from 'react';
import {useTranslation} from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteEvent} from "../../../thunks/eventThunks";
import {connect} from "react-redux";
import EventDetailsModal from "./modals/EventDetailsModal";



/**
 * This component renders the action cells of events in the table view
 */
const EventActionCell = ({ row, deleteEvent })  => {
    const { t } = useTranslation();


    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
    const [displayEventDetailsModal, setEventDetailsModal] = useState(false);
    const [eventDetailsTabIndex, setEventDetailsTabIndex] = useState(0)

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingEvent = id => {
        deleteEvent(id);
    };

    

    const showEventDetailsModal = () => {
        setEventDetailsModal(true);
    }

    const hideEventDetailsModal = () => {
        setEventDetailsModal(false);
    }

    const onClickEventDetails = () => {
        setEventDetailsTabIndex(0);
        showEventDetailsModal();
    }

    const onClickComments = () => {
        setEventDetailsTabIndex(7);
        showEventDetailsModal();
    }

    const onClickWorkflow = () => {
        setEventDetailsTabIndex(5);
        showEventDetailsModal();
    }

    const onClickAssets = () => {
        setEventDetailsTabIndex(3);
        showEventDetailsModal();
    }

    return (
        <>

            {/* Display modal for editing table view if table edit button is clicked */}
            <EventDetailsModal showModal={displayEventDetailsModal}
                               handleClose={hideEventDetailsModal}
                               tabIndex={eventDetailsTabIndex}
                               eventTitle={row.title}
                               eventId={row.id} />

            {/* Open event details */}
            {/*TODO: with-Role ROLE_UI_EVENTS_DETAILS_VIEW*/}
            <a onClick={() => onClickEventDetails()}
               className="more"
               title={t('EVENTS.EVENTS.TABLE.TOOLTIP.DETAILS')}/>

            {/* If event belongs to a series then the corresponding series details can be opened */}
            {!!row.series && (
                //{/*TODO: implement and properly call function
                //{/*TODO: with-Role ROLE_UI_SERIES_DETAILS_VIEW
                <a onClick={() => onClickSeriesDetails()}
                   className="more-series"
                   title={t('EVENTS.SERIES.TABLE.TOOLTIP.DETAILS')}/>

            )}

            {/* Delete an event */}
            {/*TODO: needs to be checked if event is published */}
            {/*TODO: with-Role ROLE_UI_EVENTS_DELETE*/}
            <a onClick={() => setDeleteConfirmation(true)}
               className="remove"
               title={t('EVENTS.EVENTS.TABLE.TOOLTIP.DELETE')}/>

            {/* Confirmation for deleting an event*/}
            {displayDeleteConfirmation && (
                <ConfirmModal close={hideDeleteConfirmation}
                              resourceName={row.title}
                              resourceType="EVENT"
                              resourceId={row.id}
                              deleteMethod={deletingEvent}/>
            )}

            {/* If the event has an preview then the editor can be opened and status if it needs to be cut is shown */}
            {!!row.has_preview && (
                // todo: When editor is implemented, fix url
                // todo: with-Role ROLE_UI_EVENTS_EDITOR_VIEW
                <a href="#!/events/events/${row.id}/tools/editor"
                   className="cut" title={row.needs_cutting ?
                    t('EVENTS.EVENTS.TABLE.TOOLTIP.EDITOR_NEEDS_CUTTING') :
                    t('EVENTS.EVENTS.TABLE.TOOLTIP.EDITOR')}>
                    {row.needs_cutting && (
                        <span id="badge" className="badge" />
                    )}
                </a>
            )}

            {/* If the event has comments and no open comments then the comment tab of event details can be opened directly */}
            {(row.has_comments && !row.has_open_comments) && (
                /*TODO: with-Role ROLE_UI_EVENTS_DETAILS_VIEW*/
                <a onClick={() => onClickComments()}
                   title={t('EVENTS.EVENTS.TABLE.TOOLTIP.COMMENTS')}
                   className="comments" />
            )}

            {/* If the event has comments and open comments then the comment tab of event details can be opened directly */}
            {(row.has_comments && row.has_open_comments) && (
                /*TODO: with-Role ROLE_UI_EVENTS_DETAILS_VIEW*/
                <a onClick={() => onClickComments()}
                   title={t('EVENTS.EVENTS.TABLE.TOOLTIP.COMMENTS')}
                   className="comments-open" />
            )}

            {/*If the event is in in a paused workflow state then a warning icon is shown and workflow tab of event
                details can be opened directly */}
            {row.workflow_state === 'PAUSED' && (
                //todo: with role ROLE_UI_EVENTS_DETAILS_WORKFLOWS_EDIT
                <a title={t('EVENTS.EVENTS.TABLE.TOOLTIP.PAUSED_WORKFLOW')}
                   onClick={() => onClickWorkflow()}
                   className="fa fa-warning"/>
            )}

            {/* Open assets tab of event details directly*/}
            {/*todo: with-role ROLE_UI_EVENTS_DETAILS_ASSETS_VIEW*/}
            <a onClick={() => onClickAssets()}
               title={t('EVENTS.EVENTS.TABLE.TOOLTIP.ASSETS')}
               className="fa fa-folder-open"/>

           {/* Open dialog for embedded code*/}
           {/*todo: with-role ROLE_UI_EVENTS_EMBEDDING_CODE_VIEW*/}
           <a onClick={() => onClickEmbeddedCode()}
              title={t('EVENTS.EVENTS.TABLE.TOOLTIP.EMBEDDING_CODE')}
              className="fa fa-link"/>
        </>
    );
}

//todo: implement!
const onClickSeriesDetails = () => {
    console.log("Should open series-details.");
}

//todo: implement!
const onClickEmbeddedCode = () => {
    console.log("Should open dialog for embedded code.");
}

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    deleteEvent: (id) => dispatch(deleteEvent(id))
});

export default connect(null, mapDispatchToProps)(EventActionCell);
