package com.venn.velocity.repo;

import com.venn.velocity.entity.LoadPerDay;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface LoadPerDayRepository extends JpaRepository<LoadPerDay, Long> {

    Optional<LoadPerDay> findByCustomerIdAndDayInUtc(String customerId, LocalDate dayInUtc);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select load from LoadPerDay load where load.customerId=:customerId and load.dayInUtc=:day")
    Optional<LoadPerDay> lockByCustomerAndDay(@Param("customerId") String customerId, @Param("day") LocalDate day);
}