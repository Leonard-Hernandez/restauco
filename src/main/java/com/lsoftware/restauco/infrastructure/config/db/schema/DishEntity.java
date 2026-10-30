package com.lsoftware.restauco.infrastructure.config.db.schema;

import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "dish")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DishEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private long price;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private int position;

    // Small collections, always needed with the dish; SUBSELECT loads them for a whole list at once
    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "dish_ingredient", joinColumns = @JoinColumn(name = "dish_id"))
    private Set<DishIngredientEmbeddable> ingredients = new HashSet<>();

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @CollectionTable(name = "dish_tag", joinColumns = @JoinColumn(name = "dish_id"))
    @Column(name = "tag_id", nullable = false)
    private Set<Long> tagIds = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<ModifierGroup> modifiers;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }
}
