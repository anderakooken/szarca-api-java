package com.szarca.szarcaapi.controller;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

/**
 * @apiNote classe para administração das informações do Szarca
 */
public class Management {

    private final JSONObject header = new JSONObject();
    private String typeField = "false";
    private final String root = "/management/param/";

    /**
     * @apiNote função principal para instanciamento de ações CRUD
     * @param jsonString
     * @return
     */
    public JSONObject controller(String jsonString) {

        JSONObject json = new JSONObject(jsonString);
        String function = json.query("/management/function").toString();

        if (function.equals("addUser")) {

            return addUser(json);

        } else if (function.equals("addSource")) {

            return addSource(json);

        }  else if (function.equals("addSecuritySource")) {

            String user = json.query("/management/param/user").toString();
            String source = json.query("/management/param/source").toString();

            return queryInsertSources(new Environment().databaseLocation(), user, source);

        } else if (function.equals("addFunction")) {

            return addFunction(json);

        } else if (function.equals("removeUser")) {

            return removeUser(json);

        } else if (function.equals("removeSecuritySource")) {

            return removeSecuritySource(json);

        } else if (function.equals("removeFunction")) {

            return removeFunction(json);

        } else if (function.equals("removeSource")) {

            return removeSource(json);

        } else {
            header.put("type", typeField);
            header.put("message", "invalid method");
            return header;
        }
    }

    private JSONObject removeFunction(JSONObject json){
        String name = json.query(root+ "name").toString();

        /**
         * Remove as funções
         */
        return queryUpdate(name, "DELETE FROM functions WHERE name = ?");
    }
    private JSONObject removeSource(JSONObject json){
        String name = json.query(root+ "name").toString();

        /**
         * Remove as fonte de dados (conexoes)
         */
        return queryUpdate(name, "DELETE FROM sources WHERE name = ?");
    }
    private JSONObject removeSecuritySource(JSONObject json){

        String user = json.query(root+ "user").toString();
        String source = json.query(root+ "source").toString();

        /**
         * Remove as fonte de dados (conexoes), informando o ID usuario e ID source
         */

        try {

            Connection conn = DriverManager.getConnection(new Environment().databaseLocation());
            PreparedStatement statement = conn.prepareStatement("DELETE FROM securitysources WHERE user = ? AND source = ?");
            statement.setString(1, user);
            statement.setString(2, source);

            int out = statement.executeUpdate();

            if (out != 0) {
                typeField = "true";
            }

            header.put("type", typeField);
            conn.close();
            return header;

        } catch (Exception e) {

            header.put("type", typeField);
            return header;
        }
    }
    private JSONObject removeUser(JSONObject json){
        String user = json.query(root+ "user").toString();

        /**
        * Remove as permissões de acesso as fontes, para depois remover o usuario em si
        */
        queryUpdate(user, "DELETE FROM securitysources WHERE user = ?");

        return queryUpdate(user, "DELETE FROM users WHERE login = ?");
    }


    /**
     * @apiNote adiciona usuarios
     * @param json
     * @return JSONOBject (Boolean)
     */
    private JSONObject addUser(JSONObject json){

        /**
         * @user : identificador único do usuario
         * @passwd : senha do usuario
         * @roles : JsonArray contendo o nome das fontes de dados (Sources), exemplo, ["oracleTrabalho","mysqlPessoal","mongoTeste"]
         *      No momento da criação, o ArrayList incluirá na base de dados os registros de acesso na "SecuritySources"
         */
        String user = json.query(root+ "user").toString();
        String passwd = json.query(root+ "passwd").toString();
        String email = json.query(root+ "email").toString();
        JSONArray roles = new JSONArray(json.query(root+ "roles").toString());

        try {

            /**
             * Verifica se usuario existe, caso não, faz o insert
             */
            String sqlQuery =
                    """
                        INSERT INTO users (login, name, hash, email, sources, status)
                            SELECT * FROM (SELECT ?,?,?,?,?,'1') AS tmp
                            WHERE NOT EXISTS (
                                SELECT login FROM users WHERE login = ?
                            );
                    """;

            Connection conn = DriverManager.getConnection(new Environment().databaseLocation());
            PreparedStatement ps1 = conn.prepareStatement(sqlQuery);
            ps1.setString(1, user);
            ps1.setString(2, user);
            ps1.setString(3, passwd);
            ps1.setString(4, email);
            ps1.setString(5, roles.toString());
            ps1.setString(6, user);

            /**
             * Inserção das permissões de acesso aos sources
             */
            for(int i=0; i < roles.length(); i++){
                queryInsertSources(new Environment().databaseLocation(), user, roles.get(i).toString());
            }

            int out = ps1.executeUpdate();

            if (out != 0) {
                typeField = "true";
            }

            header.put("type", typeField);
            conn.close();
            return header;

        } catch (Exception e) {

            header.put("type", typeField);
            return header;
        }
    }

