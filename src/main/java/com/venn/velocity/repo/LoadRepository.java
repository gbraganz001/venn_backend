package com.venn.velocity.repo;

import com.venn.velocity.entity.LoadEntity;
import org.springframework.data.jpa.repository.*;

public interface LoadRepository extends JpaRepository<LoadEntity, Long> {
    boolean existsByCustomerIdAndLoadId(String customerId, String loadId);
}