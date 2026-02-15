package dev.ccosta.aisha.web.timefilter;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DateFilterController {

    private final DateFilterSessionService dateFilterSessionService;

    public DateFilterController(DateFilterSessionService dateFilterSessionService) {
        this.dateFilterSessionService = dateFilterSessionService;
    }

    @PostMapping("/date-filter")
    public String updateFilter(
        @RequestParam DateFilterAction action,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam(defaultValue = "/dashboard") String redirectTo,
        HttpSession session
    ) {
        try {
            dateFilterSessionService.applyAction(session, action, startDate, endDate);
        } catch (IllegalArgumentException ex) {
            // Invalid custom range keeps the current filter state.
        }

        if (!redirectTo.startsWith("/")) {
            return "redirect:/dashboard";
        }

        return "redirect:" + redirectTo;
    }
}
