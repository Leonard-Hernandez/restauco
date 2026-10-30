package com.lsoftware.restauco.infrastructure.config.db.repository;

import com.lsoftware.restauco.infrastructure.config.db.schema.IngredientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JpaIngredientRepository extends JpaRepository<IngredientEntity, Long> {
    Optional<IngredientEntity> findOneById(Long id);
    List<IngredientEntity> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
