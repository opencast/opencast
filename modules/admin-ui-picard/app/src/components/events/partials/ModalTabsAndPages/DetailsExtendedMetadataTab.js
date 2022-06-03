import React from "react";
import {useTranslation} from "react-i18next";
import {Field, Formik} from "formik";
import cn from "classnames";
import _ from 'lodash';
import Notifications from "../../../shared/Notifications";
import RenderMultiField from "../../../shared/wizard/RenderMultiField";
import RenderField from "../../../shared/wizard/RenderField";
import {connect} from "react-redux";
import {getUserInformation} from "../../../../selectors/userInfoSelectors";
import {hasAccess, isJson} from "../../../../utils/utils";
import {getMetadataCollectionFieldName} from "../../../../utils/resourceUtils";

/**
 * This component renders metadata details of a certain event or series
 */
const DetailsExtendedMetadataTab = ({ resourceId, editAccessRole, metadata, updateResource,
                                             user }) => {
    const { t } = useTranslation();

    const handleSubmit = (values, catalog) => {
        updateResource(resourceId, values, catalog);
    }

    // set current values of metadata fields as initial values
    const getInitialValues = (metadataCatalog) => {
        let initialValues = {};

        // Transform metadata fields and their values provided by backend (saved in redux)
        if (!!metadataCatalog.fields && metadataCatalog.fields.length > 0) {
            metadataCatalog.fields.forEach(field => {
                let value = field.value
                if (value === 'true') {
                    value = true;
                } else if (value === 'false') {
                    value = false;
                }
                initialValues[field.id] = value;
            });
        }

        return initialValues;
    }

    const checkValidity = formik => {
        if (formik.dirty && formik.isValid
            && hasAccess(editAccessRole, user)) {
            // check if user provided values differ from initial ones
            return !_.isEqual(formik.values, formik.initialValues);
        } else {
            return false;
        }
    }

    return (
        <div className="modal-content">
            <div className="modal-body">
                {/* Notifications */}
                <Notifications context="not_corner"/>

                <div className="full-col">

                    {//iterate through metadata catalogs
                    !!metadata && metadata.length > 0 && metadata.map((catalog, key) => (
                        // initialize form
                        <Formik enableReinitialize
                                initialValues={getInitialValues(catalog)}
                                onSubmit={(values) => handleSubmit(values, catalog)}>
                            {formik => (

                                /* Render table for each metadata catalog */
                                <div className="obj tbl-details" key={key}>
                                    <header>
                                        <span>{t(catalog.title)}</span>
                                    </header>
                                    <div className="obj-container">
                                        <table className="main-tbl">
                                            <tbody>
                                            {/* Render table row for each metadata field depending on type */}
                                            {!!catalog.fields && catalog.fields.map((field, index) => (
                                                <tr key={index}>
                                                    <td>
                                                        <span>{t(field.label)}</span>
                                                        {field.required && (
                                                            <i className="required">*</i>
                                                        )}
                                                    </td>
                                                    {(field.readOnly || !hasAccess(editAccessRole, user)) ? (

                                                        // non-editable field if readOnly is set or user doesn't have edit access rights
                                                        (!!field.collection && field.collection.length !== 0) ? (
                                                            <td>
                                                                {isJson(getMetadataCollectionFieldName(field, field)) ?
                                                                    (t(JSON.parse(getMetadataCollectionFieldName(field, field)).label)) :
                                                                    (t(getMetadataCollectionFieldName(field, field)))}
                                                            </td>
                                                        ) : (
                                                            <td>{field.value}</td>
                                                        )
                                                    ) : (
                                                        <td className="editable ng-isolated-scope">
                                                            {/* Render single value or multi value editable input */}
                                                            {(field.type === "mixed_text" && field.collection.length !== 0) ? (
                                                                <Field name={field.id}
                                                                       fieldInfo={field}
                                                                       showCheck
                                                                       component={RenderMultiField}/>
                                                            ) : (
                                                                <Field name={field.id}
                                                                       metadataField={field}
                                                                       showCheck
                                                                       component={RenderField}/>
                                                            )}
                                                        </td>
                                                    )}
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>

                                    {formik.dirty && (
                                        <>
                                            {/* Render buttons for updating metadata */}
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
                                                    {t('SAVE')}
                                                </button>
                                                <button className="cancel"
                                                        onClick={() => formik.resetForm({values: ''})}>
                                                    {t('CANCEL')}
                                                </button>
                                            </footer>

                                            <div className="btm-spacer"/>
                                        </>
                                    )}
                                </div>
                            )}
                        </Formik>
                    ))}
                </div>
            </div>
        </div>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    user: getUserInformation(state)
});

export default connect(mapStateToProps)(DetailsExtendedMetadataTab);
