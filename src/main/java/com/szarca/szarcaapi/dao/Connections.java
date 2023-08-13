package com.szarca.szarcaapi.dao;

import com.mongodb.*;
import com.mongodb.util.JSON;
import com.szarca.szarcaapi.controller.Environment;
import com.szarca.szarcaapi.controller.Szarca;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.activation.*;
import java.sql.*;
import java.util.Base64;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;



import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class Connections {
    private String sqlQuery;
    static Session session;
    private JSONObject sqlParam;
    private JSONObject connParam;
    private String tempReplace;
    private String db, server , port, user, password, schema, url;
    private final JSONObject jsonCrendencial = new JSONObject();

    public Connections(String sqlQuery, JSONObject sqlParam, JSONObject connParam) {
        this.sqlQuery = sqlQuery;
        this.sqlParam = sqlParam;
        this.connParam = connParam;
        SystemParamConn();
    }

    public void SystemParamConn(){
        /*this.db = CONN.query("/database/db").toString();
        this.server = CONN.query("/database/server").toString();
        this.port = CONN.query("/database/port").toString();
        this.user = CONN.query("/database/user").toString();
        this.password = CONN.query("/database/passwd").toString();
        this.schema = CONN.query("/database/schema").toString();
        this.url = "jdbc:"+db+"://"+server+":"+port+"/"+schema+"?useOldAliasMetadataBehavior=true";*/
    }
    public Connections(String sqlQuery) {
        this.sqlQuery = sqlQuery;
        SystemParamConn();
    }

    public Boolean SQLUpdate(){
        try{

            Connection conn = DriverManager.getConnection(url,user,password);
            conn.createStatement().executeUpdate(sqlQuery);
            conn.close();

            return true;
        }catch (Exception e){
            return false;
        }
    }
    public JSONArray Query(){

        JSONArray out = new JSONArray();

        int forceColumns = 0;
        boolean noSQL = false;
        boolean useRelationalDB = false;
        boolean systemDB = false;

        String SQL;

        try{

            if(connParam.length() > 0){

                if(connParam.get("database").toString().equals("[DriverManager]")){

                    /**
                     * @apiNote funcionamento drive
                     * Recebe as consultas diretamente na solicitação
                     */

                    useRelationalDB = true;
                    url = new Environment().databaseLocation();
                    sqlQuery = new String(Base64.getDecoder().decode(sqlQuery));

                } else if(connParam.get("database").toString().equals("system")){

                    /**
                     * @apiNote acesso ao banco de dados do sistema
                     * consultas as tabelas de Usuarios/Funções/Sources etc.
                     */

                    systemDB = true;
                    useRelationalDB = true;
                    url = new Environment().databaseLocation();
                    sqlQuery = new String(Base64.getDecoder().decode(sqlQuery));

                } else if(connParam.get("database").toString().equals("smtp")) {

                    user = connParam.get("user").toString();
                    password = new String(Base64.getDecoder().decode(connParam.get("passwd").toString()));
                    sqlQuery = new String(Base64.getDecoder().decode(sqlQuery));

                    if(sqlParam != null){
                        SQL = setParam(sqlQuery, sqlParam, "plaintext");
                    } else {
                        SQL = sqlQuery;
                    }

                    jsonCrendencial.put("host", connParam.get("address").toString());
                    jsonCrendencial.put("port", connParam.get("port").toString());
                    jsonCrendencial.put("user", user);
                    jsonCrendencial.put("passwd",password);
                    jsonCrendencial.put("from", user);
                    jsonCrendencial.put("recipients", connParam.get("queryColumnsText").toString());
                    jsonCrendencial.put("subject", connParam.get("plainText").toString());
                    jsonCrendencial.put("message", SQL);

                    out.put(sendmail());

                } else if(connParam.get("database").toString().equals("oracle")) {

                    useRelationalDB = true;

                    url = "jdbc:oracle:thin:@" +
                            connParam.get("address").toString() + ":" +
                            connParam.get("port").toString() + ":" +
                            connParam.get("schema").toString() + "?useOldAliasMetadataBehavior=true";

                    user = connParam.get("user").toString();
                    password = connParam.get("passwd").toString();
                    sqlQuery = new String(Base64.getDecoder().decode(sqlQuery));
                    forceColumns = connParam.getInt("forceColumns");

                } else if(connParam.get("database").toString().equals("mysql")){

                    useRelationalDB = true;

                    url = "jdbc:mysql://" +
                            connParam.get("address").toString() + ":" +
                            connParam.get("port").toString() + "/" +
                            connParam.get("schema").toString() + "?useOldAliasMetadataBehavior=true";

                    user = connParam.get("user").toString();
                    password = connParam.get("passwd").toString();
                    sqlQuery = new String(Base64.getDecoder().decode(sqlQuery));
                    forceColumns = connParam.getInt("forceColumns");

                } else if(connParam.get("database").toString().equals("sqlserver")){

                    useRelationalDB = true;

                    user = connParam.get("user").toString();
                    password = connParam.get("passwd").toString();

                    url = "jdbc:sqlserver://" +
                            connParam.get("address").toString() + ":" +
                            connParam.get("port").toString() + ";database=" +
                            connParam.get("schema").toString() + ";user="+user+";password="+password+";"+
                            "encrypt=true;trustServerCertificate=true;"+
                            "hostNameInCertificate=cr2.eastus1-a.control.database.windows.net;"+
                            "loginTimeout=30;";

                    sqlQuery = new String(Base64.getDecoder().decode(sqlQuery));
                    forceColumns = connParam.getInt("forceColumns");

                } else if(connParam.get("database").toString().equals("mongodb")) {

                    noSQL = true;

                    try{

                        user = connParam.get("user").toString();
                        password = connParam.get("passwd").toString();

                        MongoClientURI uri =
                                new MongoClientURI(
                                        "mongodb://"+user+":"+password+"@"+connParam.get("address").toString()+
                                                ":"+connParam.get("port").toString()
                                );

                        MongoClient mongo =
                                new MongoClient(uri);

                        DB db = mongo.getDB(connParam.get("schema").toString());
                        DBCollection coll = db.getCollection(connParam.get("tableCollection").toString());

                        sqlQuery = new String(Base64.getDecoder().decode(sqlQuery));

                        if(sqlParam != null){
                            SQL = setParam(sqlQuery, sqlParam, "");
                        } else {
                            SQL = sqlQuery;
                        }

                        DBObject query = (DBObject) JSON.parse(SQL);

                        DBCursor cursor = coll.find(query);
                        while(cursor.hasNext()){
                            out.put(cursor.next());
                        }
                    } catch (Exception e){
                        System.out.println("monodb-query: " + e);
                    }
                }
            }

            if(!noSQL && useRelationalDB){

                Connection conn;

                if(!systemDB){
                    conn  = DriverManager.getConnection(url, user, password);
                } else {
                    conn  = DriverManager.getConnection(url);
                }

                if(sqlParam != null){
                    SQL = setParam(sqlQuery, sqlParam, "");
                } else {
                    SQL = sqlQuery;
                }

                PreparedStatement ps = conn.prepareStatement(SQL);

                var rs = ps.executeQuery();
                var meta = rs.getMetaData();

                while(rs.next()){
                    JSONObject row = new JSONObject();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {

                        if(forceColumns == 1) {
                            if (connParam.has("queryColumnsText")) {

                                String nameColumn = setColumn(
                                        connParam.get("queryColumnsText").toString(),
                                        meta.getColumnName(i),
                                        forceColumns);

                                if (nameColumn != null) {
                                    row.put(nameColumn, rs.getString(i));
                                }

                            }
                        } else {
                            row.put(meta.getColumnName(i), rs.getString(i));
                        }
                    }
                    out.put(row);
                }

                conn.close();
            }

        }catch (Exception e){
            System.out.println("Erro na consulta (QUERY)." + e);
        }

        return out;
    }
    private String setColumn(String listString, String obj, Integer forceColumns){

        String e = null;

        JSONArray list = new JSONArray(listString);

        for (int i = 0; i < list.length(); i++) {
            if(list.getJSONObject(i).has(obj)){
                e = list.getJSONObject(i).get(obj).toString();
            } else {
                if(forceColumns == 0){
                    e = obj;
                }
            }
        }

        return e;
    }
    private String setParam(String sql, JSONObject list, String type){
        tempReplace = sql;
        list.keySet().forEach(e->{
            String valor;

            if(type.equals("plaintext")){
                valor = "" + list.getString(e) + "";
            } else {
                valor = "'" + list.getString(e) + "'";
            }

            tempReplace = tempReplace.replace(
                    "@" + e, valor
            );
        });
        return tempReplace;
    }


    /*public JSONObject sendmail(){
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(jsonCrendencial.getString("host"));
        mailSender.setPort(jsonCrendencial.getInt("port"));
        mailSender.setUsername(jsonCrendencial.getString("user"));
        mailSender.setPassword(jsonCrendencial.getString("passwd"));
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(jsonCrendencial.getString("user"));
        message.setTo(jsonCrendencial.getString("recipients").trim().toLowerCase());
        message.setSubject(jsonCrendencial.getString("subject"));

        message.setText(jsonCrendencial.getString("message"));
        mailSender.send(message);

        return new JSONObject();
    }*/

    private Session sessionMail(){
        Properties props = new Properties();

        props.put("mail.smtp.host", jsonCrendencial.getString("host"));
        props.put("mail.smtp.socketFactory.port", jsonCrendencial.getString("port"));
        props.put("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.debug", "false");
        props.put("mail.smtp.port", jsonCrendencial.getString("port"));

        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        MailcapCommandMap mailcapCommandMap = new MailcapCommandMap();
        mailcapCommandMap.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed; x-java-fallback-entry=true");
        mailcapCommandMap.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mailcapCommandMap.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mailcapCommandMap.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mailcapCommandMap.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
        CommandMap.setDefaultCommandMap(mailcapCommandMap);

        Session session = Session.getDefaultInstance(props,

                new javax.mail.Authenticator()  {
                    protected PasswordAuthentication getPasswordAuthentication()
                    {
                        try {
                            return new
                                    PasswordAuthentication(
                                    jsonCrendencial.getString("user"),
                                    jsonCrendencial.getString("passwd")
                            );
                        } catch (Exception e) {

                            System.out.println(e);
                            return null;
                        }
                    }
                });

        session.setDebug(false);

        return session;
    }
    private JSONObject sendmail() {

        JSONObject out = new JSONObject();

        try{

            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            Message message = new MimeMessage(sessionMail());
            message.setFrom(new InternetAddress(jsonCrendencial.getString("user")));

            Address[] toUser = InternetAddress
                    .parse(jsonCrendencial.getString("recipients").trim().toLowerCase());

            message.setRecipients(Message.RecipientType.TO, toUser);
            message.setSubject(jsonCrendencial.getString("subject"));
            message.setContent(jsonCrendencial.getString("message"), "text/html");

            Transport.send(message);

            out.put("status","true");
            out.put("message","E-mail Enviado com Sucesso.");

            return out;

        }catch (Exception e){

            out.put("status","false");
            out.put("message","Falha no Envio do E-mail.");

            return out;
        }
    }
}
