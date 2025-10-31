package com.seuprojeto.marmitas.service;

import com.seuprojeto.marmitas.entity.*;
import com.seuprojeto.marmitas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepo;
    private final PedidoItemRepository pedidoItemRepo;
    private final ClienteRepository clienteRepo;
    private final ItemCardapioRepository itemRepo;
    private final DisponibilidadeRepository dispRepo;

    /* =========================================================
       CRIA "MARMITA PADRÃO DO DIA"
       - Bases com substitutos (carreteiro/feijoada) se houver no dia
       - Ovo + Salada sempre
       - Acompanhamentos do dia
       - UMA mistura do dia (se houver)
       ========================================================= */
    @Transactional
    public Pedido criarMarmitaPadrao(Long clienteId, DiaSemana dia) {
        Cliente cliente = clienteRepo.findById(clienteId).orElse(null);

        // final + sem reatribuição
        final Pedido pedido = pedidoRepo.save(
            Pedido.builder()
                .cliente(cliente)
                .tipo(TipoPedido.PADRAO)
                .status(StatusPedido.PENDENTE)
                .build()
        );

        // Bases com substitutos do dia
        addSubOuBase(pedido, SubstituiBase.ARROZ, "Arroz", dia);
        addSubOuBase(pedido, SubstituiBase.FEIJAO, "Feijão", dia);

        // Sempre acompanha
        addItem(pedido, "Ovo frito", 1);
        addItem(pedido, "Salada", 1);

        // Acompanhamentos do dia (se ativos)
        dispRepo.findByDia(dia).stream()
            .map(Disponibilidade::getItem)
            .filter(i -> i.getCategoria().getNome().equalsIgnoreCase("Acompanhamento"))
            .filter(ItemCardapio::isAtivo)
            .forEach(i -> addItem(pedido, i.getNome(), 1));

        // UMA mistura do dia (se houver e ativa)
        dispRepo.findByDia(dia).stream()
            .map(Disponibilidade::getItem)
            .filter(i -> i.getCategoria().getNome().equalsIgnoreCase("Mistura"))
            .filter(ItemCardapio::isAtivo)
            .findFirst()
            .ifPresent(i -> addItem(pedido, i.getNome(), 1));

        return pedidoRepo.findById(pedido.getId()).orElseThrow();
    }

    /* =========================================================
       CRIA PEDIDO LIVRE (vazio)
       ========================================================= */
    @Transactional
    public Pedido criarPedidoLivre(Long clienteId) {
        Cliente cliente = clienteRepo.findById(clienteId).orElse(null);
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .tipo(TipoPedido.LIVRE)
                .status(StatusPedido.PENDENTE)
                .build();
        return pedidoRepo.save(pedido);
    }

    /* =========================================================
       ADICIONA ITEM POR NOME AO PEDIDO (valida limite de 2 misturas)
       ========================================================= */
    @Transactional
    public Pedido adicionarItemPorNome(Long pedidoId, String itemNome, int quantidade) {
        if (quantidade < 1) quantidade = 1;

        Pedido pedido = pedidoRepo.findById(pedidoId).orElseThrow();
        ItemCardapio item = itemRepo.findAll().stream()
                .filter(i -> i.getNome().equalsIgnoreCase(itemNome))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado: " + itemNome));

        // ✅ valida somatório de quantidades de "Mistura"
        if (item.getCategoria().getNome().equalsIgnoreCase("Mistura")) {
            int totalMisturas = pedido.getItens().stream()
                    .filter(pi -> pi.getItem().getCategoria().getNome().equalsIgnoreCase("Mistura"))
                    .mapToInt(PedidoItem::getQuantidade)
                    .sum();
            if (totalMisturas + quantidade > 2) {
                throw new IllegalArgumentException("Limite de 2 misturas por pedido excedido.");
            }
        }

        PedidoItem pi = PedidoItem.builder()
                .pedido(pedido)
                .item(item)
                .quantidade(quantidade)
                .build();

        pedido.getItens().add(pedidoItemRepo.save(pi));
        return pedidoRepo.save(pedido);
    }

    /* =========================================================
       REMOVE UMA OCORRÊNCIA DE UM ITEM PELO NOME
       ========================================================= */
    @Transactional
    public Pedido removerItemPorNome(Long pedidoId, String itemNome) {
        Pedido pedido = pedidoRepo.findById(pedidoId).orElseThrow();

        PedidoItem pi = pedido.getItens().stream()
                .filter(it -> it.getItem().getNome().equalsIgnoreCase(itemNome))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item não está no pedido."));

        pedido.getItens().remove(pi);
        pedidoItemRepo.delete(pi);
        return pedidoRepo.save(pedido);
    }

    /* =========================================================
       HELPERS
       ========================================================= */

    // Usa substituto do dia (se existir e ativo) ou cai para a base padrão
    private void addSubOuBase(Pedido p, SubstituiBase sub, String baseNome, DiaSemana dia) {
        List<ItemCardapio> subs = itemRepo.findBySubstituiAndAtivoTrue(sub);
        ItemCardapio substitutoDoDia = subs.stream()
                .filter(ItemCardapio::isAtivo)
                .filter(i -> dispRepo.findByDia(dia).stream()
                        .anyMatch(d -> d.getItem().getId().equals(i.getId())))
                .findFirst()
                .orElse(null);

        if (substitutoDoDia != null) {
            addItem(p, substitutoDoDia.getNome(), 1);
        } else {
            addItem(p, baseNome, 1);
        }
    }

    // Adiciona efetivamente um item pelo nome (sem repetir validação de limite aqui)
    private void addItem(Pedido p, String itemNome, int qtd) {
        ItemCardapio item = itemRepo.findAll().stream()
                .filter(i -> i.getNome().equalsIgnoreCase(itemNome))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Item não encontrado: " + itemNome));

        PedidoItem pi = PedidoItem.builder()
                .pedido(p)
                .item(item)
                .quantidade(Math.max(1, qtd))
                .build();

        p.getItens().add(pedidoItemRepo.save(pi));
        pedidoRepo.save(p); // persiste o vínculo
    }
}
