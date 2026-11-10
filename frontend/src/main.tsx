import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import './i18n';
import { App } from './App';
import { AuthProvider } from './auth/AuthContext';
import { NotifyProvider } from './components/Notify';
import { ThemeModeProvider } from './theme/ThemeModeContext';

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false, refetchOnWindowFocus: false } } });

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeModeProvider>
      <QueryClientProvider client={queryClient}>
        <NotifyProvider>
          <AuthProvider>
            <BrowserRouter>
              <App />
            </BrowserRouter>
          </AuthProvider>
        </NotifyProvider>
      </QueryClientProvider>
    </ThemeModeProvider>
  </React.StrictMode>,
);
