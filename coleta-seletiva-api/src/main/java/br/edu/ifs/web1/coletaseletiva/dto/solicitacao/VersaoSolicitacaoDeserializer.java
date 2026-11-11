package br.edu.ifs.web1.coletaseletiva.dto.solicitacao;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** Exige um inteiro JSON para que, por exemplo, 0.5 não seja convertido em 0. */
public class VersaoSolicitacaoDeserializer extends ValueDeserializer<Long> {

    @Override
    public Long deserialize(JsonParser parser, DeserializationContext contexto) {
        if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return contexto.reportInputMismatch(Long.class, "A versão deve ser um número inteiro.");
        }
        return parser.getLongValue();
    }
}
