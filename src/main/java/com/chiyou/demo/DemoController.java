package com.chiyou.demo;

import com.chiyou.annotation.CHAutowired;
import com.chiyou.annotation.CHController;
import com.chiyou.annotation.CHRequestMappering;
import com.chiyou.annotation.CHRequestParam;
import com.chiyou.service.ServiceDemo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@CHController
@CHRequestMappering("/ChiYou")
public class DemoController {

    @CHAutowired()
    private ServiceDemo serviceDemo;

    @CHRequestMappering("/query")
    public void query(HttpServletRequest request, HttpServletResponse response, @CHRequestParam("name") String name){

        String result = "hi this is name "+name;
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @CHRequestMappering("/service")
    public void service(HttpServletRequest request, HttpServletResponse response, @CHRequestParam("name") String name){
        String result = serviceDemo.service(name);
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
