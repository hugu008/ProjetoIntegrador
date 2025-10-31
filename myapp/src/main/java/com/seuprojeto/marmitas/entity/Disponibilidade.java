package com.seuprojeto.marmitas.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"item_id","dia"}))
public class Disponibilidade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false)
    private ItemCardapio item;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private DiaSemana dia;
}
