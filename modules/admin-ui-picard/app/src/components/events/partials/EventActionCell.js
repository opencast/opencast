import React from 'react';
import {useTranslation} from "react-i18next";



/**
 * This component renders the action cells of events in the table view
 */
const EventActionCell = ({ row })  => {
    const { t } = useTranslation();
    return (
        <>
            {/* Open event details */}
            {/*TODO: When event details are implemented, remove placeholder */}
            {/*TODO: with-Role ROLE_UI_EVENTS_DETAILS_VIEW*/}
            <a onClick={() => onClickPlaceholder()}
               className="more"
               title={t('EVENTS.EVENTS.TABLE.TOOLTIP.DETAILS')}/>

            {/* If event belongs to a series then the corresponding series details can be opened */}
            {!!row.series && (
                //{/*TODO: When series details are implemented, remove placeholder
                //{/*TODO: with-Role ROLE_UI_SERIES_DETAILS_VIEW
                <a onClick={() => onClickPlaceholder()}
                   className="more-series"
                   title={t('EVENTS.SERIES.TABLE.TOOLTIP.DETAILS')}/>

            )}

            {/* Delete an event */}
            {/*TODO: When event action for deleting an event is implemented, remove placeholder,
            needs to be checked if event is published */}
            {/*TODO: with-Role ROLE_UI_EVENTS_DELETE*/}
            <a onClick={() => onClickPlaceholder()}
               className="remove"
               title={t('EVENTS.EVENTS.TABLE.TOOLTIP.DELETE')}/>

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
                //todo: when eventDetails are implemented, remove placeholder (opens comment-tab)
                <a onClick={() => onClickPlaceholder()}
                   title={t('EVENTS.EVENTS.TABLE.TOOLTIP.COMMENTS')}
                   className="comments" />
            )}

            {/* If the event has comments and open comments then the comment tab of event details can be opened directly */}
            {(row.has_comments && row.has_open_comments) && (
                //todo: when eventDetails are implemented, remove placeholder (opens comment-tab)
                <a onClick={() => onClickPlaceholder()}
                   title={t('EVENTS.EVENTS.TABLE.TOOLTIP.COMMENTS')}
                   className="comments-open" />
            )}

            {/*If the event is in in a paused workflow state then a warning icon is shown and workflow tab of event
                details can be opened directly */}
            {row.workflow_state === 'PAUSED' && (
                //todo: when eventDetails are implemented, remove placeholder (opens workflow-tab)
                //todo: with role ROLE_UI_EVENTS_DETAILS_WORKFLOWS_EDIT
                <a title={t('EVENTS.EVENTS.TABLE.TOOLTIP.PAUSED_WORKFLOW')}
                   onClick={() => onClickPlaceholder()}
                   className="fa fa-warning"/>
            )}

            {/* Open assets tab of event details directly*/}
            {/*Todo: when eventDetails are implemented, remove placeholder (opens asset-tab)*/}
            {/*todo: with-role ROLE_UI_EVENTS_DETAILS_ASSETS_VIEW*/}
            <a onClick={() => onClickPlaceholder()}
               title={t('EVENTS.EVENTS.TABLE.TOOLTIP.ASSETS')}
               className="fa fa-folder-open"/>

           {/* Open dialog for embedded code*/}
           {/*todo: with-role ROLE_UI_EVENTS_EMBEDDING_CODE_VIEW*/}
           <a onClick={() => onClickPlaceholder()}
              title={t('EVENTS.EVENTS.TABLE.TOOLTIP.EMBEDDING_CODE')}
              className="fa fa-link"/>
        </>
    );
};

//todo: remove if not needed anymore
const onClickPlaceholder = () => {
    console.log("In the Future here opens an other component, which is not implemented yet");
}


export default EventActionCell;
