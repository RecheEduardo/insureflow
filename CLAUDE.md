# Insureflow

API REST (Java 25, Spring Boot 4.1) que é a **fonte da verdade de apólices de seguro patrimonial**. Expõe duas portas:

- **API administrativa** (`/api/v1/policies`): o operador interno emite, consulta, endossa e cancela apólices.
- **API Open Insurance** (`/open-insurance/insurance-patrimonial/v2`): instituições participantes consultam os mesmos contratos no formato oficial, somente leitura.

O contrato oficial está versionado em `docs/reference/insurance-patrimonial.yaml`. As decisões ficam registradas em `docs/adr/`.

---

## Princípios de trabalho

- **Só gerar código quando pedido explicitamente.** Fora disso, atuar como apoio: explicar, revisar, apontar riscos.
- **Sem plano de implementação fixo.** O trabalho é iterativo e o escopo de cada passo é definido pelo prompt do usuário, nada além disso. Nunca propor, numerar nem citar fatias, etapas ou roteiro, nem anunciar o que "vem a seguir".
- **Definição de pronto de cada commit:** compila; testes passam; existe uma forma concreta de exercitar o que entrou (teste, `.http` ou Swagger); cada arquivo novo é explicável. Mudança com mais de ~10 arquivos novos deve ser dividida em commits.
- **Profundidade acima de quantidade.** Só entra o que pode ser justificado com um trade-off claro. Uma entrega menor e totalmente funcional vale mais que uma maior com algo quebrado. Um diferencial instável sai da entrega e vira ADR explicando o que faltou.
- **Toda afirmação não funcional precisa de evidência** (teste ou medição). Sem medição, otimização é só afirmação.
- **Decisão relevante gera ADR** em `docs/adr/`.
- **Vocabulário de negócio** (endosso, LMG, LMI, vigência, cosseguro) em código, testes e documentação.
- Commits pequenos e descritivos, em inglês, no padrão Conventional Commits (`feat:`, `test:`, `refactor:`, `docs:`, `chore:`).

## Convenções

| Tema | Regra |
|---|---|
| Identificadores | Inglês |
| Mensagens internas de exceção | Inglês, com código de erro estável (`LMI_ABOVE_LMG`) |
| Mensagens retornadas pela API | Português, curtas |
| Comentários | Ver "Regras de comentários" abaixo |
| Respostas Open Insurance | Reproduzir **exatamente** nomes, enums e formatos do YAML (`maxLMG`, `LMI`, `AmountDetails`), sem reinterpretar |
| Enums de domínio | Nomes idênticos aos do YAML; exceções: `PolicyStatus` e `CancellationReason`, que são do projeto |
| Dinheiro | `BigDecimal` + `Currency`; nunca `double`; mais de 2 casas é rejeitado, nunca arredondado; serializado como texto `^\d{1,16}\.\d{2}$` |
| Datas | `LocalDate` puro (`yyyy-MM-dd`), sem hora nem fuso; vigência inclusiva nas duas pontas |
| Tempo | O domínio nunca chama `now()`; recebe `today`/`now` de um `Clock` injetado |
| Modelagem | Records para objetos de valor e DTOs; sem Lombok; sem setters públicos no agregado |
| DTOs | Sufixo `DTO` em requisições, respostas e comandos de caso de uso, inclusive os records aninhados; ficam em pacote `dto` do domínio (`adapter.in.web.admin.policy.dto`, `application.policy.dto`) |
| Pacotes por domínio | Cada adapter se organiza por domínio (`adapter.in.web.admin.policy`, `adapter.out.persistence.policy`); o que serve a vários domínios (tratador de exceções, `If-Match`, resposta de erro) fica na raiz do adapter |
| Dados | Somente fictícios; documento do segurado mascarado; nenhum dado pessoal em log |
| Imagens Docker | Versões fixas (nunca `latest`) |

### Regras de comentários

**Pouquíssimos comentários, mas não nenhum.** Os trechos de complexidade alta devem ter comentário; o resto do código não tem, e comentário nunca é item de rotina de arquivo novo.

