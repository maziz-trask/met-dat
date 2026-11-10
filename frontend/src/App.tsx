import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import type { ReactElement } from 'react';
import { useAuth } from './auth/AuthContext';
import { Layout } from './components/Layout';
import { Loading } from './components/Feedback';
import { About } from './pages/About';
import { Dashboard } from './pages/Dashboard';
import { LoginPage } from './pages/LoginPage';
import { TestDetailPage } from './pages/TestDetailPage';
import { TestFormPage } from './pages/TestFormPage';
import { TestListPage } from './pages/TestListPage';
import { TestTypesPage } from './pages/TestTypesPage';
import { NotAllowed } from './pages/NotAllowed';

function RequireAuth({ children, editor }: { children: ReactElement; editor?: boolean }) {
  const { user, loading, isEditor } = useAuth();
  const loc = useLocation();
  if (loading) return <Loading />;
  if (!user) return <Navigate to="/login" state={{ from: loc.pathname }} replace />;
  if (editor && !isEditor) return <NotAllowed />;
  return children;
}

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<RequireAuth><Dashboard /></RequireAuth>} />
        <Route path="/tests" element={<RequireAuth><TestListPage /></RequireAuth>} />
        <Route path="/tests/new" element={<RequireAuth editor><TestFormPage /></RequireAuth>} />
        <Route path="/tests/:id" element={<RequireAuth><TestDetailPage /></RequireAuth>} />
        <Route path="/tests/:id/edit" element={<RequireAuth editor><TestFormPage /></RequireAuth>} />
        <Route path="/test-types" element={<RequireAuth><TestTypesPage /></RequireAuth>} />
        <Route path="/about" element={<About />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
