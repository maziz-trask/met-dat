import { createTheme } from '@mui/material';
import type { Mode } from './ThemeModeContext';

export const createAppTheme = (mode: Mode) =>
  createTheme({
    palette: {
      mode,
      primary: { main: mode === 'dark' ? '#90caf9' : '#1565c0' },
      secondary: { main: mode === 'dark' ? '#4db6ac' : '#00796b' },
      background: mode === 'dark' ? { default: '#101418', paper: '#1a2027' } : { default: '#f4f6f9', paper: '#ffffff' },
    },
    shape: { borderRadius: 8 },
    typography: { fontFamily: '"Roboto","Segoe UI",Arial,sans-serif', h4: { fontWeight: 600 }, h5: { fontWeight: 600 } },
    components: {
      MuiButton: { defaultProps: { disableElevation: true }, styleOverrides: { root: { textTransform: 'none' } } },
      MuiCard: { defaultProps: { variant: 'outlined' } },
      MuiPaper: { defaultProps: { variant: 'outlined' } },
      MuiAppBar: { styleOverrides: { root: { backgroundImage: 'none' } } },
    },
  });
