import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import {connect} from "react-redux";
import {getCurrentLanguageInformation} from "../../../../utils/utils";
import RenderMultiField from "./RenderMultiField";
import RenderField from "./RenderField";


// Get info about the current language and its date locale
const currentLanguage = getCurrentLanguageInformation();

/**
 * This component manages the pages of the new event wizard and the submission of values
 */
const EventDetailsWizard = ({ tabIndex }) => {// metadataFields,
    const { t } = useTranslation();
    const metadataFields = {fields: []};


    const [page, setPage] = useState(tabIndex);

    // Caption of steps used by Stepper
    const tabs = [
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.METADATA',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.METADATA.CAPTION',
            name: 'metadata'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.METADATA.CAPTION', //todo: here {{ catalog.title | translate }}
            name: 'metadata-extended',
            hidden: true
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.PUBLICATIONS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.PUBLICATIONS.CAPTION',
            name: 'publications'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.ASSETS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.ASSETS.CAPTION',
            name: 'assets',
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.SCHEDULING',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.SCHEDULING.CAPTION',
            name: 'scheduling',
            hidden: true
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.WORKFLOWS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.WORKFLOW_INSTANCES.TITLE', // todo: not quite right, has 2 top-level captions
            name: 'workflows'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.ACCESS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.TABS.ACCESS',
            name: 'access'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.COMMENTS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.COMMENTS.CAPTION',
            name: 'comments'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.STATISTICS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.METADATA.CAPTION',
            name: 'statistics',
            hidden: true
        }
    ];

    const openTab = (tabNr) => {
        console.log(`Should open tab "${tabNr}"`);
        setPage(tabNr);
    }

    return (
        <>
            <nav className='modal-nav' id='modal-nav'>
                <a className={cn({active: page === 0})}
                      onClick={() => openTab(0)}>
                    {t(tabs[0].tabNameTranslation)}
                </a>
                {
                    tabs[1].hidden ?
                        null :
                        <a className={cn({active: page === 1})}
                              onClick={() => openTab(1)}>
                            {t(tabs[1].tabNameTranslation)}
                        </a>
                }
                <a className={cn({active: page === 2})}
                      onClick={() => openTab(2)}>
                    {t(tabs[2].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 3})}
                      onClick={() => openTab(3)}>
                    {t(tabs[3].tabNameTranslation)}
                </a>
                {
                    tabs[4].hidden ?
                        null :
                        <a className={cn({active: page === 4})}
                              onClick={() => openTab(4)}>
                            {t(tabs[4].tabNameTranslation)}
                </a>
                }
                <a className={cn({active: page === 5})}
                      onClick={() => openTab(5)}>
                    {t(tabs[5].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 6})}
                      onClick={() => openTab(6)}>
                    {t(tabs[6].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 7})}
                      onClick={() => openTab(7)}>
                    {t(tabs[7].tabNameTranslation)}
                </a>
                {
                    tabs[8].hidden ?
                        null :
                        <a className={cn({active: page === 8})}
                              onClick={() => openTab(8)}>
                            {t(tabs[8].tabNameTranslation)}
                        </a>
                }
            </nav>

            {/* Initialize overall form */}
                        <div>
                            {page === 0 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 1 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 2 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 3  && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 4 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 5 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 6 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 7 && (
                                <CommentsPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 8 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                        </div>
        </>

    );
};

