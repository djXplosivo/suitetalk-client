package com.netsuite.suitetalk.client.v2018_1;

import com.netsuite.suitetalk.client.common.Constants;
import com.netsuite.suitetalk.client.common.EndpointVersion;
import com.netsuite.suitetalk.client.common.authentication.OAuthPassport;
import com.netsuite.suitetalk.client.common.authentication.Passport;
import com.netsuite.suitetalk.client.common.authentication.SsoPassport;
import com.netsuite.suitetalk.client.common.authentication.TokenPassport;
import com.netsuite.suitetalk.client.common.contract.Authentication;
import com.netsuite.suitetalk.client.common.contract.EndpointInfo;
import com.netsuite.suitetalk.client.common.contract.HttpHeaderHandler;
import com.netsuite.suitetalk.client.common.contract.SoapHeaderHandler;
import com.netsuite.suitetalk.client.common.utils.CommonUtils;
import com.netsuite.suitetalk.client.v2018_1.utils.Utils;
import com.netsuite.suitetalk.proxy.v2018_1.platform.NetSuitePortType;
import com.netsuite.suitetalk.proxy.v2018_1.platform.NetSuiteServiceLocator;
import org.apache.axis.AxisFault;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.SimpleTargetedChain;
import org.apache.axis.client.Stub;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.axis.transport.http.CommonsHTTPSender;
import org.apache.axis.transport.http.HTTPConstants;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.log4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ParametersAreNonnullByDefault
public class WsCoreClient implements Authentication, EndpointInfo, SoapHeaderHandler, HttpHeaderHandler {
   private static final Logger LOG = Logger.getLogger(WsCoreClient.class);
   private final NetSuitePortType port;
   private final NetSuiteServiceLocator locator;
   private final URL endpointUrl;
   private final EndpointVersion endpointVersion;
   private final String messagesUrn;
   protected final List requestHttpHeaders = new ArrayList();
   private final List responseHttpHeaders = new ArrayList();
   private HttpVersion httpProtocolVersion;
   private Passport passport;
   private SsoPassport ssoPassport;
   private TokenPassport tokenPassport;
   private OAuthPassport oAuthPassport;

   public WsCoreClient(URL endpointUrl) {
      super();
      this.httpProtocolVersion = HttpVersion.HTTP_1_1;
      SimpleProvider engineConfiguration = new SimpleProvider();
      engineConfiguration.deployTransport("http", new SimpleTargetedChain(new WsCoreClient.RequestHandler(), new CommonsHTTPSender(), new WsCoreClient.ResponseHandler()));
      this.locator = new NetSuiteServiceLocator(engineConfiguration);
      String serviceName = this.locator.getNetSuitePortAddress().split("/services/")[1];
      String endpointVersionAsString = serviceName.substring("NetSuitePort_".length());
      this.endpointVersion = new EndpointVersion(endpointVersionAsString);
      this.messagesUrn = CommonUtils.getMessagesUrn(this.endpointVersion);
      String query = endpointUrl.getQuery() == null ? "" : '?' + endpointUrl.getQuery();

      try {
         this.endpointUrl = new URL(endpointUrl, "/services/" + serviceName + query);
         this.port = this.locator.getNetSuitePort(this.endpointUrl);
         Stub stub = (Stub)this.port;
         stub.setTimeout(Constants.DEFAULT_HTTP_SOCKET_TIMEOUT);
         stub.setMaintainSession(true);
      } catch (ServiceException | MalformedURLException var7) {
         throw new RuntimeException(var7);
      }
   }

   public NetSuitePortType getPort() {
      return this.port;
   }

   protected Stub getStub() {
      return (Stub)this.getPort();
   }

   protected NetSuiteServiceLocator getLocator() {
      return this.locator;
   }

   public URL getEndpointUrl() {
      return this.endpointUrl;
   }

   public EndpointVersion getEndpointVersion() {
      return this.endpointVersion;
   }

   public String getMessagesUrn() {
      return this.messagesUrn;
   }

   public HttpVersion getHttpProtocolVersion() {
      return this.httpProtocolVersion;
   }

   public void setHttpProtocolVersion(HttpVersion httpProtocolVersion) {
      this.httpProtocolVersion = httpProtocolVersion;
   }

