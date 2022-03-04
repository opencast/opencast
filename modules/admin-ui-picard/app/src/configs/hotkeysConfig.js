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
            sequence: 'N',
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
        NEXT_DASHBOARD_FILTER: {
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
        },
        REMOVE_FILTERS: {
            name: 'remove_filters',
            description: 'HOTKEYS.DESCRIPTIONS.GENERAL.REMOVE_FILTERS',
            combo: ['r'],
            sequence: 'r',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        }
    },
    player: {
        PLAY_PAUSE: {
            name: 'play_pause',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.PLAY_PAUSE',
            combo: ['space'],
            sequence: 'space',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        PREVIOUS_FRAME: {
            name: 'previous_frame',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.PREVIOUS_FRAME',
            combo: ['left'],
            sequence: 'left',
            action: 'keyup',
            allowIn: []
        },
        NEXT_FRAME: {
            name: 'next_frame',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.NEXT_FRAME',
            combo: ['right'],
            sequence: 'right',
            action: 'keyup',
            allowIn: []
        },
        STEP_BACKWARD: {
            name: 'step_backward',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.STEP_BACKWARD',
            combo: ['ctrl', 'left'],
            sequence: 'ctrl+left',
            action: 'keyup',
            allowIn: []
        },
        STEP_FORWARD: {
            name: 'step_forward',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.STEP_FORWARD',
            combo: ['ctrl', 'right'],
            sequence: 'ctrl+right',
            action: 'keyup',
            allowIn: []
        },
        PREVIOUS_SEGMENT: {
            name: 'previous_segment',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.PREVIOUS_SEGMENT',
            combo: ['up'],
            sequence: 'up',
            action: 'keyup',
            allowIn: []
        },
        NEXT_SEGMENT: {
            name: 'next_segment',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.NEXT_SEGMENT',
            combo: ['down'],
            sequence: 'down',
            action: 'keyup',
            allowIn: []
        },
        VOLUME_UP: {
            name: 'volume_up',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.VOLUME_UP',
            combo: ['plus'],
            sequence: 'plus',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        VOLUME_DOWN: {
            name: 'volume_down',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.VOLUME_DOWN',
            combo: ['-'],
            sequence: '-',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        },
        MUTE: {
            name: 'mute',
            description: 'HOTKEYS.DESCRIPTIONS.PLAYER.MUTE',
            combo: ['m'],
            sequence: 'm',
            action: 'keyup',
            allowIn: ['INPUT', 'SELECT', 'TEXTAREA']
        }
    }
};