    /**
     * @apiNote Inserção de permissões do usuario a fonte de dados
     * @param url
     * @param user
     * @param role
     */
    private JSONObject queryInsertSources(String url, String user, String role){
        String sqlQuerySources =
                """
                    INSERT INTO securitysources (user, source, status)
                        SELECT * FROM (SELECT ?,?,'1') AS tmp
                        WHERE NOT EXISTS (
                            SELECT user, source FROM securitysources WHERE user = ? AND source = ?
                        );
                """;
        try{

            Connection conn2 = DriverManager.getConnection(url);
            PreparedStatement ps2 = conn2.prepareStatement(sqlQuerySources);
            ps2.setString(1, user);
            ps2.setString(2, role);
            ps2.setString(3, user);
            ps2.setString(4, role);

            int out = ps2.executeUpdate();

            if (out != 0) {
                typeField = "true";
            }

            header.put("type", typeField);
            conn2.close();
            return header;

        }catch (Exception e){

            header.put("type", typeField);
            return header;
        }
    }

    /**
     * @apiNote Função para inserção de novas conexões a fontes de dados
     * @param json
     * @return
     */
    private JSONObject addSource(JSONObject json){

        /**
         * @name : identificador único da fonte de informação
         * @database : sistema de gerenciamento de banco - MySQL, Oracle, MSSQL, MongoDB (Texto minusculo)
         * @host : IP ou FQDN do servidor
         * @port : Porta do serviço
         * @user : Usuario do SGBD
         * @passwd : Senha do SGBD
         * @schema : nome da base de dados
         */
        String name = json.query(root+ "name").toString();
        String database = json.query(root+ "database").toString();
        String host = json.query(root+ "host").toString();
        String port = json.query(root+ "port").toString();
        String user = json.query(root+ "user").toString();
        String passwd = json.query(root+ "passwd").toString();
        String schema = json.query(root+ "schema").toString();

        try {

            /**
             * Verifica se source existe, caso não, faz o insert
             */
            String sqlQuery =
                    """
                        INSERT INTO sources (name, sgbd, host, port, user, passwd, schema, status)
                            SELECT * FROM (SELECT ?,?,?,?,?,?,?,'1') AS tmp
                            WHERE NOT EXISTS (
                                SELECT name, sgbd, host, port, user, passwd, schema, status FROM sources WHERE name = ?
                            );
                    """;

            Connection conn = DriverManager.getConnection(new Environment().databaseLocation());
            PreparedStatement ps1 = conn.prepareStatement(sqlQuery);
            ps1.setString(1, name);
            ps1.setString(2, database);
            ps1.setString(3, host);
            ps1.setString(4, port);
            ps1.setString(5, user);
            ps1.setString(6, passwd);
            ps1.setString(7, schema);
            ps1.setString(8, name);

            int out = ps1.executeUpdate();

            if (out != 0) {
                typeField = "true";
            }

            header.put("type", typeField);
            conn.close();
            return header;

        } catch (Exception e) {

            header.put("type", typeField);
            return header;
        }
    }

