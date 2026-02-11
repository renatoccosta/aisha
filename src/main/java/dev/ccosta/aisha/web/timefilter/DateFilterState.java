package dev.ccosta.aisha.web.timefilter;

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class DateFilterState implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private DateFilterPreset preset;
    private int offset;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean custom;

    public static DateFilterState defaultState(Clock clock) {
        DateFilterState state = new DateFilterState();
        state.applyPreset(DateFilterPreset.MONTH, clock);
        return state;
    }

    public void applyPreset(DateFilterPreset targetPreset, Clock clock) {
        this.preset = targetPreset;
        this.offset = 0;
        this.custom = false;
        recalculateFromPreset(clock);
    }

    public void goPrevious(Clock clock) {
        if (custom) {
            return;
        }

        this.offset--;
        recalculateFromPreset(clock);
    }

    public void goCurrent(Clock clock) {
        if (custom) {
            return;
        }

        this.offset = 0;
        recalculateFromPreset(clock);
    }

    public void goNext(Clock clock) {
        if (custom) {
            return;
        }

        this.offset++;
        recalculateFromPreset(clock);
    }

    public void applyCustom(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be greater than or equal to start date");
        }

        this.custom = true;
        this.offset = 0;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    private void recalculateFromPreset(Clock clock) {
        LocalDate baseDate = LocalDate.now(clock);

        if (preset == DateFilterPreset.WEEK) {
            LocalDate shifted = baseDate.plusWeeks(offset);
            this.startDate = shifted.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            this.endDate = shifted.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            return;
        }

        if (preset == DateFilterPreset.MONTH) {
            LocalDate shifted = baseDate.plusMonths(offset);
            this.startDate = shifted.withDayOfMonth(1);
            this.endDate = shifted.withDayOfMonth(shifted.lengthOfMonth());
            return;
        }

        LocalDate shifted = baseDate.plusYears(offset);
        this.startDate = shifted.withDayOfYear(1);
        this.endDate = shifted.withDayOfYear(shifted.lengthOfYear());
    }

    public DateFilterPreset getPreset() {
        return preset;
    }

    public int getOffset() {
        return offset;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public boolean isCustom() {
        return custom;
    }
}
