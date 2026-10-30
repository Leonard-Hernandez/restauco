package com.lsoftware.restauco.infrastructure.config.db.repository;

import com.lsoftware.restauco.infrastructure.config.db.schema.DishEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface JpaDishRepository extends JpaRepository<DishEntity, Long> {
    Optional<DishEntity> findOneById(Long id);
    List<DishEntity> findByIdIn(Collection<Long> ids);
    List<DishEntity> findAllByOrderByCategoryIdAscPositionAscIdAsc();
    boolean existsByCategoryId(Long categoryId);
}
