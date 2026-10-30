package com.lsoftware.restauco.infrastructure.config.db.repository;

import com.lsoftware.restauco.infrastructure.config.db.schema.DiningTableEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaDiningTableRepository extends JpaRepository<DiningTableEntity, Long> {
    Optional<DiningTableEntity> findOneById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTableEntity t where t.id = :id")
    Optional<DiningTableEntity> lockById(@Param("id") Long id);

    Optional<DiningTableEntity> findByQrToken(UUID qrToken);
    List<DiningTableEntity> findAllByOrderByNumberAsc();
    boolean existsByNumber(int number);
    boolean existsByNumberAndIdNot(int number, Long id);
}
