package com.aula.marmitas.controller;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.aula.marmitas.entity.cardapio.*;
import com.aula.marmitas.repository.CardapioRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cardapio")
@RequiredArgsConstructor
public class CardapioController {

    private final CardapioRepository repo;

    // ============ PÚBLICO ============

    @GetMapping
    public List<ItemDTO> listar(@RequestParam(required = false) LocalDate data,
                                @RequestParam(required = false) TipoItem tipo) {
        var base = (tipo == null)
                ? repo.findActiveItemsOrdered()
                : repo.findActiveItemsByTipoOrdered(tipo);

        return base.stream()
                .map(ic -> toDTO(ic, disponibilidadeNoDia(ic, data)))
                .toList();
    }

    @PostMapping("/orcamento")
    public ResponseEntity<OrcamentoDTO> orcar(@RequestBody @Valid OrcamentoCommand cmd) {
        var cfg = repo.getConfigPrecosOrCreate();

        Map<Long, Integer> qtdPorItem = new HashMap<>();
        for (var it : cmd.itens()) {
            if (it.quantidade() <= 0) continue;
            qtdPorItem.merge(it.itemId(), it.quantidade(), Integer::sum);
        }
        if (qtdPorItem.isEmpty()) return ResponseEntity.badRequest().build();

        var itens = repo.findItemsByIds(qtdPorItem.keySet());
        if (itens.size() != qtdPorItem.size()) return ResponseEntity.badRequest().build();

        for (var it : itens) {
            if (!it.isAtivo()) return ResponseEntity.badRequest().build();
            if (cmd.data() != null && !disponibilidadeNoDia(it, cmd.data())) {
                return ResponseEntity.badRequest().build();
            }
        }

        int qtdMisturas = itens.stream()
                .filter(i -> i.getTipo() == TipoItem.MISTURA)
                .mapToInt(i -> qtdPorItem.get(i.getId())).sum();
        int qtdAcomp = itens.stream()
                .filter(i -> i.getTipo() == TipoItem.ACOMPANHAMENTO)
                .mapToInt(i -> qtdPorItem.get(i.getId())).sum();

        TamanhoMarmita tamanho = null;
        if (cfg.getStrategy() != Strategy.PER_ITEM) {
            if (!StringUtils.hasText(cmd.tamanho())) return ResponseEntity.badRequest().build();
            tamanho = repo.findTamanhoByNome(cmd.tamanho().trim()).orElse(null);
            if (tamanho == null) return ResponseEntity.badRequest().build();
        }

        BigDecimal subtotal;
        switch (cfg.getStrategy()) {
            case PER_ITEM -> subtotal = itens.stream()
                    .map(i -> i.getPreco().multiply(BigDecimal.valueOf(qtdPorItem.get(i.getId()))))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case FIXED_SIZE -> {
                subtotal = tamanho.getPrecoBase();
                int extraMist = Math.max(0, qtdMisturas - cfg.getIncluiMisturas());
                subtotal = subtotal.add(cfg.getPrecoExtraMistura().multiply(BigDecimal.valueOf(extraMist)));
                if (cfg.getPrecoAcompanhamentoPadrao() != null) {
                    subtotal = subtotal.add(cfg.getPrecoAcompanhamentoPadrao().multiply(BigDecimal.valueOf(qtdAcomp)));
                }
            }
            case BASE_PLUS_ADDONS -> {
                subtotal = tamanho.getPrecoBase();
                int extraMist = Math.max(0, qtdMisturas - 1);
                subtotal = subtotal.add(cfg.getPrecoExtraMistura().multiply(BigDecimal.valueOf(extraMist)));
                if (cfg.getPrecoAcompanhamentoPadrao() != null) {
                    subtotal = subtotal.add(cfg.getPrecoAcompanhamentoPadrao().multiply(BigDecimal.valueOf(qtdAcomp)));
                }
            }
            default -> throw new IllegalStateException("Estratégia inválida");
        }

        var itensOrcados = itens.stream()
                .sorted(Comparator.comparing(ItemCardapio::getNome))
                .map(i -> new OrcamentoDTO.ItemOrcado(
                        i.getId(), i.getNome(), i.getPreco(),
                        qtdPorItem.get(i.getId()),
                        i.getPreco().multiply(BigDecimal.valueOf(qtdPorItem.get(i.getId())))))
                .toList();

        var dto = new OrcamentoDTO(subtotal, cfg.getStrategy().name(),
                tamanho != null ? tamanho.getNome() : null, qtdMisturas, qtdAcomp, itensOrcados);

        return ResponseEntity.ok(dto);
    }

