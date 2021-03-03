package com.chiyou.mvcframework;

import com.chiyou.annotation.CHAutowired;
import com.chiyou.annotation.CHController;
import com.chiyou.annotation.CHRequestMappering;
import com.chiyou.annotation.CHService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CHDispatcherServlet extends HttpServlet {

    //配置文件
    private Properties contextConfig = new Properties();
    //扫描的文件类名
    private ArrayList<String> classNames = new ArrayList<>();
    //容器
    private HashMap<String,Object> ioc = new HashMap<>();
    //
    private HashMap<String,Method> handMappering = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //调用
        try {
            doDispatch(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("exception error");
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String url = req.getRequestURI();
        System.out.println("uri="+req.getRequestURI() + "url" +req.getRequestURL());
        String contextPath = req.getContextPath();
        url = url.replaceAll("/+","/").replaceAll(contextPath,"");

        if(!this.handMappering.containsKey(url)){
            resp.setStatus(404);
            resp.getWriter().write("404 not found！");
            return;
        }

        Map<String, String[]> requestMap = req.getParameterMap();

        Method method = this.handMappering.get(url);
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
//        String beanName = method.getDeclaringClass().getName();
        method.invoke(ioc.get(beanName),new Object[]{req,resp,requestMap.get("name")[0]});
    }

    @Override
    public void init(ServletConfig config) {
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3.实例化相关的类，并且存储到ioc容器
        doInsatance();

        //4.完成依赖注入
        doAutowaired();

        //5.初始化handlerMappering,将url和method建立对应关系
        doInitHanderMapper();

        System.out.println("CH spring is run");
            
    }

    private void doInitHanderMapper() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(CHController.class)){continue;}

            for (Method method : clazz.getMethods()) {
                if(!method.isAnnotationPresent(CHRequestMappering.class)){continue;}

                String baseUrl = "";
                if(clazz.isAnnotationPresent(CHRequestMappering.class)){
                    CHRequestMappering baseMappering = clazz.getAnnotation(CHRequestMappering.class);
                    baseUrl = baseMappering.value();
                }

                CHRequestMappering mappering = method.getAnnotation(CHRequestMappering.class);
                String url = ("/" + baseUrl + "/" + mappering.value()).replaceAll("/+","/");
                handMappering.put(url,method);
                System.out.println("mappering" +url +"," + method);

            }
        }
    }

    private void doAutowaired() {
        if(ioc.isEmpty()){
            return;
        }
        for (Map.Entry<String,Object> entry: ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if(!field.isAnnotationPresent(CHAutowired.class)){continue;}

                CHAutowired autowired = field.getAnnotation(CHAutowired.class);
                String beanName = autowired.value().trim();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }

                //可能为私有类,设置为true可以访问
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private void doInsatance() {
        if (classNames.isEmpty()){
            return;
        }
        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                //默认类名小写
                String beanName = toLowerFirstCase(clazz.getSimpleName());

                if (clazz.isAnnotationPresent(CHController.class)){
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);
                } else if(clazz.isAnnotationPresent(CHService.class)) {
                    //自定义命名
                    CHService annotation = clazz.getAnnotation(CHService.class);
                    if(!"".equals(annotation.value())){
                        beanName = annotation.value();
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);
                    //如果是一个接口只能初始化他的实现类
                    //TODO 这里的接口处理是怎么做的，查看一下spring源码
                    for (Class<?> in : clazz.getInterfaces()) {
                        if(ioc.containsKey(in.getName())){
                            throw new Exception("the bean name exists");
                        }
                        ioc.put(in.getName(),instance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toLowerFirstCase(String className){
        char[] chars = className.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        //获取classpath，进行遍历
        File files = new File(url.getFile());

        for (File file: files.listFiles()){

            if(file.isDirectory()){

                doScanner(scanPackage+"."+file.getName());

            } else {

                if (!file.getName().endsWith(".class")){ continue; }
                String className = scanPackage + "." + (file.getName().replace(".class",""));
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        contextConfigLocation = contextConfigLocation.replace("classpath:","");
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != is){
                try {
                    is.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        }
    }
}
