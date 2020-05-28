import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import DatePicker from "react-datepicker/es";
import filters from "../../mocks/resources/resourcesFilterResource";

const filtersList = Object.keys(filters.filters).map(key => {
    let filter = filters.filters[key];
    filter.name = key;
    return filter;
});

//todo: implement/look if really needed (handleEnddatePicker is quite similar)
function selectFilterPeriodValue() {
    console.log("select filter period value");
}
// Remove a certain filter
const removeFilter = filter => {
    console.log("filter to be removed:");
    console.log(filter);
    filter.value = "";
    console.log("remove certain filter");
}

// todo: implement
function loadFilterProfile() {
    console.log("load filter profile");
}

// todo: implement
function editFilterProfile() {
    console.log("edit filter profile");
}

// todo: implement
function removeFilterProfile() {
    console.log("remove filter profile");
}

// todo: implement
function saveProfile() {
    console.log("save profile");
}

// todo: implement
function onChangeSelectMainFilter(e) {

}


/**
 * This component renders the table filters in the upper right corner of the table
 */
const TableFilters = () => {
    const { t } = useTranslation();
    // State of the individual filters
    const [textFilter, setTextFilter] = useState('');
    const [filterMap, setFilterMap] = useState(filtersList);
    const [selectedFilter, setSelectedFilter] = useState('');
    const [secondFilter, setSecondFilter] = useState('');
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    // variables for showing different dialogs depending on what was clicked
    const [showFilterSelector, setFilterSelector] = useState(false);
    const [showFilterSettings, setFilterSettings] = useState(false);
    const [settingsMode, setSettingsMode] = useState(false);

    // Remove all selected filters, no filter should be "active" anymore
    const removeFilters = () => {
        setTextFilter('');
        setSelectedFilter('');
        setSecondFilter('');
        setStartDate('');
        setEndDate('');

        // Iterate though filterMap and set all values to "" again
        for(let filter in filterMap) {
            removeFilter(filterMap[filter]);
        }

        console.log("remove filters");
        console.log(filterMap);
    }

    // Handle changes when a item of the component is clicked
    const handleChange = e => {
        const itemName = e.target.name;
        const itemValue = e.target.value;

        if (itemName === "textFilter") {
            setTextFilter(itemValue);
        }

        if (itemName === "selectedFilter") {
            setSelectedFilter(itemValue);
        }

        // if the change is in secondFilter (filter is picked) then the selected value is saved in filterMap
        // and the filter selections are cleared
        if(itemName === "secondFilter") {
            let filter = filterMap.find(({ name }) => name === selectedFilter);
            filter.value = itemValue;
            console.log(filterMap);
            setFilterSelector(false);
            setSelectedFilter('');
            setSecondFilter('');
        }
    }

    // Set the sate of startDate and endDate picked with datepicker
    // Todo: not working correctly: you need to pick the second date twice before the value of the filter is set
    // todo: get rid of bug described before
    const handleDatepickerChange = (date, isStart) => {
        if (isStart) {
            setStartDate(date);
        } else {
            setEndDate(date);
        }
        if (startDate === '' || endDate === '') {
            return;
        }
        let filter = filterMap.find(({ name }) => name === selectedFilter);
        // Todo: better way to save the period
        filter.value = startDate + ' ' + endDate;
        setFilterSelector(false);
        setSelectedFilter('');
        console.log(startDate);

    };


    return (
        <div className="filters-container">
            {/* Text filter - Search Query */}
            {/* todo: Search icon is not showing yet*/}
            <input type="text"
                   className="search expand"
                   placeholder={t('TABLE_FILTERS.PLACEHOLDER')}
                   onChange={e => handleChange(e)}
                   name="textFilter"
                   value={textFilter}/>

            {/* Selection of filters and management of filter profiles*/}
            {/*show only if filters.filters contains filters*/}
            {!!filters.filters && (
                <div className="table-filter">
                    <div className="filters">
                        <i title={t('TABLE_FILTERS.ADD')}
                           className="fa fa-filter"
                           onClick={() => setFilterSelector(!showFilterSelector)}/>

                        {/*show if icon is clicked*/}
                        {showFilterSelector && (
                            <div>
                                {/*Check if filters in filtersMap and show corresponding selection*/}
                                {(!filterMap || false) ? (
                                    // Show if no filters in filtersList
                                    <select defaultValue={t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}
                                            className="main-filter">
                                        <option disabled>{t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}</option>
                                    </select>

                                ) : (
                                    // Show all filtersMap as selectable options
                                    <select disable_search_threshold="10"
                                            onChange={e => handleChange(e)}
                                            value={selectedFilter}
                                            name="selectedFilter"
                                            className="main-filter">
                                        <option value="" disabled>{t('TABLE_FILTERS.FILTER_SELECTION.PLACEHOLDER')}</option>
                                        {
                                            filtersList.map((filter, key) => (
                                                <option
                                                    key={key}
                                                    value={filter.name}>
                                                    {t(filter.label).substr(0, 40)}
                                                </option>
                                            ))
                                        }
                                    </select>
                                )}

                            </div>
                        )}

                        {/*Show selection of secondary filter if a main filter is chosen*/}
                        {!!selectedFilter && (
                            <div>
                                {/*Show the secondary filter depending on the type of main filter chosen (select or period)*/}
                                <FilterSwitch filterMap={filterMap}
                                              selectedFilter={selectedFilter}
                                              secondFilter={secondFilter}
                                              startDate={startDate}
                                              endDate={endDate}
                                              handleDate={handleDatepickerChange}
                                              handleChange={handleChange}/>
                            </div>
                        )
                        }

                        {/* Show for each selected filter a blue label containing its name and option */}
                        {
                            filterMap.map((filter, key) => {
                                if(!!filter.value) { return (
                                    <span className="ng-multi-value" key={key}>
                                        <span>
                                            {
                                                // Use different representation of name and value depending on type of filter
                                                {
                                                    'select': (
                                                        <span>
                                                            {t(filter.label).substr(0,40)}:
                                                            {(filter.translatable) ? (
                                                                t(filter.value).substr(0, 40)
                                                            ) : (
                                                                filter.value.substr(0, 40)
                                                            )}
                                                        </span>
                                                    ),
                                                    'period': (
                                                        <span>
                                                            <span>
                                                                {/*todo: format date range*/}
                                                                {t(filter.label).substr(0,40)} : Placeholder
                                                            </span>
                                                        </span>
                                                    )
                                                }[filter.type]
                                            }
                                        </span>
                                        {/* Remove icon in blue area around filter */}
                                        <a title={t('TABLE_FILTERS.REMOVE')} onClick={filter => removeFilter(filter)}>
                                            <i className="fa fa-times"/>
                                          </a>
                                    </span>
                                )
                            }})
                        }

                    </div>

                    {/* Remove icon to clear all filters */}
                    <i onClick={() => removeFilters}
                       title={t('TABLE_FILTERS.CLEAR')}
                       className="clear fa fa-times" />
                    {/* Settings icon to open filters profile dialog (save and editing filter profiles)*/}
                    <i onClick={() => setFilterSettings(!showFilterSettings)}
                       title={t('TABLE_FILTERS.PROFILES.FILTERS_HEADER')}
                       className="settings fa fa-cog fa-times" />

                    {/* Filter profile dialog for saving and editing filter profiles */}
                    {showFilterSettings && (
                        <div className="btn-dd filter-settings-dd df-profile-filters">
                            {/* depending on settingsMode show list of all saved profiles or the chosen profile to edit*/}
                            {settingsMode ? (
                                // if settingsMode is true the list with all saved profiles is shown
                                <div className="filters-list">
                                    <header>
                                        <a className="icon close" onClick={() => setFilterSettings(!showFilterSettings)}/>
                                        <h4>{t('TABLE_FILTERS.PROFILES.FILTERS_HEADER')}</h4>
                                    </header>
                                    <ul>
                                        {/*todo: if no profiles saved yet (profiles.length == 0)*/}
                                        <li>{t('TABLE_FILTERS.PROFILES.EMPTY')}</li>
                                        {/*todo: repeat for each profile in profiles (else-case)*/}
                                        <li>
                                            <a title="profile.description"
                                               onClick={() => loadFilterProfile()}>
                                                {/*todo: just a placeholder*/}
                                                profile.name limit 70
                                            </a>
                                            {/* Settings icon to edit profile */}
                                            <a onClick={() => editFilterProfile()}
                                               title={t('TABLE_FILTERS.PROFILES.EDIT')}
                                               className="icon edit"/>
                                            {/* Remove icon to remove profile */}
                                            <a onClick={() => removeFilterProfile()}
                                               title={t('TABLE_FILTERS.PROFILES.REMOVE')}
                                               className="icon remove"/>
                                        </li>
                                    </ul>

                                    {/* Save the currently selected filter options as new profile */}
                                    {/* settingsMode is switched and save dialog is opened*/}
                                    <div className="input-container">
                                        <div className="btn-container">
                                            <a className="save" onClick={() => setSettingsMode(!settingsMode)}>
                                                {/*todo: limit to 70*/}
                                                {t('TABLE_FILTERS.PROFILES.SAVE_FILTERS').substr(0,40)}
                                            </a>
                                        </div>
                                    </div>
                                </div>
                            ) : (
                                // if settingsMode is false then show editing dialog of selected filter profile
                                <div className="filter-details">
                                    <header>
                                        <a className="icon close" onClick={() => setFilterSettings(!showFilterSettings)} />
                                        <h4>{t('TABLE_FILTERS.PROFILES.FILTER_HEADER')}</h4>
                                    </header>
                                    {/* Input form for save/editing profile*/}
                                    <div>
                                        <label>{t('TABLE_FILTERS.PROFILES.NAME')} <i className="required">*</i></label>
                                        {/*todo: find react equivalent of ng-model (profile.name), ng-change*/}
                                        <input required
                                               name="name"
                                               type="text"
                                               placeholder={t('TABLE_FILTERS.PROFILES.NAME_PLACEHOLDER')}/>

                                        <label>{t('TABLE_FILTERS.PROFILES.DESCRIPTION')}</label>
                                        {/*todo: find react equivalent of ng-model (profile.description)*/}
                                        <textarea placeholder={t('TABLE_FILTERS.PROFILES.DESCRIPTION_PLACEHOLDER')} />
                                    </div>
                                    <div className="input-container">
                                        {/* Buttons for saving and canceling editing */}
                                        <div className="btn-container">
                                            <a onClick={() => setFilterSettings(!showFilterSettings)} className="cancel">{t('CANCEL')}</a>
                                            <a onClick={() => saveProfile} className="save">{t('SAVE')}</a>
                                        </div>
                                    </div>
                                </div>

                            )
                            }

                        </div>



                    )}


                </div>
            )}

        </div>
    );


}

