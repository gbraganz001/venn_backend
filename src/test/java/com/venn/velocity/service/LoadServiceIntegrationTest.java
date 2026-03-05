package com.venn.velocity.service;

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
class LoadServiceIntegrationTest {

    @Autowired private LoadService service;

    @Autowired private LoadRepository loadRepository;
    @Autowired private LoadPerDayRepository loadPerDayRepository;
    @Autowired private LoadPerWeekRepository loadPerWeekRepository;

    @BeforeEach
    void cleanDb() {
        loadRepository.deleteAll();
        loadPerDayRepository.deleteAll();
        loadPerWeekRepository.deleteAll();
    }

    @Test
    void idempotency_duplicateLoadIdSameCustomer_returnsNoSecondResponse_andDoesNotDoubleCount() {
        Instant t = Instant.parse("2000-02-01T11:49:58Z");
        var req = new LoadRequest("L1", "C1", "$10.00", t);

        var r1 = service.process(req);
        var r2 = service.process(req);

        assertThat(r1).isPresent();
        assertThat(r2).isEmpty();
        assertThat(loadRepository.count()).isEqualTo(1);
    }

    @Test
    void sameLoadIdDifferentCustomers_isNotDuplicate_eachGetsProcessed() {
        Instant t = Instant.parse("2000-02-01T11:49:58Z");

        var r1 = service.process(new LoadRequest("DUP", "C1", "$10.00", t));
        var r2 = service.process(new LoadRequest("DUP", "C2", "$10.00", t));

        assertThat(r1).isPresent();
        assertThat(r2).isPresent();
        assertThat(loadRepository.count()).isEqualTo(2);
    }

    @Test
    void dailyCountLimit_fourthLoadSameDay_declined() {
        Instant base = Instant.parse("2000-02-01T10:00:00Z");

        var r1 = service.process(new LoadRequest("A", "C1", "$1.00", base.plusSeconds(1))).orElseThrow();
        var r2 = service.process(new LoadRequest("B", "C1", "$1.00", base.plusSeconds(2))).orElseThrow();
        var r3 = service.process(new LoadRequest("C", "C1", "$1.00", base.plusSeconds(3))).orElseThrow();
        var r4 = service.process(new LoadRequest("D", "C1", "$1.00", base.plusSeconds(4))).orElseThrow();

        assertThat(r1.accepted()).isTrue();
        assertThat(r2.accepted()).isTrue();
        assertThat(r3.accepted()).isTrue();
        assertThat(r4.accepted()).isFalse();
    }

    @Test
    void dailyAmountLimit_secondLoadExceeds5000_declined() {
        Instant base = Instant.parse("2000-02-01T10:00:00Z");

        var r1 = service.process(new LoadRequest("A", "C1", "$3000.00", base.plusSeconds(1))).orElseThrow();
        var r2 = service.process(new LoadRequest("B", "C1", "$2500.00", base.plusSeconds(2))).orElseThrow();

        assertThat(r1.accepted()).isTrue();
        assertThat(r2.accepted()).isFalse();
    }

    @Test
    void weeklyAmountLimit_exceeds20000_declined() {
        // Week starting Monday Jan 31, 2000 (UTC)
        Instant mon = Instant.parse("2000-01-31T10:00:00Z");
        Instant tue = Instant.parse("2000-02-01T10:00:00Z");
        Instant wed = Instant.parse("2000-02-02T10:00:00Z");
        Instant thu = Instant.parse("2000-02-03T10:00:00Z");
        Instant fri = Instant.parse("2000-02-04T10:00:00Z");

        var r1 = service.process(new LoadRequest("A", "C1", "$5000.00", mon)).orElseThrow();
        var r2 = service.process(new LoadRequest("B", "C1", "$5000.00", tue)).orElseThrow();
        var r3 = service.process(new LoadRequest("C", "C1", "$5000.00", wed)).orElseThrow();
        var r4 = service.process(new LoadRequest("D", "C1", "$5000.00", thu)).orElseThrow();
        var r5 = service.process(new LoadRequest("E", "C1", "$0.01",  fri)).orElseThrow();

        assertThat(r1.accepted()).isTrue();
        assertThat(r2.accepted()).isTrue();
        assertThat(r3.accepted()).isTrue();
        assertThat(r4.accepted()).isTrue();
        assertThat(r5.accepted()).isFalse(); // exceeds weekly 20000 by 1 cent
    }

    @Test
    void midnightUtc_resetsDailyCounters_butWeeklyStillAccumulates() {
        Instant t1 = Instant.parse("2000-02-01T23:59:58Z");
        Instant t2 = Instant.parse("2000-02-01T23:59:59Z");
        Instant t3 = Instant.parse("2000-02-02T00:00:00Z");

        var r1 = service.process(new LoadRequest("A", "C1", "$4000.00", t1)).orElseThrow();
        var r2 = service.process(new LoadRequest("B", "C1", "$2000.00", t2)).orElseThrow();

        assertThat(r1.accepted()).isTrue();
        assertThat(r2.accepted()).isFalse();

        var r3 = service.process(new LoadRequest("C", "C1", "$2000.00", t3)).orElseThrow();
        assertThat(r3.accepted()).isTrue();

        LocalDate feb2 = LocalDate.of(2000, 2, 2);
        assertThat(loadPerDayRepository.findByCustomerIdAndDayInUtc("C1", feb2)).isPresent();
    }

    @Test
    void weekBoundary_mondayStartsNewWeek_weeklyCountersReset() {
        Instant monPrevWeek = Instant.parse("2000-01-31T10:00:00Z");
        Instant tuePrevWeek = Instant.parse("2000-02-01T10:00:00Z");
        Instant wedPrevWeek = Instant.parse("2000-02-02T10:00:00Z");
        Instant thuPrevWeek = Instant.parse("2000-02-03T10:00:00Z");
        Instant mondayNewWeek = Instant.parse("2000-02-07T00:00:00Z");

        var r1 = service.process(new LoadRequest("A", "C1", "$5000.00", monPrevWeek)).orElseThrow();
        var r2 = service.process(new LoadRequest("B", "C1", "$5000.00", tuePrevWeek)).orElseThrow();
        var r3 = service.process(new LoadRequest("C", "C1", "$5000.00", wedPrevWeek)).orElseThrow();
        var r4 = service.process(new LoadRequest("D", "C1", "$5000.00", thuPrevWeek)).orElseThrow();

        assertThat(r1.accepted()).isTrue();
        assertThat(r2.accepted()).isTrue();
        assertThat(r3.accepted()).isTrue();
        assertThat(r4.accepted()).isTrue();

        var r5 = service.process(new LoadRequest("E", "C1", "$1.00", mondayNewWeek)).orElseThrow();
        assertThat(r5.accepted()).isTrue();
    }
}
