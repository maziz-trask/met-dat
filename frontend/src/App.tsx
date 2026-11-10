import type { ReactElement } from 'react';
import { Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { useAuth } from './auth/AuthContext';
import { Loading } from './components/Feedback';
import { Layout } from './components/Layout';
import { About } from './pages/About';
import { Dashboard } from './pages/Dashboard';
import { LoginPage } from './pages/LoginPage';
import { NotAllowed } from './pages/NotAllowed';
import { TestDetailPage } from './pages/TestDetailPage';
import { TestFormPage } from './pages/TestFormPage';
import { TestListPage } from './pages/TestListPage';
import { TestTypesPage } from './pages/TestTypesPage';
import { UsersPage } from './pages/UsersPage';

/** Route guard (UX only – the backend enforces the same rules on every API call). */
function RequireAuth({ children, editor }: { children: ReactElement; editor?: boolean }) {
  const { user, loading, isEditor } = useAuth();
  const location = useLocation();
  if (loading) return <Loading />;
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />;
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
        <Route path="/users" element={<RequireAuth editor><UsersPage /></RequireAuth>} />
        <Route path="/about" element={<About />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
