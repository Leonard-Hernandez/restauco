package com.lsoftware.restauco.entity.menu.gateway;

import com.lsoftware.restauco.entity.menu.model.Tag;

import java.util.List;
import java.util.Optional;

public interface TagGateway {
    Tag save(Tag tag);
    Optional<Tag> findById(Long id);
    List<Tag> findAll();
    boolean existsByName(String name, Long exceptId);
    /** Also removes it from every dish. */
    void deleteById(Long id);
}
