// keymap containing information about available hotkeys
export const availableHotkeys = {
    general: {
        HOTKEY_CHEATSHEET : {
            name: 'hotkey_cheatsheet',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.CHEAT_SHEET',
            combo: ['h'],
            sequence: 'h',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        EVENT_VIEW: {
            name: 'event_view',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.EVENT_VIEW',
            combo: ['e'],
            sequence: 'e',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        SERIES_VIEW: {
            name: 'series_view',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.SERIES_VIEW',
            combo: ['s'],
            sequence: 's',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        NEW_EVENT: {
            name: 'new_event',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.NEW_EVENT',
            combo: ['n'],
            sequence: 'n',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        NEW_SERIES: {
            name: 'new_series',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.NEW_SERIES',
            combo: ['N'],
            sequence: 'shift+n',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        MAIN_MENU: {
            name: 'main_menu',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.MAIN_MENU',
            combo: ['m'],
            sequence: 'm',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        /*NEXT_DASHBOARD_FILTER: {
            name: 'select_next_dashboard_filter',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.SELECT_NEXT_DASHBOARD_FILTER',
            combo: ['f'],
            sequence: 'f',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        PREVIOUS_DASHBOARD_FILTER: {
            name: 'select_previous_dashboard_filter',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.SELECT_PREVIOUS_DASHBOARD_FILTER',
            combo: ['F'],
            sequence: 'F',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },*/
        REMOVE_FILTERS: {
            name: 'remove_filters',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.REMOVE_FILTERS',
            combo: ['r'],
            sequence: 'r',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        }
    }
};
