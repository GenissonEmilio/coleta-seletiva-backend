package br.edu.ifs.web1.coletaseletiva.model;

/** Catálogo compartilhado pelo material, pelos limites e pela cópia no item da solicitação. */
public enum UnidadeMaterial {

    KG(true),
    LITRO(true),
    UNIDADE(false);

    private final boolean fracionavel;

    UnidadeMaterial(boolean fracionavel) {
        this.fracionavel = fracionavel;
    }

    public boolean isFracionavel() {
        return fracionavel;
    }
}
