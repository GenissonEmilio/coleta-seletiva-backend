package br.edu.ifs.web1.coletaseletiva.services.material;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import br.edu.ifs.web1.coletaseletiva.exception.EntradaInvalidaException;
import br.edu.ifs.web1.coletaseletiva.model.UnidadeMaterial;

/** Regras da RN02 para estimativas, limites e quantidades efetivas, sem arredondar valores. */
@Service
public class QuantidadeMaterialService {

    public void validarPositiva(BigDecimal quantidade, UnidadeMaterial unidade, String campo) {
        validar(quantidade, unidade, campo, false);
    }

    public void validarNaoNegativa(BigDecimal quantidade, UnidadeMaterial unidade, String campo) {
        validar(quantidade, unidade, campo, true);
    }

    private void validar(BigDecimal quantidade, UnidadeMaterial unidade, String campo, boolean permiteZero) {
        if (quantidade == null) {
            throw new EntradaInvalidaException(campo, "é obrigatório");
        }
        if (quantidade.signum() < 0 || (!permiteZero && quantidade.signum() == 0)) {
            throw new EntradaInvalidaException(campo,
                    permiteZero ? "deve ser maior ou igual a zero" : "deve ser maior que zero");
        }
        BigDecimal valor = quantidade.stripTrailingZeros();
        if (!unidade.isFracionavel() && valor.scale() > 0) {
            throw new EntradaInvalidaException(campo, "deve ser inteira para UNIDADE");
        }
        if (valor.scale() > 3 || valor.precision() - valor.scale() > 9) {
            throw new EntradaInvalidaException(campo, "deve ter no máximo 9 dígitos inteiros e 3 casas decimais");
        }
    }
}
