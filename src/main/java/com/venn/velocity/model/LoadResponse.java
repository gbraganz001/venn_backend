package com.venn.velocity.model;

public record LoadResponse(
        String id,
        String customer_id,
        boolean accepted) {
}