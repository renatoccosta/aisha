package dev.ccosta.aisha.web.entry;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.springframework.format.annotation.DateTimeFormat;

class EntryFormTest {

    @Test
    void shouldUseIsoDateFormatForDateFields() throws NoSuchFieldException {
        Field movementDate = EntryForm.class.getDeclaredField("movementDate");
        Field settlementDate = EntryForm.class.getDeclaredField("settlementDate");

        DateTimeFormat movementDateFormat = movementDate.getAnnotation(DateTimeFormat.class);
        DateTimeFormat settlementDateFormat = settlementDate.getAnnotation(DateTimeFormat.class);

        assertThat(movementDateFormat).isNotNull();
        assertThat(movementDateFormat.iso()).isEqualTo(DateTimeFormat.ISO.DATE);
        assertThat(settlementDateFormat).isNotNull();
        assertThat(settlementDateFormat.iso()).isEqualTo(DateTimeFormat.ISO.DATE);
    }
}