    // ============ ADMIN ============

    @PostMapping("/itens")
    @Transactional
    public ResponseEntity<ItemDTO> criar(@RequestBody @Valid CriarItemRequest req) {
        validarItem(null, req.nome(), req.tipo(), req.preco());

        var novo = ItemCardapio.builder()
                .nome(req.nome().trim())
                .descricao(v(req.descricao()))
                .tipo(req.tipo())
                .preco(req.preco())
                .ativo(Boolean.TRUE.equals(req.ativo()))
                .ordemExibicao(req.ordemExibicao())
                .imagemUrl(v(req.imagemUrl()))
                .isPadrao(false)
                .build();

        var salvo = repo.saveItem(novo);
        return ResponseEntity.created(URI.create("/api/cardapio/itens/" + salvo.getId()))
                .body(toDTO(salvo, null));
    }

    @PutMapping("/itens/{id}")
    @Transactional
    public ResponseEntity<ItemDTO> atualizar(@PathVariable Long id,
                                             @RequestBody @Valid AtualizarItemRequest req) {
        var it = repo.findItemById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();

        validarItem(id, req.nome(), req.tipo(), req.preco());

        if (StringUtils.hasText(req.nome())) it.setNome(req.nome().trim());
        it.setDescricao(v(req.descricao()));
        if (req.tipo() != null) it.setTipo(req.tipo());
        if (req.preco() != null) it.setPreco(req.preco());
        if (req.ativo() != null) it.setAtivo(req.ativo());
        it.setOrdemExibicao(req.ordemExibicao());
        it.setImagemUrl(v(req.imagemUrl()));

        return ResponseEntity.ok(toDTO(it, null));
    }

