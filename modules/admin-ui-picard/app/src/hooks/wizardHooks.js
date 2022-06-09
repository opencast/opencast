import {useEffect, useState} from "react";

export const usePageFunctions = (initialPage, initialValues) => {
    const [page, setPage] = useState(initialPage);
    const [snapshot, setSnapshot] = useState(initialValues);
    const [pageCompleted, setPageCompleted] = useState({});

    const nextPage = values => {
        setSnapshot(values);

        // set page as completely filled out
        let updatedPageCompleted = pageCompleted;
        updatedPageCompleted[page] = true;
        setPageCompleted(updatedPageCompleted);

        setPage(page + 1);
    }

    const previousPage = values => {
        setSnapshot(values);
        setPage(page - 1);
    }

    return [snapshot, page, nextPage, previousPage, setPage, pageCompleted, setPageCompleted];
}

export const useSelectionChanges = (formik, selectedRows) => {
    const [selectedEvents, setSelectedEvents] = useState(selectedRows);
    const [allChecked, setAllChecked] = useState(true);

    // Select or deselect all rows in table
    const onChangeAllSelected = e => {
        const selected = e.target.checked;
        setAllChecked(selected);
        let changedSelection = selectedEvents.map(event => {
            return {
                ...event,
                selected: selected
            }
        });
        setSelectedEvents(changedSelection);
        formik.setFieldValue('events', changedSelection);
    };

    // Handle change of checkboxes indicating which events to consider further
    const onChangeSelected = (e, id) => {
        const selected = e.target.checked;
        let changedEvents = selectedEvents.map(event => {
            if (event.id === id) {
                return {
                    ...event,
                    selected: selected
                }
            } else {
                return event
            }
        });
        setSelectedEvents(changedEvents);
        formik.setFieldValue('events', changedEvents);

        if (!selected) {
            setAllChecked(false);
        }
        if (changedEvents.every(event => event.selected === true)) {
            setAllChecked(true);
        }
    };

    return [selectedEvents, allChecked, onChangeSelected, onChangeAllSelected];
}

export const useClickOutsideField = (childRef, isFirstField) => {
    // Indicator if currently edit mode is activated
    const [editMode, setEditMode] = useState(isFirstField);

    useEffect(() => {
        // Handle click outside the field and leave edit mode
        const handleClickOutside = e => {
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

    return [editMode, setEditMode];
}
