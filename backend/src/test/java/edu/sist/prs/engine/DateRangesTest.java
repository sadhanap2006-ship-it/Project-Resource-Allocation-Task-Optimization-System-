package edu.sist.prs.engine;

import edu.sist.prs.util.DateRanges;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DateRangesTest {

    @Test
    @DisplayName("R1 Overlap boundaries: 1-10 Jan & 10-20 Jan overlap; 1-10 Jan & 11-20 Jan do not")
    void testOverlapBoundaries() {
        LocalDate jan1 = LocalDate.of(2027, 1, 1);
        LocalDate jan10 = LocalDate.of(2027, 1, 10);
        LocalDate jan11 = LocalDate.of(2027, 1, 11);
        LocalDate jan20 = LocalDate.of(2027, 1, 20);

        // 1-10 Jan and 10-20 Jan overlap on 10 Jan
        assertThat(DateRanges.overlaps(jan1, jan10, jan10, jan20)).isTrue();
        assertThat(DateRanges.overlaps(jan10, jan20, jan1, jan10)).isTrue();

        // 1-10 Jan and 11-20 Jan do not overlap
        assertThat(DateRanges.overlaps(jan1, jan10, jan11, jan20)).isFalse();
        assertThat(DateRanges.overlaps(jan11, jan20, jan1, jan10)).isFalse();

        // Single day ranges
        assertThat(DateRanges.overlaps(jan10, jan10, jan10, jan10)).isTrue();
        assertThat(DateRanges.overlaps(jan10, jan10, jan11, jan11)).isFalse();

        // Identical ranges
        assertThat(DateRanges.overlaps(jan1, jan10, jan1, jan10)).isTrue();
    }
}
