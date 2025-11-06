package com.aula.marmitas.entity.pagamento;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "pagamento",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_pagto_pedido", columnNames = "pedido_id") // 1:1 no MVP
    },
    indexes = {
        @Index(name = "idx_pag_status", columnList = "status"),
        @Index(name = "idx_pag_metodo", columnList = "metodo"),
        @Index(name = "idx_pag_txid", columnList = "pix_txid")
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // vínculo com pedido (pode virar @ManyToOne quando a entity Pedido estiver pronta)
    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    // valores
    @Column(nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private StatusPagamento status = StatusPagamento.PENDENTE;

    // referência / observações
    @Column(length = 64)
    private String referencia; // NSU/ID externo (opcional)

    @Column(length = 255)
    private String observacao; // notas/motivo de recusa

    // específicos por método (MVP)
    @Column(name = "pix_txid", length = 100)
    private String pixTxid;

    @Lob
    @Column(name = "pix_copia_cola")
    private String pixCopiaCola;

    @Column(name = "cartao_presente", nullable = false)
    @Builder.Default
    private boolean cartaoPresente = false; // “levar maquininha”

    // timestamps do fluxo
    private Instant autorizadoEm;
    private Instant confirmadoEm;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void prePersist() {
        if (valor == null) valor = BigDecimal.ZERO;
        if (status == null) status = StatusPagamento.PENDENTE;
    }
}
