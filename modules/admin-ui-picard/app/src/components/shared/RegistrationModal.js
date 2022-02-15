import React, {useState} from 'react';
import {useTranslation} from "react-i18next";

const RegistrationModal = ({ close }) => {
    const { t } = useTranslation();

    const [state, setState] = useState('information');

    const handleClose = () => {
        close();
    };

    return (
        <>
            <div className="modal-animation modal-overlay"/>
            <section id="registration-modal"
                     className="modal active modal-open modal-animation">
                <header>
                    {/*todo: Buttons can be different depending on the state of buttons (see old UI)*/}
                    <a onClick={() => handleClose()} className="fa fa-times close-modal"/>
                    <h2>{t('ADOPTER_REGISTRATION.MODAL.CAPTION')}</h2>
                </header>

                {state === "information" && (
                    <div className="modal-content"
                         style={{display: "block"}}>
                        <div className="modal-body">
                            <div className="registration-header"
                                 style={{padding: "5px 0 15px 0"}}>
                                <h2>{t('ADOPTER_REGISTRATION.MODAL.INFORMATION_STATE.HEADER')}</h2>
                            </div>
                            <div>
                                <div className="row">
                                    <p>{t('ADOPTER_REGISTRATION.MODAL.INFORMATION_STATE.INFORMATION_PARAGRAPH_1')}</p>
                                    <br />
                                    <p>{t('ADOPTER_REGISTRATION.MODAL.INFORMATION_STATE.INFORMATION_PARAGRAPH_2')}</p>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {state === "legal_info" && (
                    <div className="modal-content" style={{display: "block"}}>
                        <div className="modal-body">
                            <div>
                                <div className="row">
                                    <div className="scrollbox">
                                        {/*todo: legal terms of use*/}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {state === "skip" && (
                    <div className="modal-content"
                         style={{display: "block"}}>
                        <div className="modal-body">
                            <div className="registration-header">
                                <h2>{t('ADOPTER_REGISTRATION.MODAL.SKIP_STATE.HEADER')}</h2>
                            </div>
                            <div>
                                <div className="row">
                                    <p>
                                        <span>{t('ADOPTER_REGISTRATION.MODAL.SKIP_STATE.TEXT')}</span>
                                        <br />
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {state === "delete_submit" && (
                    <div className="modal-content"
                         style={{display: "block"}}>
                        <div className="modal-body">
                            <p>
                                <span>{t('ADOPTER_REGISTRATION.MODAL.DELETE_SUBMIT_STATE.TEXT')}</span>
                            </p>
                        </div>
                    </div>
                )}

                {(state === "save" || state === "delete" || state === "update") && (
                    <div className="modal-content"
                         style={{display: "block"}}>
                        <div className="modal-body">
                            <div>
                                <div className="row spinner-container">
                                    <i className="fa fa-spinner fa-spin fa-4x fa-fw"/>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {state === "thank_you" && (
                    <div className="modal-content"
                         style={{display: "block"}}>
                        <div className="modal-body">
                            <div className="registration-header">
                                <h2>{t('ADOPTER_REGISTRATION.MODAL.THANK_YOU_STATE.HEADER')}</h2>
                            </div>
                            <div>
                                <div>
                                    <p>
                                        <span>
                                            {t('ADOPTER_REGISTRATION.MODAL.THANK_YOU_STATE.TEXT_LEADING_TO_PATH')}
                                        </span>
                                        <b>
                                            (<span>{t('HELP.HELP')}</span>)
                                            <span className="fa fa-question-circle"/>
                                            >
                                            <span>{t('HELP.ADOPTER_REGISTRATION')}</span>
                                        </b>
                                        <span>{t('ADOPTER_REGISTRATION.MODAL.THANK_YOU_STATE.TEXT_LEADING_AFTER_PATH')}</span>
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {state === "error" && (
                    <div className="modal-content"
                         style={{display: "block"}}>
                        <div className="modal-body">
                            <div className="registration-header">
                                <h2>{t('ADOPTER_REGISTRATION.MODAL.ERROR.HEADER')}</h2>
                            </div>
                            <div>
                                <div className="row">
                                    <p>
                                        <span>{t('ADOPTER_REGISTRATION.MODAL.ERROR.TEXT')}</span>
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                <form>
                    {state === "form" && (
                        <div className="modal-content"
                             style={{display: "block"}}>
                            <div className="modal-body">
                                <div>
                                    <fieldset>
                                        <legend>{t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.ORGANISATION')}</legend>
                                        <div className="row">
                                            <div className="col">
                                                <div className="form-group">
                                                    <input type="text"
                                                           id="adopter_organisation"
                                                           className="form-control"/>
                                                    <label className="form-control-placeholder"
                                                           for="adopter_organisation">
                                                        {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.ORGANISATION')}
                                                    </label>
                                                </div>
                                            </div>
                                            <div className="col">
                                                <div>
                                                    <input type="text"
                                                           id="adopter_department"
                                                           className="form-control"/>
                                                    <label className="form-control-placeholder"
                                                           for="adopter_department">
                                                        {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.DEPARTMENT')}
                                                    </label>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="row">
                                            <div className="col">
                                                <div className="form-group">
                                                    <select style={{color: "#666", fontWeight: "600"}}
                                                            id="adopter_country"
                                                            className="form-control">
                                                        <option value=""/>
                                                        <option value="land">Land</option>
                                                    </select>
                                                    <label className="form-control-placeholder"
                                                           for="adopter_country">
                                                        {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.COUNTRY')}
                                                    </label>
                                                </div>
                                            </div>
                                            <div className="col">
                                                <div className="form-group-pair">
                                                    <div className="form-group">
                                                        <input type="text"
                                                               id="adopter_postalcode"
                                                               className="form-control"/>
                                                        <label className="form-control-placeholder"
                                                               for="adopter_postalcode">
                                                            {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.POSTAL_CODE')}
                                                        </label>
                                                    </div>
                                                    <div className="form-group">
                                                        <input type="text"
                                                               id="adopter_city"
                                                               className="form-control"/>
                                                        <label className="form-control-placeholder"
                                                               for="adopter_city">
                                                            {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.CITY')}
                                                        </label>
                                                    </div>
                                                </div>
                                            </div>
                                        </div>
                                    </fieldset>
                                    <fieldset>
                                        <legend>{t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.CONTACT_INFO')}</legend>
                                        <div className="row">
                                            <div className="col">
                                                <div className="form-group">
                                                    <input type="text"
                                                           id="adopter_firstname"
                                                           className="form-control"/>
                                                    <label className="form-control-placeholder"
                                                           for="adopter_firstname">
                                                        {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.FIRST_NAME')}
                                                    </label>
                                                </div>
                                            </div>
                                            <div>
                                                <div>
                                                    <input type="text"
                                                           id="adopter_lastname"
                                                           className="form-control"/>
                                                    <label className="form-control-placeholder"
                                                           for="adopter_lastname">
                                                        {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.LAST_NAME')}
                                                    </label>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="row">
                                            <div className="col">
                                                <div className="form-group">
                                                    <input type="text"
                                                           id="adopter_street"
                                                           className="form-control"/>
                                                    <label className="form-control-placeholder"
                                                           for="adopter_street">
                                                        {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.STREET')}
                                                    </label>
                                                </div>
                                            </div>
                                            <div className="col">
                                                <div className="form-group">
                                                    <input type="text"
                                                           id="adopter_streetnumber"
                                                           className="form-control"/>
                                                    <label className="form-control-placeholder"
                                                           for="adopter_streetnumber">
                                                        {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.NUMBER')}
                                                    </label>
                                                </div>
                                            </div>
                                        </div>
                                        <div className="row">
                                            <div className="col">
                                                <div className="form-group">
                                                    <input id="adopter_emailadr"
                                                           type="email"
                                                           className="form-control"/>
                                                    <label className="from-control-placeholder"
                                                           for="adopter_emailadr">
                                                        {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.MAIL')}
                                                    </label>
                                                </div>
                                            </div>
                                            <div className="col">
                                                <div className="form-group form-group-checkbox">
                                                    <input type="checkbox"
                                                           id="adopter_contactme"
                                                           className="form-control"/>
                                                    <label for="adopter_contactme">
                                                        {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.CONTACT_ME')}
                                                    </label>
                                                </div>
                                            </div>
                                        </div>
                                    </fieldset>
                                    <fieldset>
                                        <legend>{t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.WHICH_DATA_TO_SHARE')}</legend>
                                        <div className="form-group form-group-checkbox">
                                            <input type="checkbox"
                                                   id="adopter_allows_statistics"
                                                   className="form-control"/>
                                            <label for="adopter_allows_statistics">{t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.USAGE_STATISTICS')}</label>
                                        </div>
                                        <div>
                                            <input type="checkbox"
                                                   id="adopter_allows_err_reports"
                                                   className="form-control"/>
                                            <label for="adopter_allows_err_reports">{t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.ERROR_REPORTS')}</label>
                                        </div>
                                    </fieldset>
                                    <fieldset>
                                        <legend>{t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.POLICY_HEADLINE')}</legend>
                                        <div>
                                            <input type="checkbox"
                                                   id="agreedToPolicy"
                                                   className="form-control"/>
                                            <label for="agreedToPolicy">
                                                <span>{t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.READ_TERMS_OF_USE_BEFORE')}</span>
                                                <span className="link"
                                                      onClick={() => console.log("Things happen")}>
                                                    {t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.READ_TERMS_OF_USE_LINK')}
                                                </span>
                                                <span>{t('ADOPTER_REGISTRATION.MODAL.FORM_STATE.READ_TERMS_OF_USE_AFTER')}</span>
                                            </label>
                                        </div>
                                    </fieldset>
                                </div>
                            </div>
                        </div>
                    )}

                    <footer>
                        <div className="pull-right">
                            {/* todo: make buttons state dependent and add corresponding on click(see old ui)*/}
                            <button className="continue-registration">submitbuttontext</button>
                            {/* todo: style={{inactive: !adopter.agreedToPolicy}}*/}
                            <button className="submit inactive">submitbuttontext</button>
                        </div>

                        <div className="pull-left">
                            {/* todo: make buttons state dependent and add corresponding on click(see old ui)*/}
                            <button className="cancel">{t('ADOPTER_REGISTRATION.MODAL.BACK')}</button>
                            <button className="danger">{t('WIZARD.DELETE')}</button>
                            <button className="cancel">{t('ADOPTER_REGISTRATION.MODAL.SKIP')}</button>
                        </div>
                    </footer>
                </form>
            </section>
        </>
    )
}

export default RegistrationModal;
