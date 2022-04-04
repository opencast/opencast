import React, {useState} from "react";
import moment from "moment";
import {getCurrentLanguageInformation} from "../../utils/utils";

const StatisticsGraph = ({ t, chartLabels, chartOptions, initialDataResolution, providerId, fromDate, onChange, exportUrl,
                             exportFileName, sourceData, timeChooseMode, toDate, totalValue, statDescription}) => {

    const availableDataResolutions = [
        { label: 'Yearly', value: 'yearly' },
        { label: 'Monthly', value: 'monthly' },
        { label: 'Daily', value: 'daily' },
        { label: 'Hourly', value: 'hourly' }
    ];

    const timeChooseModes = [
        {
            'value': 'year',
            'translation': 'Year'
        },
        {
            'value': 'month',
            'translation': 'Month'
        },
        {
            'value': 'custom',
            'translation': 'Custom'
        }
    ];

    const formatStrings = {
        month: 'MMMM YYYY',
        year: 'YYYY'
    };

    const dataResolutions = {
        month: 'daily',
        year: 'monthly'
    };

    // Get info about the current language and its date locale
    const currentLanguage = getCurrentLanguageInformation();


    const [from, setFrom] = useState(moment(fromDate).startOf(timeChooseMode).format('YYYY-MM-DD'));
    const [to, setTo] = useState(moment(fromDate).endOf(timeChooseMode).format('YYYY-MM-DD'));
    const [previousTimeChooseMode, setPreviousTimeChooseMode] = useState(timeChooseMode);
    const [dataResolution, setDataResolution] = useState(dataResolutions[timeChooseMode]);

    const change = () => {
        if (timeChooseMode === 'year' || timeChooseMode === 'month') {
            setFrom(moment(from).clone().startOf(timeChooseMode).format('YYYY-MM-DD'));
            setTo(moment(from).clone().endOf(timeChooseMode).format('YYYY-MM-DD'));
            setDataResolution(dataResolutions[timeChooseMode]);
        }

        onChange(providerId, from, to, dataResolution, timeChooseMode);
      };

    const changeTimeChooseMode = (timeChooseMode) => {
        if (timeChooseMode === previousTimeChooseMode) {
            return;
        }

        if (previousTimeChooseMode === 'custom') {
            setFrom(moment(from).startOf(timeChooseMode).format('YYYY-MM-DD'));
            setTo(moment(from).endOf(timeChooseMode).format('YYYY-MM-DD'));
            setDataResolution(dataResolutions[timeChooseMode]);
        }

        setPreviousTimeChooseMode(timeChooseMode);

        change();
    };

    const localizedMoment = (m) => {
        return moment(m).locale(currentLanguage.dateLocale.code);
    };
    const selectedName = () => {
        return localizedMoment(from).format(formatStrings[timeChooseMode]);
    };

    const previousName = () => {
        return localizedMoment(from).subtract(1, timeChooseMode + 's').format(formatStrings[timeChooseMode]);
    };

    const nextName = () => {
        return localizedMoment(from).add(1, timeChooseMode + 's').format(formatStrings[timeChooseMode]);
    };

    const selectPrevious = () => {
        setFrom(moment(from).subtract(1, timeChooseMode + 's').format('YYYY-MM-DD'));
        change();
    };

    const selectNext = () => {
        setFrom(moment(from).add(1, timeChooseMode + 's').format('YYYY-MM-DD'));
        change();
    };

    return (
        <div className="statistics-graph">
            <div className="download">
                <a className="download-icon"
                   href={exportUrl}
                   download={exportFileName()}/>
            </div>
            <div className="mode">
                {timeChooseModes.map((mode, key) => (
                    <>
                        <input type="radio"
                               value={mode.value}
                               id={providerId + '-mode-' + key}
                               onChange={() => changeTimeChooseMode()}
                               /*ng-model="timeChooseMode"*//>
                        <label htmlFor={providerId + '-mode-' + key}>
                            {t(mode.translation)}
                        </label>
                    </>
                ))}

            </div>
            <div className="total">
                <span>
                    {t('STATISTICS.TOTAL')  /* Total */}
                </span> 
                <span>
                    {totalValue}
                </span>
            </div>
            {(timeChooseMode === 'year' || timeChooseMode === 'month') && (
                <span className="preset">
                    <a className="navigation prev" onClick={() => selectPrevious()}/>
                    <div>{selectedName()}</div>
                    <a className="navigation next" onClick={() => selectNext()}/>
                </span>
            )}
            {(timeChooseMode === 'custom') && (
                <span className="custom">
                    <div className="range">
                        <span>
                            {t('STATISTICS.FROM')  /* From */}
                        </span>
                        <input datepicker
                               type="text"
                               tabIndex="4"
                               placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.START_DATE')}
                               /*ng-model="from"*/
                               onChange={() => change()}/>
                        <span>
                            {t('STATISTICS.TO')  /* To */}
                        </span>
                        <input datepicker
                               type="text"
                               tabIndex="4"
                               placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.END_DATE')}
                               /*ng-model="to"*/
                               onChange={() => change()}/>
                    </div>
                    <div>
                        <span>
                            {t('STATISTICS.GRANULARITY')}
                        </span>
                        <select chosen
                                data-width="'100px'"
                                /*ng-model="dataResolution"*/
                                onChange={() => change()}>
                            {availableDataResolutions.map((option, key) => (
                                <option key={key} value={option.value}>
                                    {option.label}
                                </option>
                            ))}
                        </select>
                    </div>
                </span>
            )}
            <canvas
                className="chart chart-bar"
                chart-data="sourceData"
                chart-options="chartOptions"
                chart-labels="chartLabels">
            </canvas>
            <p>
                {t(statDescription)}
            </p>
        </div>
    );
}

export default StatisticsGraph;