import { makeStyles } from '@material-ui/core';
import { getTimezoneOffset } from './utils';
import { NOTIFICATION_CONTEXT } from '../configs/modalConfig';
import { checkForConflicts } from '../thunks/eventThunks';

// Base style for Stepper component
export const useStepperStyle = makeStyles(theme => ({
  root: {
    background: '#eeeff0',
    height: '100px'
  },
}));

// Style of icons used in Stepper
export const useStepIconStyles = makeStyles({
  root: {
    height: 22,
    alignItems: 'center',
  },
  circle: {
    color: '#92a0ab',
    width: '20px',
    height: '20px'
  },
});