    /**
     * @apiNote Função para inserção de novas funções (SQL queryText - Base64 Obrigatorio)
     * @param json
     * @return
     */
    private JSONObject addFunction(JSONObject json){

        /**
         * @name : identificador único da função
         * @source : identificador de conexão com a fonte de dados "SOURCES"
         * @setCache : Obriga ao sistema salvar resultado temporariamente no diretorio da aplicação para consultas posteriores
         * @cacheDuration : Tempo em "minutos" que levará para o sistema remover o arquivo no diretorio
         * @queryTextParam : Deve ser um JsonArray,
         *      exemplo, ["empresa","usuario","etc"]
         *
         * @queryText : deve ser uma consulta convertida previamente em BASE64.
         *
         * @queryColumnsText : deve ser um JsonArray contendo JsonObject,
         *      exemplo : [{"Rota":"rota"},{"Vendedor":"vendedor"},{"comissao_vendedor":"comissaoVendedor"}]
         *                [{"Rota":"rota"}] -> "Rota" é o nome do campo oriundo da consulta, "rota" é o ALIAS para exibição no arquivo de retorno
         *
         * @status : 1 - Habilitado, 0 - Desabilitado
         * @tableColletion : Caso de uso em banco de dados Não Relacional (NoSQL), a exemplo, MongoDB
         * @ForceColumns : 1 - Obriga que os campos retornados saiam com meu ALIAS que é informado no @queryColumnsText, 0 - retorna campos sem ALIAS
         */

        String name = json.query(root+ "name").toString();
        String source = json.query(root+ "source").toString();
        String setCache = json.query(root+ "setCache").toString();
        String cacheDuration = json.query(root+ "cacheDuration").toString();
        String fileQueryText = json.query(root+ "fileQueryText").toString();
        String plainText = json.query(root+ "plainText").toString();
        String date = json.query(root+ "date").toString();
        String queryTextParam = json.query(root+ "queryTextParam").toString();
        String forceColumns = json.query(root+ "forceColumns").toString();
        String tableCollection = json.query(root+ "tableCollection").toString();
        String queryColumnsText = json.query(root+ "queryColumnsText").toString();
        String queryText = json.query(root+ "queryText").toString();

        try {

            /**
             * Verifica se função existe, caso não, faz o insert
             */

            String sqlQuery =
                    """
                        INSERT INTO functions (
                            name, source, setCache, cacheDuration, fileQueryText,
                            plainText, date, queryTextParam, forceColumns,
                            tableCollection, queryColumnsText, queryText, status
                        )
                            SELECT * FROM (SELECT ?,?,?,?,?,?,?,?,?,?,?,?,'1') AS tmp
                            WHERE NOT EXISTS (
                                SELECT name FROM functions WHERE name = ?
                            );
                    """;

            Connection conn = DriverManager.getConnection(new Environment().databaseLocation());
            PreparedStatement ps1 = conn.prepareStatement(sqlQuery);
            ps1.setString(1, name);
            ps1.setString(2, source);
            ps1.setString(3, setCache);
            ps1.setString(4, cacheDuration);
            ps1.setString(5, fileQueryText);
            ps1.setString(6, plainText);
            ps1.setString(7, date);
            ps1.setString(8, queryTextParam);
            ps1.setString(9, forceColumns);
            ps1.setString(10, tableCollection);
            ps1.setString(11, queryColumnsText);
            ps1.setString(12, queryText);
            ps1.setString(13, name);

            int out = ps1.executeUpdate();

            if (out != 0) {
                typeField = "true";
            }

            header.put("type", typeField);
            conn.close();
            return header;

        } catch (Exception e) {

            header.put("type", typeField);
            return header;
        }
    }

    /**
     *
     * @param id
     * @param sqlQuery
     * @return JSONObject (Boolean)
     */
    private JSONObject queryUpdate(String id, String sqlQuery){

        try {

            Connection conn = DriverManager.getConnection(new Environment().databaseLocation());
            PreparedStatement statement = conn.prepareStatement(sqlQuery);
            statement.setString(1, id);

            int out = statement.executeUpdate();

            if (out != 0) {
                typeField = "true";
            }

            header.put("type", typeField);
            conn.close();
            return header;

        } catch (Exception e) {

            header.put("type", typeField);
            return header;
        }
    }
}
