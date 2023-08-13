package com.szarca.szarcaapi.controller;

import com.szarca.szarcaapi.dao.Connections;
import jakarta.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.web.bind.annotation.*;

/**
 * @apiNote Função de consumo de arquivo JSON p/ acesso as informações distribuídas
 * @author André Cavalcante
*/
@RestController
public class Szarca {

    //PREPARE JSON-OBJECT OUTPUT DATA
    JSONObject jsonMessage = new JSONObject();
    JSONObject jsonReturn = new JSONObject();

    @PostMapping
    @RequestMapping(
            value="/",
            consumes = "application/json",
            produces="application/json"
    )
    public String Szarca(@RequestBody String json, HttpServletRequest req){

        JSONObject header = new JSONObject();
        header.put("type", "false");
        header.put("message", "incorrect parameters");
        Environment report = new Environment();

        try{
            JSONObject jsonReceived = new JSONObject(json);
            JSONObject logon =
                    new Logon(jsonReceived,req)
                        .loginUser();

            return logon.toString();

        }catch (Exception e){

            System.out.println("Szarca : " + e);
            return report.reportException(header).toString();
        }
    }

    /**
     * @apiNote Listen p/ Administrar o sistema, Criação de Usuarios, Funções e Permissões
    */
    @PostMapping
    @RequestMapping(
            value="/admin/",
            consumes = "application/json",
            produces="application/json"
    )
    public String Management(@RequestBody String json, HttpServletRequest req){

        if(new Environment().readJsonFile().getBoolean("enableManagement")){
            return new Environment()
                    .reportException(new Management().controller(json)
                    ).toString();
        } else {
            JSONObject header = new JSONObject();
            header.put("type", "false");
            header.put("message", "the management is disabled");
            Environment report = new Environment();

            return report.reportException(header).toString();
        }
    }
    @GetMapping(value="/", produces="application/json")
    public String getRequest(){

        JSONObject header = new JSONObject();
        header.put("type", "false");
        header.put("message", "Welcome! Please, authenticate.");
        Environment report = new Environment();

        return report.reportException(header).toString();
    }

    /**
     * @apiNote Instanciamento da função de chamada das consultas no banco de dados
     */

    JSONArray SourceQuery(JSONObject connFunction){

        String sqlQuery = connFunction.get("sqlText").toString();
        String sqlParam = connFunction.get("sqlQueryParam").toString();
        String functionParam = connFunction.get("functionParam").toString();

        JSONArray jsonSQLParam = new JSONArray(sqlParam);

        JSONObject getByURL = new JSONObject(functionParam);
        JSONObject jSQL = new JSONObject();

        Connections src = new Connections(sqlQuery, jSQL, connFunction);

        for (int i = 0; i < jsonSQLParam.length(); i++) {
            jSQL.put(jsonSQLParam.getString(i),
                getByURL.get(jsonSQLParam.getString(i)).toString()
            );
        }

        return src.Query();
    }
}
