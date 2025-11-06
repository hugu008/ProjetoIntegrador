package com.aula.marmitas.entity.clientes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "cliente",
       uniqueConstraints = {
           @UniqueConstraint(name = "uq_cliente_email", columnNames = "email"),
           @UniqueConstraint(name = "uq_cliente_telefone", columnNames = "telefone")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 120)
    private String email;

    @Column(length = 20)
    private String telefone;

    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private StatusCliente status = StatusCliente.ATIVO; // <-- agora funciona com Builder

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant atualizadoEm;

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EnderecoCliente> enderecos = new ArrayList<>();

    // Cinto e suspensório: garante default mesmo se alguém burlar o Builder
    @PrePersist
    void prePersist() {
        if (status == null) status = StatusCliente.ATIVO;
        if (enderecos == null) enderecos = new ArrayList<>();
    }
}
