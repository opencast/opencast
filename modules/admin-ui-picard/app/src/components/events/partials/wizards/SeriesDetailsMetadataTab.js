import React from "react";
import {useTranslation} from "react-i18next";
import {Formik, Field} from "formik";
import Notifications from "../../../shared/Notifications";
import RenderMultiField from "../../../shared/wizard/RenderMultiField";
import RenderField from "./RenderField";

const SeriesDetailsMetadataTab = ({ metadataFields }) => {
    const { t } = useTranslation();

    const handleSubmit = values => {
        console.log(values);
    }

    const getInitialValues = () => {
        let initialValues = {};
        metadataFields.fields.forEach(field => {
            initialValues[field.id] = field.value;
        });

        return initialValues;
    }

    return (
        <div className="modal-content">
            <div className="modal-body">
                <Notifications context="not-corner"/>
                <div className="full-col">
                    <div className="obj tbl-list">
                        <header className="no-expand">
                            {t('EVENTS.SERIES.DETAILS.TABS.METADATA')}
                        </header>
                        <div className="obj-container">
                            <Formik initialValues={getInitialValues()} onSubmit={values => handleSubmit(values)}>
                                <table className="main-tbl">
                                    <tbody>
                                    {/* todo: repeat for each metadata entry*/}
                                    {metadataFields.fields.map((field, key) => (
                                        <tr key={key}>
                                            <td>
                                                <span>{t(field.label)}</span>
                                                {field.required && (
                                                    <i className="required">*</i>
                                                )}
                                            </td>
                                            <td className="editable ng-isolated-scope">
                                                {/* todo: role: ROLE_UI_SERIES_DETAILS_METADATA_EDIT */}
                                                {(field.type === "mixed_text" && field.collection.length !== 0) ? (
                                                    <Field name={field.id}
                                                           fieldInfo={field}
                                                           component={RenderMultiField}/>
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
                            </Formik>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};



export default SeriesDetailsMetadataTab;
