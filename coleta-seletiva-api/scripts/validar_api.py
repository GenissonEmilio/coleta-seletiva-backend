"""Executa a suíte PostgreSQL e, opcionalmente, os oito cenários de aceite da API."""

import argparse
from contextlib import contextmanager
from datetime import datetime, timedelta, timezone
from decimal import Decimal
from functools import partial
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import tempfile
import time
from urllib.error import HTTPError
from urllib.parse import urlencode
from urllib.request import ProxyHandler, Request, build_opener
from xml.etree import ElementTree

PROJETO = Path(__file__).resolve().parents[1]
USUARIO = "coleta_validacao"
BANCO_TESTES = "coleta_testes"
BANCO_ACEITE = "coleta_aceite"
HTTP = build_opener(ProxyHandler({}))
ADMIN = {"X-Ator-Tipo": "ADMINISTRADOR"}


def verificar(condicao, mensagem):
    if not condicao:
        raise RuntimeError(mensagem)


def requisitar(base, metodo, caminho, esperado, corpo=None, ator=None):
    cabecalhos = dict(ator or {})
    dados = None
    if corpo is not None:
        cabecalhos["Content-Type"] = "application/json"
        dados = json.dumps(corpo).encode("utf-8")
    requisicao = Request(base + caminho, data=dados, headers=cabecalhos, method=metodo)
    try:
        resposta = HTTP.open(requisicao, timeout=15)
    except HTTPError as erro:
        resposta = erro
    with resposta:
        conteudo = resposta.read()
        verificar(resposta.status == esperado,
                  f"{metodo} {caminho} retornou {resposta.status}, esperado {esperado}: "
                  f"{conteudo.decode('utf-8', errors='replace')[:500]}")
    return json.loads(conteudo, parse_float=Decimal) if conteudo else None


