# RICO JSON Exporter

Aplicação **Java 17 + Spring Boot** para exportar clientes do **MariaDB** por lote, gerar JSON em múltiplos arquivos e entregar localmente ou via **SFTP**.

## Estratégia

A query SQL Server original foi usada como regra de negócio, preservando o mesmo JSON final:

- dados do cliente;
- `telefones`;
- `emails`;
- `enderecos`;
- `operacoes` filtradas pelo lote.

Para evitar um JSON gigantesco e alto consumo de memória, a aplicação não usa `FOR JSON PATH`/`JSON_ARRAYAGG` para montar tudo no banco.

A aplicação:

1. busca clientes por lote usando paginação por chave (`ndg > ultimoNdg`);
2. busca telefones, endereços e operações em lote para os NDGs da página;
3. monta cada cliente em Java;
4. escreve diretamente em um `JsonGenerator`, sem manter o arquivo completo em memória;
5. fecha o arquivo quando atinge o limite de registros ou o limite aproximado de tamanho;
6. inicia o próximo arquivo automaticamente;
7. publica cada arquivo fechado no destino configurado.

O endpoint é **assíncrono**.

Apenas um job de exportação é executado de cada vez para evitar pressão excessiva sobre o banco. Os demais ficam em fila.

---

## Divisão sugerida

Configuração inicial:

- **100.000 clientes por arquivo**;
- **250 MB aproximadamente por arquivo**;
- o primeiro limite atingido provoca a rotação.

O tamanho é aproximado porque a checagem é realizada periodicamente para evitar `flush` a cada cliente.

### Exemplo dos arquivos gerados

```text
NOG123_0001.json
NOG123_0002.json
NOG123_0003.json
...
```

Cada arquivo é um **JSON independente e válido**, contendo um array de clientes.

---

## Configuração `.env`

O Spring Boot importa o `.env` como arquivo de propriedades através de:

```yaml
spring.config.import: "optional:file:.env[.properties]"
```

Configuração local:

```properties
DB_HOST=localhost
DB_PORT=3306
DB_NAME=rico
DB_USER=admin
DB_PASSWORD=1234

EXPORT_OUTPUT_DIR=./output
EXPORT_PAGE_SIZE=2000
EXPORT_MAX_RECORDS_PER_FILE=100000
EXPORT_MAX_FILE_SIZE_MB=250
EXPORT_SIZE_CHECK_INTERVAL=500

DELIVERY_MODE=LOCAL
```

> A senha `1234` foi utilizada para o ambiente local conforme configuração inicial do projeto.

O arquivo `.env` está incluído no `.gitignore`. Em ambientes reais, **não versione credenciais**.

---

## Executar a aplicação

### Pré-requisitos

- Java 17+
- Maven 3.9+
- MariaDB acessível conforme configuração do `.env`
- bancos `rico` e `ricoaux` com as tabelas previamente criadas

### Executar com Maven

```bash
mvn clean package
mvn spring-boot:run
```

Ou execute o JAR:

```bash
java -jar target/rico-json-exporter-1.0.0.jar
```

> Execute a aplicação a partir da raiz do projeto para que o arquivo `.env` seja encontrado.

---

## Iniciar uma exportação

Para iniciar a exportação de determinado lote:

```bash
curl -X POST http://localhost:8080/api/exports/lotes/123
```

O endpoint retorna:

```text
HTTP 202 Accepted
```

Exemplo de resposta:

```json
{
  "jobId": "b3b5630e-b42e-41f5-93cb-515416980098",
  "lote": 123,
  "status": "QUEUED",
  "clientesProcessados": 0,
  "ultimoNdg": 0,
  "arquivosGerados": null,
  "erro": null,
  "criadoEm": "2026-09-03T23:00:00Z",
  "iniciadoEm": null,
  "finalizadoEm": null
}
```

---

## Consultar andamento

Para consultar o status de uma exportação:

```bash
curl http://localhost:8080/api/exports/b3b5630e-b42e-41f5-93cb-515416980098
```

### Status possíveis

- `QUEUED` — aguardando execução;
- `RUNNING` — exportação em andamento;
- `COMPLETED` — exportação concluída;
- `FAILED` — ocorreu um erro durante a exportação.

Os campos `clientesProcessados` e `ultimoNdg` são atualizados ao término de cada página processada.

---

## Arquivos locais

Com a configuração:

```properties
DELIVERY_MODE=LOCAL
```

os arquivos permanecem no diretório:

```text
./output/
```

Exemplo:

```text
output/
├── NOG123_0001.json
├── NOG123_0002.json
├── NOG123_0003.json
└── ...
```

---

## Ativar SFTP

Para habilitar o envio dos arquivos para um servidor SFTP, altere o `.env`:

