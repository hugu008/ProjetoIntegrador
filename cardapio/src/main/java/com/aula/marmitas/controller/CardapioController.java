package com.aula.marmitas.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.aula.marmitas.entity.cardapio.*;
import com.aula.marmitas.repository.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cardapio")
@RequiredArgsConstructor
public class CardapioController {

    private final ItemCardapioRepository itemRepo;
    private final ItemCardapioDefaultRepository defaultRepo;
    private final DisponibilidadeItemRepository dispItemRepo;
    private final DisponibilidadeSemanaRepository dispSemanaRepo;
    private final TamanhoMarmitaRepository tamRepo;
    private final ConfigPrecosRepository configRepo;

    // ============ PÚBLICO ============

    @GetMapping
    public List<ItemDTO> listar(@RequestParam(required = false) LocalDate data,
                                @RequestParam(required = false) TipoItem tipo) {
        var base = (tipo == null)
                ? itemRepo.findActiveItemsOrdered()
                : itemRepo.findActiveItemsByTipoOrdered(tipo);

        return base.stream()
                .map(ic -> toDTO(ic, disponibilidadeNoDia(ic, data)))
                .toList();
    }

    @PostMapping("/orcamento")
    public ResponseEntity<OrcamentoDTO> orcar(@RequestBody @Valid OrcamentoCommand cmd) {
        var cfg = getConfigPrecosOrCreate();

        Map<Long, Integer> qtdPorItem = new HashMap<>();
        for (var it : cmd.itens()) {
            if (it.quantidade() > 0) qtdPorItem.merge(it.itemId(), it.quantidade(), Integer::sum);
        }
        if (qtdPorItem.isEmpty()) return ResponseEntity.badRequest().build();

        var itens = itemRepo.findByIds(qtdPorItem.keySet());
        if (itens.size() != qtdPorItem.size()) return ResponseEntity.badRequest().build();

        for (var it : itens) {
            if (!isTrue(it.getAtivo())) return ResponseEntity.badRequest().build();
            if (cmd.data() != null && !disponibilidadeNoDia(it, cmd.data())) return ResponseEntity.badRequest().build();
        }

        int qtdMisturas = itens.stream()
                .filter(i -> i.getTipo() == TipoItem.MISTURA)
                .mapToInt(i -> qtdPorItem.get(i.getId_item_cardapio())).sum();

        int qtdAcomp = itens.stream()
                .filter(i -> i.getTipo() == TipoItem.ACOMPANHAMENTO)
                .mapToInt(i -> qtdPorItem.get(i.getId_item_cardapio())).sum();

        tamanho_marmita tamanho = null;
        if (cfg.getStrategy() != Strategy.PER_ITEM) {
            if (!StringUtils.hasText(cmd.tamanho())) return ResponseEntity.badRequest().build();
            tamanho = tamRepo.findFirstByNomeIgnoreCase(cmd.tamanho().trim()).orElse(null);
            if (tamanho == null) return ResponseEntity.badRequest().build();
        }

        BigDecimal subtotal;
        switch (cfg.getStrategy()) {
            case PER_ITEM -> subtotal = itens.stream()
                    .map(i -> bd(i.getPreco()).multiply(BigDecimal.valueOf(qtdPorItem.get(i.getId_item_cardapio()))))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            case FIXED_SIZE -> {
                subtotal = bd(tamanho.getPreco_base());
                int incluidas = (cfg.getInclui_misturas() != null ? cfg.getInclui_misturas() : 1);
                int extraMist = Math.max(0, qtdMisturas - incluidas);
                subtotal = subtotal.add(bd(cfg.getPreco_extra_mistura()).multiply(BigDecimal.valueOf(extraMist)));
                if (cfg.getPreco_acompanhamento_padrao() != null) {
                    subtotal = subtotal.add(bd(cfg.getPreco_acompanhamento_padrao()).multiply(BigDecimal.valueOf(qtdAcomp)));
                }
            }
            case BASE_PLUS_ADDONS -> {
                subtotal = bd(tamanho.getPreco_base());
                int extraMist = Math.max(0, qtdMisturas - 1);
                subtotal = subtotal.add(bd(cfg.getPreco_extra_mistura()).multiply(BigDecimal.valueOf(extraMist)));
                if (cfg.getPreco_acompanhamento_padrao() != null) {
                    subtotal = subtotal.add(bd(cfg.getPreco_acompanhamento_padrao()).multiply(BigDecimal.valueOf(qtdAcomp)));
                }
            }
            default -> throw new IllegalStateException("Estratégia inválida");
        }

        var itensOrcados = itens.stream()
                .sorted(Comparator.comparing(item_cardapio::getNome))
                .map(i -> new OrcamentoDTO.ItemOrcado(
                        i.getId_item_cardapio(), i.getNome(), bd(i.getPreco()),
                        qtdPorItem.get(i.getId_item_cardapio()),
                        bd(i.getPreco()).multiply(BigDecimal.valueOf(qtdPorItem.get(i.getId_item_cardapio())))))
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

        var novo = new item_cardapio();
        novo.setNome(req.nome().trim());
        novo.setDescricao(v(req.descricao()));
        novo.setTipo(req.tipo());
        novo.setPreco(req.preco() != null ? req.preco().doubleValue() : null);
        novo.setAtivo(bool(req.ativo()));
        novo.setOrdem_exibicao(req.ordemExibicao());
        novo.setImagem_url(v(req.imagemUrl()));
        novo.setIs_padrao(0);

        var salvo = itemRepo.save(novo);
        return ResponseEntity.created(URI.create("/api/cardapio/itens/" + salvo.getId_item_cardapio()))
                .body(toDTO(salvo, null));
    }

