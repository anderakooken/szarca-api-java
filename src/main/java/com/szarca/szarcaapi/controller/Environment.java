package com.szarca.szarcaapi.controller;

import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Environment {
    String name = "Szarca";
    String architecture = "json";
    String version = "2.3.3";
    String administrator = "@anderakooken";
    String environment = "Java";
    String ipAddress;
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * @apiNote Banco de dados SQLite p/ administração da aplicação.
     */
    public final String url = databaseLocation();

    /**
     * @apiNote Informações de versionamento do software para facilitar a identificação pelo usuario
     */
    public JSONObject properties(){

        Environment e = new Environment();
        JSONObject header = new JSONObject();

        header.put("name",          e.name);
        header.put("architecture",  e.architecture);
        header.put("environment",  e.environment);
        header.put("version",       e.version);
        header.put("author", e.administrator);

        return header;
    }

    public String databaseLocation(){

        String location =  readJsonFile().getString("databaseDirectory");

        if(location.equals("")) {
            return "jdbc:sqlite:"+ Paths.get("").toAbsolutePath().toString() +"/szarca-api.db";
        } else {
            return "jdbc:sqlite:"+ location +"/szarca-api.db";
        }
    }
    /**
     * @apiNote Dados do solicitante da informação que será agregado a mensagem de retorno
     */

    private JSONObject returnObj(JSONObject message){

        JSONObject header = new JSONObject();
        header.put("type", message.get("type"));
        header.put("ipAddress", this.ipAddress);
        header.put("date", DATE_FORMAT.format(new Date()));
        header.put("user", message.get("user"));
        header.put("message", message.get("message"));

        return header;
    }

    /**
     * @apiNote Função p/ caso de sucesso, inclui o resultset no corpo do retorno
     */

    public JSONObject report(JSONObject message){

        JSONObject header = new JSONObject();
        header.put("system", properties());
        header.put("return", returnObj(message));

        return header;
    }

    /**
     * @apiNote Em caso de falhas nas consultas, retorna uma mensagem estatica de erros
     */

    public JSONObject reportException(JSONObject message){

        JSONObject header = new JSONObject();
        header.put("system", properties());
        header.put("return", message);

        return header;
    }

    /**
     * @apiNote Leitura do arquivo de configuração - local do arquivo .db system
     */
    public JSONObject readJsonFile(){

        try{
            Path currentPath = Paths.get("");
            String cPath = currentPath.toAbsolutePath().toString();

            File file = new File(cPath+"/parameters.json");

            FileInputStream fileStream = new FileInputStream(file);
           return new JSONObject(new String(fileStream.readAllBytes()));

        }catch (Exception e){
            System.out.println("Cannot find configuration file.");
        }

        return new JSONObject();
    }

}