    @PatchMapping("/itens/{id}/ativo")
    @Transactional
    public ResponseEntity<Void> ativar(@PathVariable Long id, @RequestParam boolean value) {
        var it = repo.findItemById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();
        it.setAtivo(value);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/itens/{id}/ordem")
    @Transactional
    public ResponseEntity<Void> definirOrdem(@PathVariable Long id, @RequestParam int value) {
        var it = repo.findItemById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();
        it.setOrdemExibicao(value);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/itens/{id}/disponibilidade")
    @Transactional
    public ResponseEntity<DisponibilidadeDTO> definirDisponibilidade(@PathVariable Long id,
                                                                     @RequestBody @Valid DefinirDisponibilidadeRequest req) {
        var it = repo.findItemById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();

        var existente = repo.findDisponibilidadeData(id, req.data()).orElse(null);
        if (existente == null) {
            existente = DisponibilidadeItem.builder()
                    .item(it)
                    .data(req.data())
                    .disponivel(Boolean.TRUE.equals(req.disponivel()))
                    .quantidadeMax(req.quantidadeMax())
                    .build();
        } else {
            existente.setDisponivel(Boolean.TRUE.equals(req.disponivel()));
            existente.setQuantidadeMax(req.quantidadeMax());
        }
        var salvo = repo.saveDisponibilidadeData(existente);
        return ResponseEntity.ok(new DisponibilidadeDTO(it.getId(), salvo.getData(), salvo.isDisponivel(), salvo.getQuantidadeMax()));
    }

    @PutMapping("/itens/{id}/disponibilidade-semana")
    @Transactional
    public ResponseEntity<Void> definirDisponibilidadeSemana(@PathVariable Long id,
                                                             @RequestParam int diaSemana,
                                                             @RequestParam boolean disponivel) {
        if (diaSemana < 1 || diaSemana > 7) return ResponseEntity.badRequest().build();
        var it = repo.findItemById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();

        var sem = repo.findDisponibilidadeSemana(id, diaSemana).orElse(null);
        if (sem == null) {
            sem = DisponibilidadeSemana.builder()
                    .item(it).diaSemana(diaSemana).disponivel(disponivel).build();
        } else sem.setDisponivel(disponivel);

        repo.saveDisponibilidadeSemana(sem);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-padroes")
    @Transactional
    public ResponseEntity<ResetResumoDTO> resetPadroes() {
        int restaurados = 0, desativados = 0;

        for (var ic : repo.findPadroes(true)) {
            var def = repo.findDefaultByKey(ic.getPadraoKey()).orElse(null);
            if (def == null) continue;
            ic.setNome(def.getNome());
            ic.setDescricao(def.getDescricao());
            ic.setTipo(def.getTipo());
            ic.setPreco(def.getPreco());
            ic.setOrdemExibicao(def.getOrdemExibicao());
            ic.setImagemUrl(def.getImagemUrl());
            ic.setAtivo(true);
            restaurados++;
        }
        for (var ex : repo.findPadroes(false)) {
            if (ex.isAtivo()) { ex.setAtivo(false); desativados++; }
        }

        return ResponseEntity.ok(new ResetResumoDTO(restaurados, desativados));
    }

    @PatchMapping("/precos/estrategia")
    @Transactional
    public ResponseEntity<Void> trocarEstrategia(@RequestParam Strategy value) {
        var cfg = repo.getConfigPrecosOrCreate();
        cfg.setStrategy(value);
        repo.saveConfigPrecos(cfg);
        return ResponseEntity.noContent().build();
    }

    // ============ Helpers/DTOs ============

    private boolean disponibilidadeNoDia(ItemCardapio ic, LocalDate data) {
        if (!ic.isAtivo()) return false;
        if (data == null) return true;

        var ovr = repo.findDisponibilidadeData(ic.getId(), data);
        if (ovr.isPresent()) return ovr.get().isDisponivel();

        int dow = data.getDayOfWeek().getValue(); // 1..7
        return repo.findDisponibilidadeSemana(ic.getId(), dow)
                .map(DisponibilidadeSemana::isDisponivel)
                .orElse(true);
    }

    private ItemDTO toDTO(ItemCardapio ic, Boolean disp) {
        return new ItemDTO(ic.getId(), ic.getNome(), ic.getDescricao(), ic.getTipo(),
                ic.getPreco(), ic.isAtivo(), ic.getOrdemExibicao(), ic.getImagemUrl(), disp);
    }

    private void validarItem(Long id, String nome, TipoItem tipo, BigDecimal preco) {
        if (!StringUtils.hasText(nome)) throw new IllegalArgumentException("Nome é obrigatório");
        if (tipo == null) throw new IllegalArgumentException("Tipo é obrigatório");
        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Preço inválido");
        boolean existe = (id == null)
                ? repo.existsByNomeIgnoreCase(nome.trim())
                : repo.existsByNomeIgnoreCaseAndIdNot(nome.trim(), id);
        if (existe) throw new IllegalArgumentException("Já existe item com esse nome");
    }

    private String v(String s) { return StringUtils.hasText(s) ? s.trim() : null; }

    // DTOs

    public record ItemDTO(Long id, String nome, String descricao, TipoItem tipo,
                          BigDecimal preco, boolean ativo, Integer ordemExibicao,
                          String imagemUrl, Boolean disponivelNoDia) {}

    public record CriarItemRequest(@NotBlank String nome, String descricao,
                                   @NotNull TipoItem tipo, @NotNull @Min(0) BigDecimal preco,
                                   Boolean ativo, Integer ordemExibicao, String imagemUrl) {}

    public record AtualizarItemRequest(String nome, String descricao, TipoItem tipo,
                                       BigDecimal preco, Boolean ativo, Integer ordemExibicao,
                                       String imagemUrl) {}

    public record DefinirDisponibilidadeRequest(@NotNull LocalDate data,
                                                @NotNull Boolean disponivel,
                                                Integer quantidadeMax) {}

    public record DisponibilidadeDTO(Long itemId, LocalDate data, boolean disponivel, Integer quantidadeMax) {}

    public record OrcamentoCommand(String tamanho, LocalDate data, List<ItemQtd> itens) {
        public record ItemQtd(@NotNull Long itemId, @Min(1) int quantidade) {}
    }

    public record OrcamentoDTO(BigDecimal subtotal, String strategy, String tamanho,
                               int qtdMisturas, int qtdAcomp, List<ItemOrcado> itens) {
        public record ItemOrcado(Long itemId, String nome, BigDecimal precoUnit, int quantidade, BigDecimal total) {}
    }

    public record ResetResumoDTO(int restaurados, int desativados) {}
}

