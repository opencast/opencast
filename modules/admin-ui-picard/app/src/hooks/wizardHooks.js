import {useState} from "react";

export const usePageFunctions = (initialPage, initialValues) => {
    const [page, setPage] = useState(initialPage);
    const [snapshot, setSnapshot] = useState(initialValues);

    const nextPage = values => {
        setSnapshot(values);
        setPage(page + 1);
    }

    const previousPage = values => {
        setSnapshot(values);
        setPage(page - 1);
    }

    return [snapshot, page, nextPage, previousPage];
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
