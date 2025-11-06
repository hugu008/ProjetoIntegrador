package com.aula.marmitas.entity.pagamento;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "pagamento_evento",
    indexes = @Index(name = "idx_pgev_pag", columnList = "pagamento_id")
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PagamentoEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // relacionamento simples por enquanto (trocar para @ManyToOne se quiser depois)
    @Column(name = "pagamento_id", nullable = false)
    private Long pagamentoId;

    @Column(nullable = false, length = 40)
    private String tipo; // STATUS_CHANGE | WEBHOOK | MANUAL_CONFIRM ...

    @Enumerated(EnumType.STRING)
    @Column(name = "de_status", length = 15)
    private StatusPagamento deStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "para_status", length = 15)
    private StatusPagamento paraStatus;

    @Lob
    private String payload; // JSON do webhook / detalhes

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;
}
