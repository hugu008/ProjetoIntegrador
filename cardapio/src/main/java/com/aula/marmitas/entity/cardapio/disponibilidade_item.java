package com.aula.marmitas.entity.cardapio;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(
    name = "disponibilidade_item",
    uniqueConstraints = @UniqueConstraint(name = "uq_item_data", columnNames = {"item_id", "data"})
)
public class disponibilidade_item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK simples para item_cardapio
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private item_cardapio item;

    @Temporal(TemporalType.DATE)
    private Date data;

    private Integer disponivel;        // 0/1
    private Integer quantidade_max;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "criado_em", updatable = false)
    private Date criado_em;

    @PrePersist
    void prePersist() {
        if (disponivel == null) disponivel = 1;
        if (criado_em == null) criado_em = new Date();
    }
}
