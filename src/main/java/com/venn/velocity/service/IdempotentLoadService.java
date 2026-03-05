package com.venn.velocity.service;

import com.venn.velocity.repo.LoadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IdempotentLoadService {

    private static final Logger log = LoggerFactory.getLogger(IdempotentLoadService.class);
    private final LoadRepository loadRepo;

    public IdempotentLoadService(LoadRepository loadRepo) {
        this.loadRepo = loadRepo;
    }

    public boolean isFirstTime(String customerId, String loadId) {
        boolean exists = loadRepo.existsByCustomerIdAndLoadId(customerId, loadId);
        if (exists) {
            log.info("Duplicate load rejected: customerId={}, loadId={}", customerId, loadId);
            return false;
        }
        return true;
    }
}