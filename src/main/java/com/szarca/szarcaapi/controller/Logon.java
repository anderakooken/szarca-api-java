package com.szarca.szarcaapi.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Calendar;


/**
 * @apiNote Logon do Sistema, verificando a estrutura recebida pelo arquivo JSON
 */
public class Logon {
    private String user, passwd;
    private Integer userStatus = 0, userId;
    private String functionParam;
    private String source, sourceUser,  sourcePasswd, cache, cacheExpiration, function, sqlText, plainText, sqlQueryParam, queryColumnsText, tableCollection;
    private Integer forceColumns = 0;
    private String sourceSgbd;
    private JSONObject json;
    private final HttpServletRequest requestServlet;
    private final Szarca main = new Szarca();
    private final JSONObject connFunction = new JSONObject();
    private final JSONObject cacheObj = new JSONObject();
    private String nameFileCache;

    public Logon(JSONObject json, HttpServletRequest req){
        this.json = json;
        this.requestServlet = req;
    }
    public JSONObject loginUser(){

        JSONObject logonReturned = new JSONObject();

        user = json.query("/logon/user").toString();
        passwd = json.query("/logon/passwd").toString();
        function = json.get("function").toString();
        functionParam = json.get("param").toString().replace("@","");

        String sqlQuery = """
                select
                 
                 u.id as userId,
                 u.login as userLogin,
                 u.name as userName,
                 u.email as userEmail,
                 u.phone as userPhone,
                 u.`date` as userTimestamp,
                 u.status  as userStatus,
                 f.name as functionName,
                 f.setCache,
                 f.cacheDuration,
                 f.fileQueryText,
                 f.queryText ,
                 f.queryTextParam ,
                 f.queryColumnsText,
                 f.forceColumns,
                 f.plainText ,
                 f.tableCollection ,
                 f.`date` as functionTimestamp,
                 f.status as functionStatus,
                 s.name as sourceName,
                 s.sgbd as sourceSgbd,
                 s.host as sourceHost,
                 s.port as sourcePort,
                 s.`user` as sourceUser,
                 s.passwd as sourcePasswd,
                 s.`schema` as sourceSchema,
                 s.status as sourceStatus,
                 s3.description as secDescription,
                 s3.`type` as secType,
                 s3.ip as secIP,
                 s3.status as secStatus,
                 s4.description as appDescription ,
                 s4.`type` as appType ,
                 s4.app as appName,
                 s4.status as appStatus
                 
                 from functions f
                
                    left join sources s on s.name = f.source
                    left join securitysources s2 on s2.source = s.name
                    left join users u on s2.user = u.login
                    left join securityaddress s3 on s3.users_id = u.id
                    left join securityuseragent s4 on s4.users_id = u.id
                    left join bruteforce b on b.user_id = u.id
                    
                    where
                
                    f.name = ? and
                    u.login = ? and u.hash = ?
                    
                    and u.status = 1

                """
        ;

        JSONArray result = loginUserQuery(sqlQuery,function, user, passwd);

        if(result.length() > 0) {

            JSONObject ln = result.getJSONObject(0);

            JSONObject loginData = new JSONObject();
            loginData.put("login", ln.get("userLogin"));
            loginData.put("name", ln.get("userName"));
            loginData.put("email", ln.get("userEmail"));

            userId = Integer.parseInt(ln.get("userId").toString());

            cache = ln.get("cacheDuration").toString();
            source = ln.get("sourceName").toString();
            sourceSgbd = ln.get("sourceSgbd").toString();

            if(!sourceSgbd.equals("datalake") &&
                    !sourceSgbd.equals("url") &&
                    !sourceSgbd.equals("url-json")){

                sqlText = ln.get("queryText").toString();
                sqlQueryParam = ln.get("queryTextParam").toString();

                if(ln.has("tableCollection")) {
                    tableCollection = ln.get("tableCollection").toString();
                }
                if(ln.has("queryColumnsText")) {
                    queryColumnsText = ln.get("queryColumnsText").toString();
                }
                if(ln.has("forceColumns")) {
                    forceColumns = ln.getInt("forceColumns");
                }
                if(ln.has("plainText")) {
                    plainText = ln.getString("plainText");
                }
            }

            main.jsonReturn.put("type", "true");
            main.jsonReturn.put("user", loginData);
            main.jsonMessage.put("function", function);
            main.jsonMessage.put("source", source);

            main.jsonMessage.put("parameters", new JSONObject(functionParam));

            connFunction.put("database",   sourceSgbd);
            connFunction.put("address",    ln.get("sourceHost").toString());
            connFunction.put("port",       ln.get("sourcePort").toString());
            connFunction.put("schema",     ln.get("sourceSchema").toString());

            //it can be used by (NoSQL) databases without credencials
            if(ln.has("sourceUser")) {
                sourceUser = ln.get("sourceUser").toString();
            }
            if(ln.has("sourcePasswd")) {
                sourcePasswd = ln.get("sourcePasswd").toString();
            }

            connFunction.put("user", sourceUser);
            connFunction.put("passwd", sourcePasswd);
            connFunction.put("sqlText",     sqlText);
            connFunction.put("sqlQueryParam", sqlQueryParam);
            connFunction.put("functionParam", functionParam);
            connFunction.put("queryColumnsText", queryColumnsText);
            connFunction.put("forceColumns", forceColumns);
            connFunction.put("tableCollection", tableCollection);
            connFunction.put("plainText", plainText);

            JSONArray src_return;

            nameFileCache = "/cache/"+function+".json";

            /**
             * Verifica se existe arquivo em cache, se o parametro estiver marcado na função
             */
            if(cache.isEmpty() || cache.equals("0")){
                src_return = main.SourceQuery(connFunction);
            } else {
                src_return = cacheVerify(nameFileCache);
            }

            cacheObj.put("timeDefined",cache);
            cacheObj.put("cacheExpiration",cacheExpiration);
            main.jsonMessage.put("cache", cacheObj);

            main.jsonMessage.put("resultset", src_return);
            main.jsonReturn.put("message", main.jsonMessage);

            Environment ev = new Environment();
            ev.ipAddress = requestServlet.getRemoteAddr();

            return ev.report(main.jsonReturn);
        } else {

            logonReturned.put("system", new Environment().properties());
          
            JSONObject returnJson = new JSONObject();
            returnJson.put("type", "false");
            returnJson.put(
                    "message",
                    "Consulta não realizada. Não há permissão para fonte requisitada."
            );

            logonReturned.put("return", returnJson);
            return logonReturned;

        }
    }