- Arquivo novo nasce sem comentário. Não se comenta classe, record, método ou configuração só por existir.
- Se um trecho complexo ficar sem comentário, é falha tão real quanto comentar o óbvio.
- O código se explica por nomes, tipos e estrutura. Antes de comentar, renomeie, extraia um método ou crie um tipo.
- Só entra comentário quando o trecho é genuinamente difícil de entender e o porquê não cabe em nome nem em estrutura: algoritmo não trivial, regra de negócio sutil, workaround de restrição externa, decisão que parece errada mas é proposital.
- Quando entra: português, uma linha, explicando o porquê. Sem Javadoc de bloco.
- Nunca citar planejamento, promessa de mudança futura ("por enquanto", "provisório", "depois") nem histórico da mudança; nunca narrar o que a linha faz.
- Contexto amplo e decisões vão para ADR ou README.
- Nos arquivos `.http`, os `###` só nomeiam a requisição.
- Ao concluir uma tarefa, revisar os comentários dos arquivos criados ou alterados e remover os que não passam nesta regra.

## Comandos

```bash
./mvnw test                  # requer JDK 25 e Docker (Testcontainers)
./mvnw spring-boot:test-run  # sobe a app com Postgres e Kafka via Testcontainers
./mvnw spring-boot:run       # sobe o Postgres do compose.yaml sozinho (Docker Compose Support) e a app
```

Credenciais do `compose.yaml` são fictícias, só de desenvolvimento local, com padrão no próprio arquivo; `.env` é ignorado pelo git e `.env.example` lista o que dá para sobrescrever.

---

## Negócio

### O problema

A seguradora precisa de um cadastro de apólices patrimoniais que atenda a duas necessidades:

1. a **gestão interna do ciclo de vida** (emitir, endossar e cancelar contratos);
2. o **compartilhamento padronizado** dos contratos no formato Open Insurance.

A dor central é a **confiabilidade do dado**: ele deixa de ser interno e passa a ser visível para o mercado.

| Problema | Consequência | Regra que protege |
|---|---|---|
| Apólice duplicada | Cliente aparece com dois contratos | Unicidade, inclusive sob concorrência |
| Vigência invertida | Contrato que não protege nada | Fim não anterior ao início |
| Número da apólice alterado | Quebra a referência guardada por outras instituições | Identificadores imutáveis |
| Apólice cancelada apagada | Some um histórico que o cliente tem direito de ver | Cancelamento lógico |

### Atores

- **Operador interno** (backoffice, emissão): único que altera dados. Registra apólices após a subscrição, processa endossos e cancela contratos. Usa a API administrativa.
- **Instituição participante** (outra seguradora, corretora digital): só consulta, no formato padronizado. Usa a API Open Insurance.
- **Segurado:** não usa o sistema, mas é o dono dos dados e quem autoriza o compartilhamento.

### Ciclo de vida da apólice

- Nasce **ativa** na emissão, recebe **endossos** durante a vigência e termina **encerrada** (fim natural) ou **cancelada** (ato do operador).
- **Encerrada é derivada das datas**, não armazenada. Só o cancelamento vira estado persistido.
- **Cancelar não é apagar.** A apólice vigorou, pode ter tido sinistros e há obrigação regulatória de guarda de registros, o que também justifica a retenção perante a LGPD.

### Glossário

| Termo | Significado |
|---|---|
| Segurado | Pessoa física ou jurídica protegida pelo contrato |
| Corretor | Intermediário que vende e orienta, sem assumir risco |
| Prêmio | Valor pago pelo segurado pela proteção |
| Sinistro / Indenização | Evento coberto que gera prejuízo / pagamento da seguradora após ele |
| Franquia | Parte do prejuízo que fica a cargo do segurado |
| Proposta | Pedido analisado pela seguradora (subscrição) antes de virar apólice |
| Apólice | O contrato, com número único e vigência |
| Vigência | Período em que a proteção vale |
| Cobertura / LMI | Cada risco protegido / seu Limite Máximo de Indenização |
| LMG | Limite Máximo de Garantia: teto da seguradora considerando as coberturas em conjunto |
| Endosso | Alteração formal de uma apólice emitida; o contrato continua o mesmo |
| Bilhete | Contrato simplificado de seguro massificado, sem proposta formal |
| Certificado | Documento de cada participante de uma apólice coletiva |
| Cosseguro aceito | Participação num risco liderado por outra seguradora |
| Ramo patrimonial | Seguros de bens: residencial, empresarial, condomínio, equipamentos |

