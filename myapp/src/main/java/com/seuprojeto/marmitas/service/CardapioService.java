package com.seuprojeto.marmitas.service;

import com.seuprojeto.marmitas.entity.*;
import com.seuprojeto.marmitas.repository.DisponibilidadeRepository;
import com.seuprojeto.marmitas.repository.ItemCardapioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardapioService {

    private final ItemCardapioRepository itemRepo;
    private final DisponibilidadeRepository dispRepo;

    public DiaSemana hoje() {
        DayOfWeek dow = LocalDate.now().getDayOfWeek(); // MON..SUN
        return switch (dow) {
            case MONDAY -> DiaSemana.SEGUNDA;
            case TUESDAY -> DiaSemana.TERCA;
            case WEDNESDAY -> DiaSemana.QUARTA;
            case THURSDAY -> DiaSemana.QUINTA;
            case FRIDAY -> DiaSemana.SEXTA;
            case SATURDAY -> DiaSemana.SABADO;
            case SUNDAY -> DiaSemana.DOMINGO;
        };
    }

    public List<ItemCardapio> cardapioDoDia(DiaSemana dia) {
        // fixos ativos
        List<ItemCardapio> base = itemRepo.findBySempreDisponivelTrueAndAtivoTrue();

        // variáveis do dia
        List<ItemCardapio> variaveis = dispRepo.findByDia(dia).stream()
                .map(Disponibilidade::getItem)
                .filter(ItemCardapio::isAtivo)
                .collect(Collectors.toList());

        // junta e ordena por categoria/nome
        List<ItemCardapio> all = new ArrayList<>();
        all.addAll(base);
        all.addAll(variaveis);

        all.sort(Comparator.comparing((ItemCardapio i) -> i.getCategoria().getNome())
                           .thenComparing(ItemCardapio::getNome));
        return all;
        }
}
