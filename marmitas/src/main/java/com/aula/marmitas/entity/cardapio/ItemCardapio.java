package com.aula.marmitas.entity.cardapio;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "item_cardapio",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_item_cardapio_nome", columnNames = "nome")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ItemCardapio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoItem tipo;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    private Integer ordemExibicao;

    private String imagemUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPadrao = false;

    @Column(length = 64)
    private String padraoKey; // referencia lógica ao "default"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void prePersist() {
        if (preco == null) preco = BigDecimal.ZERO;
    }
}

