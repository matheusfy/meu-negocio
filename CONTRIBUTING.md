# Fluxo de branches e contribuição

## Branches

- **`main`** — produção. Só recebe merge de `develop`, sempre via Pull Request.
- **`develop`** — integração. É a partir dela que, futuramente, será gerada a versão publicada em ambiente de teste. Só recebe merge das branches de task, sempre via Pull Request.
- **`feature/<descrição-curta>`** — uma branch por task, criada a partir de `develop`. Exemplo: `feature/cadastro-pedido`.

```
main
 └── develop
      └── feature/cadastro-pedido
      └── feature/consulta-pedido-por-cliente
      └── ...
```

## Fluxo de trabalho

1. Atualize sua `develop` local e crie a branch da task a partir dela:
   ```bash
   git checkout develop
   git pull
   git checkout -b feature/nome-da-task
   ```
2. Desenvolva e faça commits pequenos e descritivos.
3. Suba a branch e abra um Pull Request para `develop`.
4. O pipeline de CI (`.github/workflows/ci.yml`) roda automaticamente: build, testes e checkstyle.
5. O PR só pode ser mergeado quando **todos os checks estiverem verdes** (regra configurada em Branch Protection, veja abaixo).
6. Depois de validado em `develop` (e, futuramente, no ambiente de teste), abre-se um Pull Request de `develop` para `main`, também sujeito ao mesmo gate de pipelines verdes.

## Configurando o gate no GitHub (feito uma vez, manualmente)

Isso precisa ser feito direto nas configurações do repositório (Settings → Branches → Branch protection rules), pois exige permissão de admin que a automação não tem:

1. **Regra para `develop`**
   - Require a pull request before merging
   - Require status checks to pass before merging → selecionar `Build & Testes` e `Checkstyle`
   - Require branches to be up to date before merging

2. **Regra para `main`**
   - Mesma configuração acima (PR obrigatório + checks `Build & Testes` e `Checkstyle` verdes)
   - Opcional (GitHub com Rulesets/plano que suporte): restringir para que `main` só aceite merge vindo de `develop`

Os nomes dos checks só aparecem como opção depois que o workflow `ci.yml` rodar pelo menos uma vez no repositório.
