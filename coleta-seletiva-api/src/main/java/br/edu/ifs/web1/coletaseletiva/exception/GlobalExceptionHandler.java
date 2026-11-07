package br.edu.ifs.web1.coletaseletiva.exception;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import br.edu.ifs.web1.coletaseletiva.dto.common.ErroCampoResponse;
import br.edu.ifs.web1.coletaseletiva.dto.common.ErroResponse;

/**
 * Tratamento global de erros. Converte qualquer falha no formato {@link ErroResponse}.
 * Estende ResponseEntityExceptionHandler para também padronizar os erros do próprio
 * Spring MVC (rota inexistente, método não permitido, JSON malformado etc.).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Exceções da aplicação ----------

    @ExceptionHandler(EntradaInvalidaException.class)
    public ResponseEntity<Object> entradaInvalida(EntradaInvalidaException ex) {
        return resposta(CodigoErro.ENTRADA_INVALIDA, ex.getMessage(), ex.getErros());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Object> erroNegocio(RegraNegocioException ex) {
        return resposta(ex.getCodigo(), ex.getMessage(), List.of());
    }

    // ---------- Persistência ----------

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Object> conflitoVersao(ObjectOptimisticLockingFailureException ex) {
        return resposta(CodigoErro.CONFLITO_VERSAO,
                "O registro foi alterado por outra operação. Recarregue e tente novamente.", List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> integridade(DataIntegrityViolationException ex) {
        // A causa (que pode conter SQL e nomes de tabela) vai só para o log.
        log.warn("Violação de integridade", ex);
        return resposta(CodigoErro.REGRA_NEGOCIO,
                "A operação viola uma restrição de integridade dos dados.", List.of());
    }

    // ---------- Bean Validation fora do corpo (ex.: @Validated em parâmetros) ----------

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> violacoes(ConstraintViolationException ex) {
        List<ErroCampoResponse> erros = ex.getConstraintViolations().stream()
                .map(v -> new ErroCampoResponse(ultimoNo(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return resposta(CodigoErro.ENTRADA_INVALIDA, "Revise os campos informados.", erros);
    }

    // ---------- Qualquer outra falha ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> inesperado(Exception ex) {
        log.error("Erro inesperado", ex);
        return resposta(CodigoErro.ERRO_INTERNO, "Erro interno. Tente novamente mais tarde.", List.of());
    }

    // ---------- Erros do Spring MVC ----------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErroCampoResponse> erros = new ArrayList<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erros.add(new ErroCampoResponse(e.getField(), e.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(e -> erros.add(new ErroCampoResponse(e.getObjectName(), e.getDefaultMessage())));
        return resposta(CodigoErro.ENTRADA_INVALIDA, "Revise os campos informados.", erros);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErroCampoResponse> erros = new ArrayList<>();
        ex.getParameterValidationResults().forEach(r -> r.getResolvableErrors()
                .forEach(e -> erros.add(new ErroCampoResponse(r.getMethodParameter().getParameterName(),
                        e.getDefaultMessage()))));
        return resposta(CodigoErro.ENTRADA_INVALIDA, "Revise os campos informados.", erros);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        // Não repete a mensagem do parser, que pode expor detalhes internos.
        return resposta(CodigoErro.ENTRADA_INVALIDA,
                "Corpo da requisição ausente ou malformado.", List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return resposta(CodigoErro.ENTRADA_INVALIDA, "Revise os parâmetros informados.",
                List.of(new ErroCampoResponse(ex.getParameterName(), "é obrigatório")));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String nome = ex instanceof MethodArgumentTypeMismatchException parametro
                ? parametro.getName() : ex.getPropertyName();
        String campo = nome != null ? nome : "parametro";
        return resposta(CodigoErro.ENTRADA_INVALIDA, "Revise os parâmetros informados.",
                List.of(new ErroCampoResponse(campo, "valor em formato inválido")));
    }

    /** Demais erros do Spring MVC (404 de rota, 405, 415...), no mesmo formato. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
                                                             HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        int s = statusCode.value();
        CodigoErro codigo;
        String mensagem;
        if (s == HttpStatus.NOT_FOUND.value()) {
            codigo = CodigoErro.ROTA_NAO_ENCONTRADA;
            mensagem = "Recurso não encontrado.";
        } else if (s == HttpStatus.METHOD_NOT_ALLOWED.value()) {
            codigo = CodigoErro.METODO_NAO_PERMITIDO;
            mensagem = "Método HTTP não permitido para este recurso.";
        } else if (s == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
            codigo = CodigoErro.TIPO_NAO_SUPORTADO;
            mensagem = "Tipo de conteúdo não suportado. Use application/json.";
        } else if (s >= 400 && s < 500) {
            codigo = CodigoErro.ENTRADA_INVALIDA;
            mensagem = "Requisição inválida.";
        } else {
            log.error("Erro do framework", ex);
            codigo = CodigoErro.ERRO_INTERNO;
            mensagem = "Erro interno. Tente novamente mais tarde.";
        }
        return ResponseEntity.status(statusCode).headers(headers)
                .body(ErroResponse.de(codigo, mensagem));
    }

    // ---------- Apoio ----------

    private static ResponseEntity<Object> resposta(CodigoErro codigo, String mensagem, List<ErroCampoResponse> erros) {
        return ResponseEntity.status(codigo.getStatus()).body(ErroResponse.de(codigo, mensagem, erros));
    }

    private static String ultimoNo(String caminho) {
        int i = caminho.lastIndexOf('.');
        return i >= 0 ? caminho.substring(i + 1) : caminho;
    }
}
