package com.aula.marmitas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.aula.marmitas.entity.pedido.Pedido;
import com.aula.marmitas.entity.pedido.PedidoItem;
import com.aula.marmitas.entity.pedido.PedidoStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Transactional(readOnly = true)
public class PedidoRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Pedido> findById(Long id) {
        return Optional.ofNullable(em.find(Pedido.class, id));
    }

    public List<Pedido> findByCliente(Long clienteId) {
        return em.createQuery("""
            select p from Pedido p
            where p.clienteId = :cid
            order by p.criadoEm desc
        """, Pedido.class)
        .setParameter("cid", clienteId)
        .getResultList();
    }

    @Transactional
    public Pedido save(Pedido p) {
        if (p.getId() == null) { em.persist(p); return p; }
        return em.merge(p);
    }

    @Transactional
    public void updateStatus(Long id, PedidoStatus novo, String motivoCancelamento) {
        Pedido p = em.find(Pedido.class, id);
        if (p != null) {
            p.setStatus(novo);
            if (novo == PedidoStatus.CANCELADO) {
                p.setMotivoCancelamento(motivoCancelamento);
            }
        }
    }

    public List<PedidoItem> findItens(Long pedidoId) {
        return em.createQuery("""
            select i from PedidoItem i
            where i.pedido.id = :pid
        """, PedidoItem.class)
        .setParameter("pid", pedidoId)
        .getResultList();
    }
}