const CommentsPage = ({ header, t }) => {

    const reply = {
        creationDate: "2021-04-01T13:00",
        author: {
            name: "Simone Reply"
        },
        reason: "test",
        text: "This is an example for a comment reply"
    };

    const comment = {
        creationDate: "2021-04-01T12:00",
        author: {
            name: "Simone"
        },
        reason: "test",
        text: "This is an example for a comment",
        resolvedStatus: "resolved"
    };

    const originalComment = {
        creationDate: "2021-04-01T11:00",
        author: {
            name: "Simone Original"
        },
        reason: "test",
        text: "This is an example for an original comment",
        resolvedStatus: "open"
    };

    const localizeDate = false;

    return (
        <div className="modal-content">
            <div className="modal-body">
                <div data-admin-ng-notifications="" context="events-access"></div>
                <div className="full-col">
                    <div className="obj comments">
                        <header className="no-expand">{t(header)}</header>
                        {/* Table view containing input fields for metadata */}
                        <div className="obj-container">
                            <div className="comment-container">
                                <div className="comment"> {/* ng-class="{ active : $parent.replyToId === comment.id }"
                                     ng-repeat="comment in comments track by $index">*/}
                                    <hr/>
                                        <div className="date">{ comment.creationDate | localizeDate }</div> {/*: 'dateTime' : 'short'*/}
                                        <h4>{ comment.author.name }</h4>
                                        <span className="category">
                                            <strong> {t("EVENTS.EVENTS.DETAILS.COMMENTS.REASON")} </strong>: { comment.reason /*| translate*/ }
                                        </span>
                                        <p>{ comment.text }</p>
                                        <a onClick={() => console.log("Delete comment here.")/*deleteComment(comment.id)*/}
                                           className="delete">
                                            {t('EVENTS.EVENTS.DETAILS.COMMENTS.DELETE')}
                                        </a>
                                        <a onClick={() => console.log("Reply to comment here.")/*replyTo(comment)*/}
                                           className="reply" with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_REPLY">
                                            {t('EVENTS.EVENTS.DETAILS.COMMENTS.REPLY')}
                                        </a>
                                        <span className="resolve" ng-class="{ resolved : comment.resolvedStatus }">
                                            { t('EVENTS.EVENTS.DETAILS.COMMENTS.RESOLVED') }
                                        </span>

                                        <div className="comment is-reply"> {/* ng-repeat="reply in comment.replies track by $index">*/}
                                            <hr/>
                                            <div className="date">{ reply.creationDate | localizeDate }</div> {/*: 'dateTime' : 'short'*/}
                                            <h4>{ reply.author.name }</h4>
                                            <span className="category">
                                                <strong> { t("EVENTS.EVENTS.DETAILS.COMMENTS.REASON")} </strong>: { comment.reason /*| translate */}
                                            </span>
                                            <p>
                                                <span>@{ comment.author.name }</span> { reply.text }
                                            </p>
                                            <a onClick={ () => console.log("Delete comment reply here") /*deleteCommentReply(comment.id, reply.id)*/}
                                               className="delete"
                                               with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_DELETE">
                                                <i className="fa fa-times-circle"></i> { t('EVENTS.EVENTS.DETAILS.COMMENTS.DELETE') }
                                            </a>
                                        </div>
                                </div>
                            </div>
                        </div>

                        <form className="add-comment"> {/* ng-if="replyToId === null" with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_CREATE">*/}
                <textarea  placeholder={ t('EVENTS.EVENTS.DETAILS.COMMENTS.PLACEHOLDER') }>{/*ng-model="myComment.text"*/}</textarea>
                <div>
                  <select chosen
                          pre-select-from="components.eventCommentReasons"
                          data-width="'200px'"

                          placeholder-text-single={ t('EVENTS.EVENTS.DETAILS.COMMENTS.SELECTPLACEHOLDER')}
                          >{/*ng-model="myComment.reason"*/}
                      {/*ng-options="value | translate for (id, value) in components.eventCommentReasons"*/}
                          <option value=""></option>
                  </select>
                </div>
                <button ng-class="{ disabled: !myComment.text.length || !myComment.reason.length || myComment.saving }" className="save green" onClick={ () => console.log("comment()")}> { t("SUBMIT") }
                    {/*<!-- Submit -->*/}
                </button>
              </form>

              <form className="add-comment reply"> {/* ng-if="replyToId !== null">*/}
                <textarea
                          placeholder={ t('EVENTS.EVENTS.DETAILS.COMMENTS.REPLY_TO') + "@" +  originalComment.author.name }>{/*ng-model="myComment.text"*/}</textarea>
                <button
                        className="save green"
                        onClick={console.log("reply()")}>{/*ng-class="{ disabled: !myComment.text.length || myComment.saving }"*/}
                    { t("EVENTS.EVENTS.DETAILS.COMMENTS.REPLY")}
                  {/*<!-- Reply -->*/}
                </button>
                <button className="red" onClick={console.log("exitReplyMode()")} > { t("EVENTS.EVENTS.DETAILS.COMMENTS.CANCEL_REPLY")}
                  {/*<!-- Cancel -->*/}
                </button>
                <input with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_RESOLVE" type="checkbox"  id="resolved-checkbox" className="ios"/>{/*ng-model="myComment.resolved"*/}
                <label with-role="ROLE_UI_EVENTS_DETAILS_COMMENTS_RESOLVE" for="resolved-checkbox" > { t("EVENTS.EVENTS.DETAILS.COMMENTS.RESOLVED")}
                  {/*<!-- Resolved -->*/}
                </label>
              </form>
                    </div>
                </div>
            </div>
        </div>
    );
};

const MockDataPage = ({ header, t }) => {

    return (
        <div className="modal-content">
            <div className="modal-body">
                <div className="full-col">
                    <div className="obj tbl-details">
                        <header className="no-expand">{t(header)}</header>
                        {/* Table view containing input fields for metadata */}
                        <div className="obj-container">
                            <table class="main-tbl">
                                <tr>
                                    <td>
                                        <span>Content coming soon!</span>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
});



export default connect(mapStateToProps)(EventDetailsWizard);
