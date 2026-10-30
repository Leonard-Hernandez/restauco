package com.lsoftware.restauco.infrastructure.config.db.mapper;

import com.lsoftware.restauco.entity.menu.model.Tag;
import com.lsoftware.restauco.infrastructure.config.db.schema.TagEntity;
import org.springframework.stereotype.Component;

@Component
public class TagMapper {

    public Tag toDomain(TagEntity e) {
        return new Tag(e.getId(), e.getName());
    }

    public TagEntity toEntity(Tag t) {
        return TagEntity.builder().id(t.id()).name(t.name()).build();
    }
}
