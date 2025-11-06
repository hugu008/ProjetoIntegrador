package com.aula.marmitas.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aula.marmitas.entity.clientes.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
    boolean existsByTelefone(String telefone);

    // Para edição: checar unicidade desconsiderando o próprio ID
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByTelefoneAndIdNot(String telefone, Long id);

    Page<Cliente> findByNomeContainingIgnoreCase(String q, Pageable pageable);
}
