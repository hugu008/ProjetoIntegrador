package com.aula.marmitas.controller;

import java.math.BigDecimal;
import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.aula.marmitas.entity.pagamento.*;
import com.aula.marmitas.repository.PagamentoRepository;

import com.aula.marmitas.entity.pedido.Pedido;
import com.aula.marmitas.entity.pedido.PedidoStatus;
import com.aula.marmitas.repository.PedidoRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {

    private final PagamentoRepository pagRepo;
    private final PedidoRepository pedidoRepo;

    // ========== Registrar/atualizar pagamento (1:1 por pedido) ==========

    @PostMapping
    @Transactional
    public ResponseEntity<PagamentoResponse> registrar(@RequestBody @Valid RegistrarPagamentoRequest req) {
        // 1) Pedido precisa existir
        Pedido pedido = pedidoRepo.findById(req.pedidoId()).orElse(null);
        if (pedido == null) return ResponseEntity.badRequest().build();

        // 2) Pega (ou cria) o pagamento do pedido
        Pagamento pag = pagRepo.findByPedidoId(req.pedidoId()).orElse(
                Pagamento.builder()
                        .pedidoId(req.pedidoId())
                        .valor(req.valor() != null ? req.valor() : pedido.getTotal())
                        .metodo(req.metodo())
                        .status(StatusPagamento.PENDENTE)
                        .cartaoPresente(Boolean.TRUE.equals(req.cartaoPresente()))
                        .pixCopiaCola(req.pixCopiaCola())
                        .pixTxid(req.pixTxid())
                        .referencia(req.referencia())
                        .observacao(req.observacao())
                        .build()
        );

        // 3) Atualiza campos (se vieram)
        if (req.valor() != null) pag.setValor(req.valor());
        if (req.metodo() != null) pag.setMetodo(req.metodo());
        if (req.cartaoPresente() != null) pag.setCartaoPresente(req.cartaoPresente());
        if (StringUtils.hasText(req.pixCopiaCola())) pag.setPixCopiaCola(req.pixCopiaCola());
        if (StringUtils.hasText(req.pixTxid())) pag.setPixTxid(req.pixTxid());
        if (StringUtils.hasText(req.referencia())) pag.setReferencia(req.referencia());
        if (StringUtils.hasText(req.observacao())) pag.setObservacao(req.observacao());

        var salvo = pagRepo.save(pag);
        return ResponseEntity.ok(PagamentoResponse.of(salvo));
    }

    // ========== Confirmar pagamento (muda Pedido → PAGO) ==========

    @PostMapping("/{id}/confirmar")
    @Transactional
    public ResponseEntity<Void> confirmar(@PathVariable Long id) {
        var pag = pagRepo.findById(id).orElse(null);
        if (pag == null) return ResponseEntity.notFound().build();

        // Não confirma se já está confirmado/recusado
        if (pag.getStatus() == StatusPagamento.CONFIRMADO) return ResponseEntity.noContent().build();
        if (pag.getStatus() == StatusPagamento.RECUSADO) return ResponseEntity.badRequest().build();

        // Pedido precisa estar em CRIADO para ir a PAGO
        var pedido = pedidoRepo.findById(pag.getPedidoId()).orElse(null);
        if (pedido == null) return ResponseEntity.badRequest().build();
        if (pedido.getStatus() != PedidoStatus.CRIADO) return ResponseEntity.badRequest().build();

        // Atualiza pagamento
        pag.setStatus(StatusPagamento.CONFIRMADO);
        pag.setConfirmadoEm(Instant.now());
        pagRepo.save(pag);

        // Move pedido → PAGO
        pedido.setStatus(PedidoStatus.PAGO);

        // (Opcional) registrar evento
        // pagRepo.saveEvento(PagamentoEvento.builder()
        //         .pagamentoId(pag.getId())
        //         .tipo("STATUS_CHANGE")
        //         .deStatus(StatusPagamento.PENDENTE)
        //         .paraStatus(StatusPagamento.CONFIRMADO)
        //         .build());

        return ResponseEntity.noContent().build();
    }

    // ========== Recusar pagamento ==========

    @PostMapping("/{id}/recusar")
    @Transactional
    public ResponseEntity<Void> recusar(@PathVariable Long id,
                                        @RequestParam(required = false) String motivo) {
        var pag = pagRepo.findById(id).orElse(null);
        if (pag == null) return ResponseEntity.notFound().build();

        if (pag.getStatus() == StatusPagamento.CONFIRMADO) return ResponseEntity.badRequest().build();
        if (pag.getStatus() == StatusPagamento.RECUSADO) return ResponseEntity.noContent().build();

        pag.setStatus(StatusPagamento.RECUSADO);
        if (StringUtils.hasText(motivo)) pag.setObservacao(motivo);
        pagRepo.save(pag);

        // (Opcional) evento
        // pagRepo.saveEvento(PagamentoEvento.builder()
        //         .pagamentoId(pag.getId())
        //         .tipo("STATUS_CHANGE")
        //         .deStatus(StatusPagamento.PENDENTE)
        //         .paraStatus(StatusPagamento.RECUSADO)
        //         .payload(motivo)
        //         .build());

        return ResponseEntity.noContent().build();
    }

    // ========== Consultas ==========

    @GetMapping("/{pedidoId}")
    public ResponseEntity<PagamentoResponse> obterPorPedido(@PathVariable Long pedidoId) {
        var pag = pagRepo.findByPedidoId(pedidoId).orElse(null);
        if (pag == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(PagamentoResponse.of(pag));
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<PagamentoResponse> obterPorId(@PathVariable Long id) {
        var pag = pagRepo.findById(id).orElse(null);
        if (pag == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(PagamentoResponse.of(pag));
    }

    // ========== DTOs ==========

    public record RegistrarPagamentoRequest(
            @NotNull Long pedidoId,
            MetodoPagamento metodo,
            @Min(0) BigDecimal valor,    // se null, usa total do pedido
            String referencia,
            String observacao,
            Boolean cartaoPresente,
            String pixTxid,
            String pixCopiaCola
    ) {}

    public record PagamentoResponse(
            Long id, Long pedidoId, BigDecimal valor, MetodoPagamento metodo,
            StatusPagamento status, String referencia, String observacao,
            String pixTxid, String pixCopiaCola, Boolean cartaoPresente,
            Instant confirmadoEm, Instant criadoEm, Instant atualizadoEm
    ) {
        static PagamentoResponse of(Pagamento p) {
            return new PagamentoResponse(
                    p.getId(), p.getPedidoId(), p.getValor(), p.getMetodo(),
                    p.getStatus(), p.getReferencia(), p.getObservacao(),
                    p.getPixTxid(), p.getPixCopiaCola(), p.isCartaoPresente(),
                    p.getConfirmadoEm(), p.getCriadoEm(), p.getAtualizadoEm()
            );
        }
    }
}

