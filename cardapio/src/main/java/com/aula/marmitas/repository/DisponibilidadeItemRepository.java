// DisponibilidadeItemRepository.java
package com.aula.marmitas.repository;

import com.aula.marmitas.entity.cardapio.disponibilidade_item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.Optional;

public interface DisponibilidadeItemRepository extends JpaRepository<disponibilidade_item, Long> {
    @Query("select d from disponibilidade_item d where d.item.id_item_cardapio = :itemId and d.data = :data")
    Optional<disponibilidade_item> findFirstByItemIdAndData(Long itemId, Date data);
}
