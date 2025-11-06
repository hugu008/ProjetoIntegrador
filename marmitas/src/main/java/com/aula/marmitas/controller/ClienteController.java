package com.aula.marmitas.controller;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.aula.marmitas.entity.clientes.Cliente;
import com.aula.marmitas.entity.clientes.EnderecoCliente;
import com.aula.marmitas.entity.clientes.StatusCliente;
import com.aula.marmitas.repository.ClienteRepository;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteRepository clienteRepo;

    // ======= CLIENTE =======

    @PostMapping
    @Transactional
    public ResponseEntity<ClienteResponse> criar(@RequestBody CriarClienteRequest req) {
        validarUnicidade(null, req.email(), req.telefone());

        Cliente c = Cliente.builder()
                .nome(req.nome())
                .email(v(req.email()))
                .telefone(v(req.telefone()))
                .dataNascimento(req.dataNascimento())
                .status(StatusCliente.ATIVO)
                .build();

        var salvo = clienteRepo.save(c);
        return ResponseEntity
                .created(URI.create("/api/clientes/" + salvo.getId()))
                .body(ClienteResponse.of(salvo));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obter(@PathVariable Long id) {
        return clienteRepo.findById(id)
                .map(c -> ResponseEntity.ok(ClienteResponse.of(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<ClienteMinResponse> listar(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("nome").ascending());
        Page<Cliente> res = StringUtils.hasText(q)
                ? clienteRepo.findByNomeContainingIgnoreCase(q.trim(), pageable)
                : clienteRepo.findAll(pageable);

        return res.map(ClienteMinResponse::of);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ClienteResponse> atualizar(@PathVariable Long id,
                                                     @RequestBody AtualizarClienteRequest req) {
        return clienteRepo.findById(id)
                .map(c -> {
                    validarUnicidade(id, req.email(), req.telefone());
                    if (StringUtils.hasText(req.nome())) c.setNome(req.nome().trim());
                    c.setEmail(v(req.email()));
                    c.setTelefone(v(req.telefone()));
                    c.setDataNascimento(req.dataNascimento());
                    if (req.status() != null) c.setStatus(req.status());
                    return ResponseEntity.ok(ClienteResponse.of(c));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<Void> alterarStatus(@PathVariable Long id, @RequestParam StatusCliente value) {
        var opt = clienteRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var c = opt.get();
        c.setStatus(value);
        return ResponseEntity.noContent().build();
    }


    // ======= ENDEREÇOS (usando só ClienteRepository) =======

    @GetMapping("/{id}/enderecos")
    public ResponseEntity<List<EnderecoResponse>> listarEnderecos(@PathVariable Long id) {
        return clienteRepo.findById(id)
            .map(c -> ResponseEntity.ok(c.getEnderecos().stream().map(EnderecoResponse::of).toList()))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/enderecos")
    @Transactional
    public ResponseEntity<EnderecoResponse> adicionarEndereco(@PathVariable Long id,
                                                              @RequestBody CriarEnderecoRequest req) {
        var cliente = clienteRepo.findById(id).orElse(null);
        if (cliente == null) return ResponseEntity.notFound().build();

        // Se marcar padrão, desmarca os demais; se não houver padrão ainda, primeiro vira padrão
        boolean marcarPadrao = Boolean.TRUE.equals(req.padrao()) ||
                cliente.getEnderecos().stream().noneMatch(EnderecoCliente::isPadrao);

        if (marcarPadrao) cliente.getEnderecos().forEach(e -> e.setPadrao(false));

        var novo = EnderecoCliente.builder()
                .cliente(cliente)
                .apelido(req.apelido())
                .logradouro(req.logradouro())
                .numero(req.numero())
                .complemento(req.complemento())
                .bairro(req.bairro())
                .cidade(req.cidade())
                .uf(req.uf())
                .cep(req.cep())
                .lat(req.lat())
                .lng(req.lng())
                .padrao(marcarPadrao)
                .build();

        cliente.getEnderecos().add(novo); // cascade + orphanRemoval cuidam do persist

        return ResponseEntity
                .created(URI.create("/api/clientes/%d/enderecos/%d".formatted(id, novo.getId())))
                .body(EnderecoResponse.of(novo));
    }

    @PutMapping("/{id}/enderecos/{enderecoId}")
    @Transactional
    public ResponseEntity<EnderecoResponse> atualizarEndereco(@PathVariable Long id,
                                                              @PathVariable Long enderecoId,
                                                              @RequestBody AtualizarEnderecoRequest req) {
        var cliente = clienteRepo.findById(id).orElse(null);
        if (cliente == null) return ResponseEntity.notFound().build();

        var end = cliente.getEnderecos().stream()
                .filter(e -> enderecoId.equals(e.getId()))
                .findFirst().orElse(null);
        if (end == null) return ResponseEntity.notFound().build();

        if (StringUtils.hasText(req.apelido())) end.setApelido(req.apelido());
        if (StringUtils.hasText(req.logradouro())) end.setLogradouro(req.logradouro());
        end.setNumero(req.numero());
        end.setComplemento(req.complemento());
        end.setBairro(req.bairro());
        end.setCidade(req.cidade());
        end.setUf(req.uf());
        end.setCep(req.cep());
        end.setLat(req.lat());
        end.setLng(req.lng());

        if (req.padrao() != null) {
            if (req.padrao()) {
                cliente.getEnderecos().forEach(e -> e.setPadrao(false));
                end.setPadrao(true);
            } else {
                // impedir que fique sem nenhum padrão
                long qtPadrao = cliente.getEnderecos().stream().filter(EnderecoCliente::isPadrao).count();
                if (!(end.isPadrao() && qtPadrao == 1)) end.setPadrao(false);
            }
        }

        return ResponseEntity.ok(EnderecoResponse.of(end));
    }

    @DeleteMapping("/{id}/enderecos/{enderecoId}")
    @Transactional
    public ResponseEntity<Void> removerEndereco(@PathVariable Long id, @PathVariable Long enderecoId) {
        var cliente = clienteRepo.findById(id).orElse(null);
        if (cliente == null) return ResponseEntity.notFound().build();

        var it = cliente.getEnderecos().iterator();
        boolean eraPadrao = false;
        boolean removido = false;
        while (it.hasNext()) {
            var e = it.next();
            if (enderecoId.equals(e.getId())) {
                eraPadrao = e.isPadrao();
                it.remove(); // orphanRemoval = true -> deleta
                removido = true;
                break;
            }
        }
        if (!removido) return ResponseEntity.notFound().build();

        if (eraPadrao && !cliente.getEnderecos().isEmpty()) {
            cliente.getEnderecos().get(0).setPadrao(true);
        }
        return ResponseEntity.noContent().build();
    }

    // ======= Helpers =======

    private void validarUnicidade(Long id, String email, String telefone) {
        if (StringUtils.hasText(email)) {
            if (id == null && clienteRepo.existsByEmailIgnoreCase(email.trim()))
                throw new IllegalArgumentException("Email já cadastrado");
            if (id != null && clienteRepo.existsByEmailIgnoreCaseAndIdNot(email.trim(), id))
                throw new IllegalArgumentException("Email já cadastrado em outro cliente");
        }
        if (StringUtils.hasText(telefone)) {
            if (id == null && clienteRepo.existsByTelefone(telefone.trim()))
                throw new IllegalArgumentException("Telefone já cadastrado");
            if (id != null && clienteRepo.existsByTelefoneAndIdNot(telefone.trim(), id))
                throw new IllegalArgumentException("Telefone já cadastrado em outro cliente");
        }
    }

    private String v(String s) { return StringUtils.hasText(s) ? s.trim() : null; }

    // ======= DTOs =======

    public record ClienteMinResponse(Long id, String nome, String email, String telefone, StatusCliente status) {
        static ClienteMinResponse of(Cliente c) {
            return new ClienteMinResponse(c.getId(), c.getNome(), c.getEmail(), c.getTelefone(), c.getStatus());
        }
    }

    public record ClienteResponse(Long id, String nome, String email, String telefone,
                                  LocalDate dataNascimento, StatusCliente status,
                                  List<EnderecoResponse> enderecos) {
        static ClienteResponse of(Cliente c) {
            return new ClienteResponse(
                    c.getId(), c.getNome(), c.getEmail(), c.getTelefone(),
                    c.getDataNascimento(), c.getStatus(),
                    c.getEnderecos().stream().map(EnderecoResponse::of).toList()
            );
        }
    }

    public record EnderecoResponse(Long id, String apelido, String logradouro, String numero,
                                   String complemento, String bairro, String cidade, String uf,
                                   String cep, Double lat, Double lng, boolean padrao) {
        static EnderecoResponse of(EnderecoCliente e) {
            return new EnderecoResponse(
                    e.getId(), e.getApelido(), e.getLogradouro(), e.getNumero(),
                    e.getComplemento(), e.getBairro(), e.getCidade(), e.getUf(),
                    e.getCep(), e.getLat(), e.getLng(), e.isPadrao()
            );
        }
    }

    public record CriarClienteRequest(
            @NotBlank String nome,
            @Email String email,
            @Pattern(regexp = "^[0-9()+\\-\\s.]{8,20}$", message = "telefone inválido") String telefone,
            LocalDate dataNascimento
    ) {}

    public record AtualizarClienteRequest(
            String nome,
            @Email String email,
            @Pattern(regexp = "^[0-9()+\\-\\s.]{8,20}$", message = "telefone inválido") String telefone,
            LocalDate dataNascimento,
            StatusCliente status
    ) {}

    public record CriarEnderecoRequest(
            @NotBlank String apelido,
            @NotBlank String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf,
            String cep,
            Double lat,
            Double lng,
            Boolean padrao
    ) {}

    public record AtualizarEnderecoRequest(
            String apelido,
            String logradouro,
            String numero,
            String complemento,
            String bairro,
            String cidade,
            String uf,
            String cep,
            Double lat,
            Double lng,
            Boolean padrao
    ) {}
}