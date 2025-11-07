package com.aula.marmitas.entity.cardapio;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Entity
@Table(
  name = "item_cardapio",
  uniqueConstraints = @UniqueConstraint(name = "uq_item_cardapio_nome", columnNames = "nome")
)
@Data
@NoArgsConstructor
public class item_cardapio {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id_item_cardapio;

  private String nome;
  private String descricao;

  @Enumerated(EnumType.STRING) // mantém igual ao original
  private TipoItem tipo;

  private Double preco;        // simples; se quiser precisão, troque para BigDecimal

  // 0/1 simples, mas com default via @PrePersist
  private Integer ativo;       
  private Integer ordem_exibicao;
  private String imagem_url;
  private Integer is_padrao;

  // a versão original usava uma chave lógica String, não relação
  private String padrao_key;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(updatable = false)
  private Date criado_em;

  @Temporal(TemporalType.TIMESTAMP)
  private Date atualizado_em;

  @OneToOne
  @JoinColumn(name = "padrao_key", referencedColumnName = "chave") // ajuste o nome da PK/UK da tabela alvo
  private item_cardapio_default item_cardapio_default;

  @PrePersist
  void prePersist() {
    if (preco == null) preco = 0.0;
    if (ativo == null) ativo = 1;        // true
    if (is_padrao == null) is_padrao = 0; // false
    if (criado_em == null) criado_em = new Date();
    atualizado_em = criado_em;
  }

  @PreUpdate
  void preUpdate() {
    atualizado_em = new Date();
  }
}
