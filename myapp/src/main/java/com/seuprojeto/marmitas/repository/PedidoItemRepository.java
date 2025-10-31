package com.seuprojeto.marmitas.repository;

import com.seuprojeto.marmitas.entity.PedidoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {
}
