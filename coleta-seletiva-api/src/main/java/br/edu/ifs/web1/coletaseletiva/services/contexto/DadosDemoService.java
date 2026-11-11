package br.edu.ifs.web1.coletaseletiva.services.contexto;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.ifs.web1.coletaseletiva.dto.contexto.ContextoResponse;
import br.edu.ifs.web1.coletaseletiva.model.MoradorModel;
import br.edu.ifs.web1.coletaseletiva.model.OperadorModel;
import br.edu.ifs.web1.coletaseletiva.model.PontoModel;
import br.edu.ifs.web1.coletaseletiva.repository.MoradorRepository;
import br.edu.ifs.web1.coletaseletiva.repository.OperadorRepository;
import br.edu.ifs.web1.coletaseletiva.repository.PontoRepository;

/**
 * Só existe no perfil local "demo". Ao subir, cria atores e pontos fictícios (uma vez só)
 * e informa quais atores o front pode selecionar. Todos os dados são inventados.
 */
@Service
@Profile("demo")
public class DadosDemoService implements ApplicationRunner {

    static final String EMAIL_MORADORA = "maria@demo.exemplo.org";
    static final String EMAIL_OPERADORA_A = "ana@demo.exemplo.org";
    static final String EMAIL_OPERADOR_B = "bruno@demo.exemplo.org";

    private final MoradorRepository moradorRepository;
    private final OperadorRepository operadorRepository;
    private final PontoRepository pontoRepository;

    public DadosDemoService(MoradorRepository moradorRepository, OperadorRepository operadorRepository,
            PontoRepository pontoRepository) {
        this.moradorRepository = moradorRepository;
        this.operadorRepository = operadorRepository;
        this.pontoRepository = pontoRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (operadorRepository.existsByEmail(EMAIL_OPERADORA_A)) {
            return;
        }
        MoradorModel moradora = new MoradorModel();
        moradora.setNome("Maria Demonstração");
        moradora.setEmail(EMAIL_MORADORA);
        moradorRepository.save(moradora);
        criarOperador("Ana Operadora (ponto A)", EMAIL_OPERADORA_A, criarPonto("Ecoponto Demo A", "Centro"));
        criarOperador("Bruno Operador (ponto B)", EMAIL_OPERADOR_B, criarPonto("Ecoponto Demo B", "Norte"));
    }

    @Transactional(readOnly = true)
    public List<ContextoResponse> listarAtores() {
        List<ContextoResponse> atores = new ArrayList<>();
        atores.add(new ContextoResponse(TipoAtor.ADMINISTRADOR, null, "Administrador", List.of()));
        moradorRepository.findByEmail(EMAIL_MORADORA).ifPresent(morador -> atores.add(
                new ContextoResponse(TipoAtor.MORADOR, morador.getId(), morador.getNome(), List.of())));
        for (String email : List.of(EMAIL_OPERADORA_A, EMAIL_OPERADOR_B)) {
            operadorRepository.findByEmail(email).ifPresent(operador -> atores.add(new ContextoResponse(
                    TipoAtor.OPERADOR, operador.getId(), operador.getNome(),
                    operador.getPontos().stream().map(PontoModel::getId).sorted().toList())));
        }
        return atores;
    }

    private PontoModel criarPonto(String nome, String bairro) {
        PontoModel ponto = new PontoModel();
        ponto.setNome(nome);
        ponto.setLogradouro("Rua Fictícia");
        ponto.setNumero("100");
        ponto.setBairro(bairro);
        ponto.setMunicipio("Cidade Demonstração");
        ponto.setUf("SE");
        ponto.setCep("49000000");
        ponto.setContato("contato@demo.exemplo.org");
        ponto.setHorarios("Seg a sex, 8h às 17h");
        ponto.setRecolhimentoDomiciliar(true);
        return pontoRepository.save(ponto);
    }

    private void criarOperador(String nome, String email, PontoModel ponto) {
        OperadorModel operador = new OperadorModel();
        operador.setNome(nome);
        operador.setEmail(email);
        operador.getPontos().add(ponto);
        operadorRepository.save(operador);
    }
}
