import { useEffect, useRef, useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useBranch } from '../context/BranchContext';
import api from '../api/client';
import BrandMark from './BrandMark';
import {
  LayoutDashboard,
  ShoppingCart,
  Package,
  Warehouse,
  Building2,
  Bell,
  BarChart3,
  ScrollText,
  Store,
  ChevronDown,
  User,
  Settings,
  LogOut,
} from 'lucide-react';

export default function Layout() {
  const { user, logout, hasRole } = useAuth();
  const { branchId, setBranchId } = useBranch();
  const navigate = useNavigate();
  const [unread, setUnread] = useState(0);
  const [branches, setBranches] = useState([]);
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef(null);

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

  useEffect(() => {
    api.get('/branches')
      .then((res) => setBranches(res.data))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (!menuOpen) return undefined;
    function onDown(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setMenuOpen(false);
      }
    }
    function onKey(event) {
      if (event.key === 'Escape') setMenuOpen(false);
    }
    document.addEventListener('mousedown', onDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', onDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [menuOpen]);

  function handleLogout() {
    logout();
    navigate('/login');
  }

  function handleBranchChange(event) {
    const value = event.target.value;
    setBranchId(value === '' ? null : Number(value));
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-row">
            <BrandMark size={24} />
            <span className="brand-name">SUPER<span>market</span></span>
          </div>
          <div className="brand-sub">Inventory &amp; Sales</div>
        </div>

        <div className="nav-section">Operations</div>
        <NavLink to="/dashboard"><LayoutDashboard size={18} strokeWidth={2} />Dashboard</NavLink>
        <NavLink to="/pos"><ShoppingCart size={18} strokeWidth={2} />POS / Sales</NavLink>
        <NavLink to="/products"><Package size={18} strokeWidth={2} />Products</NavLink>
        <NavLink to="/inventory"><Warehouse size={18} strokeWidth={2} />Inventory</NavLink>
        <NavLink to="/suppliers"><Building2 size={18} strokeWidth={2} />Suppliers</NavLink>

        <div className="nav-section">Insights</div>
        <NavLink to="/notifications"><Bell size={18} strokeWidth={2} />Notifications</NavLink>
        {hasRole('ADMIN', 'MANAGER') && <NavLink to="/reports"><BarChart3 size={18} strokeWidth={2} />Reports</NavLink>}
        {hasRole('ADMIN', 'MANAGER') && <NavLink to="/audit"><ScrollText size={18} strokeWidth={2} />Audit history</NavLink>}

        <div className="nav-section">Administration</div>
        <NavLink to="/branches"><Store size={18} strokeWidth={2} />Branches</NavLink>
        {hasRole('ADMIN') && <NavLink to="/users"><User size={18} strokeWidth={2} />Users</NavLink>}
      </aside>

      <div className="main">
        <header className="topbar">
          <div className="who">
            <strong>{user?.fullName}</strong>
            <span className="role-badge">{user?.role}</span>
          </div>
          <div className="topbar-actions">
            <label className="branch-select">
              <Store size={16} strokeWidth={2} />
              <select value={branchId ?? ''} onChange={handleBranchChange}>
                <option value="">All branches</option>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
            </label>

            <button className="bell" aria-label="Notifications" onClick={() => navigate('/notifications')}>
              <Bell size={18} strokeWidth={2} />
              {unread > 0 && <span className="count">{unread}</span>}
            </button>

            <div className="profile" ref={menuRef}>
              <button className="profile-trigger" onClick={() => setMenuOpen((open) => !open)}>
                <span className="avatar">{(user?.fullName || '?').trim().charAt(0).toUpperCase()}</span>
                <span className="profile-name">{user?.fullName}</span>
                <ChevronDown size={16} strokeWidth={2} />
              </button>
              {menuOpen && (
                <div className="dropdown">
                  <button className="dropdown-item" onClick={() => setMenuOpen(false)}>
                    <User size={16} strokeWidth={2} />
                    Profile
                  </button>
                  <button className="dropdown-item" onClick={() => setMenuOpen(false)}>
                    <Settings size={16} strokeWidth={2} />
                    Account Settings
                  </button>
                  <div className="dropdown-sep" />
                  <button className="dropdown-item danger" onClick={handleLogout}>
                    <LogOut size={16} strokeWidth={2} />
                    Log out
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <Outlet />
      </div>
    </div>
  );
}
