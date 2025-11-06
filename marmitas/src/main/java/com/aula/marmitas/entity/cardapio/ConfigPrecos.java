package com.aula.marmitas.entity.cardapio;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "config_precos")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ConfigPrecos {

    @Id
    @Column(columnDefinition = "TINYINT")
    private Byte id; // sempre 1 no MVP

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Strategy strategy;

    @Builder.Default
    @Column(nullable = false)
    private Integer incluiMisturas = 1;

    @Column
    @Builder.Default
    private BigDecimal precoExtraMistura = BigDecimal.ZERO;

    @Column
    @Builder.Default
    private BigDecimal precoAcompanhamentoPadrao = BigDecimal.ZERO;
}