---

## Open Insurance

Programa regulado pela SUSEP com três pilares:

- **Consentimento:** nada é compartilhado sem autorização explícita, com prazo e finalidade.
- **Padronização:** todas as seguradoras expõem os dados no mesmo formato.
- **Somente leitura:** a instituição receptora consulta, nunca altera.

**Referências verificadas em 21/10/2026:** YAML InsurancePatrimonial v2.0.0 e Manual de APIs v1.5.

**Posicionamento:** aderência **parcial e declarada** a um subconjunto do contrato. O projeto não é apresentado como certificado nem regulatoriamente conforme. O CRUD administrativo é uma extensão interna própria, não um endpoint oficial.

### O que o contrato define e o projeto segue

- **Endpoints:** listagem e policy-info. Premium e claim existem no YAML, mas ficam fora do escopo.
- **Campos obrigatórios do policy-info:** `documentType`, `policyId`, `issuanceType`, `issuanceDate`, `termStartDate`, `termEndDate`, `maxLMG`, `proposalId`, `insureds` e `insuredObjects`. O contrato exige segurados **e** objetos segurados, e os dois são implementados.
- **`policyId`:** texto de até 60 caracteres. É chave de negócio, não um identificador técnico gerado.
- **Lista vazia retorna 200 com lista vazia, nunca 404** (Manual, 4.1).
- **Status do contrato:** 400, 401, 403, 404, 405, 406, 422, 429, 500.
- **Versionamento:** major, minor e patch, com 180 dias de convivência entre majors (Manual, 4.2).
- **Especificação:** OpenAPI 3.0.0 analisado com Spectral, sem erros nem alertas (Manual, 4.1).
- **Extensibilidade:** participantes podem adicionar endpoints, operações, parâmetros opcionais e propriedades novas nas respostas (Manual, 4.9).

### Particularidades que moldam o modelo

- **O policy-info não tem campo de situação.** No modelo oficial, o cancelamento aparece indiretamente. O status é interno da API administrativa; a apólice cancelada continua aparecendo na consulta Open Insurance.
- **Não existe tipo de objeto "imóvel".** Imóvel é exposto como `type: OUTROS`, com `propertyType` e `postCode` no bloco `branchInfo`. O domínio guarda o que importa ao negócio e o mapeamento traduz.
- **`documentType` inclui valores de automóvel**, excluídos por pertencerem a outro ramo.
- **O enum de coberturas tem mais de cem valores.** O projeto usa um subconjunto residencial mais `OUTRAS`.

### Requisitos regulatórios (Manual, seção 5)

- **Tráfego:** no mínimo 300 req/s globais e 500 req/min por IP; o excedente é recusado com 429.
- **Desempenho:** p95 de até 1500 ms (média prioridade, onde entram dados transacionais de clientes).
- **Disponibilidade por endpoint:** 85% a cada 24 h e 99,5% a cada 3 meses.

### Consentimento

- No contrato real, todo endpoint exige um token vinculado a um `consentId` com status `AUTHORISED` e a permissão adequada:
  - `DAMAGES_AND_PEOPLE_PATRIMONIAL_READ` para a listagem;
  - `DAMAGES_AND_PEOPLE_PATRIMONIAL_POLICYINFO_READ` para o policy-info.
- O consentimento vale por no máximo 12 meses, e a listagem real é **por cliente**: as apólices ligadas ao consentimento.
- **Decisão:** consentimento e OAuth2 ficam fora do escopo. O ponto de extensão é um filtro na entrada Open Insurance mais a consulta por documento do segurado na porta de leitura; o domínio já guarda o documento de cada segurado. O README declara que hoje a listagem devolve todas as apólices.

---

## Arquitetura

Clean Architecture enxuta, com a separação entre HTTP, negócio e dados e sem cerimônia excessiva. **Dependências apontam para dentro.** Pacotes criados conforme o código exigir:

