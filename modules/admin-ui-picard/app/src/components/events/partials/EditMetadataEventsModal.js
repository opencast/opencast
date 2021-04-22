import React, {useEffect} from "react";
import {Formik, Field} from "formik";
import {useTranslation} from "react-i18next";
import {getSelectedRows} from "../../../selectors/tableSelectors";
import {connect} from "react-redux";
import {MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import {getCurrentLanguageInformation} from "../../../utils/utils";
import cn from "classnames";
import {getEventMetadata} from "../../../selectors/eventSelectors";
import RenderMultiField from "../../shared/wizard/RenderMultiField";
import RenderField from "./wizards/RenderField";


// Get info about the current language and its date locale
const currentLanguage = getCurrentLanguageInformation();

const EditMetadataEventsModal = ({ close, selectedRows, metadataFields }) => {
    const { t } = useTranslation();

    const initialValues = getInitialValues(metadataFields);

    const handleSubmit = values => {
        console.log('to be implemented');
        close();
    };

    return (
        <>
            <div className="modal-animation modal-overlay"/>
            <section className="modal wizard modal-animation">
                <header>
                    <a className="fa fa-times close-modal" onClick={() => close()}/>
                    <h2>{t('BULK_ACTIONS.EDIT_EVENTS_METADATA.CAPTION')}</h2>
                </header>

                {/* Loading spinner */}
                {/* todo: show only if loading */}
                <div className="modal-content">
                    <div className="modal-body">
                        <div className="loading">
                            <i className="fa fa-spinner fa-spin fa-2x fa-fw"/>
                        </div>
                    </div>
                </div>

                {/* Fatal error view */}
                {/* todo: show only if there is an error */}
                <div className="modal-content">
                    <div className="modal-body">
                        <div className="row">
                            <div className="alert sticky error">
                                {/* todo: adjust translation parameter */}
                                <p>{t('BULK_ACTIONS.EDIT_EVENTS_METADATA.FATAL_ERROR', {fatalError: 'fatalError'})}</p>
                            </div>
                        </div>
                    </div>
                </div>

                {/* todo: Request Errors View and Update Errors View (not quite sure what this is used for) */}
                <MuiPickersUtilsProvider utils={DateFnsUtils} locale={currentLanguage.dateLocale}>
                    <Formik initialValues={initialValues}
                            onSubmit={values => handleSubmit(values)}>
                        {formik => (
                            <>
                                <div className="modal-content">
                                    <div className="modal-body">
                                        <div className="full-col">
                                            <div className="obj header-description">
                                                <span>{t('BULK_ACTIONS.EDIT_EVENTS_METADATA.EDIT.DESCRIPTION')}</span>
                                            </div>
                                            <div className="obj tbl-details">
                                                <header>
                                                    <span>{t('BULK_ACTIONS.EDIT_EVENTS_METADATA.EDIT.TABLE.CAPTION')}</span>
                                                </header>
                                                <div className="obj-container">
                                                    <table className="main-tbl">
                                                        <thead>
                                                        <tr>
                                                            <th className="small"/>
                                                            <th>
                                                                {t('BULK_ACTIONS.EDIT_EVENTS_METADATA.EDIT.TABLE.FIELDS')}
                                                            </th>
                                                            <th>
                                                                {t('BULK_ACTIONS.EDIT_EVENTS_METADATA.EDIT.TABLE.VALUES')}
                                                            </th>
                                                        </tr>
                                                        </thead>
                                                        <tbody>
                                                        {/* todo: Repeat for each metadata row */}
                                                        {metadataFields.fields.map((field, key) => (
                                                            <tr key={key}>
                                                                <td>
                                                                    <input type="checkbox"/>
                                                                </td>
                                                                <td>
                                                                    <span>{t(field.label)}</span>
                                                                    {field.required && (
                                                                        <i className="required">*</i>
                                                                    )}
                                                                </td>
                                                                <td className="editable ng-isolated-scope">
                                                                    {/* Render single value or multi value input */}
                                                                    {(field.type === 'mixed_text' && field.collection.length !== 0) ? (
                                                                        <RenderMultiField fieldInformation={field}/>
                                                                    ) : (
                                                                        <Field name={field.id}
                                                                               metadataField={field}
                                                                               component={RenderField}/>
                                                                    )}
                                                                </td>
                                                            </tr>
                                                        ))}
                                                        </tbody>
                                                    </table>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <footer>
                                    <button type="submit"
                                            onClick={() => formik.handleSubmit()}
                                            disabled={!(formik.dirty && formik.isValid)}
                                            className={cn("submit",
                                                {
                                                    active: (formik.dirty && formik.isValid),
                                                    inactive: !(formik.dirty && formik.isValid)
                                                }
                                            )}>
                                        {t('WIZARD.UPDATE')}
                                    </button>
                                    <button onClick={() => close()}
                                            className="cancel">
                                        {t('WIZARD.BACK')}
                                    </button>
                                </footer>

                                <div className="btm-spacer"/>
                            </>
                        )}
                    </Formik>
                </MuiPickersUtilsProvider>
            </section>
        </>
    );
};

const getInitialValues = metadataFields => {
    // Transform metadata fields provided by backend (saved in redux)
    let initialValues = {};
    metadataFields.fields.forEach(field => {
        initialValues[field.id] = field.value;
    });

    return initialValues;
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    selectedRows: getSelectedRows(state),
    metadataFields: getEventMetadata(state)
});

export default connect(mapStateToProps)(EditMetadataEventsModal);
