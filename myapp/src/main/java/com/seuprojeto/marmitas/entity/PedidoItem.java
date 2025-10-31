package com.seuprojeto.marmitas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PedidoItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false)
    private Pedido pedido;

    @ManyToOne(optional=false)
    private ItemCardapio item;

    @Builder.Default
    private int quantidade = 1; // use 2 quando for "1 mistura" em porção dupla
}
