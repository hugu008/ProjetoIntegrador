package com.aula.marmitas.entity.cardapio;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "item_cardapio_default")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ItemCardapioDefault {

    @Id
    @Column(length = 64)
    private String padraoKey;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoItem tipo;

    @Column(nullable = false)
    private BigDecimal preco;

    private Integer ordemExibicao;

    private String imagemUrl;

    @Builder.Default
    private Integer versao = 1;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant atualizadoEm;
}
