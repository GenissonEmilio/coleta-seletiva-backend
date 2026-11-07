package br.edu.ifs.web1.coletaseletiva.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** Metadados da documentação OpenAPI (Swagger UI em /swagger-ui.html, JSON em /v3/api-docs). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coletaSeletivaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("API do Sistema de Coleta Seletiva")
                .version("v1")
                .description("Contrato da API. Erros seguem o formato ErroResponse "
                        + "(codigo, mensagem, errosCampos); listagens seguem PaginaResponse. "
                        + "Dados de demonstração fictícios; a seleção de ator não é autenticação."));
    }
}
