package com.seuprojeto.marmitas.repository;

import com.seuprojeto.marmitas.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
