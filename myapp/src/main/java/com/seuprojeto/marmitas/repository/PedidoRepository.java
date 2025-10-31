package com.seuprojeto.marmitas.repository;

import com.seuprojeto.marmitas.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @Query("""
      select distinct p
      from Pedido p
      left join fetch p.itens pi
      left join fetch pi.item it
      left join fetch it.categoria c
      where p.id = :id
    """)
    Optional<Pedido> findByIdWithItensAndCategoria(@Param("id") Long id);
}
