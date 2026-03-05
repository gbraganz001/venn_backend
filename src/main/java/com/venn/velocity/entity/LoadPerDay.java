package com.venn.velocity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "load_per_day",
        uniqueConstraints = @UniqueConstraint(name = "unique_day", columnNames = {"customer_id", "day_utc"}))
public class LoadPerDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "day_utc", nullable = false)
    private LocalDate dayInUtc;

    @Column(name = "accepted_count", nullable = false)
    private int acceptedCount;

    @Column(name = "accepted_amount_cents", nullable = false)
    private long acceptedAmountCents;
}