/*
 * Component for rendering the selection of options for the secondary filter
 * depending on the the type of the main filter. These types can be select or period.
 * In case of select, a second selection is shown. In case of period, datepicker are shown.
 */
const FilterSwitch = ({filterMap, selectedFilter, handleChange, startDate, endDate,
                          handleDate, secondFilter }) => {
    const {t} = useTranslation();

    let filter = filterMap.find(({ name }) => name === selectedFilter);
    // eslint-disable-next-line default-case
    switch(filter.type) {
        case 'select':
            return (
                <div>
                    {/*Show only if selected main filter has translatable options*/}
                    {filter.translatable ? (
                        // Show if the selected main filter has no further options
                        (!filter.options || false) ? (
                            <select defaultValue={t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}
                                    className="second-filter">
                                <option disabled>{t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}</option>
                            </select>
                        ) : (
                            // Show further options for a secondary filter
                            <select className="second-filter"
                                    onChange={() => handleChange()}
                                    value={secondFilter}
                                    name="secondFilter">
                                <option value="" disabled>{t('TABLE_FILTERS.FILTER_VALUE_SELECTION.PLACEHOLDER')}</option>
                                {
                                    filter.options.map((option, key) => (
                                        <option
                                            key={key}
                                            value={option.value}>
                                            {t(option.label).substr(0,40)}
                                        </option>
                                    ))
                                }
                            </select>
                        )

                    ) : (
                        // Show only if the selected main filter has options that are not translatable (else case from above)
                        (!filter.options || false) ? (
                            // Show if the selected main filter has no further options
                            <select defaultValue={t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}
                                    className="second-filter">
                                <option disabled>{t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}</option>
                            </select>
                        ) : (
                            // Show further options for a secondary filter
                            <select className="second-filter"
                                    onChange={e => handleChange(e)}
                                    value={secondFilter}
                                    name="secondFilter">
                                <option value="" disabled>{t('TABLE_FILTERS.FILTER_VALUE_SELECTION.PLACEHOLDER')}</option>
                                {
                                    filter.options.map((option, key) => (
                                        <option
                                            key={key}
                                            value={option.value}>
                                            {option.label.substr(0,40)}
                                        </option>
                                    ))
                                }
                            </select>
                        )

                    )
                    }

                </div>
            );
        case 'period':
            return (
                <div>
                    {/* Show datepicker for start date */}
                    {/* todo: ui is still not working right, revisit this */}
                    <DatePicker selected={startDate}
                                onChange={date => handleDate(date, true)}
                                placeholderText={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.START_DATE')}
                                className="small-search start-date"/>
                    {/*<input type="date"
                                   className="small-search start-date"
                                   onSelect={this.selectFilterPeriodValue}
                                   placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.START_DATE')}/>*/}

                    {/* Show datepicker for end date*/}
                    <DatePicker selected={endDate}
                                onChange={date => handleDate(date, false)}
                                placeholderText={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.END_DATE')}
                                className="small-search end-date"/>
                </div>
            );
    }
}



export default TableFilters;
