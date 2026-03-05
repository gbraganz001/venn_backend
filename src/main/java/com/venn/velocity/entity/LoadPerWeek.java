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
@Table(name = "load_per_week",
        uniqueConstraints = @UniqueConstraint(name = "unique_week", columnNames = {"customer_id", "weekStartDate"}))
public class LoadPerWeek {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "accepted_amount_cents", nullable = false)
    private long acceptedAmountCents;

}
