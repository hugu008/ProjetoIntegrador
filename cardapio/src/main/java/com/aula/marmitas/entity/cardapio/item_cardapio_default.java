package com.aula.marmitas.entity.cardapio;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Entity
@Table(name = "item_cardapio_default")
public class item_cardapio_default {

    @Id
    @Column(name = "padrao_key", length = 64)
    private String padrao_key;

    private String nome;
    private String descricao;

    @Enumerated(EnumType.STRING)
    private TipoItem tipo;

    private Double preco;
    private Integer ordem_exibicao;
    private String imagem_url;

    private Integer versao;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "atualizado_em")
    private Date atualizado_em;

    @PrePersist
    void prePersist() {
        if (versao == null) versao = 1;
        atualizado_em = new Date();
    }

    @PreUpdate
    void preUpdate() {
        atualizado_em = new Date();
    }
}