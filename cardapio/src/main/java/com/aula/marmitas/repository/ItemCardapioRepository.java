package com.aula.marmitas.repository;

import com.aula.marmitas.entity.cardapio.TipoItem;
import com.aula.marmitas.entity.cardapio.item_cardapio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemCardapioRepository extends JpaRepository<item_cardapio, Long> {

    @Query("""
           select i from item_cardapio i
           where i.ativo = 1
           order by i.ordem_exibicao asc nulls last, i.nome asc
           """)
    List<item_cardapio> findActiveItemsOrdered();

    @Query("""
           select i from item_cardapio i
           where i.ativo = 1 and i.tipo = :tipo
           order by i.ordem_exibicao asc nulls last, i.nome asc
           """)
    List<item_cardapio> findActiveItemsByTipoOrdered(TipoItem tipo);

    @Query("select i from item_cardapio i where i.id_item_cardapio in :ids")
    List<item_cardapio> findByIds(Collection<Long> ids);

    @Query("select count(i) from item_cardapio i where lower(i.nome) = lower(:nome)")
    long countByNomeIgnoreCase(String nome);

    @Query("select count(i) from item_cardapio i where lower(i.nome) = lower(:nome) and i.id_item_cardapio <> :id")
    long countByNomeIgnoreCaseAndIdNot(String nome, Long id);

    @Query("select i from item_cardapio i where i.is_padrao = :p")
    List<item_cardapio> findByIsPadrao(int p); // 1 ou 0

    @Query("select i from item_cardapio i where i.padrao_key = :key")
    Optional<item_cardapio> findFirstByPadraoKey(String key);
}