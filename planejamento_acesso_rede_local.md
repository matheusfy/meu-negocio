# Planejamento — Acesso pela rede local de casa

> Objetivo: abrir o Ateliê de Decantes no celular e em outros aparelhos da casa,
> na mesma rede Wi-Fi. Sem publicar nada na internet.

## Como está agora

- O Spring Boot serve **tudo** (front-end estático + API REST) na porta **8080**.
- `application.properties` fixa `server.address=0.0.0.0` → o app aceita conexões de
  qualquer aparelho da rede, não só do `localhost`.
- Não há login. Qualquer aparelho na rede que abrir o endereço consegue ler e gravar.
  Aceitável só dentro de casa (ver "Segurança" no fim).

## Testar no celular agora — passo a passo

1. **Subir o app no PC** (Docker Desktop aberto para o Postgres subir junto):
   ```
   cd meu-negocio
   JAVA_HOME="C:\Users\mathe\OneDrive\Documentos\javas\java-21-openjdk-21.0.4.0.7-1.win.jdk.x86_64\java-21-openjdk-21.0.4.0.7-1.win.jdk.x86_64" ./gradlew.bat bootRun --console=plain
   ```
   Esperar `Started MeuNegocioApplication`. No PC, abrir `http://localhost:8080`.

2. **Descobrir o IP do PC na rede.** No PowerShell:
   ```
   ipconfig
   ```
   Anotar o **Endereço IPv4** do adaptador Wi-Fi (algo como `192.168.0.42` ou `192.168.1.15`).

3. **Liberar a porta 8080 no Firewall do Windows** (uma vez). PowerShell **como administrador**:
   ```
   netsh advfirewall firewall add rule name="MeuNegocio 8080" dir=in action=allow protocol=TCP localport=8080
   ```
   (Para remover depois: `netsh advfirewall firewall delete rule name="MeuNegocio 8080"`.)

4. **No celular**, com o Wi-Fi na **mesma rede** do PC, abrir:
   ```
   http://192.168.0.42:8080
   ```
   (troque pelo IP do passo 2). Dá para "Adicionar à tela inicial" e fica com cara de app.

## Se não abrir

- **Firewall** ainda bloqueando: confirme a regra do passo 3; teste desligando o firewall por 1 min só para diagnosticar.
- **Rede de convidados / isolamento de clientes**: alguns roteadores separam o Wi-Fi de convidados do principal, ou têm "AP isolation" ligado. Use a mesma SSID do PC e desligue isolamento de clientes.
- **O IP do PC mudou**: o roteador dá IP por DHCP e ele troca. Reserve um IP fixo para o PC nas configurações do roteador (DHCP reservation / "IP estático por MAC").
- **`bootRun` falha no Flyway ("checksum mismatch" / tabela não existe)**: o Postgres de dev
  local ficou preso a uma versão antiga das migrations (as migrations foram consolidadas nos
  PRs #57–#59 e o banco nunca foi recriado). Não há tarefa `flywayRepair` neste projeto e
  `repair` não bastaria (faltam tabelas). Recriar o banco de dev — é descartável, não tem
  seed nem dados reais:
  ```
  cd meu-negocio
  docker compose down -v      # remove o container e o volume do Postgres
  ```
  No próximo `bootRun` o `spring-boot-docker-compose` sobe um Postgres limpo e o Flyway roda
  V1–V4 do zero. O CI não é afetado (lá o banco já nasce limpo a cada build).
  *(Feito em 2026-08-29; o banco antigo tinha 0 produtos.)*

## Roteiro do "de verdade" (deixar rodando em casa)

Em ordem de esforço:

1. **IP reservado + nome amigável.** Reservar o IP do PC no roteador e, opcionalmente,
   um nome (`meunegocio.local`) via mDNS/Bonjour ou pelo arquivo `hosts` dos aparelhos.
2. **App sempre no ar.** Rodar como serviço que reinicia sozinho:
   - Windows: [NSSM](https://nssm.cc/) registrando `java -jar meu-negocio.jar` como serviço, **ou**
   - Docker: `Dockerfile` do app + `compose` com `restart: unless-stopped`.
3. **Empacotar em Docker.** Um `Dockerfile` (build do jar → imagem) e um `compose.yaml`
   com **app + Postgres com volume persistente**. Adicionar rotina de backup do volume
   (dump diário do Postgres para uma pasta do OneDrive, por exemplo).
4. **HTTPS local** (tira o aviso "não seguro" do navegador): Caddy ou nginx na frente,
   com certificado gerado por [`mkcert`](https://github.com/FiloSottile/mkcert) e a CA
   instalada nos aparelhos.
5. **Acesso de fora de casa** (opcional): [Tailscale](https://tailscale.com/) ou WireGuard —
   cria uma rede privada entre os aparelhos sem abrir porta no roteador.
   **Não** encaminhar a 8080 para a internet sem antes ter login.

## Segurança

- Hoje **não há autenticação**. Enquanto for só a rede de casa, tudo bem.
- Antes de qualquer acesso de fora da LAN: adicionar `spring-boot-starter-security`
  com um login simples (a mãe + você). Fica para um PR próprio quando #5 do roteiro entrar.
- Multiusuário de verdade (`user_id` deixa de ser fixo em 1) só se isto virar produto.
