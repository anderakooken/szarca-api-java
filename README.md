# Szarca API - Java / JSON

API que permite executar consultas em diversos bancos de dados relacionais (Oracle, SQLServer [...]) e retornar os resultados no formato JSON. A ideia do software é padronizar informações distribuídas e possibilitar o uso em programas de BI, tal como Power BI, SSIS ou Pentaho, por exemplo, ou para consumo em outras API.

## Ambiente

O aplicativo usa o Springboot Framework, então é recomendado o uso do **Open JDK 17 LTS ou superior**.  Para realização dos testes de requisições no formato JSON, sugiro utilizar o [Insomnia](https://insomnia.rest/download) ou [Postman](https://www.postman.com).

## Estrutura do Software

|Arquivo |                          |                         |
|----------------|-------------------------------|-----------------------------|
|`start.bat`        | `Inicializador para Windows`            |   Executar o arquivo para iniciar com a janela do CMD   |
| `start.sh`       |`Inicializador para Linux`| |
|`szarca-api-x.x.x.jar`| Executável | Funções do sistema
|`szarca-api.db`| Banco de dados (SQLite)           | Tabelas (Usuários, Funções e Fontes de dados) |
| `cache/`       |`Diretório onde serão armazenados os arquivos em cache`| Caso seja usado nas funções, deverá atentar-se as permissões de escrita|
| `szarca-api.db.default-x.x.x.rar`       |`Banco de dados padrão`| Caso precise reinstalar o sistema|
| `parameters.json`       |Parametros de configuração do sistema| |

## Inicialização:

### Windows e Linux
Para iniciar, execute o arquivo `start.bat` ou `start.sh`. O serviço responderá na porta 9032 por padrão, recebendo as requisições no formato JSON.  Para alterar a porta, edite o arquivo `start` no seu editor de texto e altere o valor `--server.port=9032`.

### Docker / Kubernetes
Execute o arquivo `szarca-api-x.x.x.jar --server.port=9032`.

### Exemplo de requisição:

Para iniciar, execute o arquivo `start.bat` ou `start.sh`. O serviço responderá na porta 9032 por padrão, recebendo as requisições no formato JSON.  Para alterar a porta, edite o arquivo `start` no seu editor de texto e altere o valor `--server.port=9032`.

Header
```
curl --location 'http://localhost:9032/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição
```json
{
    "logon":
        {   
            "user":"root",
            "passwd":"root"
        },
        "function":"users()",
        "param":{}
}
```
|Parâmetros|                          |                         |
|----------------|-------------------------------|-----------------------------|
|user / passwd| usuário e senha do software | Por padrão, root/root 
|function| Função que trará as informações da fonte de dados | Por padrão, o sistema já possui : `users()` -> lista os usuários do sistema, `functions()` -> lista as funções e `sources()` -> lista as fontes de dados. Geralmente conexões com bancos de dados relacionais e não relacionais.
|param | Filtros que serão executados na consulta | Deverá ser informado como JSONArray. Exemplo: `"param":{"data":"00/00/0000","cliente":"idcliente"}`

### Adicionando a primeira fonte de dados

As fontes de dados são geralmente as credenciais para banco de dados e outras aplicações. Como exemplo, iniciaremos com a adição de acesso a base MySQL como um Source.

Header

*Funções de gerenciamento `"management"` devem ser enviadas para `http://localhost:9032/admin/`*
*Por padrão, o sistema já vem habilitado para receber solicitações de gerenciamento*
```
curl --location 'http://localhost:9032/admin/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição
```json
{
    "management": {
        "function":"addSource",
        "param":{
            "name" : "minhaConexao",
            "database" : "mysql",
            "host" : "127.0.0.1",
            "port" : "3306",
            "user" : "meuUsuarioMySQL",
            "passwd" : "minhaSenhaMySQL",
            "schema" : "meuSchemaMySQL"
        }
    }
}
```
|Parâmetros|                          |                         |
|----------------|-------------------------------|-----------------------------|
|function| Função que executará comandos na base do sistema | `addSource`, `addUser`, `addFunction` para adicionar fontes, usuários e funções. Para remover, basta trocar `add` por `remove`, exemplo `removeSource`.
|name| Identificador único da fonte de dados (ID) |

Ao realizar o procedimento, deverá receber a seguinte resposta:
```json
{
    "system": {
        "environment": "Java",
        "author": "@anderakooken",
        "name": "Szarca",
        "version": "2.2.0",
        "architecture": "json"
    },
    "return": {
        "type": "true"
    }
}
```
Onde no objeto `return` o tipo `type` deverá ser **`true`**.  Caso retorne **`false`**, a estrutura da requisição foi no formado incorreto ou a fonte de dados já existia.

Vamos consultar a lista de fontes de dados:

Header
```
curl --location 'http://localhost:9032/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição

```json
{
	"logon": {"user":"root","passwd":"root"},
	"function":"sources()", "param":{}
}
```

#### Remoção da Fonte de Dados

Para remover a fonte de dados, realize a requisição abaixo informando como `name` o identificador único da fonte (ID).

Header
```
curl --location 'http://localhost:9032/admin/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição
```json
{
    "management": {
        "function":"removeSource",
        "param":{
            "name" : "minhaConexao"
        }
    }
}
```

### Adicionando a primeira função

As funções são geralmente as consultas SQL aos banco de dados relacionais e não relacionais. Como exemplo, iniciaremos com a adição de uma consulta SQL para a fonte de dados que adicionamos. O sistema só aceita consultas com o texto convertido em [Base64](https://www.base64encode.org).

Vamos supor que tenhas a seguinte consulta no seu banco de dados.
```sql
SELECT nome, email, matricula FROM usuarios WHERE id_usuario = @idUsuario
```
O parâmetro `@idUsuario` terá o seu valor informado no momento das requisições da função.

A consulta deverá ser convertida em Base64 para aceitação:
```
U0VMRUNUIG5vbWUsIGVtYWlsLCBtYXRyaWN1bGEgRlJPTSB1c3VhcmlvcyBXSEVSRSBpZF91c3VhcmlvID0gQGlkVXN1YXJpbw==
```

Header
```
curl --location 'http://localhost:9032/admin/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição
```json
{
    "management": {
        "function":"addFunction",
        "param":{
            "name" : "meusUsuarios",
            "source" : "minhaConexao",
            "setCache" : "0",
            "cacheDuration" : "0",
            "queryText" : "U0VMRUNUIG5vbWUsIGVtYWlsLCBtYXRyaWN1bGEgRlJPTSB1c3VhcmlvcyBXSEVSRSBpZF91c3VhcmlvID0gQGlkVXN1YXJpbw==",
            "queryTextParam" : "[\"idUsuario\"]",
            "date" : "2022-01-15 00:00:00",
            "status" : "1",
            "forceColumns" : "0",
            "queryColumnsText" : "",
            "tableCollection" : "",
            "fileQueryText" : "",
            "plainText" : ""
            
        }
    }
}
```
#### Parâmetros Obrigatórios

|Parâmetros|                          |                         |
|----------------|-------------------------------|-----------------------------|
|name| Identificador único da função |
|source| Identificador único da fonte de dados (ID) |
|setCache| Se a função deve um arquivo na pasta cache, marque `1`, ou `0` consulta online a fonte de dados|
|cacheDuration| Tempo de duração do arquivo cache. Em minutos. |
|queryText| Consulta convertida em `Base64` |
|queryTextParam| Parâmetro para filtrar a consulta | No exemplo, `@idUsuario` <-> `["idUsuario"]`, filtrará apenas o usuário que será informado na requisição futura
|date| Data de criação | Pode ser preenchida livremente no formato indicado `2000-01-01 00:00:00`
|status| `1` para função em produção, `0` para desativar|
|forceColumns| Altera o nome das colunas do retorno e força para um Alias informado |
|tableCollection| Para uso em banco de dados não relacional| Exemplo: Coleções no MongoDB

Vamos consultar as funções:

Header
```
curl --location 'http://localhost:9032/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição

```json
{
	"logon": {"user":"root","passwd":"root"},
	"function":"functions()", "param":{}
}
```

### Criando o primeiro usuário

O usuário `root` não realiza consultas em fontes externas e funções adicionadas pelo usuário, sendo seu uso exclusivo na administração das informações do software e nesse caso, teremos que criar o primeiro login de acesso.

Para tal, execute a seguinte requisição:

Header
```
curl --location 'http://localhost:9032/admin/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição
```json
{
    "management": {
        "function":"addUser",
        "param":{
            "user" : "usuario",
             "passwd" : "senha",
            "email" : "usuario@dominio.com",
            "roles" : ["minhaConexao"]
        }
    }
}
```

|Parâmetro|                          |                         |
|----------------|-------------------------------|-----------------------------|
|roles| Poderá ser múltiplo, caso existam varias fontes | `["minhaConexao","fonte2","fonte3"]`

Vamos consultar os usuários:

Header
```
curl --location 'http://localhost:9032/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição

```json
{
	"logon": {"user":"root","passwd":"root"},
	"function":"users()", "param":{}
}
```

#### Remoção da Permissão de Usuário a uma Fonte de Dados

Para remover o acesso a fonte de dados para um determinado usuário, realize a requisição abaixo informando como `user` o nome do login do usuário e `source` o nome da fonte de dados.

Header
```
curl --location 'http://localhost:9032/admin/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição
```json
{
    "management": {
        "function":"removeSecuritySource",
        "param":{
            "user" : "usuario",
            "source" : "minhaConexao"
        }
    }
}
```

### Realizando a primeira requisição a API

Header
```
curl --location 'http://localhost:9032/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição

```json
{
    "logon":{
		"user":"usuario",
		"passwd":"senha"
	},
	"function":"meusUsuarios",
	"param":{
		"idUsuario" : "15"
	}
}

```
Exibição do conjunto de resultados

```json

{
    "system": {
        "environment": "Java",
        "author": "@anderakooken",
        "name": "Szarca",
        "version": "2.2.0",
        "architecture": "json"
    },
    "return": {
        "date": "2023-05-13 10:00:28",
        "ipAddress": "0:0:0:0:0:0:0:1",
        "type": "true",
        "message": {
            "cache": {
                "definiteTime": "0"
            },
            "function": "meusUsuarios",
            "resultset": [],
            "source": "minhaConexao",
            "parameters": {}
        },
        "user": {
            "name": "usuario",
            "login": "usuario",
            "email": "usuario@dominio.com"
        }
    }
}

```
|Retorno|                          |                         |
|----------------|-------------------------------|-----------------------------|
|resultset| Conjunto de resultados em JSONArray  |


### Bloquear o acesso do gerenciamento

Antes de colocar o sistema em produção, atente-se ao arquivo `parameters.json`, no objeto `"enableManagement" : true`. Por padrão, o sistema já é habilitado para usar o usuário `root`, nesse caso, marcado como valor `true`. Contudo, por segurança, marque o valor como `false` para evitar alterações indevidas por terceiros.

### Usando o cache

Para habilitar o armazenamento do conjunto de resultados no diretório  `cache/`, marque objeto `setCache` com o valor `1` e `cacheDuration` com o valor em minutos, exemplo `30`, onde as requisições consultarão o cache ao invés de consultar a fonte de dados. Essa função é útil em caso de dados que são atualizados em grande sazonalidade.

### Habilitar ALIAS

O ALIAS funciona basicamente igual a linguagem SQL, onde trocamos o nome do campo real por um apelido, exemplo:

```sql 
SELECT u.mat as Matricula, u.nome as NomeUsuario from Usuario 
``` 

Ao alterar o valor do `forceColumns` para `1`, o sistema solicitará que o objeto`queryColumnsText` contenha os valores a serem convertidos.

```json
{"queryColumnsText" : "[{\"mat\":\"Matricula\"},{\"nome":"NomeUsuario\"}]"}
```
* Importante, uma vez habilitado o ALIAS, todos os campos contidos na consulta deverão ser informados na `queryColumnsText` ou não haverá resposta na requisição.

### NoSQL - Bancos Não Relacionais

Atualmente o sistema permite conexões com o **MongoDB**, porém no momento da criação das funções, o valor parâmetro `tableCollection` deverá conter obrigatoriamente o nome da coleção.

### Uso de E-mails

Para envio de e-mails, será necessário incluir um fonte de dados:

Header
```
curl --location 'http://localhost:9032/admin/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição
```json
{
    "management": {
        "function":"addSource",
        "param":{
            "name" : "smtp_pessoal",
            "database" : "smtp",
            "host" : "mail.meudominio.com",
            "port" : "587",
            "user" : "eu@meudominio.com",
            "passwd" : "U0VMRUNUIG5v==",
            "schema" : "Nome/Sobrenome"
        }
    }
}
```
|Parâmetros|                          |                         |
|----------------|-------------------------------|-----------------------------|
|name| Identificador único da fonte de dados (ID) |
|database| Obrigatoriamente com o valor `smtp`| Informa ao sistema para usar protocolo de envio
|host| IP do servidor para envio de e-mails |
|schema| Nome que será exibido nos e-mails enviados|

A senha do e-mail deverá ser convertida em Base64 para aceitação:
```
U0VMRUNUIG5v==
```
#### Instruções

As funções neste caso, diferentes de consultas SQL, será o corpo do e-mail propriamente dito. Como exemplo, iniciaremos com a adição de texto para o e-mail. O sistema só aceita consultas com o texto convertido em [Base64](https://www.base64encode.org).

Vamos colocar uma mensagem de teste.
```
@saudacao, meu nome é @nome. Este e-mail é apenas um teste.
```
Os parâmetros `@saudacao` e `@nome` terão os seus valores informados no momento da requisição da função.

A consulta deverá ser convertida em Base64 para aceitação:
```
QHNhdWRhY2FvLCBtZXUgbm9tZSDDqSBAbm9tZS4gRXN0ZSBlLW1haWwgw6kgYXBlbmFzIHVtIHRlc3RlLg==
```

#### Função para Envio de E-mail

Header
```
curl --location 'http://localhost:9032/admin/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição
```json
{
    "management": {
        "function":"addFunction",
        "param":{
            "name" : "emailTeste",
            "source" : "smtp_pessoal",
            "setCache" : "0",
            "cacheDuration" : "0",
            "queryText" : "QHNhdWRhY2FvLCBtZXUgbm9tZSDDqSBAbm9tZS4gRXN0ZSBlLW1haWwgw6kgYXBlbmFzIHVtIHRlc3RlLg==",
            "queryTextParam" : "[\"saudacao\",\"nome\"]",
            "date" : "2022-01-15 00:00:00",
            "status" : "1",
            "forceColumns" : "0",
            "queryColumnsText" : "teste1@email.com,teste2@mail.com",
            "tableCollection" : "",
            "fileQueryText" : "",
            "plainText" : "Assunto do E-mail"
            
        }
    }
}
```
#### Parâmetros Obrigatórios

|Parâmetros|                     |                         |
|-------------------|-------------------------------|-----------------------------|
|name| Identificador único da função |
|source| Identificador único da fonte de dados (ID) |
|queryText| Corpo do E-mail convertido em `Base64` |
|queryTextParam| Parâmetro para filtrar a consulta | No exemplo, `@saudacao` <-> `["saudacao"]`, filtrará apenas o usuário que será informado na requisição futura |
|queryColumnsText | E-mail dos destinatários | Podem ser vários e-mails separados por `,`. |
|plainText | Titulo/Assunto do E-mail |

#### Restrições no uso do `queryColumnsText` no E-mail

O sistema não aceitará que esse objeto tenha seus valores recebidos por um parâmetro, porque não serve ao proposito da aplicação. No caso, o desenvolvedor deverá criar sua própria rotina em scripts ou aplicações externas.

#### Chamada de Função - E-mail

Header
```
curl --location 'http://localhost:9032/' \
--header 'Content-Type: application/json' \
--data ''
```
Requisição
```json
{
    "logon":{
		"user":"seu_usuario",
		"passwd":"sua_senha"
	},
	"function":"emailTeste",
	"param":{
		"saudacao" : "ola, bom dia.",
        "nome" : "John Doe"
	}
}
```
Exibição do conjunto de resultados

```json
{
    "system": {
        "environment": "Java",
        "administrator": "@anderakooken",
        "name": "Szarca",
        "version": "2.3.0",
        "architecture": "json"
    },
    "return": {
        "date": "2023-05-17 14:02:58",
        "ipAddress": "0:0:0:0:0:0:0:1",
        "type": "true",
        "message": {
            "cache": {
                "definiteTime": "0"
            },
            "function": "emailTeste",
            "resultset": [
                {
                    "message": "E-mail Enviado com Sucesso.",
                    "status": "true"
                }
            ],
            "source": "smtp_pessoal",
            "parameters": {
                "saudacao": "ola, bom dia",
                "nome": "John Doe"
            }
        },
        "user": {
            "name": "anderakooken",
            "login": "anderakooken",
            "email": "you@mail.com"
        }
    }
}
```