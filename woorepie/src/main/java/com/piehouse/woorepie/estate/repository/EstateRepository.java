package com.piehouse.woorepie.estate.repository;

import com.piehouse.woorepie.estate.entity.Estate;
import com.piehouse.woorepie.estate.entity.EstateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstateRepository extends JpaRepository<Estate, Long> {

    @Query("SELECT e.tokenAmount FROM Estate e WHERE e.estateId = :estateId")
    Optional<Long> findTokenAmountByEstateId(@Param("estateId") Long estateId);
    
    List<Estate> findByEstateStatus(EstateStatus estateStatus);

    List<Estate> findByEstateStatusIn(List<EstateStatus> estateStatuses);

    List<Estate> findByAgent_AgentId(Long agentId);


}
