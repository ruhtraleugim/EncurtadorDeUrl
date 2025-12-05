# Encurtador de URL 

É a minha visão do **porquê** e **das decisões que tomei** para que o encurtador funcione em altíssima escala, 24/7, por 10 anos, com URLs extremamente curtas.

---

## 🎯 Objetivo do sistema
Criar um encurtador de URL que opere sem interrupção, capaz de lidar com **100 milhões de novas URLs por dia**, e que mantenha tudo salvo por **10 anos**, sem perder performance, sem gerar colisões e mantendo as URLs curtas.

---

## 🧠 Decisões principais

### 1. **Charset definido: Base62 (0–9, a–z, A–Z)**
Decidi usar Base62 porque é o conjunto mais compacto possível que ainda é universalmente compatível e não causa problema em nenhum navegador.
Isso também maximiza o espaço de combinações.

### 2. **Tamanho do código curto: 7 caracteres**
Não escolhi 7 à toa.
Com 100M novas URLs por dia durante 10 anos, teremos **365 bilhões de códigos**.

- 62^6 não aguenta isso.
- 62^7 aguenta com sobra — então essa foi a escolha natural.

Decisão: Comprimento variavel começando em 5 caracteres indo até 7; pois evita que "chutem as urls".

### 3. **Banco de dados: Cassandra / ScyllaDB**
Escolhi Cassandra porque:
- escala horizontal praticamente infinita;
- escrita barata e distribuída (e nós teremos MUITA escrita);
- leitura rápida quando combinada com cache;
- tolerância a falhas absurda.

Postgres sharded seria possível, mas muito mais caro e frágil para o tráfego esperado.

### 4. **Cache pesado para leitura: Redis + CDN**
Como a proporção é **1 gravação para 10 leituras**, fica obvio que:
- Não podemos ir ao banco para cada GET.
- A CDN deve responder a maior parte dos redirecionamentos.
- Redis entra como cache quente para os misses.

### 5. **Armazenamento por 10 anos**
O total de dados brutos chega facilmente acima de **100 TB**.
Por isso, a decisão foi:
- dados recentes ficam no banco principal;
- dados antigos vão para armazenamento frio — mas sem perder a capacidade de restaurar quando necessário.

### 6. **API simples e direta**
Endpoint principal: `POST /api/v1/shorten`.
Sem complicação.
Apenas recebe uma URL, valida e retorna o código curto.

### 7. **Validação: máximo 100 bytes**
Decidi impor esse limite porque:
- evita payload gigante;
- mantém o sistema previsível;
- ajuda no armazenamento e indexação.

### 8. **Política de TTL: 10 anos fixos**
O sistema aceita TTL menor, mas nunca maior.
Isso garante consistência e permite controlar o ciclo de vida dos dados.

### 9. **Geração de código: contador distribuído + Base62**
Nada de aleatoriedade.
Aleatório é fácil, mas gera colisões e exige lock ou checagem.
Decidi por algo determinístico:
- contador global shardeado;
- conversão para Base62;
- prefixo implícito por worker, se necessário.

Resultado: geração estável, sem colisão e com throughput absurdo.

### 10. **Observabilidade obrigatória**
Decidi que o sistema só é aceitável se tiver:
- logs estruturados;
- métricas de latência e taxa de erro;
- alertas de tráfego anômalo;
- tracing distribuído.

Sem isso, não dá pra operar algo que funciona 24/7.

---

## 🔐 Decisões sobre segurança

### 🔒 Como cada proteção será implementada

#### **1. Rate Limit distribuído (proteção contra flood)**
Decisão: usar **Token Bucket distribuído em Redis**.
- Cada IP recebe um "balde" com X requisições por segundo.
- A cada encurtamento, o sistema tenta consumir 1 token.
- Se o balde estiver vazio → `429 Too Many Requests`.
- Redis garante operações atômicas e funciona em cluster.

**Protege contra:** bots, automação agressiva e geração em massa de URLs.

---

#### **2. Filtro contra URLs maliciosas (anti-phishing e anti-malware)**
Pipeline de validação com múltiplas camadas:
1. Regex básico para validar formato.
2. Bloqueio de esquemas perigosos: `javascript:`, `data:`, `file:`.
3. Verificação de domínios suspeitos (blocklist).
4. Integração com APIs de segurança (Google Safe Browsing / Cloudflare Security).
5. Normalização da URL (remover unicode invisível, espaços, caracteres suspeitos).

**Protege contra:** malware, phishing, golpes e exploração via links.

O unico problema ainda é o usuario.

---

#### **3. Bloqueio automático de padrões suspeitos (detecção de abuso)**
Sistema de heurística + regras automáticas:
- explosão súbita de requisições por IP;
- URLs muito parecidas sendo criadas em massa (indicador de automação);
- parâmetros ofuscados ou padrões de spam;
- repetição de URLs para domínios com má reputação.

Ações automáticas:
- bloqueio de IP temporário;
- aumento de rate limit para o infrator (ban progressivo);
- marcação de domínio como suspeito;
- exigência de captcha;
- alerta enviado para observabilidade.

**Protege contra:** spam, abuso automatizado, ataques, exploração do encurtador.

---
- rate limit: não dá para alguém disparar milhões de encurtamentos por minuto;
- filtro contra URLs maliciosas;
- bloqueio automático de padrões suspeitos.

Sistema robusto exige proteção contra abuso.

---

## 🌐 Decisão de Roteamento: CDN na frente sempre
O GET /{code} nunca deveria bater diretamente no backend.
Decisão: toda resolução passa por CDN (Cloudflare, AWS CloudFront ou similar).

---

## 🛠 Por que Java + Spring?
- confiável;
- estável sob carga extrema;
- fácil de escalar;
- ecossistema maduro para observabilidade e segurança.

Com o tráfego esperado, Node/Express ou Python/Django sofreriam muito mais.

---

## 🚀 Resumo da visão final
Este encurtador não é um "projetinho".
Ele foi pensado para durar **10 anos**, com **centenas de bilhões** de registros, funcionando **sem cair** e respondendo em **milissegundos**.

Tudo aqui foi decidido com foco em:
- escala;
- disponibilidade;
- baixo custo por operação;
- simplicidade operacional;
- robustez.
