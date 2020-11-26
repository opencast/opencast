import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import {useField} from "formik";

const childRef = React.createRef();

/**
 * This component renders an editable field for multiple values depending on the type of the corresponding metadata
 */
const RenderMultiField = ({ metadataField }) => {
    // Indicator if currently edit mode is activated
    const [editMode, setEditMode] = useState(false);
    // Temporary storage for value user currently types in
    const [inputValue, setInputValue] = useState('');

    // Formik hook for getting data of specific form field
    // DON'T delete meta, hook works with indices not variable names
    const [field, meta, helpers] = useField(metadataField.id);

    useEffect(() => {
        const handleClickOutside = e => {
            // Handle click outside the field and leave edit mode
            if(childRef.current && !childRef.current.contains(e.target)) {
                setEditMode(false);
            }
        }

        // Focus current field
        if (childRef && childRef.current && editMode === true) {
            childRef.current.focus();
        }

        // Adding event listener for detecting click outside
        window.addEventListener('mousedown', handleClickOutside);

        return () => {
            window.removeEventListener('mousedown', handleClickOutside);
        }
    }, [editMode]);

    // Handle change of value user currently types in
    const handleChange = e => {
        const itemValue = e.target.value;
        setInputValue(itemValue);
    }

    // Handle key down even and add inputValue to formik field value
    const handleKeyDown = (event) => {
        // Check if pressed key is Enter
        if (event.keyCode === 13 && inputValue !== "") {
            event.preventDefault();
            // add current inputValue to formik field value
            field.value[field.value.length] = inputValue;
            helpers.setValue(field.value);
            // reset inputValue
            setInputValue("");
        }
    }

    // Remove item/value from inserted field values
    const removeItem = key => {
        field.value.splice(key, 1);
        helpers.setValue(field.value);
    }

    return (
        // Render editable field for multiple values depending on type of metadata field
        // (types: see metadata.json retrieved from backend)
      <>
          {(metadataField.type === "mixed_text" && metadataField.collection.length !== 0) ? (
              <EditableMultiSelect metadataFieldCollection={metadataField.collection}
                                   fieldValue={field.value}
                                   editMode={editMode}
                                   setEditMode={setEditMode}
                                   inputValue={inputValue}
                                   removeItem={removeItem}
                                   handleChange={handleChange}
                                   handleKeyDown={handleKeyDown}/>
          ) : (metadataField.type === "mixed_text" && (
              <EditableMultiValue fieldValue={field.value}
                                  editMode={editMode}
                                  setEditMode={setEditMode}
                                  inputValue={inputValue}
                                  removeItem={removeItem}
                                  handleChange={handleChange}
                                  handleKeyDown={handleKeyDown}/>
          ))}
      </>
    );
}

// Renders multi select
const EditableMultiSelect = ({ fieldValue, metadataFieldCollection, editMode, setEditMode, inputValue, removeItem, handleChange,
                                 handleKeyDown }) => {
    const { t } = useTranslation();

    return (
        editMode ? (
            <>
                <div ref={childRef}>
                    <div onBlur={() => setEditMode(false)}>
                        <input name="inputValue"
                               value={inputValue}
                               type="text"
                               onKeyDown={e => handleKeyDown(e)}
                               onChange={e => handleChange(e)}
                               placeholder={t('EDITABLE.MULTI.PLACEHOLDER')}
                               list="data-list"
                        />
                        {/* Display possible options for values as some kind of dropdown */}
                        <datalist id="data-list">
                            {metadataFieldCollection.map((item, key) => (
                                <option key={key}>{item.value}</option>
                            ))}
                        </datalist>
                    </div>
                    {/* Render blue label for all values already in fieldValue array */}
                    {(fieldValue instanceof Array && fieldValue.length !== 0) ? (fieldValue.map((item, key) => (
                        <span className="ng-multi-value"
                              key={key}>
                        {item}
                            <a onClick={() => removeItem(key)}>
                            <i className="fa fa-times" />
                        </a>
                    </span>
                    ))) : null}
                </div>
            </>

        ) : (
            <div onClick={() => setEditMode(true)}>
                {/* Show values when not in editing mode */}
                {(fieldValue instanceof Array && fieldValue.length !== 0) ? (
                    <ul>
                        {fieldValue.map((item, key) => (
                            <li key={key}>
                                <span>{item}</span>
                            </li>
                        ))}
                    </ul>
                ) : (
                    <span className="editable preserve-newlines">
                            {""}
                    </span>
                )}
                <i className="edit fa fa-pencil-square"/>
            </div>
        )
    );
};

// Renders editable field input for multiple values
const EditableMultiValue = ({ fieldValue, editMode, setEditMode, inputValue, removeItem, handleChange,
                                handleKeyDown}) => {
    const { t } = useTranslation();

    return (
        editMode ? (
            <>
                <div onBlur={() => setEditMode(false)}
                     ref={childRef}>
                    <input name="inputValue"
                           value={inputValue}
                           type="text"
                           onKeyDown={e => handleKeyDown(e)}
                           onChange={e => handleChange(e)}
                           placeholder={t('EDITABLE.MULTI.PLACEHOLDER')}
                    />
                </div>
                {/* Render blue label for all values already in fieldValue array */}
                {(fieldValue instanceof Array && fieldValue.length !== 0) ? (fieldValue.map((item, key) => (
                    <span className="ng-multi-value"
                          key={key}>
                        {item}
                        <a onClick={() => removeItem(key)}>
                            <i className="fa fa-times" />
                        </a>
                    </span>
                ))) : null}
            </>
        ) : (
            <div onClick={() => setEditMode(true)}>
                {/* Show values when not in editing mode */}
                {(fieldValue instanceof Array && fieldValue.length !== 0) ? (
                    <ul>
                        {fieldValue.map((item, key) => (
                        <li key={key}>
                            <span>{item}</span>
                        </li>
                        ))}
                    </ul>
                ) : (
                    <span className="editable preserve-newlines">
                        {""}
                    </span>
                )}
                <i className="edit fa fa-pencil-square"/>
            </div>
        )
    );
};

export default RenderMultiField;