    /**
     * @apiNote Função p/ capturar as permissoes do usuario para os Sources e login
     * * é extraido a consulta armazenada no banco em BASE64 e os parametros de filtros
     */

    private JSONArray loginUserQuery(String sqlQuery, String function, String user, String passwd){

        JSONArray out = new JSONArray();

        Path currentPath = Paths.get("");
        String cPath = currentPath.toAbsolutePath().toString();

        try {

            Connection conn = DriverManager.getConnection(new Environment().databaseLocation());
            PreparedStatement ps = conn.prepareStatement(sqlQuery);

            ps.setString(1, function);
            ps.setString(2, user);
            ps.setString(3, passwd);

            var rs = ps.executeQuery();
            var meta = rs.getMetaData();

            while (rs.next()) {
                JSONObject row = new JSONObject();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnName(i), rs.getString(i));
                }
                out.put(row);
            }

            conn.close();
        } catch (Exception e){
            System.out.println("loginUserQuery : " + e);
        }

        return out;
    }

    /**
     * @apiNote Função verificadora de arquivos armazenados na pasta de cache, caso a função armazenada solicite
     */
    private JSONArray cacheVerify(String nameFileCache){

        cacheExpiration = "";

        String jsonString = "";
        JSONArray conn = new JSONArray();

        JSONArray src_return = new JSONArray();

        try {
            Path currentPath = Paths.get("");
            String cPath = currentPath.toAbsolutePath().toString();

            File file = new File(cPath + nameFileCache);

            if(file.exists()){

                Calendar cld = Calendar.getInstance();
                cld.setTimeInMillis(file.lastModified());
                cld.add(Calendar.MINUTE,Integer.parseInt(cache));

                if(cld.after(Calendar.getInstance())){

                    cacheExpiration = Environment.DATE_FORMAT.format(cld.getTime());

                    FileInputStream fileStream  = new FileInputStream(file);
                    JSONArray jsonReturn = new JSONArray(new String (fileStream.readAllBytes()));
                    fileStream.close();
                    return jsonReturn;
                } else {
                    conn = main.SourceQuery(connFunction);
                    writeFileCache(cPath + nameFileCache, conn.toString());
                    return conn;
                }
            } else {
                conn = main.SourceQuery(connFunction);
                writeFileCache(cPath + nameFileCache, conn.toString());
                return conn;
            }
        }catch (Exception e){
            System.out.println("cacheVerify : " + e);
        }

        return src_return;
    }
    /**
     * @apiNote Escreve arquivos (Exemplo: cache)
     */

    private void writeFileCache(String fileURL, String data) throws Exception{
        FileOutputStream jsonFileOutput = new FileOutputStream(fileURL);
        jsonFileOutput.write(data.getBytes());
        jsonFileOutput.close();
    }
}
