package com.supermarket.security;

import com.supermarket.domain.Role;
import com.supermarket.exception.ForbiddenException;
import com.supermarket.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Reads the authenticated user from the JWT in the SecurityContext.
 * JWT claims: {@code sub}=email, {@code uid}=user id, {@code role}=RBAC role,
 * {@code branch}=branch id (may be null for admins), {@code name}=full name.
 */
@Component
public class AuthContext {

    public record AuthUser(Long id, String email, String name, Role role, Long branchId) {

        public boolean isAdmin() {
            return role == Role.ADMIN;
        }
    }

    /** Returns the current user or empty when nobody is authenticated. */
    public AuthUser current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return null;
        }
        Object rawUid = jwt.getClaim("uid");
        Object rawBranch = jwt.getClaim("branch");
        Long uid = rawUid instanceof Number number ? number.longValue() : null;
        Long branch = rawBranch instanceof Number number ? number.longValue() : null;
        String role = jwt.getClaimAsString("role");
        return new AuthUser(uid, jwt.getSubject(), jwt.getClaimAsString("name"),
                role == null ? null : Role.valueOf(role), branch);
    }

    /** Returns the current user or throws 401. */
    public AuthUser require() {
        AuthUser user = current();
        if (user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }

    /**
     * Branch-level authorization: admins can access every branch,
     * everyone else only their own branch.
     */
    public AuthUser requireBranchAccess(Long branchId) {
        AuthUser user = require();
        if (user.isAdmin()) {
            return user;
        }
        if (user.branchId() == null || (branchId != null && !user.branchId().equals(branchId))) {
            throw new ForbiddenException("You are not allowed to access data for branch " + branchId);
        }
        return user;
    }
}
