package com.aula.marmitas.entity.cardapio;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "config_precos")
public class config_precos {

    @Id
    // pode ser Byte; se preferir, troque para Integer
    private Byte id; // sempre 1 no MVP

    @Enumerated(EnumType.STRING)
    private Strategy strategy;

    private Integer inclui_misturas;
    private Double preco_extra_mistura;
    private Double preco_acompanhamento_padrao;

    @PrePersist
    void prePersist() {
        if (inclui_misturas == null) inclui_misturas = 1;
        if (preco_extra_mistura == null) preco_extra_mistura = 0.0;
        if (preco_acompanhamento_padrao == null) preco_acompanhamento_padrao = 0.0;
    }
}

