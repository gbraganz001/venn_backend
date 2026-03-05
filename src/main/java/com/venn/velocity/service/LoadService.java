package com.venn.velocity.service;

import com.venn.velocity.entity.LoadPerDay;
import com.venn.velocity.entity.LoadPerWeek;
import com.venn.velocity.entity.LoadEntity;
import com.venn.velocity.model.LoadRequest;
import com.venn.velocity.model.LoadResponse;
import com.venn.velocity.repo.LoadPerDayRepository;
import com.venn.velocity.repo.LoadPerWeekRepository;
import com.venn.velocity.repo.LoadRepository;
import com.venn.velocity.util.LoadUtil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class LoadService {
    private static final Logger log = LoggerFactory.getLogger(LoadService.class);

    private static final long DAILY_LIMIT_CENTS = 5000L * 100;
    private static final long WEEKLY_LIMIT_CENTS = 20000L * 100;
    private static final int DAILY_COUNT_LIMIT = 3;

    private final LoadRepository loadRepo;
    private final LoadPerDayRepository dayRepo;
    private final LoadPerWeekRepository weekRepo;
    private final IdempotentLoadService idempotentLoadService;

    public LoadService(LoadRepository attemptRepo,
                       LoadPerDayRepository dayRepo,
                       LoadPerWeekRepository weekRepo,
                       IdempotentLoadService idempotentLoadService) {
        this.loadRepo = attemptRepo;
        this.dayRepo = dayRepo;
        this.weekRepo = weekRepo;
        this.idempotentLoadService = idempotentLoadService;
    }

    @Transactional
    public Optional<LoadResponse> process(LoadRequest req) {
        long amountCents = LoadUtil.parseCents(req.load_amount());
        LocalDate day = LoadUtil.utcDay(req.time());
        LocalDate weekStart = LoadUtil.utcWeekStart(req.time());

        if (!idempotentLoadService.isFirstTime(req.customer_id(), req.id())) {
            return Optional.empty();
        }

        LoadEntity attempt = new LoadEntity();
        attempt.setLoadId(req.id());
        attempt.setCustomerId(req.customer_id());
        attempt.setEventTime(req.time());
        attempt.setAmountCents(LoadUtil.parseCents(req.load_amount()));
        attempt.setAccepted(false);

        loadRepo.save(attempt);
        LoadPerDay dayAgg = dayRepo.lockByCustomerAndDay(req.customer_id(), day)
                .orElseGet(() -> {
                    LoadPerDay d = new LoadPerDay();
                    d.setCustomerId(req.customer_id());
                    d.setDayInUtc(day);
                    d.setAcceptedCount(0);
                    d.setAcceptedAmountCents(0);
                    return dayRepo.save(d);
                });

        LoadPerWeek weekAgg = weekRepo.lockByCustomerAndWeek(req.customer_id(), weekStart)
                .orElseGet(() -> {
                    LoadPerWeek customerWeekAggregate = new LoadPerWeek();
                    customerWeekAggregate.setCustomerId(req.customer_id());
                    customerWeekAggregate.setWeekStartDate(weekStart);
                    customerWeekAggregate.setAcceptedAmountCents(0);
                    return weekRepo.save(customerWeekAggregate);
                });

        boolean accepted = true;
        if (dayAgg.getAcceptedCount() + 1 > DAILY_COUNT_LIMIT) accepted = false;
        if (dayAgg.getAcceptedAmountCents() + amountCents > DAILY_LIMIT_CENTS) accepted = false;
        if (weekAgg.getAcceptedAmountCents() + amountCents > WEEKLY_LIMIT_CENTS) accepted = false;

        attempt.setAccepted(accepted);

        if (accepted) {
            dayAgg.setAcceptedCount(dayAgg.getAcceptedCount() + 1);
            dayAgg.setAcceptedAmountCents(dayAgg.getAcceptedAmountCents() + amountCents);
            weekAgg.setAcceptedAmountCents(weekAgg.getAcceptedAmountCents() + amountCents);
            dayRepo.save(dayAgg);
            weekRepo.save(weekAgg);
        }

        loadRepo.save(attempt);
        return Optional.of(new LoadResponse(req.id(), req.customer_id(), accepted));
    }

}