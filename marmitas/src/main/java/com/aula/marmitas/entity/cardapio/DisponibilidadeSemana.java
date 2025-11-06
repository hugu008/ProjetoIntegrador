package com.aula.marmitas.entity.cardapio;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "disponibilidade_semana",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_item_dia", columnNames = {"item_id", "dia_semana"})
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class DisponibilidadeSemana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_disp_sem_item"))
    private ItemCardapio item;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana; // 1..7

    @Column(nullable = false)
    @Builder.Default
    private boolean disponivel = true;
}
