package com.seuprojeto.marmitas.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Cliente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private String nome;

    private String telefone;
    private String email;

    @Column(length=1000)
    private String endereco;

    private Double latitude;   // -23.31...
    private Double longitude;  // -51.16...

    @Builder.Default
    private LocalDateTime dataCadastro = LocalDateTime.now();
}
