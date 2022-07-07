import React, {useState} from "react";
import Select from "react-select";
import {useTranslation} from "react-i18next";
import {DateTimePicker} from "@material-ui/pickers";
import {createMuiTheme, ThemeProvider} from "@material-ui/core";
import cn from "classnames";
import {useClickOutsideField} from "../../../hooks/wizardHooks";
import {isJson} from "../../../utils/utils";
import {getMetadataCollectionFieldName} from "../../../utils/resourceUtils";


const childRef = React.createRef();
/**
 * This component renders an editable field for single values depending on the type of the corresponding metadata
 */
const RenderField = ({ field, metadataField, form, showCheck=false, isFirstField=false }) => {
    const { t } = useTranslation();

    // Indicator if currently edit mode is activated
    const [editMode, setEditMode] = useClickOutsideField(childRef, isFirstField);

    // Handle key down event and check if pressed key leads to leaving edit mode
    const handleKeyDown = (event, type) => {
        const { key } = event;
        // keys pressable for leaving edit mode
        const keys = ['Escape', 'Tab', 'Enter'];

        if (type !== 'textarea' && keys.indexOf(key) > -1) {
            setEditMode(false);
        }
    };

    return (
        // Render editable field depending on type of metadata field
        // (types: see metadata.json retrieved from backend)
        <>
            {metadataField.type === "time" && (
                <EditableSingleValueTime field={field}
                                         text={field.value}
                                         editMode={editMode}
                                         setEditMode={setEditMode}
                                         form={form}
                                         showCheck={showCheck}/>
            )}
            {(metadataField.type === "text" && !!metadataField.collection && metadataField.collection.length > 0) && (
                <EditableSingleSelect metadataField={metadataField}
                                      field={field}
                                      form={form}
                                      text={
                                        isJson(getMetadataCollectionFieldName(metadataField, field)) ?
                                            (t(JSON.parse(getMetadataCollectionFieldName(metadataField, field)).label)) :
                                            (t(getMetadataCollectionFieldName(metadataField, field)))
                                      }
                                      editMode={editMode}
                                      setEditMode={setEditMode}
                                      showCheck={showCheck}
                                      handleKeyDown={handleKeyDown}/>
            )}
            {(metadataField.type === "ordered_text") && (
                <EditableSingleSelect metadataField={metadataField}
                                      field={field}
                                      form={form}
                                      text={field.value}
                                      editMode={editMode}
                                      setEditMode={setEditMode}
                                      showCheck={showCheck}
                                      handleKeyDown={handleKeyDown}/>
            )}
            {(metadataField.type === "text" && !(!!metadataField.collection && metadataField.collection.length !== 0)) && (
                <EditableSingleValue field={field}
                                     form={form}
                                     text={field.value}
                                     editMode={editMode}
                                     setEditMode={setEditMode}
                                     isFirst={isFirstField}
                                     showCheck={showCheck}
                                     handleKeyDown={handleKeyDown}/>
            )}
            {metadataField.type === "text_long" && (
                <EditableSingleValueTextArea field={field}
                                             text={field.value}
                                             form={form}
                                             editMode={editMode}
                                             setEditMode={setEditMode}
                                             isFirst={isFirstField}
                                             showCheck={showCheck}
                                             handleKeyDown={handleKeyDown}/>
            )}
            {metadataField.type === "date" && (
                <EditableDateValue field={field}
                                   text={field.value}
                                   form={form}
                                   editMode={editMode}
                                   setEditMode={setEditMode}
                                   showCheck={showCheck}/>
            )}
            {metadataField.type === "boolean" && (
                <EditableBooleanValue field={field}
                                      form={form}
                                      showCheck={showCheck}
                                      handleKeyDown={handleKeyDown}/>
            )}
        </>
    );
};

// Renders editable field for a boolean value
const EditableBooleanValue = ({ field, handleKeyDown, form: { initialValues }, showCheck }) => {
    return (
        <div
             onKeyDown={e => handleKeyDown(e, "input")}
             ref={childRef}>
            <input type="checkbox" checked={field.value} {...field}/>
            <i className="edit fa fa-pencil-square"/>
            {showCheck && (
                <i className={cn("saved fa fa-check", { active: (initialValues[field.name] !== field.value) })}/>
            )}
        </div>

    );
};

