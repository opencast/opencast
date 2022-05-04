import React from "react";
import { useTranslation } from 'react-i18next';
import { Field } from "formik";
import RenderMultiField from '../../../shared/wizard/RenderMultiField';
import RenderField from '../../../shared/wizard/RenderField';
import WizardNavigationButtons from '../../../shared/wizard/WizardNavigationButtons';

const NewMetadataExtendedPage = ({ previousPage, nextPage, formik, header, extendedMetadataFields }) => {

  const { t } = useTranslation();

        return (
            <>
              <div className="modal-content">
                <div className="modal-body">
                  <div className="full-col">
                    <div className="obj tbl-list">
                      <header className="no-expand">{t(header)}</header>
                      <div className="obj-container">
                        <table className="main-tbl">
                          <tbody>
                            {!!extendedMetadataFields.fields && extendedMetadataFields.fields.map((field, key) => (
                              <tr key={key}>
                                <td>
                                  <span>{t(field.label)}</span>
                                  {field.required && (
                                    <i className="required">*</i>
                                  )}
                                </td>
                                <td>
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
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              {/* Button for navigation to next page */}
              <WizardNavigationButtons noValidation
                                       nextPage={nextPage}
                                       previousPage={previousPage}
                                       formik={formik}/>
            </>
        );
}

export default NewMetadataExtendedPage;