    @PutMapping("/itens/{id}")
    @Transactional
    public ResponseEntity<ItemDTO> atualizar(@PathVariable Long id,
                                             @RequestBody @Valid AtualizarItemRequest req) {
        var it = itemRepo.findById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();

        validarItem(id, req.nome(), req.tipo(), req.preco());

        if (StringUtils.hasText(req.nome())) it.setNome(req.nome().trim());
        it.setDescricao(v(req.descricao()));
        if (req.tipo() != null) it.setTipo(req.tipo());
        if (req.preco() != null) it.setPreco(req.preco().doubleValue());
        if (req.ativo() != null) it.setAtivo(bool(req.ativo()));
        it.setOrdem_exibicao(req.ordemExibicao());
        it.setImagem_url(v(req.imagemUrl()));

        return ResponseEntity.ok(toDTO(it, null));
    }

    @PatchMapping("/itens/{id}/ativo")
    @Transactional
    public ResponseEntity<Void> ativar(@PathVariable Long id, @RequestParam boolean value) {
        var it = itemRepo.findById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();
        it.setAtivo(value ? 1 : 0);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/itens/{id}/ordem")
    @Transactional
    public ResponseEntity<Void> definirOrdem(@PathVariable Long id, @RequestParam int value) {
        var it = itemRepo.findById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();
        it.setOrdem_exibicao(value);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/itens/{id}/disponibilidade")
    @Transactional
    public ResponseEntity<DisponibilidadeDTO> definirDisponibilidade(@PathVariable Long id,
                                                                     @RequestBody @Valid DefinirDisponibilidadeRequest req) {
        var it = itemRepo.findById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();

        var existente = dispItemRepo.findFirstByItemIdAndData(id, toDate(req.data())).orElse(null);
        if (existente == null) {
            existente = new disponibilidade_item();
            existente.setItem(it);
            existente.setData(toDate(req.data()));
            existente.setDisponivel(bool(req.disponivel()));
            existente.setQuantidade_max(req.quantidadeMax());
        } else {
            existente.setDisponivel(bool(req.disponivel()));
            existente.setQuantidade_max(req.quantidadeMax());
        }
        var salvo = dispItemRepo.save(existente);
        return ResponseEntity.ok(new DisponibilidadeDTO(
                it.getId_item_cardapio(), toLocalDate(salvo.getData()), isTrue(salvo.getDisponivel()), salvo.getQuantidade_max()));
    }

    @PutMapping("/itens/{id}/disponibilidade-semana")
    @Transactional
    public ResponseEntity<Void> definirDisponibilidadeSemana(@PathVariable Long id,
                                                             @RequestParam int diaSemana,
                                                             @RequestParam boolean disponivel) {
        if (diaSemana < 1 || diaSemana > 7) return ResponseEntity.badRequest().build();

        var it = itemRepo.findById(id).orElse(null);
        if (it == null) return ResponseEntity.notFound().build();

        var sem = dispSemanaRepo.findFirstByItemIdAndDiaSemana(id, diaSemana).orElse(null);
        if (sem == null) {
            sem = new disponibilidade_semana();
            sem.setItem(it);
            sem.setDia_semana(diaSemana);
            sem.setDisponivel(disponivel ? 1 : 0);
        } else {
            sem.setDisponivel(disponivel ? 1 : 0);
        }

        dispSemanaRepo.save(sem);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-padroes")
    @Transactional
    public ResponseEntity<ResetResumoDTO> resetPadroes() {
        int restaurados = 0, desativados = 0;

        for (var ic : itemRepo.findByIsPadrao(1)) {
            var def = defaultRepo.findById(ic.getPadrao_key()).orElse(null);
            if (def == null) continue;
            ic.setNome(def.getNome());
            ic.setDescricao(def.getDescricao());
            ic.setTipo(def.getTipo());
            ic.setPreco(def.getPreco());
            ic.setOrdem_exibicao(def.getOrdem_exibicao());
            ic.setImagem_url(def.getImagem_url());
            ic.setAtivo(1);
            restaurados++;
        }
        for (var ex : itemRepo.findByIsPadrao(0)) {
            if (isTrue(ex.getAtivo())) { ex.setAtivo(0); desativados++; }
        }
        return ResponseEntity.ok(new ResetResumoDTO(restaurados, desativados));
    }

    @PatchMapping("/precos/estrategia")
    @Transactional
    public ResponseEntity<Void> trocarEstrategia(@RequestParam Strategy value) {
        var cfg = getConfigPrecosOrCreate();
        cfg.setStrategy(value);
        configRepo.save(cfg);
        return ResponseEntity.noContent().build();
    }

    // ============ Helpers/DTOs ============

    private boolean disponibilidadeNoDia(item_cardapio ic, LocalDate data) {
        if (!isTrue(ic.getAtivo())) return false;
        if (data == null) return true;

        var ovr = dispItemRepo.findFirstByItemIdAndData(ic.getId_item_cardapio(), toDate(data));
        if (ovr.isPresent()) return isTrue(ovr.get().getDisponivel());

        int dow = data.getDayOfWeek().getValue(); // 1..7
        return dispSemanaRepo.findFirstByItemIdAndDiaSemana(ic.getId_item_cardapio(), dow)
                .map(d -> isTrue(d.getDisponivel()))
                .orElse(true);
    }

    private ItemDTO toDTO(item_cardapio ic, Boolean disp) {
        return new ItemDTO(
                ic.getId_item_cardapio(), ic.getNome(), ic.getDescricao(), ic.getTipo(),
                bd(ic.getPreco()), isTrue(ic.getAtivo()), ic.getOrdem_exibicao(), ic.getImagem_url(), disp
        );
    }

    private void validarItem(Long id, String nome, TipoItem tipo, BigDecimal preco) {
        if (!StringUtils.hasText(nome)) throw new IllegalArgumentException("Nome é obrigatório");
        if (tipo == null) throw new IllegalArgumentException("Tipo é obrigatório");
        if (preco == null || preco.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Preço inválido");

        boolean existe = (id == null)
                ? itemRepo.countByNomeIgnoreCase(nome.trim()) > 0
                : itemRepo.countByNomeIgnoreCaseAndIdNot(nome.trim(), id) > 0;

        if (existe) throw new IllegalArgumentException("Já existe item com esse nome");
    }

    private String v(String s) { return StringUtils.hasText(s) ? s.trim() : null; }

    private config_precos getConfigPrecosOrCreate() {
        return configRepo.findById((byte) 1).orElseGet(() -> {
            var c = new config_precos();
            c.setId((byte) 1);
            c.setStrategy(Strategy.PER_ITEM);
            c.setInclui_misturas(1);
            c.setPreco_extra_mistura(0.0);
            c.setPreco_acompanhamento_padrao(0.0);
            return configRepo.save(c);
        });
    }

    // —— conversões utilitárias (Double/Integer <-> BigDecimal/boolean/Date) ——
    private static BigDecimal bd(Double v) {
        if (v == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }
    private static boolean isTrue(Integer v) { return v != null && v == 1; }
    private static int bool(Boolean b) { return (b != null && b) ? 1 : 0; }

    private static java.util.Date toDate(LocalDate d) {
        return java.sql.Date.valueOf(d);
    }
    private static LocalDate toLocalDate(java.util.Date d) {
        return new java.sql.Date(d.getTime()).toLocalDate();
    }

    // DTOs (inalterados)
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