```
br.com.eduardoreche.insureflow
├── InsureflowApplication        raiz de composição, no pacote raiz (fora de qualquer camada)
├── domain                       Java puro: model, model.enums, exception
├── application                  casos de uso, portas (interfaces), transação
├── adapter
│   ├── in.web.admin             API administrativa, um pacote por domínio (policy)
│   ├── in.web.openinsurance     API Open Insurance
│   ├── in.messaging             consumidores Kafka (auditoria, notificação)
│   ├── out.persistence          um pacote por domínio (policy): JPA, entidade espelho, mapeador, JSONB
│   └── out.messaging            outbox e repasse para o Kafka
└── infrastructure               configuração técnica (Clock, OpenAPI, Kafka, interaction id)
```

Regras, verificadas por ArchUnit:

- `domain` não depende de nada: sem Spring, JPA, Jackson ou Bean Validation.
- `application` depende só de `domain`. O único elemento de framework tolerado é `@Transactional`.
- Adapters dependem de `application`/`domain` e **nunca uns dos outros**.
- A entidade JPA é um espelho no adapter de persistência, com mapeador. O custo é baixo porque só a apólice vira entidade relacional.

### Duas portas com perfis opostos

As decisões não funcionais derivam desta assimetria:

| | Administrativa | Open Insurance |
|---|---|---|
| Uso | Escrita e leitura, operador interno | Somente leitura, instituições externas |
| Volume | Baixo | Alto (≥ 300 req/s) |
| Prioridade | Consistência e integridade | Disponibilidade e latência |

**Por que portas separadas:** o Manual permite estender a API oficial com novas operações, mas ela é vinculada ao token de consentimento do cliente, e a escrita administrativa não tem relação com isso.

### Princípios do domínio

- **Validação em duas camadas:**
  - Borda: Bean Validation nos DTOs; erro de formato vira **400**.
  - Domínio: rejeita o que é inválido para o negócio, venha de onde vier (API, seed, evento), com `BusinessRuleViolationException` e **422**.
  - Campo obrigatório nulo dentro do domínio é bug (a borda deveria ter barrado) e falha com `NullPointerException`.
- Objetos de valor são imutáveis e validados na construção: um objeto inválido não pode existir.
- Invariantes que dependem do conjunto pertencem à raiz do agregado, não às partes.
- Exceções carregam código estável e não conhecem HTTP. A tradução é do adapter web.
- Todas as exceções de negócio ficam em `domain.exception`, inclusive as lançadas pela aplicação ou pelo adapter (`PolicyNotFoundException`, `PolicyVersionMismatchException`): a versão é campo do agregado, e o tratador web encontra todas num lugar só.

---

## Domínio

### `Policy` (raiz do agregado, igualdade por `id`)

| Campo | Tipo | Regra |
|---|---|---|
| `id` | UUID | Chave técnica interna |
| `policyId` | texto ≤ 60 | Chave de negócio, única, imutável |
| `documentType` | `DocumentType` | Imutável |
| `groupCertificateId` | texto ≤ 60, opcional | Obrigatório se, e somente se, `CERTIFICADO` |
| `issuanceType` | `IssuanceType` | Imutável |
| `issuanceDate` | data | Imutável; não pode ser futura na criação |
| `term` | `Term` | Alterável por endosso |
| `maxLmg` | `Money` | Alterável por endosso |
| `proposalId` | texto ≤ 60 | Imutável; referência de origem, sem cadastro próprio |
| `productName` | texto ≤ 70 | Alterável; exigido pela listagem; produto é atributo, não entidade |
| `insureds` / `insuredObjects` | listas | Ao menos um de cada; alteráveis por endosso |
| `status`, `cancelledAt`, `cancellationReason` | | Mudam só pelo cancelamento |
| `version` | long | Versão otimista, exposta como `ETag` |
| `createdAt` / `updatedAt` | instante | Auditoria técnica |

**Comportamentos:**

- `create(..., today, now)`: gera o id, nasce `ACTIVE` e rejeita emissão futura.
- `restore(...)`: usado só pela persistência; não reaplica regras dependentes do tempo.
- `endorse(...)`: recebe **apenas** os campos alteráveis, então identificadores são imutáveis por construção. Lança `PolicyCancelledException` se a apólice estiver cancelada. Valida antes de aplicar.
- `cancel(reason, now)`: cancelamento lógico; se já estiver cancelada, não faz nada e não falha (sustenta o DELETE idempotente).
- `isInForceOn(date)`: não cancelada e com a data dentro da vigência.