```properties
DELIVERY_MODE=SFTP

SFTP_HOST=sftp.exemplo.com
SFTP_PORT=22
SFTP_USER=usuario
SFTP_PASSWORD=senha
SFTP_REMOTE_DIRECTORY=/upload
SFTP_ALLOW_UNKNOWN_KEYS=false
```

Os arquivos continuam sendo criados inicialmente em:

```text
./output/
```

Assim que cada arquivo é fechado, ele é enviado ao servidor **SFTP**.

A implementação utiliza **Spring Integration SFTP** e realiza a transferência utilizando um nome temporário antes da renomeação para o nome definitivo.

Para produção, mantenha:

```properties
SFTP_ALLOW_UNKNOWN_KEYS=false
```

e configure a validação da chave SSH do servidor conforme a política de segurança do ambiente.

---

## SQL usado como base

A seleção principal corresponde à parte externa da query original:

```sql
SELECT
    TRIM(c.cpf_cnpj) AS cpfCnpj,
    TRIM(c.nome) AS nomeCliente,
    c.tipo_pessoa AS tipoPessoa,
    c.data_nascimento AS dataNascimento,
    c.sexo,
    c.estado_civil AS estadoCivil,
    c.profissao,
    c.ndg AS codigo,
    TRIM(c.email) AS email
FROM rico.cadastros c
WHERE c.ndg > :lastNdg
  AND EXISTS (
      SELECT 1
      FROM rico.operacoes o
      WHERE o.ndg = c.ndg
        AND o.lote = :lote
  )
ORDER BY c.ndg
LIMIT :limit;
```

As coleções da query original são consultadas em lote utilizando:

```sql
IN (:ndgs)
```

Isso evita uma consulta individual para cada cliente e também evita que o MariaDB precise construir um documento JSON gigantesco.

---

## Índices recomendados

Para o volume proposto, recomenda-se manter pelo menos os seguintes índices:

```sql
CREATE INDEX idx_operacoes_ndg_lote
    ON rico.operacoes (ndg, lote);

CREATE INDEX idx_telefones_ndg
    ON rico.cad_telefones (ndg);

CREATE INDEX idx_enderecos_ndg
    ON rico.cad_enderecos (ndg);

CREATE INDEX idx_est_piso_operacao
    ON ricoaux.tbl_est_piso (id_operacao);
```

A coluna:

```text
cadastros.ndg
```

deve ser uma **chave primária** ou possuir um **índice único**.

---

## Fluxo da exportação

O fluxo principal da aplicação é:

```text
Requisição HTTP
      │
      ▼
POST /api/exports/lotes/{lote}
      │
      ▼
Job entra na fila
      │
      ▼
Busca clientes do lote
      │
      ▼
Paginação por NDG
      │
      ▼
Busca dados relacionados
      │
      ├── Telefones
      ├── Endereços
      └── Operações
      │
      ▼
Monta cliente em Java
      │
      ▼
Streaming com JsonGenerator
      │
      ▼
Arquivo atingiu o limite?
      │
      ├── Não ──► continua escrevendo
      │
      └── Sim
            │
            ▼
      Fecha arquivo JSON
            │
            ▼
      NOG{lote}_{sequencia}.json
            │
            ▼
      Entrega configurada
            │
      ┌─────┴─────┐
      ▼           ▼
    LOCAL        SFTP
```

---

## Estratégia para grandes volumes

A aplicação foi projetada considerando bases com grande quantidade de registros.

Em vez de carregar todos os clientes em memória:

```text
Banco → milhões de registros → memória → JSON
```

a aplicação trabalha de forma incremental:

```text
Banco
  ↓
Página de clientes
  ↓
Processamento
  ↓
JsonGenerator
  ↓
Arquivo
  ↓
Próxima página
```

Essa estratégia reduz o consumo de memória da JVM e permite processar volumes muito maiores de dados.

---

## Observações para produção

Atualmente, os estados dos jobs são mantidos **em memória**.

Caso seja necessário permitir retomada após o reinício da aplicação, uma evolução recomendada é persistir checkpoints em uma tabela de controle contendo, por exemplo:

```text
lote
ultimo_ndg
sequencia_arquivo
status
data_inicio
data_fim
```

Dessa forma, uma exportação interrompida poderia continuar a partir do último NDG processado.

Para volumes muito grandes, também pode ser adicionada compactação:

```text
.json.gz
```

Exemplo:

```text
NOG123_0001.json.gz
NOG123_0002.json.gz
NOG123_0003.json.gz
```

A compactação pode reduzir significativamente o tráfego de rede e o espaço de armazenamento durante o envio via SFTP, sem alterar a estratégia de processamento por streaming.
