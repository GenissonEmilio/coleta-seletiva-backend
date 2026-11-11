"""Mede consultas HTTP e confere o reinício da API com 1.000 solicitações fictícias."""

import argparse
from decimal import Decimal
import math
from pathlib import Path
import subprocess
import tempfile
from time import perf_counter
from urllib.parse import urlencode

try:
    from validar_api import (ADMIN, PROJETO, USUARIO, ambiente_spring, caminho_jar,
                             postgres, requisitar, servidor, verificar)
except ModuleNotFoundError as erro:
    if erro.name != "validar_api":
        raise
    raise SystemExit("Integre a API-17 ao checkout para reutilizar o ambiente de validação.") from erro

BANCO = "coleta_desempenho"
PERIODO = {"inicio": "2026-09-08T00:00:00Z", "fim": "2026-10-01T00:00:00Z"}
AMOSTRAS = 30
AQUECIMENTO = 5
META_MS = 2000


def psql(container, *argumentos, **opcoes):
    return subprocess.run([
        "docker", "exec", "-i", container, "psql", "-X", "-U", USUARIO, "-d", BANCO,
        "-v", "ON_ERROR_STOP=1", *argumentos,
    ], check=True, **opcoes)


def carregar_dados(container):
    with Path(__file__).with_name("dados_desempenho.sql").open() as arquivo:
        psql(container, "-f", "-", stdin=arquivo, stdout=subprocess.DEVNULL)
    resultado = psql(container, "-At", "-c",
                     "SELECT count(*), min(id) FILTER (WHERE protocolo = 'DEMO-0006'), "
                     "(SELECT count(*) FROM item_solicitacao), "
                     "(SELECT count(*) FROM historico_solicitacao) FROM solicitacao",
                     capture_output=True, text=True).stdout.strip()
    quantidade, exemplo, itens, eventos = map(int, resultado.split("|"))
    verificar((quantidade, itens, eventos) == (1000, 2000, 2331), f"Carga inesperada: {resultado}")
    print(f"Carga: {quantidade} solicitações, {itens} itens e {eventos} eventos.", flush=True)
    return exemplo


def consulta(base, caminho, parametros=None, ator=ADMIN):
    sufixo = "?" + urlencode(parametros) if parametros else ""
    return requisitar(base, "GET", "/api/v1" + caminho + sufixo, 200, ator=ator)


def conferir(base, exemplo):
    detalhe = consulta(base, f"/solicitacoes/{exemplo}")
    verificar(detalhe["protocolo"] == "DEMO-0006" and detalhe["versao"] == 3
              and [evento["situacaoNova"] for evento in detalhe["historico"]] ==
              ["SOLICITADA", "AGENDADA", "EM_RECOLHIMENTO", "CONCLUIDA"],
              "A solicitação de exemplo perdeu a versão ou a sequência do histórico.")
    pagina = consulta(base, "/solicitacoes", {"size": 1})
    verificar(pagina["totalElements"] == 1000, "A API precisa listar as 1.000 solicitações.")
    situacoes = consulta(base, "/indicadores/solicitacoes")
    verificar({linha["situacao"]: linha["total"] for linha in situacoes} ==
              {"SOLICITADA": 167, "AGENDADA": 167, "RECUSADA": 167,
               "CANCELADA": 167, "EM_RECOLHIMENTO": 166, "CONCLUIDA": 166},
              "As contagens por situação divergiram da carga.")
    recolhimentos = consulta(base, "/indicadores/recolhimentos")["content"]
    verificar({(linha["nomeMaterial"], linha["unidade"]): linha["quantidadeEfetiva"]
               for linha in recolhimentos} ==
              {("Papel", "KG"): 415, ("Plástico", "UNIDADE"): 166, ("Óleo", "LITRO"): Decimal("103.75")},
              "Os totais efetivos por material e unidade divergiram da carga.")
    return detalhe, pagina, situacoes, recolhimentos


def p95(tempos):
    return sorted(tempos)[math.ceil(len(tempos) * 0.95) - 1]


