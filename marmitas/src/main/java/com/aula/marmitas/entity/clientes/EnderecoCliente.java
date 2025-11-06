package com.aula.marmitas.entity.clientes;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "endereco_cliente")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class EnderecoCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_end_cliente"))
    private Cliente cliente;

    @Column(nullable = false, length = 30)
    private String apelido;

    @Column(nullable = false, length = 120)
    private String logradouro;

    @Column(length = 20)  private String numero;
    @Column(length = 60)  private String complemento;
    @Column(length = 60)  private String bairro;
    @Column(length = 60)  private String cidade;
    @Column(length = 2)   private String uf;
    @Column(length = 9)   private String cep;

    @Column private Double lat;
    @Column private Double lng;

    @Column(nullable = false)
    @Builder.Default
    private boolean padrao = false; 
}

