/* this file contains syles as javascript objects for syled components */

// colors
const colorDropDownMain = "#aaa";
const colorDropDownNormalFocus = "#5897fb";
const colorDropDownDarkerFocus = "#2a62bc";

export const dropDownStyle = (type) => {
	const width =
		type === "theme" || type === "newTheme" || type === "workflow"
			? "100%"
			: type === "time"
			? 70
			: type === "aclRole"
			? 360
			: type === "aclTemplate" || type === "comment"
			? 200
			: 250;

	return {
		container: (provided, state) => ({
			...provided,
			width: width,
			position: "relative",
			display: "inline-block",
			verticalAlign: "middle",
			font: "inherit",
			outline: "none",
			paddingTop: 0,
			paddingBottom: 0,
			marginTop: 0,
			marginBottom: 0,
			marginRight: 1,
		}),
		control: (provided, state) => ({
			...provided,
			marginBottom: 0,
			border: `1px solid ${colorDropDownMain}`,
			borderColor: state.selectProps.menuIsOpen
				? colorDropDownNormalFocus
				: colorDropDownMain,
			hoverBorderColor: state.selectProps.menuIsOpen
				? colorDropDownNormalFocus
				: colorDropDownMain,
			boxShadow: state.selectProps.menuIsOpen
				? `0 0 0 1px ${colorDropDownNormalFocus}`
				: `0 0 0 1px ${colorDropDownMain}`,
			borderRadius: 4,
			paddingTop: 0,
			paddingBottom: 0,
			"&:hover": {
				borderColor: colorDropDownMain,
			},
		}),
		dropdownIndicator: (provided, state) => ({
			...provided,
			transform: state.selectProps.menuIsOpen
				? "rotate(180deg)"
				: "rotate(0deg)",
			paddingTop: 0,
			paddingBottom: 0,
			color: colorDropDownMain,
			"&:hover": {
				color: colorDropDownNormalFocus,
			},
		}),
		indicatorSeparator: (provided, state) => ({
			...provided,
			display: "none",
		}),
		input: (provided, state) => ({
			...provided,
			position: "relative",
			zIndex: 1010,
			margin: 0,
			whiteSpace: "nowrap",
			verticalAlign: "middle",
			border: "none",
			paddingTop: 0,
			paddingBottom: 0,
		}),
		menu: (provided, state) => ({
			...provided,
			zIndex: 9000,
			marginTop: 1,
			border: "none",
		}),
		menuList: (provided, state) => ({
			...provided,
			marginTop: 0,
			border: `1px solid ${colorDropDownMain}`,
			borderRadius: 4,
		}),
		noOptionsMessage: (provided, state) => ({
			...provided,
			textAlign: "left",
			paddingTop: 0,
			paddingBottom: 0,
		}),
		option: (provided, state) => ({
			...provided,
			paddingTop:
				type === "aclRole" || type === "aclTemplate" || type === "comment"
					? 5
					: 0,
			paddingBottom:
				type === "aclRole" || type === "aclTemplate" || type === "comment"
					? 5
					: 0,
			backgroundColor: state.isSelected
				? colorDropDownDarkerFocus
				: state.isFocused
				? colorDropDownNormalFocus
				: "white",
			color: state.isFocused || state.isSelected ? "white" : provided.color,
			cursor: "pointer",
			overflowWrap: "normal",
			lineHeight: type === "comment" ? "105%" : "inherit",
		}),
		singleValue: (provided, state) => ({
			...provided,
			marginTop: 0,
			marginBottom: 0,
			paddingTop: 0,
			paddingBottom: 0,
		}),
		valueContainer: (provided, state) => ({
			...provided,
			marginTop: 0,
			marginBottom: 0,
			paddingLeft: 5,
			paddingTop: 0,
			paddingBottom: 0,
		}),
	};
};

export const dropDownSpacingTheme = (theme) => ({
	...theme,
	spacing: {
		...theme.spacing,
		controlHeight: 25,
		baseUnit: 2,
	},
});

export const overflowStyle = {
	overflow: "auto",
	overflowWrap: "normal",
};
