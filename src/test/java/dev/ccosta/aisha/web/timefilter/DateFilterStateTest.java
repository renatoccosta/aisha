package dev.ccosta.aisha.web.timefilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class DateFilterStateTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-02-11T12:00:00Z"), ZoneId.of("UTC"));

    @Test
    void shouldUseCurrentMonthAsDefault() {
        DateFilterState state = DateFilterState.defaultState(FIXED_CLOCK);

        assertThat(state.getPreset()).isEqualTo(DateFilterPreset.MONTH);
        assertThat(state.isCustom()).isFalse();
        assertThat(state.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(state.getEndDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    void shouldNavigateMonthPeriods() {
        DateFilterState state = DateFilterState.defaultState(FIXED_CLOCK);

        state.goPrevious(FIXED_CLOCK);
        assertThat(state.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(state.getEndDate()).isEqualTo(LocalDate.of(2026, 1, 31));

        state.goCurrent(FIXED_CLOCK);
        assertThat(state.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(state.getEndDate()).isEqualTo(LocalDate.of(2026, 2, 28));

        state.goNext(FIXED_CLOCK);
        assertThat(state.getStartDate()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(state.getEndDate()).isEqualTo(LocalDate.of(2026, 3, 31));
    }

    @Test
    void shouldApplyWeekPreset() {
        DateFilterState state = DateFilterState.defaultState(FIXED_CLOCK);

        state.applyPreset(DateFilterPreset.WEEK, FIXED_CLOCK);

        assertThat(state.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 9));
        assertThat(state.getEndDate()).isEqualTo(LocalDate.of(2026, 2, 15));
    }

    @Test
    void shouldApplyCustomInterval() {
        DateFilterState state = DateFilterState.defaultState(FIXED_CLOCK);

        state.applyCustom(LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 20));

        assertThat(state.isCustom()).isTrue();
        assertThat(state.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(state.getEndDate()).isEqualTo(LocalDate.of(2026, 1, 20));
    }

    @Test
    void shouldRejectInvalidCustomInterval() {
        DateFilterState state = DateFilterState.defaultState(FIXED_CLOCK);

        assertThatThrownBy(() -> state.applyCustom(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 10)))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
