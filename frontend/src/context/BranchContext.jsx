import { createContext, useContext, useEffect, useState } from 'react';
import { useAuth } from './AuthContext';

const BranchContext = createContext(null);

export function BranchProvider({ children }) {
  const { user } = useAuth();
  const [branchId, setBranchId] = useState(() => user?.branchId ?? null);

  useEffect(() => {
    setBranchId(user?.branchId ?? null);
  }, [user]);

  return (
    <BranchContext.Provider value={{ branchId, setBranchId }}>
      {children}
    </BranchContext.Provider>
  );
}

export function useBranch() {
  return useContext(BranchContext);
}
