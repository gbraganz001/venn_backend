package com.venn.velocity.repo;

import com.venn.velocity.entity.LoadPerDay;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class LoadPerDayRepositoryTest {

    @Autowired
    private LoadPerDayRepository loadPerDayRepository;

    @Test
    void findByCustomerIdAndDayUtc_returnsSavedEntity() {
        String customerId = "35";
        LocalDate dayUtc = LocalDate.of(2000, 2, 1);

        LoadPerDay saved = loadPerDayRepository.save(
                LoadPerDay.builder()
                        .customerId(customerId)
                        .dayInUtc(dayUtc)
                        .acceptedCount(2)
                        .acceptedAmountCents(123_45)
                        .build()
        );

        // when
        var found = loadPerDayRepository.lockByCustomerAndDay(customerId, dayUtc);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getCustomerId()).isEqualTo(customerId);
        assertThat(found.get().getDayInUtc()).isEqualTo(dayUtc);
        assertThat(found.get().getAcceptedCount()).isEqualTo(2);
        assertThat(found.get().getAcceptedAmountCents()).isEqualTo(123_45);
    }

    @Test
    void findByCustomerIdAndDayUtc_returnsEmptyWhenNotFound() {
        var found = loadPerDayRepository.lockByCustomerAndDay("does-not-exist", LocalDate.of(2000, 2, 1));
        assertThat(found).isEmpty();
    }

    @Test
    void uniqueConstraint_preventsDuplicateRowsForSameCustomerAndDay() {
        // given
        String customerId = "35";
        LocalDate dayUtc = LocalDate.of(2000, 2, 1);

        loadPerDayRepository.saveAndFlush(
                LoadPerDay.builder()
                        .customerId(customerId)
                        .dayInUtc(dayUtc)
                        .acceptedCount(1)
                        .acceptedAmountCents(100)
                        .build()
        );

        // when / then
        assertThatThrownBy(() ->
                loadPerDayRepository.saveAndFlush(
                        LoadPerDay.builder()
                                .customerId(customerId)
                                .dayInUtc(dayUtc) // same day + same customer => violates unique constraint
                                .acceptedCount(2)
                                .acceptedAmountCents(200)
                                .build()
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}