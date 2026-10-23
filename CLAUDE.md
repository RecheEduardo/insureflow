# Insureflow

Desafio técnico para Engenheiro de Software Pleno (Open Insurance, Seguro Patrimonial). API REST em Java 25 que gerencia apólices de seguro patrimonial: um CRUD administrativo de autoria própria e consultas inspiradas na API InsurancePatrimonial do Open Insurance. 

Este arquivo guarda só o que é verdadeiro se rotas, payloads, campos, banco ou mensageria mudarem.

## Hierarquia das fontes

- **Case:** define escopo e prioridades. Em conflito com qualquer outro texto, inclusive este, o case vence.
- **Código e testes:** definem o que a aplicação faz hoje. Se este arquivo divergir deles, apontar a divergência; nunca alterar código só para obedecer ao texto.
- **YAML oficial** (`docs/reference/insurance-patrimonial.yaml`): referência de nomes e estrutura, não lei. Seguir ou não cada nome, enum, wrapper ou envelope é decisão do usuário, caso a caso, registrada no mapeamento do README. Verbosidade ou repetição do contrato externo não é motivo para copiá-la.
- **Manual de APIs e demais documentos da SUSEP:** contexto. Nada deles vira requisito sem que o case peça; o que for relevante e ficar de fora vira limitação declarada no README.

## Escopo (do case)

- Consultas inspiradas no Swagger oficial: listagem de apólices com paginação simples e policy-info de uma apólice. Subconjunto coerente de campos, documentado. O case recomenda `policyId`, `documentType`, `issuanceType`, `issuanceDate`, `termStartDate`, `termEndDate`, `proposalId`, `maxLMG` e ao menos um segurado ou objeto segurado.
- CRUD administrativo próprio: criar, consultar, alterar e excluir/cancelar. Rotas e recursos são livres, desde que documentados; avalia-se modelagem de API, semântica HTTP e organização do domínio, não a cópia das rotas sugeridas.
- As operações de escrita são extensões internas fictícias. Nunca apresentar a solução como certificada ou regulatoriamente aderente. Consentimento e OAuth2 estão fora do escopo obrigatório.
- Executável localmente, sem conta em nuvem nem credenciais corporativas; nenhum segredo real no repositório.

## Prioridades

- Do case: primeiro o CRUD com persistência e regras; depois testes e README; por fim as consultas no formato Open Insurance e os diferenciais, conforme o tempo.
- policy-info e CRUD são o foco principal. Os demais GETs do padrão são opcionais.
- Qualidade, testes e decisões justificadas acima de quantidade de funcionalidades. Uma entrega menor e totalmente funcional vale mais que uma maior com algo quebrado.
- Diferenciais não são eliminatórios e sua ausência não reduz a nota. É preferível um CRUD consistente e testado a integrações incompletas.
- Um diferencial só entra com trade-off explícito e prova por teste. Diferencial instável sai da entrega e vira limitação declarada.
- Sem sobreengenharia: separar HTTP, negócio e dados sem arquitetura excessivamente elaborada.

## Avaliação

- O case vale 100 pontos; os diferenciais são discutidos qualitativamente.
- É valorizada a capacidade de explicar decisões, trade-offs e limitações.
- A conversa técnica cobre: demonstração do CRUD, mapeamento do Swagger, persistência, testes e escolhas de arquitetura.

## Destaques

Derivado do case e da vaga (desenho de solução, qualidade e confiabilidade, documentação, code review, eventos, observabilidade, modelagem de dados, SOLID e Clean Architecture, vivência regulatória):

- Toda decisão tem alternativa considerada e trade-off que caiba em duas frases. Sem isso, não entra.
- Toda afirmação não funcional (concorrência, desempenho, resiliência) tem evidência: teste ou medição. Sem medição, otimização é só afirmação.
- As regras difíceis são provadas por teste, não só descritas.
- Consistência entre controllers e entre as APIs: mesmo envelope de erro, mesmo estilo de validação, mesma semântica HTTP para o mesmo caso.
- Limitações e aderência parcial declaradas com honestidade no README.
- Operável por quem não escreveu: sobe com um comando, erros são rastreáveis e nenhum dado pessoal vaza em log.
- SOLID, Clean Architecture e padrões aplicados só onde há justificativa dita.
- Cada arquivo é explicável pelo autor na conversa técnica. Menos é mais.

## Entregáveis obrigatórios

Checklist de qualidade, todos exigidos pelo case:

- Java 25; execução local sem conta em nuvem.
- Separação entre camada HTTP, lógica de negócio e acesso a dados.
- Regras mínimas: identificador único, inclusive sob concorrência, por mecanismo do banco; fim da vigência não anterior ao início; campos obrigatórios e formatos validados; identificadores imutáveis não atualizáveis sem justificativa explícita; comportamento de exclusão/cancelamento e de consulta de registros excluídos definido e documentado; respostas HTTP coerentes; ao menos uma regra de negócio adicional documentada.
- Somente dados fictícios.
- Testes unitários das regras principais e ao menos um teste de integração de endpoint ou persistência.
- Contratos documentados: OpenAPI gerado pela aplicação ou exemplos de requisição e resposta no README.
- README com: pré-requisitos; build; execução; inicialização do banco; testes; exemplos `curl` de criação, consulta, atualização e exclusão; decisões arquiteturais, incluindo o motivo da escolha do banco e a estratégia de unicidade e consulta; limitações; mapeamento para o YAML oficial (campo implementado, adaptado ou omitido, com motivo); declaração de uso de IA.

## Onde cada informação vive

- Endpoints, status HTTP, payloads, campos e enums: código, testes, OpenAPI gerado e README.
- Mapeamento para o YAML oficial e limitações: README.
- Stack, alternativas descartadas e decisões com trade-off: README e, quando o usuário autorizar, ADR em `docs/adr/`.
- Este arquivo: objetivo, princípios, restrições e convenções.

