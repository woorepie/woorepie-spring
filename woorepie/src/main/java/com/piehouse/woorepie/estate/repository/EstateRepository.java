package com.piehouse.woorepie.estate.repository;

import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstateRepository extends JpaRepository<Estate, Long> {
    
    Optional<Integer> findTokenAmountByEstateId(Long estateId);
    
    List<Estate> findByEstateStatus(EstateStatus estateStatus);

    List<Estate> findByEstateStatusIn(List<EstateStatus> estateStatuses);

    List<Estate> findByAgent_AgentId(Long agentId);


}
