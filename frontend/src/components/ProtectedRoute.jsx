import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Route guard: requires a JWT, and optionally one of the given RBAC roles.
 * Mirrors the @PreAuthorize rules enforced by the backend.
 */
export default function ProtectedRoute({ roles }) {
  const { token, user, hasRole } = useAuth();

  if (!token) {
    return <Navigate to="/login" replace />;
  }

  if (roles && roles.length > 0 && !hasRole(...roles)) {
    return (
      <div className="page">
        <div className="card empty-state">
          <h2>Access denied</h2>
          <p>
            Your role (<strong>{user?.role}</strong>) is not allowed to open this page.
          </p>
        </div>
      </div>
    );
  }

  return <Outlet />;
}