**Invariantes do agregado** (na criação e em todo endosso):

- ao menos um segurado e um objeto segurado;
- `groupCertificateId` se e somente se `CERTIFICADO`;
- identificação do objeto segurado única na apólice;
- LMI de cada cobertura ≤ LMG, na mesma moeda;
- vigência de cada cobertura contida na vigência da apólice.

### Objetos de valor

- **`Term`:** início e fim obrigatórios, fim ≥ início, inclusiva. Responde se contém uma data, se contém outra vigência e se já terminou numa data.
- **`Money`:** valor decimal e moeda ISO-4217 (padrão BRL). Não negativo. Mais de duas casas é rejeitado; a escala é normalizada em duas para que valores iguais sejam iguais. Comparar moedas diferentes é rejeitado.
- **`Address`** (nacional): tipo de logradouro, nome, número, complemento e bairro opcionais, cidade, UF e CEP. Endereço internacional é limitação documentada.
- **`Insured`:** documento (CPF ou CNPJ fictício), tipo (CPF, CNPJ, OUTROS), nome, data de nascimento (obrigatória para CPF e OUTROS), e-mail opcional e endereço. O `toString` mascara o documento e omite dados pessoais.
- **`InsuredObject`:** identificação, tipo de imóvel, descrição, CEP, valor do bem opcional e coberturas (cópia imutável). Exige ao menos uma cobertura, ao menos uma principal e nenhum código repetido.
- **`Coverage`:** ramo (4 dígitos), código, descrição (obrigatória se `OUTRAS`), processo SUSEP, LMI, vigência, indicador de principal, característica, tipo e periodicidade de prêmio. Valida só o que sabe sobre si; regras que dependem da apólice ficam na apólice.

### Enums

- `DocumentType`: `APOLICE_INDIVIDUAL`, `BILHETE`, `CERTIFICADO`.
- `IssuanceType`: `EMISSAO_PROPRIA`, `COSSEGURO_ACEITO`.
- `PolicyStatus` (do projeto): `ACTIVE`, `CANCELLED`.
- `CancellationReason` (do projeto): `PEDIDO_DO_SEGURADO`, `FALTA_DE_PAGAMENTO`, `PERDA_TOTAL`, `OUTROS`.
- `CoverageCode`: `INCENDIO`, `QUEDA_RAIO`, `EXPLOSAO`, `DANOS_ELETRICOS`, `ROUBO_SUBTRACAO_BENS`, `VENDAVAL`, `QUEBRA_VIDROS`, `RESPONSABILIDADE_CIVIL_FAMILIAR`, `OUTRAS`.
- `IdentificationType`, `PropertyType`, `CoverageFeature`, `CoverageType`, `PremiumPeriodicity`, `StreetType`, `Uf`: conforme o YAML.

**Regras de negócio além das mínimas:**

1. Apólice cancelada não aceita endossos.
2. O LMI de cada cobertura não pode exceder o LMG da apólice.
3. A vigência de cada cobertura deve estar contida na vigência da apólice.

### Exceções → HTTP

A tradução para status acontece só no adapter web.

| Exceção | Status |
|---|---|
| `BusinessRuleViolationException` | 422 |
| `PolicyNotFoundException` | 404 |
| `DuplicatePolicyException` (restrição única do banco) | 409 |
| `PolicyCancelledException` | 409 |
| Formato inválido / campo imutável no endosso | 400 |
| `If-Match` divergente (aplicação) | 412 |

---

## Contratos HTTP

### Administrativa: `/api/v1/policies`

| Operação | Respostas |
|---|---|
| `POST` | 201 + `Location` + `ETag`; 409 duplicada; 400; 422. `Idempotency-Key` opcional: mesma chave e mesmo corpo devolvem a resposta original; corpo diferente, 422 |
| `GET /{policyId}` | 200 + `ETag`, inclusive cancelada (com status); 404 |
| `PATCH /{policyId}` + `If-Match` | 200; 412 versão divergente; 409 cancelada; 400 campo desconhecido ou imutável; 422 |
| `DELETE /{policyId}` | 204 (cancelamento lógico); repetir também dá 204; 404. Motivo opcional, padrão `OUTROS` |

