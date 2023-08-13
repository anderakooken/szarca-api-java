package com.szarca.szarcaapi.controller;

import com.szarca.szarcaapi.dao.Connections;

import jakarta.servlet.http.HttpServletRequest;

public class Security {

    private String userName;
    private String function;
    private HttpServletRequest requestServlet;

    public Security(String userName, String function, HttpServletRequest requestServlet) {
        this.userName = userName;
        this.function = function;
        this.requestServlet = requestServlet;
    }

    public void setBruteForceCount(){
        new Connections(
                "UPDATE bruteforce SET `count` = (`count` + 1), date = NOW() WHERE "+
                        "user_id = (select u.id from users u where "+
                        "u.login  = '"+ userName +"' )"
                ).SQLUpdate();

        new Connections(
                "INSERT INTO bruteforcelogs (user, date, plainText, ip) "+
                        "VALUES ('"+ userName +"', NOW(), '"+function+"', '"+ requestServlet.getRemoteAddr() +"')"
        ).SQLUpdate();
    }
    public void resetBruteForceCount(){
        new Connections(
                "UPDATE bruteforce SET `count` = 0 WHERE "+
                        "user_id = (select u.id from users u where "+
                        "u.login  = '"+ userName +"' )"
        ).SQLUpdate();
    }
    public void setBruteForceLog(){

    }
}
