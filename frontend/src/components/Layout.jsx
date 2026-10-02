import { useEffect, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/client';

/** Sidebar + top bar shell rendered around every authenticated page. */
export default function Layout() {
  const { user, logout, hasRole } = useAuth();
  const navigate = useNavigate();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    let active = true;
    const load = () =>
      api
        .get('/notifications/unread-count')
        .then((res) => active && setUnread(res.data.unread || 0))
        .catch(() => {});
    load();
    const id = setInterval(load, 30000);
    return () => {
      active = false;
      clearInterval(id);
    };
  }, []);

  function handleLogout() {
    logout();
    navigate('/login');
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          SUPER<span>market</span>
          <div style={{ fontSize: 11, fontWeight: 400, color: '#8fa0c4' }}>
            Inventory &amp; Sales
          </div>
        </div>

        <div className="nav-section">Operations</div>
        <NavLink to="/dashboard">Dashboard</NavLink>
        <NavLink to="/pos">POS / Sales</NavLink>
        <NavLink to="/products">Products</NavLink>
        <NavLink to="/inventory">Inventory</NavLink>
        <NavLink to="/suppliers">Suppliers</NavLink>

        <div className="nav-section">Insights</div>
        <NavLink to="/notifications">Notifications</NavLink>
        {hasRole('ADMIN', 'MANAGER') && <NavLink to="/reports">Reports</NavLink>}
        {hasRole('ADMIN', 'MANAGER') && <NavLink to="/audit">Audit history</NavLink>}

        <div className="nav-section">Administration</div>
        <NavLink to="/branches">Branches</NavLink>
        {hasRole('ADMIN') && <NavLink to="/users">Users</NavLink>}
      </aside>

      <div className="main">
        <header className="topbar">
          <div className="who">
            <strong>{user?.fullName}</strong>
            <span className="role-badge">{user?.role}</span>
            {user?.branchId ? <span> · Branch #{user.branchId}</span> : <span> · All branches</span>}
          </div>
          <div className="topbar-actions">
            <button className="bell" onClick={() => navigate('/notifications')}>
              🔔{unread > 0 && <span className="count">{unread}</span>}
            </button>
            <button className="btn secondary" onClick={handleLogout}>
              Log out
            </button>
          </div>
        </header>

        <Outlet />
      </div>
    </div>
  );
}
