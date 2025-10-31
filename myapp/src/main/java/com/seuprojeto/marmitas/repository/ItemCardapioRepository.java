package com.seuprojeto.marmitas.repository;

import com.seuprojeto.marmitas.entity.ItemCardapio;
import com.seuprojeto.marmitas.entity.SubstituiBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCardapioRepository extends JpaRepository<ItemCardapio, Long> {
    List<ItemCardapio> findBySempreDisponivelTrueAndAtivoTrue();
    List<ItemCardapio> findBySubstituiAndAtivoTrue(SubstituiBase substitui);
}