- O pedido de criação não tem id, status, versão nem datas técnicas.
- O pedido de endosso só contém campos alteráveis, e propriedades desconhecidas são rejeitadas. Assim não dá para alterar identificadores pela própria estrutura do contrato.

### Open Insurance: `/open-insurance/insurance-patrimonial/v2`

| Operação | Respostas |
|---|---|
| `GET /insurance-patrimonial` | 200 com `data` (marca → sociedades → apólices), `links`, `meta`; lista vazia é 200 |
| `GET /insurance-patrimonial/{policyId}/policy-info` | 200; 404 |

- **Paginação por página:** `page` (padrão 1, mín. 1) e `page-size` (padrão 25, máx. 1000). A resposta traz `meta` (`totalRecords`, `totalPages`) e `links` (`self`, `first`, `prev`, `next`, `last`). Paginação por cursor não é compatível.
- Ordenação determinística por `policyId`. Apólices canceladas continuam aparecendo.
- A listagem mostra marca → sociedades (nome e CNPJ) → apólices (`policyId`, `productName`). Marca, sociedade e CNPJ fictícios vêm de configuração.

### Transversal às duas APIs

- `x-fapi-interaction-id` (UUID): gerado se ausente, devolvido na resposta e usado como identificador de correlação em todos os logs.
- Envelope de erro do contrato nas duas APIs: `errors[1..13]` com `code`, `title`, `detail` e `requestDateTime` (UTC), mais `meta`.
- OpenAPI 3.0 gerado pelo springdoc, com grupos `admin` e `open-insurance`.

---

## Eventos e mensageria

- **Eventos:** apólice emitida, endossada e cancelada.
- **Conteúdo do evento:** id do evento, tipo, `policyId`, versão, instante, interaction id e um retrato da apólice.
- **Consumidores**, em grupos independentes:
  - `policy-audit`: histórico de alterações;
  - `policy-notification`: aviso ao segurado e ao corretor, simulado em log.
- **Kafka (KRaft):** log durável com grupos independentes. A queda de um grupo não afeta o outro, e ele recupera o atraso ao voltar.
- **Transactional outbox:** o evento é gravado na mesma transação da apólice e publicado depois por um processo de repasse. Sem isso, banco e Kafka podem divergir.
- **`policyId` como chave da mensagem:** garante a ordem dos eventos de uma apólice dentro da partição.
- **Entrega ao menos uma vez:** consumidores idempotentes descartam repetidos pelo id do evento.

---

## Decisões não funcionais

### Integridade e concorrência

- **Unicidade garantida pelo banco** (`UNIQUE` em `policy_id`), não por verificação prévia, que falha sob concorrência. A violação vira 409.
- **Defesa em profundidade:** `CHECK` de fim ≥ início no banco, além do domínio.
- **Lock otimista com versão:** detecta o conflito e recusa o perdedor, sem serializar operações. É adequado porque conflitos de escrita no backoffice são raros.
- **A versão chega ao cliente** via `ETag`/`If-Match`. Sem isso, a proteção cobriria só milissegundos no servidor, e não a janela real de minutos entre dois operadores.
- **Idempotência não é unicidade:** idempotência trata o mesmo pedido repetido (retry de rede); unicidade trata pedidos diferentes disputando o mesmo `policyId`.

### Desempenho da leitura

- O caminho crítico é a consulta Open Insurance.
- **Riscos conhecidos:** N+1 nos dados aninhados, carregamento de dados desnecessários e o custo da contagem total exigida pela paginação.
- **Escada de soluções:** projeções só com o necessário → dados aninhados de uma vez → índices alinhados à ordenação e à contagem → transações somente leitura → SQL escrito à mão só onde a ferramenta atrapalhar.
- **Modelo de dados híbrido:**
  - colunas relacionais para o que precisa de restrição, filtro e ordenação (`policy_id`, datas, status, versão);
  - JSONB para segurados, objetos e coberturas, lidos de uma vez e no formato próximo da resposta oficial.
