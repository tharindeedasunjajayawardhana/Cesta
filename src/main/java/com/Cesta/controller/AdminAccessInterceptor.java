
package com.Cesta.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        HttpSession session = request.getSession(false);

        if (session == null
                || !(session.getAttribute("cestaCustomerId") instanceof Long)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        Object role = session.getAttribute("cestaRole");

        if (!"ADMIN".equals(role)) {
            response.sendRedirect(request.getContextPath() + "/account");
            return false;
        }

        return true;
    }
}
