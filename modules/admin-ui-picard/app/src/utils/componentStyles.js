/* this file contains syles as javascript objects for syled components */

// colors
const colorDropDownMain = '#aaa';
const colorDropDownNormalFocus = '#5897fb';
const colorDropDownDarkerFocus = '#2a62bc';

export const dropDownStyle = (type) => {
    const width = type === 'time' ? 70 : 250

    return {
        container: (provided, state) => ({
            ...provided,
            width: width,
            position: 'relative',
            display: 'inline-block',
            verticalAlign: 'middle',
            font: 'inherit',
            outline: 'none'
        }),
        control: (provided, state) => ({
            ...provided,
            marginBottom: 0,
            border: `1px solid ${colorDropDownMain}`,
            borderColor: state.selectProps.menuIsOpen ? colorDropDownNormalFocus : colorDropDownMain,
            hoverBorderColor: state.selectProps.menuIsOpen ? colorDropDownNormalFocus : colorDropDownMain,
            boxShadow: state.selectProps.menuIsOpen ?  `0 0 0 1px ${colorDropDownNormalFocus}` : `0 0 0 1px ${colorDropDownMain}`,
            borderRadius: 4,
            "&:hover": {
                borderColor: colorDropDownMain
            }
        }),
        dropdownIndicator: (provided, state) => ({
            ...provided,
            transform: state.selectProps.menuIsOpen ? 'rotate(180deg)' : 'rotate(0deg)',
            color: colorDropDownMain,
            "&:hover": {
                color: colorDropDownNormalFocus
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
            border: 'none'
        }),
        menu: (provided, state) => ({
            ...provided,
            zIndex: 9000,
            marginTop: 1,
            border: 'none'
        }),
        menuList: (provided, state) => ({
            ...provided,
            marginTop: 0,
            border: `1px solid ${colorDropDownMain}`,
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
            backgroundColor: state.isSelected ? colorDropDownDarkerFocus : state.isFocused ? colorDropDownNormalFocus : 'white',
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
    };
}

export const dropDownSpacingTheme = (theme) => ({
    ...theme,
    spacing: {
        ...theme.spacing,
        controlHeight: 30,
        baseUnit: 2,
    }
});