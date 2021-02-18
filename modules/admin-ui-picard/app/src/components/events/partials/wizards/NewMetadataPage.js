import React from "react";
import {useTranslation} from "react-i18next";
import {Field} from "formik";
import cn from 'classnames';
import RenderField from "./RenderField";
import RenderMultiField from "./RenderMultiField";

/**
 * This component renders the metadata page for new events and series in the wizards.
 */
const NewMetadataPage = ({ metadataFields, nextPage, formik, header }) => {
    const { t } = useTranslation();

    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <div className="obj tbl-list">
                            <header className="no-expand">{t(header)}</header>
                            {/* Table view containing input fields for metadata */}
                            <div className="obj-container">
                                <table className="main-tbl">
                                    <tbody>
                                    {/* Render table row for each metadata field depending on type*/}
                                    {metadataFields.fields.map((field, key) =>
                                        (
                                            <tr key={key}>
                                                <td>
                                                    <span>{t(field.label)}</span>
                                                    {field.required ? (
                                                        <i className="required">*</i>
                                                    ) : null}
                                                </td>
                                                <td className="editable ng-isolated-scope">
                                                    {/* Render single value or multi value input */}
                                                    {(field.type === "mixed_text" && field.collection.length !== 0) ? (
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

            {/* Button for navigation to next page */}
            <footer>
                <button type="submit"
                        className={cn("submit",
                            {
                                active: (formik.dirty && formik.isValid),
                                inactive: !(formik.dirty && formik.isValid)
                            })}
                        disabled={!(formik.dirty && formik.isValid)}
                        onClick={() => {
                            console.log("CURRENT FORMIK VALUES");
                            console.log(formik.values);
                            nextPage(formik.values);
                        }}
                        tabIndex="100">{t('WIZARD.NEXT_STEP')}</button>
            </footer>

            <div className="btm-spacer"/>
        </>
    )
};

export default NewMetadataPage;
