package io.github.vmarins2005.tarifas.web;

import io.github.vmarins2005.tarifas.aplicacao.CadastroDeTarifas;
import io.github.vmarins2005.tarifas.dominio.Tarifa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tarifas")
class TarifasController {

    private final CadastroDeTarifas cadastro;

    TarifasController(CadastroDeTarifas cadastro) {
        this.cadastro = cadastro;
    }

    /** O corpo da requisição é um tipo do web, e não o do domínio — ADR 0002. */
    record TarifaPedida(
            @NotBlank String codigo, @NotBlank String descricao, @Positive long valorEmCentavos) {

        Tarifa paraDominio() {
            return new Tarifa(codigo, descricao, valorEmCentavos);
        }
    }

    @PostMapping
    ResponseEntity<Tarifa> cadastrar(@Valid @RequestBody TarifaPedida pedida) {
        var criada = cadastro.cadastrar(pedida.paraDominio());
        return ResponseEntity.created(URI.create("/tarifas/" + criada.codigo())).body(criada);
    }

    @GetMapping("/{codigo}")
    Tarifa buscar(@PathVariable String codigo) {
        return cadastro.buscar(codigo);
    }

    @GetMapping
    List<Tarifa> listar() {
        return cadastro.listar();
    }
}
