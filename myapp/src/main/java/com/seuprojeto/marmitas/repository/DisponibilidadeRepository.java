package com.seuprojeto.marmitas.repository;

import com.seuprojeto.marmitas.entity.DiaSemana;
import com.seuprojeto.marmitas.entity.Disponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {
    List<Disponibilidade> findByDia(DiaSemana dia);
}
