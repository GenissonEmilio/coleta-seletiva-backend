package br.edu.ifs.web1.coletaseletiva.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "solicitacao")
public class SolicitacaoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String protocolo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "morador_id", nullable = false)
    private MoradorModel morador;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ponto_id", nullable = false)
    private PontoModel ponto;

    /** De qual cadastro veio a cópia. Os campos end* abaixo são a cópia em si (RN04). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "endereco_id", nullable = false)
    private EnderecoModel endereco;

    @Column(nullable = false, length = 150)
    private String endLogradouro;

    @Column(length = 10)
    private String endNumero;

    @Column(nullable = false)
    private boolean endSemNumero;

    @Column(nullable = false, length = 80)
    private String endBairro;

    @Column(nullable = false, length = 80)
    private String endMunicipio;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 2)
    private String endUf;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 8)
    private String endCep;

    @Column(length = 80)
    private String endComplemento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoSolicitacao situacao = SituacaoSolicitacao.SOLICITADA;

    @Column(columnDefinition = "text")
    private String observacoes;

    @Column(nullable = false)
    private OffsetDateTime criadaEm;

    private OffsetDateTime agendadaPara;

    private OffsetDateTime concluidaEm;

    /** Concorrência otimista (RN06): o Hibernate soma 1 a cada alteração e recusa escrita desatualizada. */
    @Version
    @Column(nullable = false)
    private Long versao;

    @OneToMany(mappedBy = "solicitacao", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemSolicitacaoModel> itens = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }

    public MoradorModel getMorador() {
        return morador;
    }

    public void setMorador(MoradorModel morador) {
        this.morador = morador;
    }

    public PontoModel getPonto() {
        return ponto;
    }

    public void setPonto(PontoModel ponto) {
        this.ponto = ponto;
    }

    public EnderecoModel getEndereco() {
        return endereco;
    }

    /** Guarda a referência e copia os dados do endereço como estão agora. */
    public void copiarEndereco(EnderecoModel origem) {
        this.endereco = origem;
        this.endLogradouro = origem.getLogradouro();
        this.endNumero = origem.getNumero();
        this.endSemNumero = origem.isSemNumero();
        this.endBairro = origem.getBairro();
        this.endMunicipio = origem.getMunicipio();
        this.endUf = origem.getUf();
        this.endCep = origem.getCep();
        this.endComplemento = origem.getComplemento();
    }

    public String getEndLogradouro() {
        return endLogradouro;
    }

    public String getEndNumero() {
        return endNumero;
    }

    public boolean isEndSemNumero() {
        return endSemNumero;
    }

    public String getEndBairro() {
        return endBairro;
    }

    public String getEndMunicipio() {
        return endMunicipio;
    }

    public String getEndUf() {
        return endUf;
    }

    public String getEndCep() {
        return endCep;
    }

    public String getEndComplemento() {
        return endComplemento;
    }

    public SituacaoSolicitacao getSituacao() {
        return situacao;
    }

    public void setSituacao(SituacaoSolicitacao situacao) {
        this.situacao = situacao;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public OffsetDateTime getCriadaEm() {
        return criadaEm;
    }

    public void setCriadaEm(OffsetDateTime criadaEm) {
        this.criadaEm = criadaEm;
    }

    public OffsetDateTime getAgendadaPara() {
        return agendadaPara;
    }

    public void setAgendadaPara(OffsetDateTime agendadaPara) {
        this.agendadaPara = agendadaPara;
    }

    public OffsetDateTime getConcluidaEm() {
        return concluidaEm;
    }

    public void setConcluidaEm(OffsetDateTime concluidaEm) {
        this.concluidaEm = concluidaEm;
    }

    public Long getVersao() {
        return versao;
    }

    public List<ItemSolicitacaoModel> getItens() {
        return itens;
    }
}
