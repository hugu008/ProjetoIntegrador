// DisponibilidadeSemanaRepository.java
package com.aula.marmitas.repository;

import com.aula.marmitas.entity.cardapio.disponibilidade_semana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DisponibilidadeSemanaRepository extends JpaRepository<disponibilidade_semana, Long> {
    @Query("select s from disponibilidade_semana s where s.item.id_item_cardapio = :itemId and s.dia_semana = :dia")
    Optional<disponibilidade_semana> findFirstByItemIdAndDiaSemana(Long itemId, Integer dia);
}
