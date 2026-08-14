# Fluxo de branches e contribuição

## Branches

- **`main`** — produção. Só recebe merge de `develop`, sempre via Pull Request.
- **`develop`** — integração. É a partir dela que, futuramente, será gerada a versão publicada em ambiente de teste. Só recebe merge das branches de task, sempre via Pull Request.
- **`issue-N-DescricaoTarefa`** — uma branch por task, criada a partir de `develop`, vinculada ao número da issue no GitHub. `N` é o número da issue e `DescricaoTarefa` é PascalCase, sem espaços/acentos/hífens. Exemplo: `issue-12-CadastroPedido`.

```
main
 └── develop
      └── issue-12-CadastroPedido
      └── issue-13-ConsultaPedidoPorCliente
      └── ...
```

## Fluxo de trabalho

1. Atualize sua `develop` local e crie a branch da task a partir dela, usando o nome da issue:
   ```bash
   git checkout develop
   git pull
   git checkout -b issue-12-CadastroPedido
   ```
2. Rode `./gradlew build` uma vez (dentro de `meu-negocio/`) — isso também configura automaticamente os hooks locais de validação de mensagem de commit e de nome de branch (veja abaixo).
3. Desenvolva e faça commits pequenos e descritivos, seguindo o padrão de mensagem de commit (veja abaixo).
4. Suba a branch e abra um Pull Request para `develop`.
5. O pipeline de CI (`.github/workflows/ci.yml`) roda automaticamente: validação das mensagens de commit, build, testes e checkstyle.
6. O PR só pode ser mergeado quando **todos os checks estiverem verdes** (regra configurada em Branch Protection, veja abaixo).
7. Depois de validado em `develop` (e, futuramente, no ambiente de teste), abre-se um Pull Request de `develop` para `main`, também sujeito ao mesmo gate de pipelines verdes.

## Padrão de mensagem de commit

Formato: `[tipo]: descrição` (escopo opcional: `[tipo(escopo)]: descrição`).

Tipos válidos: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`, `build`.

Exemplos:
```
[feat]: adiciona cancelamento de pedido
[feat(pedido)]: adiciona cancelamento de pedido
[fix]: corrige status inconsistente ao cancelar pedido
[docs]: atualiza README com instruções de build
```

A regra é validada em dois lugares:
- **Localmente**, via git hook (`.githooks/commit-msg`) — instalado automaticamente ao rodar `./gradlew build`. Para instalar manualmente sem buildar: `git config core.hooksPath .githooks`.
- **No CI**, via job `Commit Lint` em toda Pull Request — garante o padrão mesmo se alguém não tiver o hook local configurado.

## Padrão de nome de branch

Formato: `issue-N-DescricaoTarefa`, onde `N` é o número da issue no GitHub e `DescricaoTarefa` está em PascalCase (sem espaços, acentos ou hífens extras).

Exemplos:
```
issue-12-CadastroPedido
issue-13-ConsultaPedidoPorCliente
```

`develop` e `main` são as únicas exceções ao padrão (não precisam seguir esse formato).

Também validado em dois lugares:
- **Localmente**, via git hook (`.githooks/pre-push`) — instalado junto com o hook de commit, mesmo `./gradlew build`.
- **No CI**, via job `Branch Name Lint` em toda Pull Request, validando o nome da branch de origem.

## Configurando o gate no GitHub (feito uma vez, manualmente)

Isso precisa ser feito direto nas configurações do repositório (Settings → Branches → Branch protection rules), pois exige permissão de admin que a automação não tem. **Já configurado** para `develop` e `main`, documentado aqui para referência:

1. **Regra para `develop`**
   - Require a pull request before merging
   - Require status checks to pass before merging → `Commit Lint`, `Branch Name Lint`, `Build & Testes` e `Checkstyle`
   - Require branches to be up to date before merging

2. **Regra para `main`**
   - Mesma configuração acima
   - Opcional (GitHub com Rulesets/plano que suporte): restringir para que `main` só aceite merge vindo de `develop`

Os nomes dos checks só aparecem como opção depois que o workflow `ci.yml` rodar pelo menos uma vez no repositório.