- **Spring MVC com virtual threads:** escala o I/O sem a complexidade da programação reativa.
- **CQRS com modelo de leitura separado** fica só no README: a medição mostra que o modelo atual atende, e o texto indica o ponto em que a separação passaria a compensar, ao custo de consistência eventual.

### Retenção e arquivamento (visão no README, não implementado)

- Lifecycle policies e Glacier Deep Archive são recursos do S3, que guarda objetos, não registros. Seria preciso exportar os dados antes.
- Recuperar do Deep Archive leva de 12 a 48 h, o que é incompatível com uma apólice que ainda precisa ser consultável.
- **Desenho correto, por idade e frequência de acesso:**
  - banco operacional para apólices ativas e dentro da janela de compartilhamento;
  - S3 com transição gradual de classes para contratos fora da janela, mas dentro do prazo de guarda obrigatória.
- Registros de apólice são pequenos, então isso só compensa em escala real.

### Operação, observabilidade e privacidade

- **Healthcheck e métricas** como evidência das metas de disponibilidade.
- **Limite de taxa** na porta Open Insurance, com resposta 429. Fica na aplicação ou no gateway, com a escolha explícita.
- **Observabilidade:** logs estruturados, o interaction id em todos os logs e métricas expostas para coleta.
- **Privacidade (LGPD):** somente dados fictícios, nenhum dado pessoal em log e exposição apenas do necessário.

### Evidências

| Risco | Mecanismo | Prova |
|---|---|---|
| Apólice duplicada sob concorrência | Restrição única no banco | Teste concorrente 201/409 |
| Atualização perdida entre operadores | Lock otimista via `ETag`/`If-Match` | Teste 200/412 |
| Banco e Kafka divergentes | Transactional outbox | Evento do banco até os dois grupos |
| Evento duplicado | Consumidor idempotente | Teste de reentrega |
| Latência e escala da leitura | JSONB, projeções, índices, somente leitura | Contagem de queries e k6 (300 req/s, p95 ≤ 1500 ms) |
| Tráfego excessivo | Limite de taxa (429) | Teste ou ADR |
| Rastreabilidade | `x-fapi-interaction-id` | Teste de eco |
| Vazamento de dado pessoal | Mascaramento | Teste do `toString` |
| Erosão das camadas | Regras de dependência | ArchUnit |
| Contrato fora do padrão | OpenAPI 3.0 + Spectral | CI |

---

## Stack

| Escolha | Justificativa |
|---|---|
| Java 25 | LTS atual |
| Spring Boot 4.1.x | Linha vigente com suporte oficial a Java 25; a 4.0 perde suporte OSS em dezembro de 2026 |
| Spring MVC + virtual threads | Escalabilidade de I/O sem programação reativa |
| Bean Validation | Validação de formato na borda |
| Spring Data JPA | Porta de escrita (versão otimista e mapeamento); consultas sob medida na leitura |
| PostgreSQL | Unicidade nativa, transação cobrindo apólice e outbox, JSONB |
| Flyway | Esquema versionado e explícito (restrições, outbox, índices, JSONB) e inicialização reprodutível |
| Spring for Apache Kafka (KRaft) | Outbox e grupos de consumidores independentes |
| Actuator + Prometheus | Healthcheck e métricas, incluindo o p95 da carga |
| Testcontainers | Integração contra Postgres e Kafka reais |
| Docker Compose | Execução local com um comando, sem conta em nuvem |
| springdoc-openapi | Documentação gerada pela aplicação, em OpenAPI 3.0, com grupos separados |
| ArchUnit | Prova executável da separação de camadas |
| Spectral | Lint da especificação, exigido pelo Manual de APIs |
| GitHub Actions | Esteira de build, testes e lint |
| k6 | Carga com a meta regulatória como critério de aprovação |
| Bucket4j (opcional) | Limite de taxa na aplicação, se não ficar no gateway |

**Alternativa considerada: DynamoDB.** A escrita condicional resolve unicidade e versão com elegância. Foi descartado porque a paginação com total de registros e os relacionamentos exigem modelagem mais cuidadosa, e o outbox vira outro padrão (Streams). O fator decisivo foi o encaixe com os requisitos.

