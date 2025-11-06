package com.aula.marmitas.entity.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "pedido")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // vínculo com cliente (pode virar @ManyToOne depois)
    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    // usado quando strategy != PER_ITEM (ex.: P/M/G)
    @Column(length = 20)
    private String tamanho;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entrega", nullable = false, length = 20)
    @Builder.Default
    private TipoEntrega tipoEntrega = TipoEntrega.RETIRADA;

    // snapshot de endereço (histórico do pedido)
    @Column(name = "endereco_logradouro", length = 120)  private String enderecoLogradouro;
    @Column(name = "endereco_numero", length = 20)       private String enderecoNumero;
    @Column(name = "endereco_complemento", length = 60)  private String enderecoComplemento;
    @Column(name = "endereco_bairro", length = 60)       private String enderecoBairro;
    @Column(name = "endereco_cidade", length = 60)       private String enderecoCidade;
    @Column(name = "endereco_uf", length = 2)            private String enderecoUf;
    @Column(name = "endereco_cep", length = 9)           private String enderecoCep;
    @Column                                              private Double lat;
    @Column                                              private Double lng;

    @Column(length = 255) private String observacaoCliente;
    @Column(length = 255) private String motivoCancelamento;

    @Column(name = "levar_maquininha", nullable = false)
    @Builder.Default
    private boolean levarMaquininha = false;

    @Column(name = "troco_para")
    private BigDecimal trocoPara;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO; // calculado na criação

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PedidoStatus status = PedidoStatus.CRIADO;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PedidoItem> itens = new ArrayList<>();

    @Version
    @Builder.Default
    private Integer version = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant atualizadoEm;

    public void addItem(PedidoItem item) {
        item.setPedido(this);
        this.itens.add(item);
    }

    @PrePersist
    void prePersist() {
        if (total == null) total = BigDecimal.ZERO;
        if (status == null) status = PedidoStatus.CRIADO;
        if (tipoEntrega == null) tipoEntrega = TipoEntrega.RETIRADA;
        if (itens == null) itens = new ArrayList<>();
    }
}
