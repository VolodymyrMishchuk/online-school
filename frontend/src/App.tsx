import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import LandingPage from './pages/LandingPage';
import CatalogPage from './pages/CatalogPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import MagicLoginPage from './pages/MagicLoginPage';
import DashboardLayout from './layouts/DashboardLayout';
import AllCoursesPage from './pages/AllCoursesPage';
import AllModulesPage from './pages/AllModulesPage';
import AllLessonsPage from './pages/AllLessonsPage';
import MyCoursesPage from './pages/MyCoursesPage';
import MyModulesPage from './pages/MyModulesPage';
import MyLessonsPage from './pages/MyLessonsPage';
import { UsersPage } from './pages/UsersPage';
import SettingsPage from './pages/SettingsPage';

const queryClient = new QueryClient();

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <Router>
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/catalog" element={<CatalogPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/auth/reset-password" element={<ResetPasswordPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/magic-login" element={<MagicLoginPage />} />
          <Route path="/dashboard" element={<DashboardLayout />}>
            <Route index element={<Navigate to="/dashboard/my-courses" replace />} />
            <Route path="all-courses" element={<AllCoursesPage />} />
            <Route path="all-modules" element={<AllModulesPage />} />
            <Route path="all-lessons" element={<AllLessonsPage />} />
            <Route path="my-courses" element={<MyCoursesPage />} />
            <Route path="my-lessons" element={<MyLessonsPage />} />
            <Route path="users" element={<UsersPage />} />
            <Route path="settings" element={<SettingsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </Router>
    </QueryClientProvider>
  );
}

export default App;
