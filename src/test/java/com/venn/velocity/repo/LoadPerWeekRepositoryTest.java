package com.venn.velocity.repo;

import com.venn.velocity.entity.LoadPerWeek;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class LoadPerWeekRepositoryTest {

    @Autowired
    private LoadPerWeekRepository repository;

    @Test
    void shouldSaveAndFindByCustomerAndWeek() {

        String customerId = "35";
        LocalDate weekStart = LocalDate.of(2000, 1, 31); // Monday

        LoadPerWeek entity = LoadPerWeek.builder()
                .customerId(customerId)
                .weekStartDate(weekStart)
                .acceptedAmountCents(50000)
                .build();

        repository.save(entity);
        var result = repository.lockByCustomerAndWeek(customerId, weekStart);
        assertThat(result).isPresent();
        assertThat(result.get().getCustomerId()).isEqualTo(customerId);
        assertThat(result.get().getWeekStartDate()).isEqualTo(weekStart);
        assertThat(result.get().getAcceptedAmountCents()).isEqualTo(50000);
    }

    @Test
    void shouldReturnEmptyWhenWeekNotFound() {

        var result = repository.lockByCustomerAndWeek(
                "unknown",
                LocalDate.of(2000, 1, 31)
        );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotAllowDuplicateCustomerWeek() {

        String customerId = "35";
        LocalDate weekStart = LocalDate.of(2000, 1, 31);

        repository.saveAndFlush(
                LoadPerWeek.builder()
                        .customerId(customerId)
                        .weekStartDate(weekStart)
                        .acceptedAmountCents(1000)
                        .build()
        );

        assertThatThrownBy(() ->
                repository.saveAndFlush(
                        LoadPerWeek.builder()
                                .customerId(customerId)
                                .weekStartDate(weekStart)
                                .acceptedAmountCents(2000)
                                .build()
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }
}
