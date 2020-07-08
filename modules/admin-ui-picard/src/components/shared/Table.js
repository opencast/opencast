import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import * as ts from "../../selectors/tableSelectors";
import * as ta from "../../actions/tableActions";
import * as tt from "../../thunks/tableThunks";
import {connect} from "react-redux";
import cn from 'classnames';

const containerPageSize = React.createRef();

const Table = ({table, selectAll, deselectAll, rowSelectionChanged, updatePageSize, templateMap, pageOffset, pages,
                   goToPage, updatePages, setOffset}) => {
    // Size options for pagination
    const sizeOptions = [10, 20, 50, 100];

    const directAccessible = getDirectAccessiblePages(pages, table.pagination);

    const { t } = useTranslation();

    // State of dropdown menu
    const [showPageSizes, setShowPageSizes] = useState(false);

    useEffect(() => {
        // Function for handling clicks outside of an open dropdown menu
        const handleClickOutside = e => {
            if (containerPageSize.current && !containerPageSize.current.contains(e.target)) {
                setShowPageSizes(false);
            }
        }

        // Event listener for handle a click outside of dropdown menu
        window.addEventListener('mousedown', handleClickOutside);

        return () => {
            window.removeEventListener('mousedown', handleClickOutside);
        }
    });

    const lengthDivStyle = {
        position: "absolute",
        visibility: "hidden",
        height: "auto",
        width: "auto",
        whiteSpace: "nowrap"
    };
    const loadingTdStyle = {
        textAlign: "center"
    };

    // Select or deselect all rows on a page
    const onChangeAllSelected = e => {
        const selected = e.target.checked
        if (selected) {
            selectAll();
        } else {
            deselectAll();
        }

    };

    const changePageSize = size => {
        updatePageSize(size);
        setOffset(0);
        updatePages();
    }

    // Apply a column template and render corresponding components
    const applyColumnTemplate = (row, column) => {
        let Template = templateMap[column.template];
        return <Template row={row} />
    }

    // Navigation to previous page possible?
    const isNavigatePrevious = () => {
        return pageOffset > 0;
    }

    // Navigation to next page possible?
    const isNavigateNext = () => {
        return pageOffset < pages.length - 1;
    }


    return (
        <>
            <div className="action-bar">
              <ul>
                  {/* todo: look what data-open-modal and data-table is */}
                  <li><a>{t('TABLE_EDIT')}</a></li>
              </ul>
            </div>
            <div id="length-div" style={lengthDivStyle}>
            </div>
            <table className={'main-tbl highlight-hover'}>
                <thead>
                    <tr>
                        {/* Only show if multiple selection is possible */}
                        {table.multiSelect ? (
                            <th className="small">
                                {/*Checkbox to select all rows*/}
                                <input type="checkbox" onChange={e => onChangeAllSelected(e)}/>
                            </th>
                        ) : null}

                        {/* todo: repeat for each column in table.columns and if not column.deactivated*/}
                        {/* todo: onClick for table.sortby(column) how??*/}
                        {/* TODO: SORTING!!!*/}
                        {table.columns.map((column, key) => (
                            <th key={key} className={cn({ 'col-sort': table.predicate === column.name, 'sortable': column.sortable })}>
                            <span>
                                {t(column.label)}
                                {/* Show only if column is sortable*/}
                                {column.sortable ? (
                                    <i className={cn("sort", { asc: table.predicate === column.name && !table.reverse, desc: table.predicate === column.name && table.reverse })}/>
                                ) : null}
                            </span>
                            </th>
                        )) }

                    </tr>
                </thead>
                <tbody>
                {(table.loading && table.rows.length === 0) ? (
                    // todo: put Loading in Redux state of table
                    <tr>
                        <td colSpan={table.columns.length} style={loadingTdStyle}>
                            <i className="fa fa-spinner fa-spin fa-2x fa-fw"/>
                        </td>
                    </tr>
                ) : ((!table.loading && table.rows.length === 0) ? (
                    //Show if no results and table is not loading
                    <tr>
                        <td colSpan={table.columns.length}>{t('TABLE_NO_RESULT')}</td>
                    </tr>
                ) : (
                    //Repeat for each row in table.rows
                    table.rows.map((row) => (
                            <tr key={row.id}>
                                {/* Show if multi selection is possible */}
                                {/* Checkbox for selection of row */}
                                {table.multiSelect && (
                                    <td><input type="checkbox" checked={row.selected} onChange={() => rowSelectionChanged(row.id)}/></td>
                                )}
                                {/* Populate table */}
                                {table.columns.map((column, key) => (
                                    (!column.template && !column.translate && !column.deactivated) ?
                                        <td key={key}>
                                            {row[column.name]}
                                        </td>
                                     : (!column.template && column.translate && !column.deactivated) ?
                                        //Show only if column not template, translate, not deactivated
                                        <td key={key}>
                                            {t(row[column.name])}
                                        </td>
                                        : (!!column.template) ?
                                            // if column has a template then apply it
                                            <td key={key}>
                                                {applyColumnTemplate(row, column)}
                                            </td>
                                            : <td/>
                                ))}
                            </tr>
                        ))
                    ))}
                </tbody>
            </table>

            {/* Selection of page size */}
            <div id="tbl-view-controls-container">
                <div className="drop-down-container small flipped" onClick={() => setShowPageSizes(!showPageSizes)} ref={containerPageSize}>
                    <span>{table.pagination.limit}</span>
                    {/* Drop down menu for selection of page size */}
                    {showPageSizes && (
                        <ul className="dropdown-ul">
                            {sizeOptions.map((size, key) => (
                                <li key={key}><a onClick={() => changePageSize(size)}>{size}</a></li>
                            ))}
                        </ul>
                    )}
                </div>

                {/* Pagination and navigation trough pages */}
                <div className="pagination">
                    <a className={cn("prev",{disabled: !isNavigatePrevious()})}
                       onClick={() => goToPage(pageOffset - 1)}/>
                    {directAccessible.map((page, key) => (
                        page.active ? (
                                <a className="active">{page.label}</a>
                            ) : (
                                <a key={key} onClick={() => goToPage(page.number)}>{page.label}</a>
                            )
                    ))}

                    <a className={cn("next",{disabled: !isNavigateNext()})}
                       onClick={() => goToPage(pageOffset + 1)}/>
                </div>
            </div>
        </>
    );
}

