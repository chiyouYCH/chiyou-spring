package com.chiyou.service;

import com.chiyou.annotation.CHService;

@CHService()
public class ServiceDemoImpl implements ServiceDemo{
    @Override
    public String service(String name) {
        return name+" ,hi this is a method";
    }
}
