package com.venn.velocity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "load",
        uniqueConstraints = @UniqueConstraint(name="unique_customer_load", columnNames={"customer_id","load_id"}))
public class LoadEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="load_id", nullable=false)
    private String loadId;

    @Column(name="customer_id", nullable=false)
    private String customerId;

    @Column(name="event_time", nullable=false)
    private Instant eventTime;

    @Column(name="amount_cents", nullable=false)
    private long amountCents;

    @Column(name="accepted", nullable=false)
    private boolean accepted;
}
