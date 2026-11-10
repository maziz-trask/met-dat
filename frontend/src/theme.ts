import { createTheme } from '@mui/material';

export const theme = createTheme({
  palette: { primary: { main: '#1565c0' }, secondary: { main: '#00796b' }, background: { default: '#f4f6f9' } },
  shape: { borderRadius: 8 },
  typography: { fontFamily: '"Roboto","Segoe UI",Arial,sans-serif', h4: { fontWeight: 600 }, h5: { fontWeight: 600 } },
  components: {
    MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: { root: { textTransform: 'none' } } },
    MuiCard: { defaultProps: { variant: 'outlined' } },
    MuiPaper: { defaultProps: { variant: 'outlined' } },
  },
});
