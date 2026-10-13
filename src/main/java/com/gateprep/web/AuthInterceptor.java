package com.gateprep.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Blocks the protected endpoints unless the session holds a logged-in user id. */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String USER_ID = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        HttpSession session = request.getSession(false);
        Object id = session == null ? null : session.getAttribute(USER_ID);
        if (id == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Please log in\"}");
            return false;
        }
        request.setAttribute(USER_ID, id);
        return true;
    }
}