// get all pages directly accessible from current page
const getDirectAccessiblePages = (pages, pagination) => {
    let startIndex = pagination.offset - pagination.directAccessibleNo,
        endIndex = pagination.offset + pagination.directAccessibleNo,
        directAccessible = [], i, pageToPush;

    if (startIndex < 0) {
        // Adjust range if selected range is too low
        endIndex = endIndex - startIndex;
        startIndex = 0;
    }

    if (endIndex >= pages.length) {
        // Adjust range if selected range is too high
        startIndex = startIndex - (endIndex - pages.length) - 1;
        endIndex = pages.length -1;
    }

    // Adjust start and end index to numbers that are possible
    endIndex = Math.min(pages.length - 1, endIndex);
    startIndex = Math.max(0, startIndex);

    for (i = startIndex; i <= endIndex; i++) {
        if (i === startIndex && startIndex !== 0) {
            // Add first item if start index is not 0 (first page always direct accessible)
            pageToPush = pages[0];
        }
        else if (i === endIndex && endIndex !== pages.length - 1) {
            // Add last item if end index is not real end (last page always direct accessible)
            pageToPush = pages[pages.length - 1];
        } else if ((i === startIndex + 1 && startIndex !== 0)
            || (i === endIndex - 1 && endIndex !== pages.length - 1)) {
            // Add .. at second or second last position if start or end index is not 0
            pageToPush = {
                number: i,
                label: '..',
                active: i === pagination.offset
            };

        }
        else {
            pageToPush = pages[i];
        }
        directAccessible.push(pageToPush);
    }

    //todo: in old code is here something with maxLabel (Check if this is also needed)

    return directAccessible;
}


const mapStateToProps = state => ({
    table: state.table,
    pageOffset: ts.getPageOffset(state),
    pages: ts.getTablePages(state)
});

const mapDispatchToProps = dispatch => ({
    selectAll: () => dispatch(ta.selectAll()),
    deselectAll: () => dispatch(ta.deselectAll()),
    rowSelectionChanged: (id, selected) => dispatch(ta.selectRow(id, selected)),
    updatePageSize: size => dispatch(ta.updatePageSize(size)),
    goToPage: pageNumber => dispatch(tt.goToPage(pageNumber)),
    updatePages: () => dispatch(tt.updatePages()),
    setOffset: pageNumber => dispatch(ta.setOffset(pageNumber))
});

export default connect(mapStateToProps, mapDispatchToProps)(Table);
