package com.aula.marmitas.entity.cardapio;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(
    name = "disponibilidade_semana",
    uniqueConstraints = @UniqueConstraint(name = "uq_item_dia", columnNames = {"item_id", "dia_semana"})
)
public class disponibilidade_semana {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK simples para item_cardapio
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private item_cardapio item;

    @Column(name = "dia_semana", nullable = false)
    private Integer dia_semana; // 1..7

    // 0/1 simples
    private Integer disponivel;

    @PrePersist
    void prePersist() {
        if (disponivel == null) disponivel = 1;
    }
}