// Renders editable field for a data value
const EditableDateValue = ({ field, text, form: { setFieldValue, initialValues }, editMode, setEditMode, showCheck }) => {
    const { t } = useTranslation();

    const theme = createMuiTheme({
        props: {
            MuiDialog: {
                style: {
                    zIndex: '2147483550',
                }
            }
        }
    });

    return (
        editMode ? (
            <div>
                <ThemeProvider theme={theme}>
                    <DateTimePicker name={field.name}
                                    value={field.value}
                                    onChange={value => setFieldValue(field.name, value)}
                                    onClose={() => setEditMode(false)}
                                    fullWidth
                                    format="MM/dd/yyyy"/>
                </ThemeProvider>
            </div>
        ) : (
            <div onClick={() => setEditMode(true)}>
                <span className="editable preserve-newlines" >
                    {t('dateFormats.dateTime.short', {dateTime: new Date(text)}) || ''}
                </span>
                <i className="edit fa fa-pencil-square"/>
                {showCheck && (
                    <i className={cn("saved fa fa-check", { active: (initialValues[field.name] !== field.value) })}/>
                )}
            </div>
        )

    );
};

// renders editable field for selecting value via dropdown
const EditableSingleSelect = ({ field, metadataField, text, editMode, setEditMode, handleKeyDown,
                                   form: { setFieldValue, initialValues }, showCheck }) => {
    const { t } = useTranslation();

    const [ search, setSearch ] = useState({
        text: '',
        filteredCollection: metadataField.collection
    });

    const filterBySearch = (filterText) => {
        if (metadataField.id === 'language') {
            return metadataField.collection.filter(item => t(item.name).toLowerCase().includes(filterText));
        } else if (metadataField.id === 'isPartOf') {
            return metadataField.collection.filter(item => item.name.toLowerCase().includes(filterText));
        } else {
            return metadataField.collection.filter(item => item.value.toLowerCase().includes(filterText));
        }
    }

    const handleSearch = async (searchText) => {
        setSearch({
            text: searchText,
            filteredCollection: filterBySearch(searchText.toLowerCase())
        });
    }

    /*
     * the Select component needs options to have an internal value and a displayed label
     * this function formats metadata options as provided by the backend into that scheme
     * it takes the options and provides the correct label to display for this kind of metadata,
     * as well as adding an empty option, if available
     */
    const formatOptions = unformattedOptions => {
        const formattedOptions = [];
        if (metadataField.value === '' || !metadataField.required) {
            formattedOptions.push({
                value: '',
                label: `-- ${t('SELECT_NO_OPTION_SELECTED')} --`
            });
        }
        if (metadataField.id === 'language') {
            for (const item of unformattedOptions) {
                formattedOptions.push({
                    value: item.value,
                    label: t(item.name)
                });
            }
        } else {
            // if selection of series then use item name as option label else use item value
            if (metadataField.id === 'isPartOf') {
                for (const item of unformattedOptions) {
                    formattedOptions.push({
                        value: item.value,
                        label: item.name
                    });
                }
            } else {
                for (const item of unformattedOptions) {
                    formattedOptions.push({
                        value: item.value,
                        label: item.value
                    });
                }
            }
        }
        return formattedOptions;
    }

    const dropDownStyle = {
        container: (provided, state) => ({
            ...provided,
            width: 250,
            position: 'relative',
            display: 'inline-block',
            verticalAlign: 'middle',
            font: 'inherit',
            outline: 'none'
        }),
        control: (provided, state) => ({
            ...provided,
            marginBottom: 0,
            border: '1px solid #aaa',
            borderColor: state.selectProps.menuIsOpen ? '#5897fb' : '#aaa',
            hoverBorderColor: state.selectProps.menuIsOpen ? '#5897fb' : '#aaa',
            boxShadow: state.selectProps.menuIsOpen ?  '0 0 0 1px #5897fb' : '0 0 0 1px #aaa',
            borderRadius: 4,
            "&:hover": {
                borderColor: '#aaa'
            }
        }),
        dropdownIndicator: (provided, state) => ({
            ...provided,
            transform: state.selectProps.menuIsOpen ? 'rotate(180deg)' : 'rotate(0deg)',
            color: '#aaa',
            "&:hover": {
                color: '#5897fb'
            }
        }),
        indicatorSeparator: (provided, state) => ({
            ...provided,
            width: 0,
            visibility: 'hidden'
        }),
        input: (provided, state) => ({
            ...provided,
            position: 'relative',
            zIndex: 1010,
            margin: 0,
            whiteSpace: 'nowrap',
            verticalAlign: 'middle',
            background: 'url("../../img/search.png") no-repeat 180px 9px',
            border: 'none'
        }),
        menu: (provided, state) => ({
            ...provided,
            marginTop: 1,
            border: 'none'
        }),
        menuList: (provided, state) => ({
            ...provided,
            marginTop: 0,
            border: '1px solid #aaa',
            borderRadius: 4
        }),
        noOptionsMessage: (provided, state) => ({
            ...provided,
            textAlign: 'left'
        }),
        option: (provided, state) => ({
            ...provided,
            height: 25,
            paddingTop: 0,
            paddingBottom: 0,
            backgroundColor: state.isSelected ? '#2a62bc' : state.isFocused ? '#5897fb' : 'white',
            color: state.isFocused || state.isSelected ? 'white' : provided.color,
            cursor: 'pointer',
            "&:hover": {
                height: 25
            }
        }),
        singleValue: (provided, state) => ({
            ...provided,
            marginTop: 0,
            marginBottom: 0,
        }),
        valueContainer: (provided, state) => ({
            ...provided,
            marginTop: 0,
            marginBottom: 0,
        })
    }

    return (
        editMode ? (
            <div onBlur={() => setEditMode(false)}
                 onKeyDown={e => handleKeyDown(e, "select")}
                 ref={childRef}
                 data-width="'250px'"
            >
                <Select autoFocus
                        styles={dropDownStyle}
                        defaultMenuIsOpen
                        isSearchable
                        value={{value: field.value, label: text === '' ? `-- ${t('SELECT_NO_OPTION_SELECTED')} --` : text}}
                        inputValue={search.text}
                        options={formatOptions(search.filteredCollection)}
                        placeholder={`-- ${t('SELECT_NO_OPTION_SELECTED')} --`}
                        noOptionsMessage={() => 'No matching results.'}
                        onInputChange={value => handleSearch(value)}
                        onChange={element => setFieldValue(field.name, element.value)}
                />
            </div>
        ) : (
             <div onClick={() => setEditMode(true)}>
                 <span className="editable preserve-newlines">
                     { text || t('SELECT_NO_OPTION_SELECTED') }
                 </span>
                 <i className="edit fa fa-pencil-square"/>
                 {showCheck && (
                     <i className={cn("saved fa fa-check", { active: (initialValues[field.name] !== field.value) })}/>
                 )}
             </div>
        )
    );
};