   public void setHttpHeader(String name, String value) {
      this.requestHttpHeaders.add(new MimeHeader(name, value));
   }

   public void unsetHttpHeader(String name) {
      List<MimeHeader> httpHeaders = this.requestHttpHeaders;
      this.requestHttpHeaders.removeAll(httpHeaders.stream().filter((header) -> header.getName().equals(name)).collect(Collectors.toList()));
   }

   public boolean isHttpHeaderSet(String name) {
      Iterator var2 = this.requestHttpHeaders.iterator();

      MimeHeader header;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         header = (MimeHeader)var2.next();
      } while(!header.getName().equals(name));

      return true;
   }

   public List getHttpHeaders() {
      return Collections.unmodifiableList(this.requestHttpHeaders);
   }

   public void clearHttpHeaders() {
      this.requestHttpHeaders.clear();
   }

   public List getResponseHttpHeaders() {
      return Collections.unmodifiableList(this.responseHttpHeaders);
   }

   public List<String> getResponseHttpHeader(String name) {
      List<MimeHeader> responseHttpHeaders1 = this.getResponseHttpHeaders();
      Stream<MimeHeader> mimeHeaderStream = responseHttpHeaders1.stream().filter((header) -> header.getName().equals(name));
      return mimeHeaderStream.map(MimeHeader::getValue).collect(Collectors.toList());

      //return (List)this.getResponseHttpHeaders().stream().filter((Header header) ->
      //header.getName().equals(name)).map(MimeHeader::getValue).collect(Collectors.toList());
   }

   public SOAPHeaderElement getSoapHeader(String namespace, String headerName) {
      return this.getStub().getHeader(namespace, headerName);
   }

   public synchronized void addSoapHeader(@Nullable String namespace, String headerName, Object value) {
      this.getStub().setHeader(namespace == null ? "" : namespace, headerName, value);
   }

   public void addSoapHeader(String headerName, Object value) {
      this.addSoapHeader(this.getMessagesUrn(), headerName, value);
   }

   public void removeSoapHeader(@Nullable String namespace, String headerName) {
      if (!CommonUtils.isEmptyString(headerName)) {
         String headerNamespace = namespace == null ? "" : namespace;

         if (this.isSoapHeaderSet(headerNamespace, headerName)) {
            Stub stub = this.getStub();
            SOAPHeaderElement[] headers = stub.getHeaders();
            List remainingHeaders = (List)Arrays.stream(headers).filter((header) ->
                    !headerName.equals(header.getLocalName()) || !headerNamespace.equals(header.getNamespaceURI())).collect(Collectors.toList());
            synchronized(this) {
               stub.clearHeaders();
               remainingHeaders.forEach(header -> stub.setHeader((SOAPHeaderElement) header));
            }
         }
      }
   }

   public void removeSoapHeader(String headerName) {
      this.removeSoapHeader(this.getMessagesUrn(), headerName);
   }

   public boolean isSoapHeaderSet(String namespace, String headerName) {
      return this.getStub().getHeader(namespace, headerName) != null;
   }

   public boolean isSoapHeaderSet(String headerName) {
      return this.isSoapHeaderSet(this.getMessagesUrn(), headerName);
   }

   protected void replaceSoapHeader(String headerName, Object value) {
      this.removeSoapHeader(headerName);
      this.addSoapHeader(headerName, value);
   }

   protected void updateTokenPassportInMessage(MessageContext messageContext, TokenPassport tokenPassport) {
      QName tokenPassportQName = new QName(this.getMessagesUrn(), "tokenPassport");

      try {
         SOAPHeader soapHeader = messageContext.getCurrentMessage().getSOAPHeader();
         if (tokenPassport.isAutomaticallyUpdated()) {
            tokenPassport.update();
         }

         soapHeader.addChildElement(new SOAPHeaderElement(tokenPassportQName, Utils.convertTokenPassport(tokenPassport)));
      } catch (SOAPException var5) {
         var5.printStackTrace();
      }

   }

   public Passport getPassport() {
      return this.passport;
   }

   public void setPassport(Passport passport) {
      this.passport = passport;
   }

   public void unsetPassport() {
      this.setRequestLevelCredentials(false);
      this.passport = null;
   }

   public void setRequestLevelCredentials(boolean useRequestLevelCredentials) {
      if (useRequestLevelCredentials && this.passport == null) {
         throw new IllegalStateException("Passport must be set before setting Request-Level Credentials");
      } else {
         if (useRequestLevelCredentials) {
            this.replaceSoapHeader("passport", Utils.convertPassport(this.passport));
         } else {
            this.removeSoapHeader("passport");
         }

      }
   }

   public void setRequestLevelCredentials(Passport passport) {
      this.setPassport(passport);
      this.setRequestLevelCredentials(true);
   }

   public SsoPassport getSsoPassport() {
      return this.ssoPassport;
   }

   public void setSsoPassport(SsoPassport ssoPassport) {
      this.ssoPassport = ssoPassport;
   }

   public void unsetSsoPassport() {
      this.ssoPassport = null;
      this.removeSoapHeader("ssoPassport");
   }

   public TokenPassport getTokenPassport() {
      return this.tokenPassport;
   }

   public void setTokenPassport(TokenPassport tokenPassport) {
      this.tokenPassport = tokenPassport;
   }

   public void unsetTokenPassport() {
      this.tokenPassport = null;
   }

   public OAuthPassport getOAuthPassport() {
      return this.oAuthPassport;
   }

   public void setOAuthPassport(OAuthPassport oAuthPassport) {
      this.oAuthPassport = oAuthPassport;
   }

   public void unsetOAuthPassport() {
      this.oAuthPassport = null;
   }

   public void setAutomaticSessionManagement(boolean automaticSessionManagement) {
      this.getStub().setMaintainSession(automaticSessionManagement);
   }

   protected static void logMessage(Message message, boolean isRequest) throws AxisFault {
      if (message != null && message.getSOAPPartAsString() != null) {
         LOG.info(CommonUtils.getLogMessage(message.getSOAPPartAsString(), isRequest));
      }

   }

   private class ResponseHandler extends SimpleTargetedChain {
      private ResponseHandler() {
         super();
      }

      public void invoke(MessageContext messageContext) throws AxisFault {
         WsCoreClient.this.responseHttpHeaders.clear();
         Message responseMessage = messageContext.getResponseMessage();
         if (responseMessage != null) {
            MimeHeaders headers = responseMessage.getMimeHeaders();
            Iterator headersIterator = headers.getAllHeaders();

            while(headersIterator.hasNext()) {
               WsCoreClient.this.responseHttpHeaders.add(headersIterator.next());
            }
         }

         WsCoreClient.logMessage(messageContext.getResponseMessage(), false);
         super.invoke(messageContext);
      }

      // $FF: synthetic method
      ResponseHandler(Object x1) {
         this();
      }
   }

   private class RequestHandler extends SimpleTargetedChain {
      private RequestHandler() {
         super();
      }

      public void invoke(MessageContext messageContext) throws AxisFault {
         messageContext.setProperty("axis.transport.version", HttpVersion.HTTP_1_1.equals(WsCoreClient.this.getHttpProtocolVersion()) ? HTTPConstants.HEADER_PROTOCOL_V11 : HTTPConstants.HEADER_PROTOCOL_V10);
         List<MimeHeader> mimeHeaders = new ArrayList(WsCoreClient.this.requestHttpHeaders);
         if (WsCoreClient.this.oAuthPassport != null) {
            mimeHeaders.add(new MimeHeader(WsCoreClient.this.oAuthPassport.getOAuthHttpHeaderName(), WsCoreClient.this.oAuthPassport.getOAuthHttpHeaderValue()));
         }

         if (!mimeHeaders.isEmpty()) {
            Hashtable headersMap = new Hashtable(mimeHeaders.size());
            mimeHeaders.forEach(header -> {
               String var10000 = (String)headersMap.put(header.getName(), header.getValue());
            });
            messageContext.setProperty("HTTP-Request-Headers", headersMap);
         }

         if (WsCoreClient.this.tokenPassport != null) {
            WsCoreClient.this.updateTokenPassportInMessage(messageContext, WsCoreClient.this.tokenPassport);
         }

         WsCoreClient.logMessage(messageContext.getRequestMessage(), true);
         super.invoke(messageContext);
      }

      // $FF: synthetic method
      RequestHandler(Object x1) {
         this();
      }
   }
}
