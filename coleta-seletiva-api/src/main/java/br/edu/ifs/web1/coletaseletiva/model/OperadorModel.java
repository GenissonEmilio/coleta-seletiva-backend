package br.edu.ifs.web1.coletaseletiva.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "operador")
public class OperadorModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(nullable = false)
    private boolean ativo = true;

    /** Tabela operador_ponto: só as duas chaves, então basta um @ManyToMany. O Set não aceita repetido. */
    @ManyToMany
    @JoinTable(name = "operador_ponto",
            joinColumns = @JoinColumn(name = "operador_id"),
            inverseJoinColumns = @JoinColumn(name = "ponto_id"))
    private Set<PontoModel> pontos = new HashSet<>();

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Set<PontoModel> getPontos() {
        return pontos;
    }
}
