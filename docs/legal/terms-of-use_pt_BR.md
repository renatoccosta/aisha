# Termos de Uso da AI$HA

Última atualização: 07 de março de 2026.

## 1. Sobre a AI$HA e estes termos

A AI$HA é uma aplicação open source de gestão financeira pessoal com recursos de IA, distribuída para operação self-hosted.

Estes Termos de Uso descrevem as condições de utilização do software e os limites de responsabilidade entre:

- Os mantenedores do projeto open source.
- A parte que instala e opera uma instância específica ("Operador").
- Os usuários finais dessa instância.

Ao utilizar a AI$HA, você reconhece estes termos.

## 2. Licença open source

A AI$HA é distribuída sob a licença definida no arquivo `LICENSE` do repositório.

Seus direitos de uso, modificação e redistribuição dependem dessa licença. Em caso de conflito entre este documento e a licença, prevalece a licença.

## 3. Modelo self-hosted e responsabilidade

A AI$HA não é oferecida pela equipe do projeto como SaaS hospedado.

Em implantações self-hosted, o Operador é responsável por:

- Infraestrutura, hospedagem e controles de segurança.
- Gestão de acesso, backups e recuperação de desastres.
- Conformidade com leis e regulações aplicáveis.
- Definição de uso aceitável e regras de governança para os usuários da instância.

A equipe do projeto não possui acesso automático aos dados armazenados em instâncias self-hosted de terceiros.

## 4. Finalidade de uso e natureza financeira do software

A AI$HA foi projetada para apoiar rotinas de gestão financeira pessoal, como controle de contas, categorias, lançamentos e relatórios.

O software e seus recursos assistidos por IA são fornecidos para suporte informacional e operacional. Eles não constituem aconselhamento jurídico, tributário, contábil, de investimento ou fiduciário.

Usuários e Operadores permanecem integralmente responsáveis por:

- Decisões financeiras tomadas com base nas saídas da aplicação.
- Validação da qualidade dos dados e das expectativas de cálculo.
- Cumprimento de obrigações contábeis, tributárias e regulatórias da jurisdição aplicável.

## 5. Recursos assistidos por IA

A AI$HA pode oferecer sugestões locais por IA (por exemplo, sugestão de categoria) com base nos dados disponíveis na instância.

As saídas de IA podem ser incompletas ou incorretas e devem ser revisadas por usuários antes de serem utilizadas em registros ou decisões financeiras.

## 6. Baseline de segurança e autenticação

A aplicação inclui controles de segurança no baseline atual, como:

- Acesso autenticado para rotas protegidas.
- Proteção CSRF para requisições que alteram estado.
- Controles de sessão (proteção contra fixation, timeout por inatividade, timeout absoluto e limite de sessões concorrentes).
- Hash de senha para contas locais.

Como a AI$HA é self-hosted, o Operador deve complementar esses controles com boas práticas de segurança de infraestrutura.

## 7. Dados e auditabilidade

A AI$HA foi desenhada para apoiar correção e rastreabilidade de registros financeiros.

Operadores e usuários devem evitar uso não autorizado ou fraudulento, incluindo manipulação indevida de dados financeiros históricos, titularidade de contas ou registros relevantes para auditoria.

## 8. Disponibilidade e suporte

O software é fornecido "no estado em que se encontra" (as is), nos termos da licença do projeto.

A comunidade open source e os mantenedores não garantem operação ininterrupta, ausência de erros ou adequação para todos os contextos financeiros.

## 9. Limitação de responsabilidade

Na extensão máxima permitida pela legislação aplicável, mantenedores e contribuidores do projeto não se responsabilizam por perdas diretas ou indiretas decorrentes do uso, má configuração ou impossibilidade de uso da AI$HA.

Isso inclui, sem limitação, perda de dados, perdas financeiras, problemas de conformidade e indisponibilidade de serviço.

## 10. Alterações destes termos

Estes termos podem ser atualizados para refletir mudanças legais, técnicas ou funcionais.

A data de "Última atualização" no topo indica a versão atualmente publicada.
