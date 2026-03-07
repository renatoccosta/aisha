# Política de Privacidade da AI$HA

Última atualização: 07 de março de 2026.

## 1. Escopo desta política

A AI$HA é uma aplicação de gestão financeira pessoal com recursos de IA, distribuída para uso self-hosted.

Neste modelo, quem instala e opera a instância ("Operador") é o responsável por definir a finalidade do tratamento dos dados, a base legal aplicável e o período de retenção. O software não é oferecido como SaaS pela equipe do projeto.

## 2. Dados tratados pela aplicação

A aplicação pode tratar os seguintes dados, conforme o uso da instância:

- Dados de contas financeiras: título, descrição, saldo inicial e data de saldo inicial.
- Dados de categorias: título, descrição e hierarquia de categorias.
- Dados de lançamentos financeiros: conta, datas de movimentação e liquidação, descrição, categoria, observações, valor e identificador externo (quando informado).
- Dados de autenticação local: nome de usuário, hash de senha (BCrypt) e status de habilitação da conta.
- Dados de identidade federada (quando OAuth2/OIDC estiver habilitado): provedor, subject do provedor, e-mail e data/hora de criação do vínculo.
- Dados técnicos de sessão e requisição: identificador de sessão, endereço IP de origem, cabeçalho `X-Correlation-Id` (ou ID gerado automaticamente).

A aplicação não armazena senha em texto puro.

## 3. Finalidades do tratamento

Os dados são tratados para:

- Permitir autenticação e controle de acesso.
- Registrar, categorizar, consultar, importar e manter lançamentos financeiros.
- Gerar visões e relatórios financeiros no painel.
- Oferecer sugestão de categoria por IA local para apoiar o preenchimento de lançamentos.
- Garantir segurança operacional, rastreabilidade e diagnóstico de falhas por meio de logs técnicos.

## 4. Como a autenticação funciona

A AI$HA suporta autenticação local com sessão de servidor e pode suportar login federado OAuth2/OIDC, conforme configuração do Operador.

Controles de segurança implementados no baseline atual:

- Login por formulário (`/login`) e logout (`/logout`).
- CSRF habilitado globalmente.
- Proteção contra session fixation (migração de sessão após login).
- Timeout por inatividade de 45 minutos.
- Timeout absoluto de sessão de 12 horas.
- Limite de 1 sessão concorrente por usuário (novo login substitui o anterior).
- Cookie de sessão com `HttpOnly` e `SameSite=Lax`.
- Em perfil `prod`, cookie com `Secure=true`.

## 5. Logs e rastreabilidade

A aplicação registra eventos técnicos necessários à operação e segurança, incluindo:

- Sucesso e falha de autenticação.
- Logout.
- Timeout absoluto de sessão.
- Erros não tratados que retornam resposta ao usuário.

Os logs incluem metadados técnicos como usuário (quando aplicável), IP de origem, método/caminho da requisição, tipo de exceção e stack trace, além do ID de correlação.

A aplicação foi projetada para não registrar senhas em logs.

## 6. Uso de IA e minimização

As sugestões de categoria são geradas localmente na instância da aplicação, com base nos dados de lançamentos e categorias disponíveis no banco da própria instância.

Não há envio automático de dados financeiros para serviços externos de IA pelo fluxo padrão atual.

## 7. Compartilhamento de dados

No modo self-hosted padrão, a aplicação não realiza compartilhamento automático de dados pessoais com terceiros.

Quando o Operador habilita login federado (por exemplo, OIDC), ocorre troca de dados de autenticação com o provedor escolhido para viabilizar o login.

## 8. Retenção e exclusão

A retenção dos dados depende das decisões de configuração e governança do Operador da instância.

O software fornece operações de manutenção e exclusão de dados no domínio da aplicação, respeitando as regras de integridade referencial e consistência transacional.

## 9. Segurança

A AI$HA adota medidas técnicas no código-base para reduzir risco de acesso indevido e apoiar auditoria, incluindo:

- Hash de senha com BCrypt.
- Controles de sessão descritos nesta política.
- Validação de entrada no backend.
- Uso de migration versionada (Flyway) para evolução de schema.
- IDs de correlação para rastreamento de incidentes.

Nenhuma medida é absoluta. O Operador também deve adotar boas práticas de infraestrutura, rede, backups e gestão de credenciais.

## 10. Direitos do titular e contato

Como a AI$HA é self-hosted, solicitações de titulares de dados (acesso, correção, exclusão, portabilidade e outros direitos previstos em lei) devem ser direcionadas ao Operador da instância específica.

A equipe do projeto open source não possui acesso automático aos dados das instâncias self-hosted.

## 11. Alterações desta política

Esta política pode ser atualizada para refletir mudanças legais, técnicas ou funcionais. A data de "Última atualização" no topo do documento indica a versão vigente.
