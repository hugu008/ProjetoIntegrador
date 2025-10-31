package com.seuprojeto.marmitas;

import com.seuprojeto.marmitas.entity.Cliente;
import com.seuprojeto.marmitas.entity.DiaSemana;
import com.seuprojeto.marmitas.entity.Pedido;
import com.seuprojeto.marmitas.repository.ClienteRepository;
import com.seuprojeto.marmitas.repository.PedidoRepository;
import com.seuprojeto.marmitas.service.CardapioService;
import com.seuprojeto.marmitas.service.PedidoService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SimulationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SimulationRunner.class);

    private final ClienteRepository clienteRepo;
    private final PedidoService pedidoService;
    private final CardapioService cardapioService;
    private final PedidoRepository pedidoRepo;

    @Override
    public void run(String... args) {
        log.info("=== Iniciando simulação de pedidos ===");

        // 1) clientes demo
        Cliente joao = clienteRepo.save(Cliente.builder()
                .nome("João Silva").telefone("44999999999")
                .endereco("Rua A, 123").latitude(-23.31).longitude(-51.16).build());

        Cliente maria = clienteRepo.save(Cliente.builder()
                .nome("Maria Souza").telefone("44988887777")
                .endereco("Rua B, 456").build());

        // 2) marmita PADRÃO do DIA para João (usa o 'hoje' da service)
        DiaSemana hoje = cardapioService.hoje();
        Pedido pedidoPadrao = pedidoService.criarMarmitaPadrao(joao.getId(), hoje);
        // recarrega com fetch-join para imprimir fora de transação
        pedidoPadrao = pedidoRepo.findByIdWithItensAndCategoria(pedidoPadrao.getId()).orElseThrow();
        log.info("[PADRAO] Pedido {} criado para {} (dia={}):", pedidoPadrao.getId(), joao.getNome(), hoje);
        imprimirResumoPedido(pedidoPadrao);

        // 3) pedido LIVRE para Maria
        Pedido livre = pedidoService.criarPedidoLivre(maria.getId());
        // Bases + acompanhamentos fixos
        pedidoService.adicionarItemPorNome(livre.getId(), "Arroz", 1);
        pedidoService.adicionarItemPorNome(livre.getId(), "Feijão", 1);
        pedidoService.adicionarItemPorNome(livre.getId(), "Ovo frito", 1);
        pedidoService.adicionarItemPorNome(livre.getId(), "Salada", 1);
        // Misturas (duas no máximo)
        pedidoService.adicionarItemPorNome(livre.getId(), "Bife frito", 1);
        pedidoService.adicionarItemPorNome(livre.getId(), "Filé de tilápia frita", 1);

        // Tentativa de 3ª mistura (deve falhar e ser tratada)
        try {
            pedidoService.adicionarItemPorNome(livre.getId(), "Frango frito", 1);
        } catch (Exception e) {
            log.warn("Esperado: não adicionou terceira mistura. Motivo: {}", e.getMessage());
        }

        // recarrega com fetch-join para impressão
        livre = pedidoRepo.findByIdWithItensAndCategoria(livre.getId()).orElseThrow();
        log.info("[LIVRE] Pedido {} criado para {}:", livre.getId(), maria.getNome());
        imprimirResumoPedido(livre);

        log.info("=== Fim da simulação de pedidos ===");
    }

    private void imprimirResumoPedido(Pedido p) {
        log.info("  Tipo: {}, Status: {}", p.getTipo(), p.getStatus());
        p.getItens().forEach(pi ->
                log.info("   - {} ({}), qtd={}",
                        pi.getItem().getNome(),
                        pi.getItem().getCategoria().getNome(),
                        pi.getQuantidade()));
    }
}
