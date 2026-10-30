package com.lsoftware.restauco.usecase.menu;

import com.lsoftware.restauco.entity.menu.exception.DuplicateNameException;
import com.lsoftware.restauco.entity.menu.exception.TagNotFoundException;
import com.lsoftware.restauco.entity.menu.gateway.TagGateway;
import com.lsoftware.restauco.entity.menu.model.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ManageTagUseCase {

    private final TagGateway gateway;

    @Transactional
    public Tag create(String name) {
        String clean = name.strip();
        if (gateway.existsByName(clean, null)) throw new DuplicateNameException();
        return gateway.save(new Tag(null, clean));
    }

    @Transactional
    public Tag update(Long id, String name) {
        gateway.findById(id).orElseThrow(TagNotFoundException::new);
        String clean = name.strip();
        if (gateway.existsByName(clean, id)) throw new DuplicateNameException();
        return gateway.save(new Tag(id, clean));
    }

    @Transactional
    public void delete(Long id) {
        gateway.findById(id).orElseThrow(TagNotFoundException::new);
        gateway.deleteById(id);
    }
}
