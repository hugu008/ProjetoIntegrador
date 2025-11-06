package com.aula.marmitas.entity.pedido;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pedido_item")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PedidoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // dono
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_peditem_pedido"))
    private Pedido pedido;

    // referência ao item do cardápio (pode virar @ManyToOne depois)
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantidade = 1;

    // preço congelado no momento da criação
    @Column(name = "preco_unit", nullable = false)
    private BigDecimal precoUnit;

    @Column(name = "total_item", nullable = false)
    private BigDecimal totalItem;

    @PrePersist
    @PreUpdate
    void computeTotals() {
        if (quantidade == null || quantidade < 1) quantidade = 1;
        if (precoUnit == null) precoUnit = BigDecimal.ZERO;
        totalItem = precoUnit.multiply(BigDecimal.valueOf(quantidade));
    }
}
