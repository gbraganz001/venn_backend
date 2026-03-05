package com.venn.velocity.model;

import java.time.Instant;

public record LoadRequest(
        String id,
        String customer_id,
        String load_amount,
        Instant time) {
}