import { Alert, Button } from '@mui/material';
import { Link } from 'react-router-dom';

export const NotAllowed = () => (
  <Alert severity="warning" action={<Button component={Link} to="/tests" color="inherit" size="small">Back to tests</Button>}>
    This page is only available to the Editor role. Use the role switcher (top right) to sign in as an Editor.
  </Alert>
);
