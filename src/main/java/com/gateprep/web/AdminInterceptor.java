package com.gateprep.web;

import com.gateprep.model.User;
import com.gateprep.repo.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Runs after AuthInterceptor. Lets only administrators through. */
@Component
public class AdminInterceptor implements HandlerInterceptor {
    private final UserRepository users;

    public AdminInterceptor(UserRepository users) {
        this.users = users;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        Object id = request.getAttribute(AuthInterceptor.USER_ID);
        User u = id == null ? null : users.findById((Long) id).orElse(null);
        if (u == null || !"ADMIN".equals(u.role)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Administrators only\"}");
            return false;
        }
        return true;
    }
}