**Excluídos de propósito:**

| Item | Motivo |
|---|---|
| Spring Security | Sem consentimento e OAuth2 no escopo, bloquearia endpoints sem demonstrar nada |
| H2 | Comporta-se diferente do Postgres justamente em locks e restrições |
| Spring Data REST | Expõe repositórios diretamente, pulando as regras de negócio |
| WebFlux | Virtual threads entregam a escala necessária com modelo mais simples |
| Lombok | Records cobrem objetos de valor e DTOs; o domínio fica puro |

---

## Testes

- **Domínio:** JUnit puro, sem Spring, com datas fixas; cobre cada invariante e cada código de erro.
- **Aplicação:** orquestração com portas falsas, incluindo a gravação do evento de outbox junto da apólice.
- **Integração da API administrativa:** contra Postgres real, cobrindo cada código de status.
- **Integração Open Insurance:** paginação e links, lista vazia com 200, eco do interaction id e formato monetário.
- **Concorrência:** duas criações simultâneas com o mesmo `policyId` (201 e 409) e dois endossos com a mesma versão (200 e 412).
- **Outbox até o Kafka:** o evento sai do banco e chega aos dois grupos de consumidores.
- **Arquitetura** (ArchUnit) e **carga** (k6).
- Sempre contra Postgres e Kafka reais via Testcontainers. **Nunca H2.**

---

## Fora do escopo (declarado no README)

- Consentimento e OAuth2, com o ponto de extensão descrito na seção Open Insurance.
- Endpoints de prêmio e sinistro.
- Endereço internacional.
- CQRS com modelo de leitura separado e arquivamento em camadas: ficam como visão de arquitetura.
- Outros consumidores de eventos (analytics, data lake).

## Decisões em aberto

- Endosso de apólice já encerrada pelo tempo (prorrogação): permitir ou bloquear.
- Cancelamento de apólice encerrada: rejeitar com 409 ou aceitar só para registrar o motivo.
- Emissão posterior ao início da vigência: a tendência é permitir, porque cobertura retroativa existe no mercado.
- Status no policy-info: só interno (padrão) ou extensão declarada.
- Janela de compartilhamento de apólices encerradas ou canceladas: confirmar no Manual de Escopo de Dados e Serviços v7.2 antes de fechar o ADR de cancelamento.
- Limitação de taxa na aplicação ou no gateway.
- Seed de dados fictícios (ao menos uma apólice ativa, uma cancelada e uma encerrada): sempre ou só no perfil local.

## Documentação de destino

- **ADRs** (contexto, opções, decisão, consequências):
  1. PostgreSQL e modelo híbrido
  2. Unicidade e concorrência
  3. Cancelamento lógico e retenção
  4. Outbox e Kafka
  5. MVC com virtual threads
  6. Escopo sem consentimento
- **README:**
  - como executar com um comando;
  - endpoints e exemplos;
  - tabela de mapeamento campo oficial ↔ domínio (implementado, adaptado ou omitido, com motivo);
  - links para os ADRs;
  - resultados de carga;
  - limitações conhecidas;
  - declaração de uso de IA.
- **Estado final:** sobe localmente sem conta em nuvem. O CRUD completo e as consultas Open Insurance rodam no formato oficial. As regras difíceis (unicidade, versão, outbox) são provadas por testes, e cada número de desempenho vem de medição.

## Referências

- YAML oficial InsurancePatrimonial: https://raw.githubusercontent.com/br-openinsurance/areadesenvolvedor/main/documentation/source/files/swagger/current/insurance-patrimonial.yaml
- Manual de APIs do Open Insurance v1.5: https://www.gov.br/susep/pt-br/assuntos/open-insurance/arquivos/ManualdeAPIs1_5.pdf/@@display-file/file
- Documentos de referência da SUSEP: https://www.gov.br/susep/pt-br/assuntos/open-insurance/documentos_de_referencia
- Manual de Escopo de Dados e Serviços v7.2: https://www.gov.br/susep/pt-br/assuntos/open-insurance/arquivos/manual_de_escopo_de_dados_e_servicos_v7-2-1.pdf/@@display-file/file
