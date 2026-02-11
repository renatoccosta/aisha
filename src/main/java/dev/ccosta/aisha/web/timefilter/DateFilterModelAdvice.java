package dev.ccosta.aisha.web.timefilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class DateFilterModelAdvice {

    private final DateFilterSessionService dateFilterSessionService;

    public DateFilterModelAdvice(DateFilterSessionService dateFilterSessionService) {
        this.dateFilterSessionService = dateFilterSessionService;
    }

    @ModelAttribute("globalDateFilter")
    public DateFilterState globalDateFilter(HttpSession session) {
        return dateFilterSessionService.getOrCreate(session);
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isBlank()) {
            return request.getRequestURI();
        }
        return request.getRequestURI() + "?" + queryString;
    }
}
