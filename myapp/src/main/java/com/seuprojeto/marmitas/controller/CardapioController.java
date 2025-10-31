package com.seuprojeto.marmitas.controller;

import com.seuprojeto.marmitas.entity.DiaSemana;
import com.seuprojeto.marmitas.entity.Disponibilidade;
import com.seuprojeto.marmitas.entity.ItemCardapio;
import com.seuprojeto.marmitas.repository.DisponibilidadeRepository;
import com.seuprojeto.marmitas.service.CardapioService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/cardapio")
public class CardapioController {

    private final CardapioService cardapioService;
    private final DisponibilidadeRepository dispRepo;

    /**
     * GET /cardapio/hoje
     * Retorna APENAS uma lista (array) de itens ativos do dia.
     * Esse formato é o que o seu index.html espera.
     */
    @GetMapping("/hoje")
    public List<ItemDTO> hoje() {
        DiaSemana dia = cardapioService.hoje();
        List<ItemDTO> itens = dispRepo.findByDia(dia).stream()
                .map(Disponibilidade::getItem)
                .filter(ItemCardapio::isAtivo)
                .map(ItemDTO::of)
                .toList();

        if (dia == DiaSemana.DOMINGO && itens.isEmpty()) {
            // usar QUARTA como placeholder
            itens = dispRepo.findByDia(DiaSemana.QUARTA).stream()
                    .map(Disponibilidade::getItem)
                    .filter(ItemCardapio::isAtivo)
                    .map(ItemDTO::of)
                    .toList();
        }
        return itens;
    }


    /**
     * GET /cardapio/dia/{dia}
     * Ex.: /cardapio/dia/SEGUNDA
     */
    @GetMapping("/dia/{dia}")
    public List<ItemDTO> porDia(@PathVariable("dia") DiaSemana dia) {
        return dispRepo.findByDia(dia).stream()
                .map(Disponibilidade::getItem)
                .filter(ItemCardapio::isAtivo)
                .map(ItemDTO::of)
                .toList();
    }

    // ===== DTO usado no front =====
    @Data
    @AllArgsConstructor
    static class ItemDTO {
        private Long id;
        private String nome;
        private String descricao;
        private String categoria;      // Base, Mistura, Acompanhamento, Salada
        private boolean sempreDisponivel;
        private boolean ativo;
        private String substitui;      // ARROZ/FEIJAO/NENHUM
        private String imagemUrl;

        static ItemDTO of(ItemCardapio i) {
            return new ItemDTO(
                    i.getId(),
                    i.getNome(),
                    i.getDescricao(),
                    i.getCategoria().getNome(),
                    i.isSempreDisponivel(),
                    i.isAtivo(),
                    i.getSubstitui().name(),
                    i.getImagemUrl()
            );
        }
    }
}
