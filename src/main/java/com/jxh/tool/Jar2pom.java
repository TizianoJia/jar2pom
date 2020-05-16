package com.jxh.tool;

import com.alibaba.fastjson.JSONObject;
import com.jxh.tool.entity.Docs;
import com.jxh.tool.entity.SolrSearch;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 通过Jar包SHA1或MD5生成Pom文件
 *
 * @author JiaXiaohei
 * @date 2020-05-16
 */
public class Jar2pom {

    /**
     * Maven库
     */
    public static final String nexusUrl = "https://search.maven.org/solrsearch/select?start=0&rows=1";

    public static void main(String[] args) {
        //先通过Jar的SHA1查询 如果不存在则解析Manifest查询
        File libs = new File("D:\\lib");
        for (File jar : libs.listFiles()) {

            System.out.println("<!--  " + jar.getName() + " -->");

            Docs doc = getPomByChecksum(jar);
            if (null != doc) {
                System.out.println("<!--  Search by Checksum -->");
                System.out.println(assemblePomElement(doc).asXML());
                continue;
            }

            doc = getPomByManifest(jar);
            if (null != doc) {
                System.out.println("<!--  Search by Manifest -->");
                System.out.println(assemblePomElement(doc).asXML());
                continue;
            }

            System.out.println("<!--  No data was found -->");

            System.out.println();
        }

    }

    /**
     * 通过Jar SHA1返回Pom dependency
     *
     * @param file
     * @return
     */
    public static Docs getPomByChecksum(File file) {
        Docs docs = null;

        String checkSum = getCheckSum(file, "SHA1");
        String jsonString = doGet(nexusUrl + "&q=1:" + checkSum);
        SolrSearch solrSearch = JSONObject.parseObject(jsonString, SolrSearch.class);

        if (solrSearch.getResponseHeader().getStatus() == 0 && solrSearch.getResponse().getDocs().size() > 0) {
            docs = solrSearch.getResponse().getDocs().get(0);
        }
        return docs;
    }


    /**
     * 通过Jar Manifest返回Pom dependency
     *
     * @param file
     * @return
     */
    public static Docs getPomByManifest(File file) {
        Docs docs = null;

        try {
            JarFile jarfile = new JarFile(file);
            Manifest mainmanifest = jarfile.getManifest();
            jarfile.close();
            if (null == mainmanifest) {
                return null;
            }
            String a = null, v = null;
            if (mainmanifest.getMainAttributes().containsKey(new Attributes.Name("Extension-Name"))) {
                a = mainmanifest.getMainAttributes().getValue(new Attributes.Name("Extension-Name"));
            } else if (mainmanifest.getMainAttributes().containsKey(new Attributes.Name("Implementation-Title"))) {
                a = mainmanifest.getMainAttributes().getValue(new Attributes.Name("Implementation-Title"));
            } else if (mainmanifest.getMainAttributes().containsKey(new Attributes.Name("Specification-Title"))) {
                a = mainmanifest.getMainAttributes().getValue(new Attributes.Name("Specification-Title"));
            }
            if (a != null && a.length() != 0) {
                a = a.replace("\"", "").replace(" ", "-");
            }
            if (mainmanifest.getMainAttributes().containsKey(new Attributes.Name("Bundle-Version"))) {
                v = mainmanifest.getMainAttributes().getValue(new Attributes.Name("Bundle-Version"));
            } else if (mainmanifest.getMainAttributes().containsKey(new Attributes.Name("Implementation-Version"))) {
                v = mainmanifest.getMainAttributes().getValue(new Attributes.Name("Implementation-Version"));
            } else if (mainmanifest.getMainAttributes().containsKey(new Attributes.Name("Specification-Version"))) {
                v = mainmanifest.getMainAttributes().getValue(new Attributes.Name("Specification-Version"));
            }
            if (v != null && v.length() != 0) {
                v = v.replace("\"", "").replace(" ", "-");
            }
            String jsonString = doGet(nexusUrl + "&q=a:" + a + "%20v:" + v);
            SolrSearch solrSearch = JSONObject.parseObject(jsonString, SolrSearch.class);

            if (solrSearch.getResponseHeader().getStatus() == 0) {
                docs = solrSearch.getResponse().getDocs().get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return docs;
    }

    /**
     * 组装 dependency
     *
     * @param docs
     * @return
     */
    public static Element assemblePomElement(Docs docs) {
        Element dependency = new DOMElement("dependency");
        if (docs != null) {
            dependency.addAttribute("groupId", docs.getG());
            dependency.addAttribute("artifactId", docs.getA());
            dependency.addAttribute("version", docs.getV());
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
