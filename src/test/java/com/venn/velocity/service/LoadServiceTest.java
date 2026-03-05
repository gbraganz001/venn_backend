package com.venn.velocity.service;

import com.venn.velocity.entity.LoadPerDay;
import com.venn.velocity.entity.LoadPerWeek;
import com.venn.velocity.model.LoadRequest;
import com.venn.velocity.repo.LoadPerDayRepository;
import com.venn.velocity.repo.LoadPerWeekRepository;
import com.venn.velocity.repo.LoadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.runner.enabled=false")
@ActiveProfiles("test")
class LoadServiceTest {

    @Autowired private LoadService service;

    @Autowired private LoadRepository loadRepository;
    @Autowired private LoadPerDayRepository loadPerDayRepository;
    @Autowired private LoadPerWeekRepository loadPerWeekRepository;

    @BeforeEach
    void cleanDb() {
        // order matters if you have FK constraints; even without, it's safe
        loadRepository.deleteAll();
        loadPerDayRepository.deleteAll();
        loadPerWeekRepository.deleteAll();
    }

    @Test
    void sameLoadIdDifferentCustomers_isNotDuplicate_eachGetsAResponse() {
        Instant t = Instant.parse("2000-02-01T11:49:58Z");

        var r1 = service.process(new LoadRequest("DUP", "35", "$10.00", t));
        var r2 = service.process(new LoadRequest("DUP", "36", "$10.00", t));

        assertThat(r1).isPresent();
        assertThat(r2).isPresent();
        assertThat(loadRepository.count()).isEqualTo(2);
    }

    @Test
    void duplicateLoadIdForSameCustomer_isIgnored_noSecondResponse_andNoDoubleCounting() {
        Instant t = Instant.parse("2000-02-01T11:49:58Z");
        LoadRequest req = new LoadRequest("24407", "35", "$1011.06", t);

        var r1 = service.process(req);
        var r2 = service.process(req);

        assertThat(r1).isPresent();
        assertThat(r2).isEmpty();

        assertThat(loadRepository.count()).isEqualTo(1);
        if (r1.get().accepted()) {
            LocalDate day = LocalDate.of(2000, 2, 1);
            LocalDate weekStart = LocalDate.of(2000, 1, 31); // Monday

            LoadPerDay d = loadPerDayRepository.findByCustomerIdAndDayInUtc("35", day).orElseThrow();
            assertThat(d.getAcceptedCount()).isEqualTo(1);
            assertThat(d.getAcceptedAmountCents()).isEqualTo(101106);

            LoadPerWeek w = loadPerWeekRepository.findByCustomerIdAndWeekStartDate("35", weekStart).orElseThrow();
            assertThat(w.getAcceptedAmountCents()).isEqualTo(101106);
        }
    }
}