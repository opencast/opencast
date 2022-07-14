import React, {useState} from "react";
import {Bar} from "react-chartjs-3";
import {useTranslation} from "react-i18next";
import {dropDownStyle} from "../../utils/componentStyles";
import {formatDropDownOptions, handleSearch} from "../../utils/dropDownUtils";
import Select from "react-select";

/**
 * This component provides a bar chart for visualising (statistics) data
 */
const DropDown = ({value, text, options, type, required, handleChange, placeholder, tabIndex,
                      autoFocus=false, defaultOpen=false, disabled=false}) => {
    const { t } = useTranslation();

    const [ search, setSearch ] = useState({
        text: '',
        filteredCollection: options
    });

    return (
        <Select tabIndex={tabIndex}
                styles={dropDownStyle}
                defaultMenuIsOpen={defaultOpen}
                autoFocus={autoFocus}
                isSearchable
                value={{value: value, label: text === '' ? placeholder : text}}
                inputValue={search.text}
                options={formatDropDownOptions(search.filteredCollection, type, value, required, t)}
                placeholder={placeholder}
                noOptionsMessage={() => 'No matching results.'}
                onInputChange={value => handleSearch(value, type, options, setSearch, t)}
                onChange={element => handleChange(element)}
                isDisabled={disabled}
        />
    );
}

export default DropDown;