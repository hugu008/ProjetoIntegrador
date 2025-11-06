package com.aula.marmitas.entity.cardapio;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tamanho_marmita")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class TamanhoMarmita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String nome; // P, M, G

    @Column(nullable = false)
    private BigDecimal precoBase;

    @Column(nullable = false)
    private Integer maxMisturas;

    @Column(nullable = false)
    private Integer maxAcompanhamentos;
}

