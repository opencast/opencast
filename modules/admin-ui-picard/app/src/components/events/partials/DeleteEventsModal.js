import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";

const DeleteEventsModal = ({ close }) => {
    const { t } = useTranslation();

    const [placeholder, setPlaceholder] = useState('');

    const handlePlaceholder = () => {
        console.log('to be implemented');
    }
    return (
        <>
            <div className="modal-animation modal-overlay"/>
            <section className="modal active modal-open"
                     id="delete-events-status-modal"
                     style={{display: "block"}}>
                <header>
                    <a onClick={close}
                       className="fa fa-times close-modal"/>
                   <h2>{t('BULK_ACTIONS.DELETE.EVENTS.CAPTION')}</h2>
                </header>

                <div className="modal-content active">
                    <div className="modal-body">

                        <div className="full-col">
                            <div className="list-obj">
                                <div className="modal-alert danger obj">
                                    <p>{t('BULK_ACTIONS.DELETE_EVENTS_WARNING_LINE1')}</p>
                                    <p>{t('BULK_ACTIONS.DELETE_EVENTS_WARNING_LINE2')}</p>
                                </div>
                                {/*todo: only show if scheduling Authorized*/}
                                <div>
                                    <p>{t('BULK_ACTIONS.DELETE.EVENTS.UNAUTHORIZED')}</p>
                                </div>

                                <div className="full-col">
                                    <div className="obj">
                                        <header>{t('BULK_ACTIONS.DELETE.EVENTS.DELETE_EVENTS')}</header>
                                        <table className="main-tbl">
                                            <thead>
                                                <tr>
                                                    <th className="small">
                                                        {/*todo: replace placeholder*/}
                                                        <input type="checkbox"
                                                               value={placeholder}
                                                               onChange={() => handlePlaceholder()}
                                                               className="select-all-cbox"/>
                                                    </th>
                                                    <th>{t('EVENTS.EVENTS.TABLE.TITLE')}</th>
                                                    <th>{t('EVENTS.EVENTS.TABLE.PRESENTERS')}</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {/*todo: repeat for each marked event*/}
                                                <tr>
                                                    <td>
                                                        <input className="child-cbox"
                                                               name="selection"
                                                               type="checkbox"
                                                               value={placeholder}
                                                               onChange={() => handlePlaceholder()}/>
                                                    </td>
                                                    <td>row.title</td>
                                                    <td>
                                                        {/*todo: repeat for each presenter*/}
                                                        <span className="metadata-entry">presenter</span>
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <footer>
                    <button onClick={() => handlePlaceholder()}
                            // todo: validation
                            className={cn("danger", {active: true})}>
                        {t('WIZARD.DELETE')}
                    </button>
                    <button onClick={() => close()} className="cancel">{t('CANCEL')}</button>
                </footer>

                <div className="btm-spacer"/>
            </section>
        </>
    );
};

export default DeleteEventsModal;
