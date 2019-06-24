package com.vtech.vte.im.kc;

import java.io.IOException;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;


/**
 * @author Peter Liang
 * @version 1.0
 *
 */
public class LLWsXmppServiceWrapper {

    private static Map<String, String> envUrlMap = new HashMap<String, String>();
    static {
        envUrlMap.put("DE", "https://de.vtechda.com/wservices/m_XMPP.asmx");
        envUrlMap.put("HK", "https://hk.vtechda.com/wservices/m_XMPP.asmx");
        envUrlMap.put("PROD", "https://www.vtechda.com/wservices/m_XMPP.asmx");
    }
    public static void main(String[] args) {
        getLoginInfo("peter_liang@vtech.com","Usapa981265","DE");
    }

    public static String[] getLoginInfo(String username, String password, String env) {

        HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = new HttpConnectionManagerParams();
        params.setConnectionTimeout(10000);
        params.setSoTimeout(60000);
        params.setDefaultMaxConnectionsPerHost(10);
        params.setMaxTotalConnections(200);

        connectionManager.setParams(params);

        HttpClient client = new HttpClient();
        ProtocolSocketFactory pfactory = new MySSLProtocolSocketFactory();
        Protocol https = new Protocol("https", pfactory , 443);
        Protocol.registerProtocol("https", https);

        String url = envUrlMap.get(env);

        PostMethod post = new PostMethod(url);
        post.addRequestHeader("Content-Type", "text/xml; charset=utf-8");

        // vtechtest1001a@victor.vtech.com / 123456Ww
        String xmlPost = "<?xml version=\"1.0\" encoding=\"utf-16\"?>"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">"
                + "<soap:Body>"
                + "<IMLoginV3 xmlns=\"VTechDA.WService\">"
                + "<sCountryLang>USeng</sCountryLang>"
                + "<sUsername>"
                + username
                + "</sUsername>"
                + "<sPassword>"
                + password
                + "</sPassword>"
                + "<sDeviceID />"
                + "<sDeviceType>Android</sDeviceType>"
                + "<sAppVer>100</sAppVer>"
                + "<sUUID />"
                + "<sKCSystemVer>KidiConnect</sKCSystemVer>"
                + "</IMLoginV3>"
                + "</soap:Body>"
                + "</soap:Envelope>";

        System.out.println(xmlPost);

        post.setRequestEntity(new StringRequestEntity(xmlPost));

        try {
            int status = client.executeMethod(post);

            if (status == 200) {

                String xmlString = post.getResponseBodyAsString();

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new InputSource(new StringReader(xmlString)));

                String sJIDNode = document.getElementsByTagName("sJIDNode").item(0).getTextContent();
                String sKCToken = document.getElementsByTagName("sKCToken").item(0).getTextContent();
                String sDomainName = document.getElementsByTagName("sDomainName").item(0).getTextContent();

                String[] ret = new String[3];
                ret[0] = sJIDNode;
                ret[1] = sKCToken;
                ret[2] = sDomainName;
                return ret;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class MySSLProtocolSocketFactory implements ProtocolSocketFactory {

        private SSLContext sslcontext = null;

        private SSLContext createSSLContext() {
            SSLContext sslcontext = null;
            try {
                sslcontext = SSLContext.getInstance("SSL");
                sslcontext.init(null,
                        new TrustManager[] { new TrustAnyTrustManager() },
                        new java.security.SecureRandom());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
            return sslcontext;
        }

        private SSLContext getSSLContext() {
            if (this.sslcontext == null) {
                this.sslcontext = createSSLContext();
            }
            return this.sslcontext;
        }

        public Socket createSocket(Socket socket, String host, int port,
                                   boolean autoClose) throws IOException, UnknownHostException {
            return getSSLContext().getSocketFactory().createSocket(socket, host,
                    port, autoClose);
        }

        public Socket createSocket(String host, int port) throws IOException,
                UnknownHostException {
            return getSSLContext().getSocketFactory().createSocket(host, port);
        }

        public Socket createSocket(String host, int port, InetAddress clientHost,
                                   int clientPort) throws IOException, UnknownHostException {
            return getSSLContext().getSocketFactory().createSocket(host, port,
                    clientHost, clientPort);
        }

        public Socket createSocket(String host, int port, InetAddress localAddress,
                                   int localPort, HttpConnectionParams params) throws IOException,
                UnknownHostException, ConnectTimeoutException {
            if (params == null) {
                throw new IllegalArgumentException("Parameters may not be null");
            }
            int timeout = params.getConnectionTimeout();
            SocketFactory socketfactory = getSSLContext().getSocketFactory();
            if (timeout == 0) {
                return socketfactory.createSocket(host, port, localAddress,
                        localPort);
            } else {
                Socket socket = socketfactory.createSocket();
                SocketAddress localaddr = new InetSocketAddress(localAddress,
                        localPort);
                SocketAddress remoteaddr = new InetSocketAddress(host, port);
                socket.bind(localaddr);
                socket.connect(remoteaddr, timeout);
                return socket;
            }
        }
    }

    private static class TrustAnyTrustManager implements X509TrustManager {

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {}

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {}
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[] {};
        }
    }
}