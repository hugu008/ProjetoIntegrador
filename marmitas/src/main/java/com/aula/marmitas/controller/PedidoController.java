package com.aula.marmitas.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.aula.marmitas.entity.cardapio.*;
import com.aula.marmitas.repository.CardapioRepository;
import com.aula.marmitas.entity.clientes.Cliente;
import com.aula.marmitas.entity.clientes.StatusCliente;
import com.aula.marmitas.repository.ClienteRepository;
import com.aula.marmitas.entity.pedido.*;
import com.aula.marmitas.repository.PedidoRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoRepository pedidoRepo;
    private final ClienteRepository clienteRepo;
    private final CardapioRepository cardapioRepo;

    // ===================== Criar Pedido =====================

    @PostMapping
    @Transactional
    public ResponseEntity<PedidoResponse> criar(@RequestBody @Valid CriarPedidoRequest req) {
        // 1) Cliente válido e ATIVO
        Cliente cli = clienteRepo.findById(req.clienteId()).orElse(null);
        if (cli == null || cli.getStatus() != StatusCliente.ATIVO) return ResponseEntity.badRequest().build();

        // 2) Configuração de preço e tamanho (se preciso)
        ConfigPrecos cfg = cardapioRepo.getConfigPrecosOrCreate();
        TamanhoMarmita tamanho = null;
        if (cfg.getStrategy() != Strategy.PER_ITEM) {
            if (!StringUtils.hasText(req.tamanho())) return ResponseEntity.badRequest().build();
            tamanho = cardapioRepo.findTamanhoByNome(req.tamanho().trim()).orElse(null);
            if (tamanho == null) return ResponseEntity.badRequest().build();
        }

        // 3) Validar itens (ativos e disponíveis na data)
        LocalDate data = Optional.ofNullable(req.data()).orElse(LocalDate.now());
        Map<Long, Integer> qtdPorItem = new HashMap<>();
        for (var it : req.itens()) {
            if (it.quantidade() <= 0) continue;
            qtdPorItem.merge(it.itemId(), it.quantidade(), Integer::sum);
        }
        if (qtdPorItem.isEmpty()) return ResponseEntity.badRequest().build();

        var itens = cardapioRepo.findItemsByIds(qtdPorItem.keySet());
        if (itens.size() != qtdPorItem.size()) return ResponseEntity.badRequest().build();

        for (var i : itens) {
            if (!i.isAtivo()) return ResponseEntity.badRequest().build();
            if (!disponivelNoDia(i, data)) return ResponseEntity.badRequest().build();
        }

        // 4) Regras de montagem (contagens)
        int qtdBases = countByTipo(itens, qtdPorItem, TipoItem.BASE);
        int qtdMisturas = countByTipo(itens, qtdPorItem, TipoItem.MISTURA);
        int qtdAcomp = countByTipo(itens, qtdPorItem, TipoItem.ACOMPANHAMENTO);

        // limites simples (ajuste conforme sua regra)
        int maxBases = 1;
        if (qtdBases > maxBases) return ResponseEntity.badRequest().build();
        if (cfg.getStrategy() == Strategy.FIXED_SIZE && qtdMisturas > tamanho.getMaxMisturas())
            return ResponseEntity.badRequest().build();

        // 5) Calcular total conforme estratégia
        BigDecimal total;
        switch (cfg.getStrategy()) {
            case PER_ITEM -> {
                total = BigDecimal.ZERO;
                for (var i : itens) {
                    BigDecimal parc = i.getPreco().multiply(BigDecimal.valueOf(qtdPorItem.get(i.getId())));
                    total = total.add(parc);
                }
            }
            case FIXED_SIZE -> {
                total = tamanho.getPrecoBase();
                int extraMist = Math.max(0, qtdMisturas - cfg.getIncluiMisturas());
                total = total.add(cfg.getPrecoExtraMistura().multiply(BigDecimal.valueOf(extraMist)));
                if (cfg.getPrecoAcompanhamentoPadrao() != null) {
                    total = total.add(cfg.getPrecoAcompanhamentoPadrao().multiply(BigDecimal.valueOf(qtdAcomp)));
                }
            }
            case BASE_PLUS_ADDONS -> {
                total = tamanho.getPrecoBase();               // inclui 1 base + 1 mistura conceitualmente
                int extraMist = Math.max(0, qtdMisturas - 1);
                total = total.add(cfg.getPrecoExtraMistura().multiply(BigDecimal.valueOf(extraMist)));
                if (cfg.getPrecoAcompanhamentoPadrao() != null) {
                    total = total.add(cfg.getPrecoAcompanhamentoPadrao().multiply(BigDecimal.valueOf(qtdAcomp)));
                }
            }
            default -> throw new IllegalStateException("Estratégia inválida");
        }

        // 6) Criar entidade Pedido + snapshot de endereço
        Pedido p = Pedido.builder()
                .clienteId(req.clienteId())
                .tamanho(cfg.getStrategy() == Strategy.PER_ITEM ? null : tamanho.getNome())
                .tipoEntrega(Optional.ofNullable(req.tipoEntrega()).orElse(TipoEntrega.RETIRADA))
                .enderecoLogradouro(req.enderecoLogradouro())
                .enderecoNumero(req.enderecoNumero())
                .enderecoComplemento(req.enderecoComplemento())
                .enderecoBairro(req.enderecoBairro())
                .enderecoCidade(req.enderecoCidade())
                .enderecoUf(req.enderecoUf())
                .enderecoCep(req.enderecoCep())
                .lat(req.lat())
                .lng(req.lng())
                .observacaoCliente(req.observacaoCliente())
                .levarMaquininha(Boolean.TRUE.equals(req.levarMaquininha()))
                .trocoPara(req.trocoPara())
                .total(total)
                .status(PedidoStatus.CRIADO)
                .build();

        // 7) Itens com preço congelado
        for (var i : itens) {
            int q = qtdPorItem.get(i.getId());
            p.addItem(PedidoItem.builder()
                    .itemId(i.getId())
                    .quantidade(q)
                    .precoUnit(i.getPreco())
                    .totalItem(i.getPreco().multiply(BigDecimal.valueOf(q)))
                    .build());
        }

        var salvo = pedidoRepo.save(p);
        return ResponseEntity.ok(PedidoResponse.of(salvo));
    }

    // ===================== Consultas =====================

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> obter(@PathVariable Long id) {
        var p = pedidoRepo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(PedidoResponse.of(p));
    }

    @GetMapping
    public List<PedidoMinResponse> listarPorCliente(@RequestParam Long clienteId) {
        return pedidoRepo.findByCliente(clienteId).stream()
                .map(PedidoMinResponse::of)
                .toList();
    }

    // ===================== Status =====================

    @PatchMapping("/{id}/status")
    @Transactional
    public ResponseEntity<Void> alterarStatus(@PathVariable Long id,
                                              @RequestParam PedidoStatus value,
                                              @RequestParam(required = false) String motivoCancelamento) {
        var opt = pedidoRepo.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        var p = opt.get();

        if (!podeTransitar(p.getStatus(), value)) return ResponseEntity.badRequest().build();
        if (value == PedidoStatus.CANCELADO && !StringUtils.hasText(motivoCancelamento))
            return ResponseEntity.badRequest().build();

        p.setStatus(value);
        if (value == PedidoStatus.CANCELADO) p.setMotivoCancelamento(motivoCancelamento);
        return ResponseEntity.noContent().build();
    }

    // ===================== Helpers =====================

    private boolean disponivelNoDia(ItemCardapio i, LocalDate data) {
        var ovr = cardapioRepo.findDisponibilidadeData(i.getId(), data);
        if (ovr.isPresent()) return ovr.get().isDisponivel();
        int dow = data.getDayOfWeek().getValue();
        return cardapioRepo.findDisponibilidadeSemana(i.getId(), dow)
                .map(DisponibilidadeSemana::isDisponivel)
                .orElse(true);
    }

    private int countByTipo(List<ItemCardapio> itens, Map<Long,Integer> qtd, TipoItem tipo) {
        return itens.stream().filter(i -> i.getTipo() == tipo)
                .mapToInt(i -> qtd.get(i.getId())).sum();
    }

    private boolean podeTransitar(PedidoStatus de, PedidoStatus para) {
        return switch (de) {
            case CRIADO   -> (para == PedidoStatus.PAGO || para == PedidoStatus.CANCELADO);
            case PAGO     -> (para == PedidoStatus.PREPARO || para == PedidoStatus.CANCELADO);
            case PREPARO  -> (para == PedidoStatus.ENTREGUE || para == PedidoStatus.CANCELADO);
            case ENTREGUE, CANCELADO -> false;
        };
    }

    // ===================== DTOs =====================

    public record CriarPedidoRequest(
            @NotNull Long clienteId,
            String tamanho,               // obrigatório se strategy != PER_ITEM
            LocalDate data,               // se null, usa hoje
            TipoEntrega tipoEntrega,      // RETIRADA|ENTREGA
            // snapshot de endereço (para ENTREGA)
            String enderecoLogradouro, String enderecoNumero, String enderecoComplemento,
            String enderecoBairro, String enderecoCidade, String enderecoUf, String enderecoCep,
            Double lat, Double lng,
            String observacaoCliente,
            Boolean levarMaquininha,
            BigDecimal trocoPara,
            List<ItemQtd> itens
    ) {
        public record ItemQtd(@NotNull Long itemId, @Min(1) int quantidade) {}
    }

    public record PedidoMinResponse(Long id, Long clienteId, String tamanho,
                                    PedidoStatus status, BigDecimal total) {
        static PedidoMinResponse of(Pedido p) {
            return new PedidoMinResponse(p.getId(), p.getClienteId(),
                    p.getTamanho(), p.getStatus(), p.getTotal());
        }
    }

    public record PedidoResponse(Long id, Long clienteId, String tamanho, PedidoStatus status,
                                 TipoEntrega tipoEntrega, BigDecimal total, String observacaoCliente,
                                 String enderecoLogradouro, String enderecoNumero, String enderecoComplemento,
                                 String enderecoBairro, String enderecoCidade, String enderecoUf, String enderecoCep,
                                 Double lat, Double lng,
                                 List<Item> itens) {
        public record Item(Long itemId, Integer quantidade, BigDecimal precoUnit, BigDecimal totalItem) {}
        static PedidoResponse of(Pedido p) {
            var itens = p.getItens().stream()
                    .map(i -> new Item(i.getItemId(), i.getQuantidade(), i.getPrecoUnit(), i.getTotalItem()))
                    .toList();
            return new PedidoResponse(
                    p.getId(), p.getClienteId(), p.getTamanho(), p.getStatus(),
                    p.getTipoEntrega(), p.getTotal(), p.getObservacaoCliente(),
                    p.getEnderecoLogradouro(), p.getEnderecoNumero(), p.getEnderecoComplemento(),
                    p.getEnderecoBairro(), p.getEnderecoCidade(), p.getEnderecoUf(), p.getEnderecoCep(),
                    p.getLat(), p.getLng(), itens
            );
        }
    }
}

