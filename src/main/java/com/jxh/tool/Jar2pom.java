package com.jxh.tool;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;

/**
 * 通过Jar包SHA1或MD5生成Pom文件
 *
 * @author JiaXiaohei
 * @date 2018-03-16
 */
public class Jar2pom {

    /**
     * Maven库
     */
    public static final String nexusUrl = "http://maven.aliyun.com/nexus/service/local/lucene/search?sha1=";

    public static void main(String[] args) {

        File libs = new File("D:\\lib");
        for (File jar : libs.listFiles()) {
            System.out.println("<!--  " + jar.getName() + " -->");
            if (!getPomByChecksum(jar).isTextOnly()) {
                System.out.println("<!--  Search by checksum -->");
                System.out.println(getPomByChecksum(jar).asXML());
            } else {
                System.out.println("<!--  No data was found -->");
            }
            System.out.println();
        }

    }

    /**
     * 通过文件返回Pom
     *
     * @param file
     * @return
     */
    public static Element getPomByChecksum(File file) {
        Element dependency = new DOMElement("dependency");
        String checkSum = getCheckSum(file, "SHA1");
        String xml = doGet(nexusUrl + checkSum);
        if (xml != null && xml.length() != 0) {
            try {
                Document document = DocumentHelper.parseText(xml);
                Element dataElement = document.getRootElement().element("data");
                if (dataElement.getText() != null && dataElement.getText().length() != 0) {
                    Element artifactElement = dataElement.element("artifact");
                    dependency.add((Element) artifactElement.element("groupId").clone());
                    dependency.add((Element) artifactElement.element("artifactId").clone());
                    dependency.add((Element) artifactElement.element("version").clone());
                }
            } catch (DocumentException e) {
                e.printStackTrace();
            }
        }
        return dependency;
    }

    /**
     * 发起Get请求
     *
     * @param url
     * @return
     */
    public static String doGet(String url) {
        String srtResult = null;
        CloseableHttpClient httpCilent = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse httpResponse = httpCilent.execute(httpGet);
            srtResult = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                httpCilent.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return srtResult;
    }

    /**
     * 计算CheckSum
     *
     * @param file
     * @param algorithm SHA1 or MD5
     * @return
     */
    public static String getCheckSum(File file, String algorithm) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance(algorithm);
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(16);
    }

}
