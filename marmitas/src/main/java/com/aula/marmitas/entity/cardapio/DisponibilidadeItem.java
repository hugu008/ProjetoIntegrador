package com.aula.marmitas.entity.cardapio;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "disponibilidade_item",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_item_data", columnNames = {"item_id", "data"})
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DisponibilidadeItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_disp_item"))
    private ItemCardapio item;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    @Builder.Default
    private boolean disponivel = true;

    private Integer quantidadeMax;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;
}