@contextmanager
def postgres():
    nome = "coleta-validacao-" + secrets.token_hex(4)
    senha = secrets.token_urlsafe(24)
    ambiente = dict(os.environ, POSTGRES_USER=USUARIO, POSTGRES_DB=BANCO_TESTES, POSTGRES_PASSWORD=senha)
    subprocess.run([
        "docker", "run", "--rm", "-d", "--name", nome, "--label", "org.coletaseletiva.validacao=true",
        "--memory=512m", "--memory-swap=512m", "--cpus=1", "--pids-limit=128",
        "--tmpfs", "/var/lib/postgresql/data:rw,size=256m", "-p", "127.0.0.1::5432",
        "-e", "POSTGRES_USER", "-e", "POSTGRES_DB", "-e", "POSTGRES_PASSWORD", "postgres:17",
    ], env=ambiente, check=True, stdout=subprocess.DEVNULL)
    try:
        for _ in range(45):
            pronto = subprocess.run(["docker", "exec", nome, "pg_isready", "-h", "127.0.0.1",
                                     "-U", USUARIO, "-d", BANCO_TESTES],
                                    stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if pronto.returncode == 0:
                break
            time.sleep(1)
        else:
            raise RuntimeError("O PostgreSQL descartável não iniciou no prazo.")
        porta = subprocess.check_output(["docker", "port", nome, "5432/tcp"], text=True).strip().rsplit(":", 1)[1]
        yield nome, porta, senha
    finally:
        subprocess.run(["docker", "stop", nome], check=True, stdout=subprocess.DEVNULL)


def ambiente_spring(porta, senha, banco):
    # Evita que as configurações locais da IDE ou de outro projeto escolham o banco dos testes.
    ignoradas = {"JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "MAVEN_ARGS", "MAVEN_OPTS"}
    ambiente = {k: v for k, v in os.environ.items()
                if not k.startswith(("SPRING_", "SERVER_")) and k not in ignoradas}
    ambiente.update(SPRING_DATASOURCE_URL=f"jdbc:postgresql://127.0.0.1:{porta}/{banco}",
                    SPRING_DATASOURCE_USERNAME=USUARIO, SPRING_DATASOURCE_PASSWORD=senha,
                    MAVEN_OPTS="-Xmx384m -XX:ActiveProcessorCount=2")
    return ambiente


def caminho_jar():
    pom = ElementTree.parse(PROJETO / "pom.xml")
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    artefato = pom.findtext("m:artifactId", namespaces=ns)
    versao = pom.findtext("m:version", namespaces=ns)
    return PROJETO / "target" / f"{artefato}-{versao}.jar"


@contextmanager
def servidor(jar, ambiente, pasta):
    log = pasta / "aplicacao.log"
    with log.open("w") as saida:
        processo = subprocess.Popen([
            "java", "-Xmx448m", "-XX:ActiveProcessorCount=2", "-jar", str(jar),
            "--server.address=127.0.0.1", "--server.port=0", "--spring.profiles.active=default",
        ], env=ambiente, cwd=pasta, stdout=saida, stderr=subprocess.STDOUT)
        try:
            for _ in range(120):
                texto = log.read_text(errors="replace")
                porta = re.search(r"Tomcat started on port (\d+)", texto)
                if porta and "Started ColetaSeletivaApiApplication in " in texto:
                    break
                if processo.poll() is not None:
                    raise RuntimeError("A aplicação encerrou durante a inicialização.\n" + texto[-2500:])
                time.sleep(0.5)
            else:
                raise RuntimeError("A aplicação não iniciou no prazo.\n" + texto[-2500:])
            yield f"http://127.0.0.1:{porta.group(1)}", processo.pid
        finally:
            if processo.poll() is None:
                processo.terminate()
                try:
                    processo.wait(timeout=12)
                except subprocess.TimeoutExpired:
                    processo.kill()
                    processo.wait()


def linhas_do_banco(container):
    return subprocess.check_output([
        "docker", "exec", container, "psql", "-U", USUARIO, "-d", BANCO_ACEITE, "-At", "-c",
        "SELECT (SELECT count(*) FROM solicitacao), (SELECT count(*) FROM item_solicitacao), "
        "(SELECT count(*) FROM historico_solicitacao)",
    ], text=True).strip()


def resultado(numero, texto):
    print(f"Cenário {numero}/8 — {texto}", flush=True)


def executar_fluxo(api, container):
    endereco = {"logradouro": "Rua do Ensaio", "numero": "10", "bairro": "Centro",
                "municipio": "Aracaju", "uf": "SE", "cep": "49000000"}
    ponto = api("POST", "/pontos", 201,
                dict(endereco, nome="Ecoponto de aceite", recolhimentoDomiciliar=True))["id"]
    papel = api("POST", "/materiais", 201, {"nome": "Papel", "unidade": "KG"})["id"]
    plastico = api("POST", "/materiais", 201, {"nome": "Plástico", "unidade": "UNIDADE"})["id"]
    vidro = api("POST", "/materiais", 201, {"nome": "Vidro", "unidade": "KG"})["id"]
    for material in (papel, plastico):
        api("PUT", f"/pontos/{ponto}/materiais/{material}", 201, {})
        filtro = urlencode({"municipio": "Aracaju", "materialId": material})
        encontrados = api("GET", f"/publico/pontos?{filtro}", 200)["content"]
        verificar([item["id"] for item in encontrados] == [ponto], "Filtro público retornou outro ponto.")
    filtro = urlencode({"municipio": "Lagarto", "materialId": papel})
    verificar(not api("GET", f"/publico/pontos?{filtro}", 200)["content"],
              "O filtro por município aceitou um ponto de outra cidade.")
    resultado(1, "OK | ponto com papel e plástico encontrado por município e material.")

    morador_id = api("POST", "/moradores", 201,
                     {"nome": "Morador de aceite", "email": "morador.aceite@exemplo.com"})["id"]
    endereco_id = api("POST", f"/moradores/{morador_id}/enderecos", 201, endereco)["id"]
    operador_id = api("POST", "/operadores", 201,
                      {"nome": "Operador de aceite", "email": "operador.aceite@exemplo.com"}, ADMIN)["id"]
    api("PUT", f"/operadores/{operador_id}/pontos/{ponto}", 201, ator=ADMIN)
    morador = {"X-Ator-Tipo": "MORADOR", "X-Ator-Id": str(morador_id)}
    operador = {"X-Ator-Tipo": "OPERADOR", "X-Ator-Id": str(operador_id)}
    itens = [{"materialId": papel, "quantidadeEstimada": 4.5},
             {"materialId": plastico, "quantidadeEstimada": 6}]

    def solicitar(materiais, esperado=201):
        return api("POST", "/solicitacoes", esperado,
                   {"enderecoId": endereco_id, "pontoId": ponto, "itens": materiais}, morador)

    def detalhe(solicitacao):
        return api("GET", f"/solicitacoes/{solicitacao['id']}", 200, ator=morador)

    def acao(solicitacao, nome, corpo=None, esperado=200, ator=operador):
        return api("POST", f"/solicitacoes/{solicitacao['id']}/{nome}", esperado,
                   dict(corpo or {}, versao=solicitacao["versao"]), ator)

    def situacoes(solicitacao):
        return [evento["situacaoNova"] for evento in solicitacao["historico"]]

    primeira = solicitar(itens)
    para_cancelar = solicitar(itens)
    gravada = detalhe(primeira)
    verificar(primeira["protocolo"] and primeira["protocolo"] != para_cancelar["protocolo"]
              and gravada["protocolo"] == primeira["protocolo"]
              and {item["materialId"] for item in gravada["itens"]} == {papel, plastico}
              and situacoes(gravada) == ["SOLICITADA"] and linhas_do_banco(container) == "2|4|2",
              "A criação não persistiu protocolos distintos, dois itens por solicitação e histórico inicial.")
    resultado(2, "OK | HTTP 201, protocolos distintos e banco com 2 solicitações, 4 itens e 2 eventos.")

    antes = linhas_do_banco(container)
    erro = solicitar([itens[0], {"materialId": vidro, "quantidadeEstimada": 1}], 409)
    depois = linhas_do_banco(container)
    verificar(erro["codigo"] == "MATERIAL_INCOMPATIVEL" and depois == antes,
              f"A incompatibilidade deixou gravação parcial ou outro código. Antes {antes}, depois {depois}.")
    resultado(3, f"OK | 409 MATERIAL_INCOMPATIVEL, contagens do banco {antes} → {depois}.")

    agora = datetime.now(timezone.utc).replace(microsecond=0)
    passada = (agora - timedelta(days=1)).isoformat()
    agenda = (agora + timedelta(days=1)).isoformat()
    outra_data = (agora + timedelta(days=2)).isoformat()
    erro = acao(primeira, "agendamento", {"agendadaPara": passada}, 400)
    verificar(erro["codigo"] == "ENTRADA_INVALIDA"
              and any(campo["campo"] == "agendadaPara" for campo in erro["errosCampos"])
              and detalhe(primeira) == gravada,
              "Agendamento passado não preservou os dados ou não informou o campo.")
    resultado(4, "OK | 400 no campo agendadaPara, SOLICITADA na versão 0 e histórico inicial.")

    primeira = acao(primeira, "agendamento", {"agendadaPara": agenda})
    primeira = acao(primeira, "inicio")
    primeira = acao(primeira, "conclusao", {"itens": [
        {"materialId": papel, "quantidadeEfetiva": 2.5},
        {"materialId": plastico, "quantidadeEfetiva": 3},
    ]})
    primeira = detalhe(primeira)
    verificar(situacoes(primeira) == ["SOLICITADA", "AGENDADA", "EM_RECOLHIMENTO", "CONCLUIDA"]
              and primeira["versao"] == 3 and primeira["concluidaEm"]
              and primeira["historico"][-1]["responsavelId"] == operador_id
              and {item["materialId"]: item["quantidadeEfetiva"] for item in primeira["itens"]}
              == {papel: Decimal("2.5"), plastico: 3},
              "Execução não persistiu efetivos, operador, data ou histórico completo.")
    resultado(5, "OK | CONCLUIDA na versão 3, papel 2,5 KG, plástico 3 UNIDADE e quatro eventos.")

    para_cancelar = acao(para_cancelar, "agendamento", {"agendadaPara": agenda})
    cancelada = acao(para_cancelar, "cancelamento", {"motivo": "Morador desistiu"}, ator=morador)
    for nome, corpo in (("inicio", {}), ("conclusao", {"itens": [
        {"materialId": papel, "quantidadeEfetiva": 1}, {"materialId": plastico, "quantidadeEfetiva": 0},
    ]})):
        verificar(acao(cancelada, nome, corpo, 409)["codigo"] == "TRANSICAO_INVALIDA",
                  f"{nome} aceitou uma solicitação cancelada.")
    cancelada = detalhe(cancelada)
    verificar(situacoes(cancelada) == ["SOLICITADA", "AGENDADA", "CANCELADA"] and cancelada["versao"] == 2,
              "A solicitação cancelada recebeu alteração indevida.")
    resultado(6, "OK | CANCELADA na versão 2, início e conclusão rejeitados com 409.")

    api("PUT", f"/materiais/{papel}", 200, {"nome": "Papel", "unidade": "LITRO"})
    nova = solicitar(itens)
    nova = acao(nova, "agendamento", {"agendadaPara": agenda})
    nova = acao(nova, "inicio")
    nova = acao(nova, "tentativa-sem-sucesso", {"agendadaPara": outra_data, "motivo": "Portão fechado"})
    nova = acao(nova, "reagendamento", {
        "agendadaPara": (agora + timedelta(days=3)).isoformat(), "motivo": "Disponibilidade do morador",
    })
    eventos = nova["historico"]
    verificar(situacoes(nova) == ["SOLICITADA", "AGENDADA", "EM_RECOLHIMENTO", "AGENDADA", "AGENDADA"]
              and nova["versao"] == 4 and len({eventos[i]["agendamentoNovo"] for i in (1, 3, 4)}) == 3
              and eventos[3]["situacaoAnterior"] == "EM_RECOLHIMENTO"
              and eventos[3]["responsavelId"] == operador_id and eventos[3]["motivo"] == "Portão fechado"
              and eventos[4]["motivo"] == "Disponibilidade do morador"
              and nova["agendadaPara"] == eventos[4]["agendamentoNovo"],
              "A tentativa não preservou o primeiro horário, o operador, o motivo e a nova data.")
    resultado(7, "OK | AGENDADA na versão 4 após a falha e o reagendamento, com três horários no histórico.")

    nova = acao(nova, "inicio")
    nova = acao(nova, "conclusao", {"itens": [
        {"materialId": papel, "quantidadeEfetiva": 1.25}, {"materialId": plastico, "quantidadeEfetiva": 0},
    ]})
    return [detalhe(primeira), detalhe(cancelada), detalhe(nova)], morador, papel, plastico, agora


def conferir_reinicio(api, registros, morador, papel, plastico, agora):
    for registro in registros:
        relido = api("GET", f"/solicitacoes/{registro['id']}", 200, ator=morador)
        verificar(relido == registro, f"A solicitação {registro['id']} mudou após reiniciar a aplicação.")
    filtro = urlencode({"inicio": (agora - timedelta(minutes=5)).isoformat(),
                        "fim": (agora + timedelta(minutes=15)).isoformat()})
    recolhimentos = api("GET", f"/indicadores/recolhimentos?{filtro}", 200, ator=ADMIN)
    totais = {(linha["materialId"], linha["unidade"]): linha["quantidadeEfetiva"]
              for linha in recolhimentos["content"]}
    verificar(recolhimentos["totalElements"] == 3
              and totais == {(papel, "KG"): Decimal("2.5"), (papel, "LITRO"): Decimal("1.25"),
                             (plastico, "UNIDADE"): 3}, f"Indicadores de recolhimento incorretos: {totais}.")
    situacoes = api("GET", f"/indicadores/solicitacoes?{filtro}", 200, ator=ADMIN)
    verificar({linha["situacao"]: linha["total"] for linha in situacoes} ==
              {"SOLICITADA": 0, "AGENDADA": 0, "RECUSADA": 0, "CANCELADA": 1,
               "EM_RECOLHIMENTO": 0, "CONCLUIDA": 2}, "Contagem por situação incorreta após o reinício.")


def aceite(container, porta, senha):
    subprocess.run(["docker", "exec", container, "createdb", "-w", "-U", USUARIO, BANCO_ACEITE], check=True)
    ambiente = ambiente_spring(porta, senha, BANCO_ACEITE)
    jar = caminho_jar()
    with tempfile.TemporaryDirectory(prefix="coleta-aceite-") as diretorio:
        pasta = Path(diretorio)
        with servidor(jar, ambiente, pasta) as (base, pid_inicial):
            rotas = requisitar(base, "GET", "/v3/api-docs", 200)["paths"]
            verificar("/api/v1/solicitacoes/{id}/conclusao" in rotas and "/api/v1/indicadores/recolhimentos" in rotas,
                      "O roteiro de aceite requer a API-13 e a API-16 integradas no checkout.")
            dados = executar_fluxo(partial(requisitar, base + "/api/v1"), container)
        with servidor(jar, ambiente, pasta) as (base, pid_reinicio):
            verificar(pid_reinicio != pid_inicial, "O segundo servidor precisa ser um novo processo.")
            conferir_reinicio(partial(requisitar, base + "/api/v1"), *dados)
            resultado(8, f"OK | PID {pid_inicial} → {pid_reinicio}, três solicitações relidas. "
                      "Papel 2,5 KG e 1,25 LITRO, plástico 3 UNIDADE. Duas concluídas e uma cancelada.")
    print("Aceite concluído: 8 de 8 cenários aprovados.", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--aceite", action="store_true", help="gera o JAR e executa também o roteiro HTTP com reinício")
    argumentos = parser.parse_args()
    with postgres() as (container, porta, senha):
        print("Executando a suíte com PostgreSQL 17 descartável.", flush=True)
        subprocess.run([
            "sh", "./mvnw", "-B", "-ntp", "-T", "1", "package" if argumentos.aceite else "test",
            "-DargLine=-Xmx448m -XX:ActiveProcessorCount=2", "-DforkCount=1", "-DreuseForks=true",
        ], cwd=PROJETO, env=ambiente_spring(porta, senha, BANCO_TESTES), check=True)
        if argumentos.aceite:
            aceite(container, porta, senha)


if __name__ == "__main__":
    main()
