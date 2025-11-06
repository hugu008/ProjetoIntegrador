package com.aula.marmitas.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.aula.marmitas.entity.pagamento.Pagamento;
import com.aula.marmitas.entity.pagamento.PagamentoEvento;
import com.aula.marmitas.entity.pagamento.StatusPagamento;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
@Transactional(readOnly = true)
public class PagamentoRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Pagamento> findById(Long id) {
        return Optional.ofNullable(em.find(Pagamento.class, id));
    }

    public Optional<Pagamento> findByPedidoId(Long pedidoId) {
        List<Pagamento> list = em.createQuery("""
            select p from Pagamento p where p.pedidoId = :pid
        """, Pagamento.class).setParameter("pid", pedidoId).getResultList();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Transactional
    public Pagamento save(Pagamento p) {
        if (p.getId() == null) { em.persist(p); return p; }
        return em.merge(p);
    }

    @Transactional
    public void updateStatus(Long id, StatusPagamento status) {
        Pagamento p = em.find(Pagamento.class, id);
        if (p != null) {
            p.setStatus(status);
        }
    }

    // ===== Eventos (opcional) =====
    @Transactional
    public void saveEvento(PagamentoEvento e) {
        if (e.getId() == null) em.persist(e);
        else em.merge(e);
    }
}
