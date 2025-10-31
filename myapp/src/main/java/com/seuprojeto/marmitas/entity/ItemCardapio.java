package com.seuprojeto.marmitas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ItemCardapio {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String nome;

    @Column(length=1000)
    private String descricao;

    @ManyToOne(optional=false)
    private Categoria categoria;

    @Builder.Default
    private boolean sempreDisponivel = false; // arroz, feijão, ovo, bife, tilápia, salada

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubstituiBase substitui = SubstituiBase.NENHUM; // ARROZ | FEIJAO | NENHUM

    @Builder.Default
    private boolean ativo = true; // funcionário pausa/reativa

    private String imagemUrl; // caminho/URL da foto
}