def medir(base, exemplo):
    detalhe = consulta(base, f"/solicitacoes/{exemplo}")
    ponto = detalhe["pontoId"]
    material = detalhe["itens"][0]["materialId"]
    consultas = [
        ("Pontos por município e material", "/publico/pontos",
         {"municipio": "Cidade Demonstração", "materialId": material, "page": 1, "size": 1}, None),
        ("Solicitações por período, situação e ponto", "/solicitacoes",
         dict(PERIODO, situacao="CONCLUIDA", pontoId=ponto, page=1, size=20), ADMIN),
        ("Detalhe com itens e histórico", f"/solicitacoes/{exemplo}", {}, ADMIN),
        ("Indicador de situações por período", "/indicadores/solicitacoes", PERIODO, ADMIN),
        ("Recolhimentos por período", "/indicadores/recolhimentos", dict(PERIODO, page=1, size=2), ADMIN),
    ]
    resultados = {nome: [] for nome, *_ in consultas}
    for rodada in range(AQUECIMENTO + AMOSTRAS):
        for nome, caminho, parametros, ator in consultas:
            inicio = perf_counter()
            resposta = consulta(base, caminho, parametros, ator)
            ms = (perf_counter() - inicio) * 1000
            verificar(resposta.get("content", resposta) if isinstance(resposta, dict) else resposta,
                      f"A consulta de desempenho retornou uma página ou resposta vazia: {nome}")
            if rodada >= AQUECIMENTO:
                resultados[nome].append(ms)

    todos = [tempo for amostra in resultados.values() for tempo in amostra]
    print("| Consulta | Amostras | p95 (ms) | Máxima (ms) |", flush=True)
    print("| --- | ---: | ---: | ---: |", flush=True)
    for nome, amostra in [*resultados.items(), ("Total", todos)]:
        print(f"| {nome} | {len(amostra)} | {p95(amostra):.2f} | {max(amostra):.2f} |", flush=True)
    verificar(all(p95(amostra) <= META_MS for amostra in resultados.values()),
              "Pelo menos uma consulta excedeu a meta de p95 ≤ 2 s.")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--usar-jar-existente", action="store_true",
                        help="reutiliza o JAR gerado pelo roteiro de aceite deste checkout")
    argumentos = parser.parse_args()

    with postgres() as (container, porta, senha):
        subprocess.run(["docker", "exec", container, "createdb", "-w", "-U", USUARIO, BANCO], check=True)
        ambiente = ambiente_spring(porta, senha, BANCO)
        if not argumentos.usar_jar_existente:
            subprocess.run(["sh", "./mvnw", "-B", "-ntp", "-T", "1", "-DskipTests", "package"],
                           cwd=PROJETO, env=ambiente, check=True)
        jar = caminho_jar()
        verificar(jar.is_file(), f"Gere o JAR neste checkout antes de reutilizá-lo: {jar}")
        ambiente["SPRING_PROFILES_INCLUDE"] = "demo"
        versao = psql(container, "-At", "-c", "SHOW server_version", capture_output=True, text=True)
        print(f"PostgreSQL {versao.stdout.strip()}", flush=True)
        with tempfile.TemporaryDirectory(prefix="coleta-desempenho-") as diretorio:
            pasta = Path(diretorio)
            with servidor(jar, ambiente, pasta) as (base, pid_inicial):
                rotas = requisitar(base, "GET", "/v3/api-docs", 200)["paths"]
                verificar("/api/v1/indicadores/recolhimentos" in rotas and "/api/v1/demo/atores" in rotas,
                          "Execute com a API-16 integrada e com o perfil demo disponível.")
                atores = consulta(base, "/demo/atores", ator=None)
                exemplo = carregar_dados(container)
                antes = conferir(base, exemplo)
            with servidor(jar, ambiente, pasta) as (base, pid_reinicio):
                verificar(pid_inicial != pid_reinicio and antes == conferir(base, exemplo)
                          and atores == consulta(base, "/demo/atores", ator=None),
                          "O reinício alterou os registros, os indicadores ou os atores fictícios.")
                print(f"Reinício confirmado: PID {pid_inicial} → {pid_reinicio}, mesmo PostgreSQL.", flush=True)
                medir(base, exemplo)
    print("Desempenho aprovado: p95 de todas as consultas ≤ 2 s.", flush=True)


if __name__ == "__main__":
    main()
