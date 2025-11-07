package com.aula.marmitas.entity.cardapio;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "tamanho_marmita")
public class tamanho_marmita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20)
    private String nome; // P, M, G

    private Double preco_base;
    private Integer max_misturas;
    private Integer max_acompanhamentos;
}

