// TamanhoMarmitaRepository.java
package com.aula.marmitas.repository;

import com.aula.marmitas.entity.cardapio.tamanho_marmita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TamanhoMarmitaRepository extends JpaRepository<tamanho_marmita, Long> {
    @Query("select t from tamanho_marmita t where lower(t.nome) = lower(:nome)")
    Optional<tamanho_marmita> findFirstByNomeIgnoreCase(String nome);
}