// Renders editable text area
const EditableSingleValueTextArea = ({ field, text, editMode, setEditMode, handleKeyDown, form: { initialValues }, showCheck, isFirst }) => {
    return (
        editMode ? (
            <div onBlur={() => setEditMode(false)}
                 onKeyDown={e => handleKeyDown(e, "textarea")}
                 ref={childRef}>
                <textarea {...field} autoFocus={isFirst} className="editable vertical-resize"/>
            </div>
        ) : (
            <div onClick={() => setEditMode(true)}>
                <span className="editable preserve-newlines">{text || ''}</span>
                <i className="edit fa fa-pencil-square"/>
                {showCheck && (
                    <i className={cn("saved fa fa-check", { active: (initialValues[field.name] !== field.value) })}/>
                )}
            </div>
        )
    );
};

// Renders editable input for single value
const EditableSingleValue = ({ field, form: { initialValues }, text, editMode, setEditMode, handleKeyDown, showCheck, isFirst }) => {
    return (
        editMode ? (
            <div onBlur={() => setEditMode(false)}
                 onKeyDown={e => handleKeyDown(e, "input")}
                 ref={childRef}>
                <input {...field} autoFocus={isFirst} type="text"/>
            </div>
        ) : (
            <div onClick={() => setEditMode(true)}>
                <span className="editable preserve-newlines" >{text || ''}</span>
                <i className="edit fa fa-pencil-square"/>
                {showCheck && (
                    <i className={cn("saved fa fa-check", { active: (initialValues[field.name] !== field.value) })}/>
                )}
            </div>
        )
    );
};

// Renders editable field for time value
const EditableSingleValueTime = ({ field, text, form: { setFieldValue, initialValues }, editMode, setEditMode, showCheck }) => {
    const { t } = useTranslation();

    const theme = createMuiTheme({
        props: {
            MuiDialog: {
                style: {
                    zIndex: '2147483550',
                }
            }
        }
    });

    return (
        editMode ? (
            <div>
                <ThemeProvider theme={theme}>
                    <DateTimePicker name={field.name}
                                    value={field.value}
                                    onChange={value => setFieldValue(field.name, value)}
                                    onClose={() => setEditMode(false)}
                                    fullWidth/>
                </ThemeProvider>
            </div>
        ) : (
            <div onClick={() => setEditMode(true)}>
                <span className="editable preserve-newlines">
                    {t('dateFormats.dateTime.short', {dateTime: new Date(text)}) || ''}
                </span>
                <i className="edit fa fa-pencil-square"/>
                {showCheck && (
                    <i className={cn("saved fa fa-check", { active: (initialValues[field.name] !== field.value) })}/>
                )}
            </div>
        )
    );
};


export default RenderField;
