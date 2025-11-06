package com.aula.marmitas.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.aula.marmitas.entity.cardapio.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

@Repository
@Transactional(readOnly = true)
public class CardapioRepository {

    @PersistenceContext
    private EntityManager em;

    // ===== ItemCardapio =====

    public Optional<ItemCardapio> findItemById(Long id) {
        return Optional.ofNullable(em.find(ItemCardapio.class, id));
    }

    public List<ItemCardapio> findActiveItemsOrdered() {
        return em.createQuery("""
            select i from ItemCardapio i
            where i.ativo = true
            order by i.ordemExibicao asc nulls last, i.nome asc
        """, ItemCardapio.class).getResultList();
    }

    public List<ItemCardapio> findActiveItemsByTipoOrdered(TipoItem tipo) {
        return em.createQuery("""
            select i from ItemCardapio i
            where i.ativo = true and i.tipo = :tipo
            order by i.ordemExibicao asc nulls last, i.nome asc
        """, ItemCardapio.class).setParameter("tipo", tipo).getResultList();
    }

    public List<ItemCardapio> findItemsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return em.createQuery("""
            select i from ItemCardapio i where i.id in :ids
        """, ItemCardapio.class).setParameter("ids", ids).getResultList();
    }

    public boolean existsByNomeIgnoreCase(String nome) {
        Long c = em.createQuery("""
            select count(i) from ItemCardapio i
            where lower(i.nome) = lower(:n)
        """, Long.class).setParameter("n", nome).getSingleResult();
        return c > 0;
    }

    public boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id) {
        Long c = em.createQuery("""
            select count(i) from ItemCardapio i
            where lower(i.nome) = lower(:n) and i.id <> :id
        """, Long.class).setParameter("n", nome).setParameter("id", id).getSingleResult();
        return c > 0;
    }

    @Transactional
    public ItemCardapio saveItem(ItemCardapio i) {
        if (i.getId() == null) { em.persist(i); return i; }
        return em.merge(i);
    }

    public List<ItemCardapio> findPadroes(boolean isPadrao) {
        return em.createQuery("""
            select i from ItemCardapio i where i.isPadrao = :p
        """, ItemCardapio.class).setParameter("p", isPadrao).getResultList();
    }

    public Optional<ItemCardapio> findByPadraoKey(String key) {
        List<ItemCardapio> list = em.createQuery("""
            select i from ItemCardapio i where i.padraoKey = :k
        """, ItemCardapio.class).setParameter("k", key).getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    // ===== ItemCardapioDefault (somente leitura no app) =====

    public Optional<ItemCardapioDefault> findDefaultByKey(String key) {
        return Optional.ofNullable(em.find(ItemCardapioDefault.class, key));
    }

    // ===== Disponibilidade por data =====

    public Optional<DisponibilidadeItem> findDisponibilidadeData(Long itemId, LocalDate data) {
        List<DisponibilidadeItem> list = em.createQuery("""
            select d from DisponibilidadeItem d
            where d.item.id = :id and d.data = :data
        """, DisponibilidadeItem.class)
            .setParameter("id", itemId)
            .setParameter("data", data)
            .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Transactional
    public DisponibilidadeItem saveDisponibilidadeData(DisponibilidadeItem d) {
        if (d.getId() == null) { em.persist(d); return d; }
        return em.merge(d);
    }

    // ===== Disponibilidade por dia da semana =====

    public Optional<DisponibilidadeSemana> findDisponibilidadeSemana(Long itemId, int diaSemana) {
        List<DisponibilidadeSemana> list = em.createQuery("""
            select s from DisponibilidadeSemana s
            where s.item.id = :id and s.diaSemana = :dw
        """, DisponibilidadeSemana.class)
            .setParameter("id", itemId)
            .setParameter("dw", diaSemana)
            .getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Transactional
    public DisponibilidadeSemana saveDisponibilidadeSemana(DisponibilidadeSemana s) {
        if (s.getId() == null) { em.persist(s); return s; }
        return em.merge(s);
    }

    // ===== Configuração de preços / tamanhos =====

    public ConfigPrecos getConfigPrecosOrCreate() {
        ConfigPrecos c = em.find(ConfigPrecos.class, (byte)1);
        if (c == null) {
            c = ConfigPrecos.builder()
                    .id((byte)1).strategy(Strategy.PER_ITEM)
                    .incluiMisturas(1)
                    .precoExtraMistura(BigDecimal.ZERO)
                    .precoAcompanhamentoPadrao(BigDecimal.ZERO)
                    .build();
            em.persist(c);
        }
        return c;
    }

    @Transactional
    public void saveConfigPrecos(ConfigPrecos c) {
        if (c.getId() == null) c.setId((byte)1);
        if (em.find(ConfigPrecos.class, c.getId()) == null) em.persist(c);
        else em.merge(c);
    }

    public Optional<TamanhoMarmita> findTamanhoByNome(String nome) {
        TypedQuery<TamanhoMarmita> q = em.createQuery("""
            select t from TamanhoMarmita t where lower(t.nome) = lower(:n)
        """, TamanhoMarmita.class).setParameter("n", nome);
        List<TamanhoMarmita> list = q.getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
