package dev.ccosta.aisha.web.timefilter;

import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class DateFilterSessionService {

    static final String SESSION_KEY = "globalDateFilter";

    private final Clock clock;

    public DateFilterSessionService() {
        this(Clock.systemDefaultZone());
    }

    DateFilterSessionService(Clock clock) {
        this.clock = clock;
    }

    public DateFilterState getOrCreate(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof DateFilterState state) {
            return state;
        }

        DateFilterState state = DateFilterState.defaultState(clock);
        session.setAttribute(SESSION_KEY, state);
        return state;
    }

    public void applyAction(
        HttpSession session,
        DateFilterAction action,
        LocalDate startDate,
        LocalDate endDate
    ) {
        DateFilterState state = getOrCreate(session);

        if (action == DateFilterAction.SET_WEEK) {
            state.applyPreset(DateFilterPreset.WEEK, clock);
            return;
        }

        if (action == DateFilterAction.SET_MONTH) {
            state.applyPreset(DateFilterPreset.MONTH, clock);
            return;
        }

        if (action == DateFilterAction.SET_YEAR) {
            state.applyPreset(DateFilterPreset.YEAR, clock);
            return;
        }

        if (action == DateFilterAction.GO_PREVIOUS) {
            state.goPrevious(clock);
            return;
        }

        if (action == DateFilterAction.GO_CURRENT) {
            state.goCurrent(clock);
            return;
        }

        if (action == DateFilterAction.GO_NEXT) {
            state.goNext(clock);
            return;
        }

        state.applyCustom(startDate, endDate);
    }
}