## Trabalho com a IA

- **Só gerar código quando pedido explicitamente.** Fora disso, apoiar: explicar, revisar, apontar riscos.
- **O escopo de cada passo é o prompt do usuário, nada além.** Nunca propor, numerar nem citar fatias, etapas ou roteiro, nem anunciar o que "vem a seguir".
- **Seguir o padrão existente.** Antes de criar controller, DTO, configuração ou teste, ler o equivalente que já existe e repetir sua forma. Variação só com motivo dito.
- Não implementar o que o contrato oficial permite ou o Manual exige se o case não pede.
- Contradição entre case, código e este arquivo: apontar explicitamente, nunca resolver em silêncio.
- ADR e documentação de decisões só quando o usuário disser que é hora.
- **Definição de pronto:** compila; testes passam; existe uma forma concreta de exercitar o que entrou (teste, `.http` ou Swagger); cada arquivo novo é explicável.

## Convenções

| Tema | Regra |
|---|---|
| Identificadores | Inglês |
| Mensagens internas de exceção | Inglês, com código de erro estável (ex.: `LMI_ABOVE_LMG`) |
| Mensagens retornadas pela API | Português, curtas |
| Vocabulário | De negócio (endosso, LMG, LMI, vigência, cosseguro) em código, testes e documentação |
| Modelagem | Records para objetos de valor e DTOs; sem Lombok; sem setters públicos no agregado |
| DTOs | Sufixo `DTO` em requisições, respostas e comandos de caso de uso, inclusive records aninhados; em subpacote `dto` do domínio dentro da camada |
| Pacotes | Cada camada e cada adapter se organizam por domínio; o que serve a vários domínios fica na raiz da camada ou do adapter |
| Dinheiro | `BigDecimal` com moeda; nunca `double`; mais de duas casas é rejeitado, nunca arredondado |
| Datas | `LocalDate` puro, sem hora nem fuso |
| Tempo | O domínio nunca chama `now()`; recebe a data ou o instante de um `Clock` injetado |
| Dados | Somente fictícios; documento do segurado mascarado; nenhum dado pessoal em log |
| Credenciais locais | Fictícias e só de desenvolvimento; `.env` fora do git |
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

## Arquitetura

Camadas de domínio, aplicação e adapters, com as dependências apontando para dentro:

- O domínio é Java puro: sem Spring, JPA, Jackson ou Bean Validation.
- A aplicação depende só do domínio. O único elemento de framework tolerado é `@Transactional`.
- Adapters dependem de aplicação e domínio, nunca uns dos outros.
- O modelo de persistência fica no adapter, com mapeador; o domínio não conhece o banco.

Princípios do domínio:

- Validação em duas camadas: formato na borda; regra de negócio no domínio, venha a entrada de onde vier (API, seed, evento).
- Campo obrigatório nulo dentro do domínio é bug (a borda deveria ter barrado) e falha com `NullPointerException`.
- Objetos de valor são imutáveis e validados na construção: um objeto inválido não pode existir.
- Invariantes que dependem do conjunto pertencem à raiz do agregado, não às partes.
- Exceções de negócio carregam código estável, ficam juntas no domínio e não conhecem HTTP; a tradução para status é só do adapter web.

## Negócio

A dor central é a confiabilidade do dado: ele deixa de ser interno e passa a ser visível para o mercado.

- **Operador interno** (backoffice): único que altera dados, pela API administrativa.
- **Instituição participante:** só consulta, no formato inspirado no Open Insurance.
- **Segurado:** não usa o sistema, mas é o dono dos dados.

Invariantes:

- A unicidade da apólice é garantida pelo banco, não por verificação prévia, que falha sob concorrência.
- Identificadores são imutáveis; o pedido de alteração não deve conseguir expressá-los.
- Vigência com fim não anterior ao início, inclusiva nas duas pontas; a vigência de cada cobertura fica contida na da apólice.
- O LMI de cada cobertura não excede o LMG da apólice.
- Cancelar não é apagar: a apólice vigorou, pode ter tido sinistros e há obrigação de guarda de registros. O encerramento pelo fim da vigência deriva das datas, não é armazenado.
- Apólice cancelada não aceita alterações.
- Idempotência não é unicidade: uma trata o mesmo pedido repetido; a outra, pedidos diferentes disputando o mesmo identificador.

| Termo | Significado |
|---|---|
| Segurado / Corretor | Quem o contrato protege / intermediário que vende, sem assumir risco |
| Prêmio | Valor pago pelo segurado pela proteção |
| Sinistro / Indenização | Evento coberto que gera prejuízo / pagamento da seguradora após ele |
| Franquia | Parte do prejuízo que fica a cargo do segurado |
| Proposta | Pedido analisado na subscrição antes de virar apólice |
| Apólice / Vigência | O contrato, com número único / período em que a proteção vale |
| Cobertura / LMI | Cada risco protegido / seu Limite Máximo de Indenização |
| LMG | Limite Máximo de Garantia: teto da seguradora considerando as coberturas em conjunto |
| Endosso | Alteração formal de uma apólice emitida; o contrato continua o mesmo |
| Bilhete / Certificado | Contrato simplificado sem proposta formal / documento de cada participante de apólice coletiva |
| Cosseguro aceito | Participação num risco liderado por outra seguradora |

## Testes

- Domínio: JUnit puro, sem framework, com datas fixas; cobre cada invariante e cada código de erro.
- Integração sempre contra a infraestrutura real via Testcontainers; nunca um substituto em memória que se comporta diferente em locks e restrições.
- Em teste de fatia do Spring, propriedades e beans de configuração de que o código depende precisam ser declarados no próprio teste.
