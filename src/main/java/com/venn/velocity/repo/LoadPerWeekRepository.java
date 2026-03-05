package com.venn.velocity.repo;

import com.venn.velocity.entity.LoadPerWeek;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface LoadPerWeekRepository extends JpaRepository<LoadPerWeek, Long> {
    Optional<LoadPerWeek> findByCustomerIdAndWeekStartDate(String customerId, LocalDate weekStartDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select load from LoadPerWeek load where load.customerId=:customerId and load.weekStartDate=:weekStart")
    Optional<LoadPerWeek> lockByCustomerAndWeek(@Param("customerId") String customerId, @Param("weekStart") LocalDate weekStart);
}
