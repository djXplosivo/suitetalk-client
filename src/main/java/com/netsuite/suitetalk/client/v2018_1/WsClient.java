package com.netsuite.suitetalk.client.v2018_1;

import com.netsuite.suitetalk.client.common.authentication.OAuthPassport;
import com.netsuite.suitetalk.client.common.authentication.Passport;
import com.netsuite.suitetalk.client.common.authentication.SsoPassport;
import com.netsuite.suitetalk.client.common.authentication.TokenPassport;
import com.netsuite.suitetalk.client.common.contract.WebServicesSoapClient;
import com.netsuite.suitetalk.client.common.utils.CommonUtils;
import com.netsuite.suitetalk.client.v2018_1.utils.Utils;
import com.netsuite.suitetalk.proxy.v2018_1.platform.core.*;
import com.netsuite.suitetalk.proxy.v2018_1.platform.core.types.*;
import com.netsuite.suitetalk.proxy.v2018_1.platform.messages.*;
import org.apache.axis.Message;
import org.apache.axis.client.Call;
import org.apache.axis.message.MimeHeaders;
import org.apache.axis.message.SOAPHeaderElement;
import org.apache.axis.soap.MessageFactoryImpl;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.rpc.ServiceException;
import javax.xml.soap.*;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.*;
import java.util.stream.Collectors;

@ParametersAreNonnullByDefault
public class WsClient extends WsCoreClient implements WebServicesSoapClient {
   private static final Logger LOG = Logger.getLogger(WsClient.class);
   private String sessionId;
   private String applicationId;
   private String lastSearchJobId;
   private String lastGetPostingTransactionSummaryJobId;
   private Preferences preferences;
   private SearchPreferences searchPreferences;

   public WsClient(URL url) {
      super(url);
      this.setHttpProtocolVersion(HttpVersion.HTTP_1_0);
   }

   public WsClient(Passport passport, URL url) {
      this(url);
      this.setPassport(passport);
   }

   public WsClient(SsoPassport ssoPassport, URL url) {
      this(url);
      this.setSsoPassport(ssoPassport);
   }

   public WsClient(TokenPassport tokenPassport, URL url) {
      this(url);
      this.setTokenPassport(tokenPassport);
   }

   public WsClient(OAuthPassport oAuthPassport, URL url) {
      this(url);
      this.setOAuthPassport(oAuthPassport);
   }

   protected Preferences getPreferences() {
      if (this.preferences == null) {
         this.preferences = new Preferences();
      }

      if (!this.isSoapHeaderSet("preferences")) {
         this.addSoapHeader("preferences", this.preferences);
      }

      return this.preferences;
   }

   protected SearchPreferences getSearchPreferences() {
      if (this.searchPreferences == null) {
         this.searchPreferences = new SearchPreferences();
      }

      if (!this.isSoapHeaderSet("searchPreferences")) {
         this.addSoapHeader("searchPreferences", this.searchPreferences);
      }

      return this.searchPreferences;
   }

   public void setIgnoreReadOnlyFields(boolean ignoreReadOnlyFields) {
      this.getPreferences().setIgnoreReadOnlyFields(ignoreReadOnlyFields);
   }

   public void setWarningAsError(boolean warningAsError) {
      this.getPreferences().setWarningAsError(warningAsError);
   }

   public void setDisableMandatoryCustomFieldValidation(boolean disableValidation) {
      this.getPreferences().setDisableMandatoryCustomFieldValidation(disableValidation);
   }

   public void setDisableSystemNotesForCustomFields(boolean disableSystemNotes) {
      this.getPreferences().setDisableSystemNotesForCustomFields(disableSystemNotes);
   }

   public void setRunServerSuiteScriptAndWorkflowTriggers(boolean runServerSuiteScriptAndWorkflowTriggers) {
      this.getPreferences().setRunServerSuiteScriptAndTriggerWorkflows(runServerSuiteScriptAndWorkflowTriggers);
   }

   public void setBodyFieldsOnly(boolean bodyFieldsOnly) {
      this.getSearchPreferences().setBodyFieldsOnly(bodyFieldsOnly);
   }

   public void setPageSize(int pageSize) {
      this.getSearchPreferences().setPageSize(pageSize);
   }

   public void setReturnSearchColumns(boolean returnSearchColumns) {
      this.getSearchPreferences().setReturnSearchColumns(returnSearchColumns);
   }

   public void setSearchPreferences(boolean bodyFieldsOnly, int pageSize, boolean returnSearchColumns) {
      this.setBodyFieldsOnly(bodyFieldsOnly);
      this.setPageSize(pageSize);
      this.setReturnSearchColumns(returnSearchColumns);
   }

   public String getApplicationId() {
      return this.applicationId;
   }

   public void setApplicationId(String applicationId) {
      if (applicationId != null && !applicationId.equals(this.getApplicationId())) {
         this.applicationId = applicationId;
         ApplicationInfo applicationInfo = new ApplicationInfo();
         applicationInfo.setApplicationId(applicationId);
         this.replaceSoapHeader("applicationInfo", applicationInfo);
      }
   }

   public void unsetApplicationId() {
      this.applicationId = null;
      this.removeSoapHeader("applicationInfo");
   }

   public String getSessionId() {
      return this.sessionId;
   }

   public void setSessionId(String sessionId) {
      this.sessionId = sessionId;
      this.setAutomaticSessionManagement(false);
      this.setHttpHeader("Cookie", "JSESSIONID=" + sessionId);
   }

   public void unsetSessionId() {
      this.sessionId = null;
      this.removeSessionIdFromRequestHeaders();
      this.setAutomaticSessionManagement(true);
   }

   protected void removeSessionIdFromRequestHeaders() {
      MimeHeader sessionCookie = null;
      Iterator var2 = this.requestHttpHeaders.iterator();

      while(var2.hasNext()) {
         MimeHeader header = (MimeHeader)var2.next();
         if ("Cookie".equals(header.getName()) && header.getValue().contains("JSESSIONID")) {
            sessionCookie = header;
            break;
         }
      }

      if (sessionCookie != null) {
         int index = this.requestHttpHeaders.indexOf(sessionCookie);
         this.requestHttpHeaders.remove(sessionCookie);
         String cookieWithoutSessionId = CommonUtils.getCookieWithoutSessionId(sessionCookie.getValue());
         if (!cookieWithoutSessionId.isEmpty()) {
            this.requestHttpHeaders.add(index, new MimeHeader(sessionCookie.getName(), cookieWithoutSessionId));
         }
      }

   }

   public String getLastJobId() {
      try {
         Call call = this.getStub()._getCall();
         Message message = call.getResponseMessage();
         SOAPHeader soapHeader = message.getSOAPHeader();
         SOAPHeaderElement documentInfo = (SOAPHeaderElement)soapHeader.getElementsByTagName("documentInfo").item(0);
         SOAPElement jobId = (SOAPElement)documentInfo.getElementsByTagName("nsId").item(0);
         return jobId.getValue();
      } catch (NullPointerException | SOAPException var6) {
         return null;
      }
   }

   public SessionResponse callLogin(Passport passport) throws RemoteException {
      com.netsuite.suitetalk.proxy.v2018_1.platform.core.Passport endpointSpecificPassport = Utils.convertPassport(passport);
      SessionResponse sessionResponse;
      synchronized(this) {
         sessionResponse = this.getPort().login(endpointSpecificPassport);
      }

      this.saveSessionId(sessionResponse);
      return sessionResponse;
   }

   public SessionResponse callLogin() throws RemoteException {
      Passport passport = this.getPassport();
      if (passport == null) {
         throw new IllegalStateException("Passport must be set before invoking login operation.");
      } else {
         return this.callLogin(passport);
      }
   }

   public boolean login(Passport passport) {
      SessionResponse sessionResponse;
      try {
         sessionResponse = this.callLogin(passport);
      } catch (RemoteException var4) {
         LOG.warn("Login failed", var4);
         return false;
      }

      return sessionResponse.getStatus().isIsSuccess();
   }

   public boolean login() {
      Passport passport = this.getPassport();
      if (passport == null) {
         throw new IllegalStateException("Passport must be set before invoking login operation.");
      } else {
         return this.login(passport);
      }
   }

   private void saveSessionId(SessionResponse sessionResponse) {
      if (sessionResponse.getStatus().isIsSuccess()) {
         List cookiesWithSession = (List)this.getResponseHttpHeader("Set-Cookie")
                 .stream().filter((cookie) -> cookie.contains("JSESSIONID")).collect(Collectors.toList());
         
         if (cookiesWithSession.isEmpty()) {
            this.sessionId = null;
         } else {
            this.sessionId = CommonUtils.parseSessionIdFromCookie((String)cookiesWithSession.get(0));
         }
      }

   }

   public SessionResponse callLogout() throws RemoteException {
      SessionResponse sessionResponse;
      synchronized(this) {
         sessionResponse = this.getPort().logout();
      }

      this.clearSessionId(sessionResponse);
      return sessionResponse;
   }

   public boolean logout() {
      SessionResponse sessionResponse;
      try {
         sessionResponse = this.callLogout();
      } catch (RemoteException var3) {
         LOG.warn("Logout failed", var3);
         return false;
      }

      return sessionResponse.getStatus().isIsSuccess();
   }

   private void clearSessionId(SessionResponse sessionResponse) {
      if (sessionResponse.getStatus().isIsSuccess()) {
         this.sessionId = null;
      }

   }

   public SessionResponse callMapSso(Passport netsuitePassport, SsoPassport thirdPartyPassport) throws RemoteException {
      SsoCredentials ssoCredentials = new SsoCredentials();
      ssoCredentials.setEmail(netsuitePassport.getEmail());
      ssoCredentials.setPassword(netsuitePassport.getPassword());
      ssoCredentials.setAccount(netsuitePassport.getAccount());
      ssoCredentials.setRole(Utils.createRecordRef(netsuitePassport.getRole()));
      ssoCredentials.setAuthenticationToken(thirdPartyPassport.getAuthenticationToken());
      ssoCredentials.setPartnerId(thirdPartyPassport.getPartnerId());
      SessionResponse sessionResponse;
      synchronized(this) {
         sessionResponse = this.getPort().mapSso(ssoCredentials);
      }

      this.saveSessionId(sessionResponse);
      return sessionResponse;
   }

   public boolean mapSso(Passport netsuitePassport, SsoPassport thirdPartyPassport) throws RemoteException {
      SessionResponse sessionResponse;
      try {
         sessionResponse = this.callMapSso(netsuitePassport, thirdPartyPassport);
      } catch (RemoteException var5) {
         LOG.warn("SSO mapping failed", var5);
         return false;
      }

      return sessionResponse.getStatus().isIsSuccess();
   }

   public SessionResponse callSsoLogin(SsoPassport ssoPassport) throws RemoteException {
      com.netsuite.suitetalk.proxy.v2018_1.platform.core.SsoPassport endpointSsoPassport = Utils.convertSsoPassport(ssoPassport);
      SessionResponse sessionResponse;
      synchronized(this) {
         sessionResponse = this.getPort().ssoLogin(endpointSsoPassport);
      }

      this.saveSessionId(sessionResponse);
      return sessionResponse;
   }

   public boolean ssoLogin(SsoPassport ssoPassport) throws RemoteException {
      SessionResponse sessionResponse;
      try {
         sessionResponse = this.callSsoLogin(ssoPassport);
      } catch (RemoteException var4) {
         LOG.warn("SSO login failed", var4);
         return false;
      }

      return sessionResponse.getStatus().isIsSuccess();
   }

   public synchronized WriteResponse callAddRecord(Record record) throws RemoteException {
      return this.getPort().add(record);
   }

   public String addRecord(Record record) throws RemoteException {
      WriteResponse writeResponse = this.callAddRecord(record);
      return writeResponse.getStatus().isIsSuccess() ? Utils.getInternalId(writeResponse.getBaseRef()) : null;
   }

   public synchronized WriteResponseList callAddRecords(Record... records) throws RemoteException {
      return this.getPort().addList(records);
   }

   public WriteResponseList callAddRecords(List records) throws RemoteException {
      return this.callAddRecords((Record[])records.toArray(new Record[records.size()]));
   }

   public List addRecords(Record... records) throws RemoteException {
      return Utils.getInternalIds(this.callAddRecords(records));
   }

   public List addRecords(List records) throws RemoteException {
      return this.addRecords((Record[])records.toArray(new Record[records.size()]));
   }

   public synchronized WriteResponse callUpdateRecord(Record record) throws RemoteException {
      return this.getPort().update(record);
   }

   public String updateRecord(Record record) throws RemoteException {
      WriteResponse writeResponse = this.callUpdateRecord(record);
      return writeResponse.getStatus().isIsSuccess() ? Utils.getInternalId(writeResponse.getBaseRef()) : null;
   }

   public synchronized WriteResponseList callUpdateRecords(Record... records) throws RemoteException {
      return this.getPort().updateList(records);
   }

   public WriteResponseList callUpdateRecords(List records) throws RemoteException {
      return this.callUpdateRecords((Record[])records.toArray(new Record[records.size()]));
   }

   public List updateRecords(Record... records) throws RemoteException {
      return Utils.getInternalIds(this.callUpdateRecords(records));
   }

   public List updateRecords(List records) throws RemoteException {
      return this.updateRecords((Record[])records.toArray(new Record[records.size()]));
   }

   public synchronized WriteResponse callUpsertRecord(Record record) throws RemoteException {
      return this.getPort().upsert(record);
   }

   public String upsertRecord(Record record) throws RemoteException {
      WriteResponse writeResponse = this.callUpsertRecord(record);
      return writeResponse.getStatus().isIsSuccess() ? Utils.getInternalId(writeResponse.getBaseRef()) : null;
   }

   public synchronized WriteResponseList callUpsertRecords(Record... records) throws RemoteException {
      return this.getPort().upsertList(records);
   }

   public WriteResponseList callUpsertRecords(List records) throws RemoteException {
      return this.callUpsertRecords((Record[])records.toArray(new Record[records.size()]));
   }

   public List upsertRecords(Record... records) throws RemoteException {
      return Utils.getInternalIds(this.callUpsertRecords(records));
   }

   public List upsertRecords(List records) throws RemoteException {
      return this.upsertRecords((Record[])records.toArray(new Record[records.size()]));
   }

   public synchronized ReadResponse callGetRecord(BaseRef baseRef) throws RemoteException {
      return this.getPort().get(baseRef);
   }

   public ReadResponse callGetRecord(String internalId, RecordType recordType) throws RemoteException {
      return this.callGetRecord(Utils.createRecordRef(internalId, recordType));
   }

   public Record getRecord(BaseRef baseRef) throws RemoteException {
      return this.callGetRecord(baseRef).getRecord();
   }

   public Record getRecord(String internalId, RecordType recordType) throws RemoteException {
      return this.getRecord(Utils.createRecordRef(internalId, recordType));
   }

   public ReadResponse callGetRecordByExternalId(String externalId, RecordType recordType) throws RemoteException {
      return this.callGetRecord(Utils.createRecordRefWithExternalId(externalId, recordType));
   }

   public Record getRecordByExternalId(String externalId, RecordType recordType) throws RemoteException {
      return this.callGetRecordByExternalId(externalId, recordType).getRecord();
   }

   public synchronized ReadResponseList callGetRecords(BaseRef... baseRefs) throws RemoteException {
      return this.getPort().getList(baseRefs);
   }

   public ReadResponseList callGetRecords(List refs) throws RemoteException {
      return this.callGetRecords((BaseRef[])refs.toArray(new BaseRef[refs.size()]));
   }

   public List getRecords(BaseRef... baseRefs) throws RemoteException {
      return Utils.getRecords(this.callGetRecords(baseRefs));
   }

   public List getRecords(List refs) throws RemoteException {
      return this.getRecords((BaseRef[])refs.toArray(new BaseRef[refs.size()]));
   }

   public synchronized WriteResponse callDeleteRecord(BaseRef deleteRecordReference, @Nullable DeletionReason deletionReason) throws RemoteException {
      return this.getPort().delete(deleteRecordReference, deletionReason);
   }

   public WriteResponse callDeleteRecord(String internalId, RecordType recordType, @Nullable RecordRef deletionReasonCode, @Nullable String deletionReasonMemo) throws RemoteException {
      return this.callDeleteRecord((BaseRef)Utils.createRecordRef(internalId, recordType), (DeletionReason)this.getDeletionReason(deletionReasonCode, deletionReasonMemo));
   }

   public WriteResponse callDeleteRecord(String internalId, RecordType recordType) throws RemoteException {
      return this.callDeleteRecord((BaseRef)Utils.createRecordRef(internalId, recordType), (DeletionReason)null);
   }

   public WriteResponse callDeleteRecordByExternalId(String externalId, RecordType recordType) throws RemoteException {
      return this.callDeleteRecord((BaseRef)Utils.createRecordRefWithExternalId(externalId, recordType), (DeletionReason)null);
   }

   public WriteResponse callDeleteRecordByExternalId(String externalId, RecordType recordType, @Nullable RecordRef deletionReasonCode, @Nullable String deletionReasonMemo) throws RemoteException {
      return this.callDeleteRecord((BaseRef)Utils.createRecordRefWithExternalId(externalId, recordType), (DeletionReason)this.getDeletionReason(deletionReasonCode, deletionReasonMemo));
   }

   public boolean deleteRecord(BaseRef baseRef, @Nullable DeletionReason deletionReason) throws RemoteException {
      return this.callDeleteRecord(baseRef, deletionReason).getStatus().isIsSuccess();
   }

   public boolean deleteRecord(String internalId, RecordType recordType, @Nullable RecordRef deletionReasonCode, @Nullable String deletionReasonMemo) throws RemoteException {
      return this.deleteRecord((BaseRef)Utils.createRecordRef(internalId, recordType), (DeletionReason)this.getDeletionReason(deletionReasonCode, deletionReasonMemo));
   }

   public boolean deleteRecord(String internalId, RecordType recordType) throws RemoteException {
      return this.deleteRecord((BaseRef)Utils.createRecordRef(internalId, recordType), (DeletionReason)null);
   }

   public boolean deleteRecordByExternalId(String externalId, RecordType recordType, @Nullable RecordRef deletionReasonCode, @Nullable String deletionReasonMemo) throws RemoteException {
      return this.deleteRecord((BaseRef)Utils.createRecordRefWithExternalId(externalId, recordType), (DeletionReason)this.getDeletionReason(deletionReasonCode, deletionReasonMemo));
   }

   public boolean deleteRecordByExternalId(String externalId, RecordType recordType) throws RemoteException {
      return this.deleteRecord((BaseRef)Utils.createRecordRefWithExternalId(externalId, recordType), (DeletionReason)null);
   }

   private DeletionReason getDeletionReason(@Nullable RecordRef deletionReasonCode, @Nullable String deletionMemo) {
      if (deletionReasonCode == null && deletionMemo == null) {
         return null;
      } else {
         DeletionReason deletionReason = new DeletionReason();
         deletionReason.setDeletionReasonCode(deletionReasonCode);
         deletionReason.setDeletionReasonMemo(deletionMemo);
         return deletionReason;
      }
   }

   public synchronized WriteResponseList callDeleteRecords(@Nullable DeletionReason deletionReason, BaseRef... baseRefs) throws RemoteException {
      return this.getPort().deleteList(baseRefs, deletionReason);
   }

   public WriteResponseList callDeleteRecords(BaseRef... baseRefs) throws RemoteException {
      return this.callDeleteRecords((DeletionReason)null, (BaseRef[])baseRefs);
   }

   public WriteResponseList callDeleteRecords(@Nullable DeletionReason deletionReason, List baseRefs) throws RemoteException {
      return this.callDeleteRecords(deletionReason, (BaseRef[])baseRefs.toArray(new BaseRef[baseRefs.size()]));
   }

   public WriteResponseList callDeleteRecords(List baseRefs) throws RemoteException {
      return this.callDeleteRecords((DeletionReason)null, (List)baseRefs);
   }

   public List deleteRecords(@Nullable DeletionReason deletionReason, BaseRef... baseRefs) throws RemoteException {
      return Utils.getSuccess(this.callDeleteRecords(deletionReason, baseRefs));
   }

   public List deleteRecords(BaseRef... baseRefs) throws RemoteException {
      return Utils.getSuccess(this.callDeleteRecords(baseRefs));
   }

   public List deleteRecords(@Nullable DeletionReason deletionReason, List baseRefs) throws RemoteException {
      return this.deleteRecords(deletionReason, (BaseRef[])baseRefs.toArray(new BaseRef[baseRefs.size()]));
   }

   public List deleteRecords(List baseRefs) throws RemoteException {
      return this.deleteRecords((DeletionReason)null, (List)baseRefs);
   }

   public synchronized GetDeletedResult callGetDeletedRecords(GetDeletedFilter getDeletedFilter, int pageIndex) throws RemoteException {
      return this.getPort().getDeleted(getDeletedFilter, pageIndex);
   }

   public List getDeletedRecords(GetDeletedFilter getDeletedFilter, int pageIndex) throws RemoteException {
      GetDeletedResult getDeletedResult = this.callGetDeletedRecords(getDeletedFilter, pageIndex);
      if (!getDeletedResult.getStatus().isIsSuccess()) {
         return null;
      } else {
         DeletedRecord[] deletedRecords = getDeletedResult.getDeletedRecordList().getDeletedRecord();
         return deletedRecords != null && deletedRecords.length != 0 ? Arrays.asList(deletedRecords) : Collections.emptyList();
      }
   }

   public synchronized GetAllResult callGetAllRecords(GetAllRecordType getAllRecordType) throws RemoteException {
      return this.getPort().getAll(new GetAllRecord(getAllRecordType));
   }

   public List getAllRecords(GetAllRecordType getAllRecordType) throws RemoteException {
      GetAllResult getAllResult = this.callGetAllRecords(getAllRecordType);
      if (!getAllResult.getStatus().isIsSuccess()) {
         return null;
      } else {
         Record[] allRecords = getAllResult.getRecordList().getRecord();
         return allRecords != null && allRecords.length != 0 ? Arrays.asList(allRecords) : Collections.emptyList();
      }
   }

   public synchronized ReadResponse callInitialize(InitializeRecord initializeRecord) throws RemoteException {
      return this.getPort().initialize(initializeRecord);
   }

   public ReadResponse callInitialize(InitializeRef reference, InitializeType type) throws RemoteException {
      InitializeRecord initializeRecord = new InitializeRecord();
      initializeRecord.setReference(reference);
      initializeRecord.setType(type);
      return this.callInitialize(initializeRecord);
   }

   public Record initialize(InitializeRecord initializeRecord) throws RemoteException {
      return this.callInitialize(initializeRecord).getRecord();
   }

   public Record initialize(InitializeRef reference, InitializeType type) throws RemoteException {
      InitializeRecord initializeRecord = new InitializeRecord();
      initializeRecord.setReference(reference);
      initializeRecord.setType(type);
      return this.initialize(initializeRecord);
   }

   public synchronized ReadResponseList callInitializeList(InitializeRecord... initializeRecords) throws RemoteException {
      return this.getPort().initializeList(initializeRecords);
   }

   public ReadResponseList callInitializeList(List initializeRecords) throws RemoteException {
      return this.callInitializeList((InitializeRecord[])initializeRecords.toArray(new InitializeRecord[initializeRecords.size()]));
   }

   public List initializeList(InitializeRecord... initializeRecords) throws RemoteException {
      ReadResponseList readResponseList = this.callInitializeList(initializeRecords);
      ReadResponse[] readResponses = readResponseList.getReadResponse();
      return readResponses == null ? null : (List)Arrays.stream(readResponses).map(ReadResponse::getRecord).collect(Collectors.toList());
   }

   public List initializeList(List initializeRecords) throws RemoteException {
      return this.initializeList((InitializeRecord[])initializeRecords.toArray(new InitializeRecord[initializeRecords.size()]));
   }

   public SearchResult callSearch(SearchRecord searchRecord) throws RemoteException {
      SearchResult searchResult;
      synchronized(this) {
         searchResult = this.getPort().search(searchRecord);
      }

      this.lastSearchJobId = this.getLastJobId();
      return searchResult;
   }

   public List search(SearchRecord searchRecord) throws RemoteException {
      SearchResult searchResult = this.callSearch(searchRecord);
      return Utils.getSearchResults(searchResult);
   }

   public synchronized SearchResult callSearchMore(int pageIndex) throws RemoteException {
      return this.getPort().searchMore(pageIndex);
   }

   public List searchMore(int pageIndex) throws RemoteException {
      return Utils.getSearchResults(this.callSearchMore(pageIndex));
   }

   public synchronized SearchResult callSearchNext() throws RemoteException {
      return this.getPort().searchNext();
   }

   public List searchNext() throws RemoteException {
      return Utils.getSearchResults(this.callSearchNext());
   }

   public SearchResult callSearchMoreWithId(int pageIndex) throws RemoteException {
      if (this.lastSearchJobId == null) {
         throw new IllegalStateException("Operation search has to be called before calling callSearchMoreWithId");
      } else {
         return this.callSearchMoreWithId(this.lastSearchJobId, pageIndex);
      }
   }

   public List searchMoreWithId(int pageIndex) throws RemoteException {
      return Utils.getSearchResults(this.callSearchMoreWithId(pageIndex));
   }

   public synchronized SearchResult callSearchMoreWithId(String jobId, int pageIndex) throws RemoteException {
      return this.getPort().searchMoreWithId(jobId, pageIndex);
   }

   public List searchMoreWithId(String jobId, int pageIndex) throws RemoteException {
      return Utils.getSearchResults(this.callSearchMoreWithId(jobId, pageIndex));
   }

   public List searchAll(SearchRecord searchRecord) throws RemoteException {
      SearchResult searchResult = this.callSearch(searchRecord);
      List firstPageResults = Utils.getSearchResults(searchResult);
      if (firstPageResults == null) {
         return null;
      } else if (firstPageResults.isEmpty()) {
         return firstPageResults;
      } else {
         List foundRecords = new ArrayList(searchResult.getTotalRecords());
         foundRecords.addAll(firstPageResults);
         int totalPages = searchResult.getTotalPages();
         String searchId = searchResult.getSearchId();

         for(int pageIndex = 2; pageIndex <= totalPages; ++pageIndex) {
            SearchResult searchMoreResult = this.callSearchMoreWithId(searchId, pageIndex);
            List otherPageResults = Utils.getSearchResults(searchMoreResult);
            if (otherPageResults == null) {
               return null;
            }

            foundRecords.addAll(otherPageResults);
         }

         return foundRecords;
      }
   }

   public synchronized AsyncStatusResult callAsyncAddList(Record... records) throws RemoteException {
      return this.getPort().asyncAddList(records);
   }

   public AsyncStatusResult callAsyncAddList(List records) throws RemoteException {
      return this.callAsyncAddList((Record[])records.toArray(new Record[records.size()]));
   }

   public String asyncAddList(Record... records) throws RemoteException {
      return this.callAsyncAddList(records).getJobId();
   }

   public String asyncAddList(List records) throws RemoteException {
      return this.asyncAddList((Record[])records.toArray(new Record[records.size()]));
   }

   public synchronized AsyncStatusResult callAsyncGetList(BaseRef... refs) throws RemoteException {
      return this.getPort().asyncGetList(refs);
   }

   public AsyncStatusResult callAsyncGetList(List refs) throws RemoteException {
      return this.callAsyncGetList((BaseRef[])refs.toArray(new BaseRef[refs.size()]));
   }

   public String asyncGetList(BaseRef... refs) throws RemoteException {
      return this.callAsyncGetList(refs).getJobId();
   }

   public String asyncGetList(List refs) throws RemoteException {
      return this.asyncGetList((BaseRef[])refs.toArray(new BaseRef[refs.size()]));
   }

   public synchronized AsyncStatusResult callAsyncUpdateList(Record... records) throws RemoteException {
      return this.getPort().asyncUpdateList(records);
   }

   public AsyncStatusResult callAsyncUpdateList(List records) throws RemoteException {
      return this.callAsyncUpdateList((Record[])records.toArray(new Record[records.size()]));
   }

   public String asyncUpdateList(Record... records) throws RemoteException {
      return this.callAsyncUpdateList(records).getJobId();
   }

   public String asyncUpdateList(List records) throws RemoteException {
      return this.asyncUpdateList((Record[])records.toArray(new Record[records.size()]));
   }

   public synchronized AsyncStatusResult callAsyncUpsertList(Record... records) throws RemoteException {
      return this.getPort().asyncUpsertList(records);
   }

   public AsyncStatusResult callAsyncUpsertList(List records) throws RemoteException {
      return this.callAsyncUpsertList((Record[])records.toArray(new Record[records.size()]));
   }

   public String asyncUpsertList(Record... records) throws RemoteException {
      return this.callAsyncUpsertList(records).getJobId();
   }

   public String asyncUpsertList(List records) throws RemoteException {
      return this.asyncUpsertList((Record[])records.toArray(new Record[records.size()]));
   }

   public synchronized AsyncStatusResult callAsyncDeleteList(@Nullable DeletionReason deletionReason, BaseRef... refs) throws RemoteException {
      return this.getPort().asyncDeleteList(refs, deletionReason);
   }

   public AsyncStatusResult callAsyncDeleteList(BaseRef... refs) throws RemoteException {
      return this.callAsyncDeleteList((DeletionReason)null, (BaseRef[])refs);
   }

   public AsyncStatusResult callAsyncDeleteList(@Nullable DeletionReason deletionReason, List refs) throws RemoteException {
      return this.callAsyncDeleteList(deletionReason, (BaseRef[])refs.toArray(new BaseRef[refs.size()]));
   }

   public AsyncStatusResult callAsyncDeleteList(List refs) throws RemoteException {
      return this.callAsyncDeleteList((DeletionReason)null, (List)refs);
   }

   public String asyncDeleteList(BaseRef... refs) throws RemoteException {
      return this.callAsyncDeleteList(refs).getJobId();
   }

   public String asyncDeleteList(List refs) throws RemoteException {
      return this.asyncDeleteList((BaseRef[])refs.toArray(new BaseRef[refs.size()]));
   }

   public synchronized AsyncStatusResult callAsyncSearch(SearchRecord searchRecord) throws RemoteException {
      return this.getPort().asyncSearch(searchRecord);
   }

   public String asyncSearch(SearchRecord searchRecord) throws RemoteException {
      return this.callAsyncSearch(searchRecord).getJobId();
   }

   public synchronized AsyncStatusResult callAsyncInitializeList(InitializeRecord... initializeRecords) throws RemoteException {
      return this.getPort().asyncInitializeList(initializeRecords);
   }

   public AsyncStatusResult callAsyncInitializeList(List initializeRecords) throws RemoteException {
      return this.callAsyncInitializeList((InitializeRecord[])initializeRecords.toArray(new InitializeRecord[initializeRecords.size()]));
   }

   public String asyncInitializeList(InitializeRecord... initializeRecords) throws RemoteException {
      return this.callAsyncInitializeList(initializeRecords).getJobId();
   }

   public String asyncInitializeList(List initializeRecords) throws RemoteException {
      return this.asyncInitializeList((InitializeRecord[])initializeRecords.toArray(new InitializeRecord[initializeRecords.size()]));
   }

   public synchronized AsyncStatusResult callCheckAsyncStatus(String jobId) throws RemoteException {
      return this.getPort().checkAsyncStatus(jobId);
   }

   public AsyncStatusType checkAsyncStatus(String jobId) throws RemoteException {
      return this.callCheckAsyncStatus(jobId).getStatus();
   }

   public synchronized AsyncResult callGetAsyncResult(String jobId, int pageIndex) throws RemoteException {
      return this.getPort().getAsyncResult(jobId, pageIndex);
   }

   public AsyncResult getAsyncResult(String jobId, int pageIndex) throws RemoteException {
      return this.callGetAsyncResult(jobId, pageIndex);
   }

   public synchronized WriteResponse callAttach(AttachReference attachReference) throws RemoteException {
      return this.getPort().attach(attachReference);
   }

   public WriteResponse callAttach(BaseRef attachTo, BaseRef attachedRecord) throws RemoteException {
      AttachBasicReference attachReference = new AttachBasicReference();
      attachReference.setAttachTo(attachTo);
      attachReference.setAttachedRecord(attachedRecord);
      return this.callAttach(attachReference);
   }

   public boolean attach(AttachReference attachReference) throws RemoteException {
      WriteResponse writeResponse = this.callAttach(attachReference);
      return writeResponse.getStatus() != null && writeResponse.getStatus().isIsSuccess();
   }

   public boolean attach(BaseRef attachTo, BaseRef attachedRecord) throws RemoteException {
      AttachBasicReference attachReference = new AttachBasicReference();
      attachReference.setAttachTo(attachTo);
      attachReference.setAttachedRecord(attachedRecord);
      return this.attach(attachReference);
   }

   public synchronized WriteResponse callDetach(DetachReference detachReference) throws RemoteException {
      return this.getPort().detach(detachReference);
   }

   public WriteResponse callDetach(BaseRef detachFrom, BaseRef detachedRecord) throws RemoteException {
      DetachBasicReference detachReference = new DetachBasicReference();
      detachReference.setDetachFrom(detachFrom);
      detachReference.setDetachedRecord(detachedRecord);
      return this.callDetach(detachReference);
   }

   public boolean detach(DetachReference detachReference) throws RemoteException {
      WriteResponse writeResponse = this.callDetach(detachReference);
      return writeResponse.getStatus() != null && writeResponse.getStatus().isIsSuccess();
   }

   public boolean detach(BaseRef detachFrom, BaseRef detachedRecord) throws RemoteException {
      DetachBasicReference detachReference = new DetachBasicReference();
      detachReference.setDetachFrom(detachFrom);
      detachReference.setDetachedRecord(detachedRecord);
      return this.detach(detachReference);
   }

   public WriteResponse callAttachContact(BaseRef attachTo, RecordRef contact, RecordRef contactRole) throws RemoteException {
      AttachContactReference attachContactReference = new AttachContactReference();
      attachContactReference.setAttachTo(attachTo);
      attachContactReference.setContact(contact);
      attachContactReference.setContactRole(contactRole);
      return this.callAttach(attachContactReference);
   }

   public WriteResponse callAttachContact(BaseRef attachTo, RecordRef contact) throws RemoteException {
      return this.callAttachContact(attachTo, contact, (RecordRef)null);
   }

   public boolean attachContact(BaseRef attachTo, RecordRef contact, RecordRef contactRole) throws RemoteException {
      WriteResponse writeResponse = this.callAttachContact(attachTo, contact, contactRole);
      return writeResponse.getStatus() != null && writeResponse.getStatus().isIsSuccess();
   }

   public boolean attachContact(BaseRef attachTo, RecordRef contact) throws RemoteException {
      return this.attachContact(attachTo, contact, (RecordRef)null);
   }

   public synchronized GetServerTimeResult callGetServerTime() throws RemoteException {
      return this.getPort().getServerTime();
   }

   public Calendar getServerTime() throws RemoteException {
      GetServerTimeResult serverTimeResult = this.callGetServerTime();
      return serverTimeResult.getStatus().isIsSuccess() ? serverTimeResult.getServerTime() : null;
   }

   public synchronized GetDataCenterUrlsResult callGetDataCenterUrls(String companyId) throws RemoteException {
      return this.getPort().getDataCenterUrls(companyId);
   }

   public DataCenterUrls getDataCenterUrls(String companyId) throws RemoteException {
      GetDataCenterUrlsResult dataCenterUrlsResult = this.callGetDataCenterUrls(companyId);
      return dataCenterUrlsResult.getStatus().isIsSuccess() ? dataCenterUrlsResult.getDataCenterUrls() : null;
   }

   public synchronized SessionResponse callChangeEmail(ChangeEmail changeEmail) throws RemoteException {
      return this.getPort().changeEmail(changeEmail);
   }

   public SessionResponse callChangeEmail(String password, String newEmail, boolean justThisAccount) throws RemoteException {
      ChangeEmail changeEmail = new ChangeEmail();
      changeEmail.setCurrentPassword(password);
      changeEmail.setNewEmail(newEmail);
      changeEmail.setNewEmail2(newEmail);
      changeEmail.setJustThisAccount(justThisAccount);
      return this.callChangeEmail(changeEmail);
   }

   public SessionResponse callChangeEmail(String password, String newEmail) throws RemoteException {
      return this.callChangeEmail(password, newEmail, true);
   }

   private boolean getSuccessFromSessionResponse(SessionResponse sessionResponse) {
      return sessionResponse.getStatus() != null && sessionResponse.getStatus().isIsSuccess();
   }

   public boolean changeEmail(ChangeEmail changeEmail) throws RemoteException {
      return this.getSuccessFromSessionResponse(this.callChangeEmail(changeEmail));
   }

   public boolean changeEmail(String password, String newEmail, boolean justThisAccount) throws RemoteException {
      return this.getSuccessFromSessionResponse(this.callChangeEmail(password, newEmail, justThisAccount));
   }

   public boolean changeEmail(String password, String newEmail) throws RemoteException {
      return this.getSuccessFromSessionResponse(this.callChangeEmail(password, newEmail));
   }

   public synchronized SessionResponse callChangePassword(ChangePassword changePassword) throws RemoteException {
      return this.getPort().changePassword(changePassword);
   }

   public SessionResponse callChangePassword(String currentPassword, String newPassword) throws RemoteException {
      ChangePassword changePassword = new ChangePassword();
      changePassword.setCurrentPassword(currentPassword);
      changePassword.setNewPassword(newPassword);
      changePassword.setNewPassword2(newPassword);
      return this.callChangePassword(changePassword);
   }

   public boolean changePassword(ChangePassword changePassword) throws RemoteException {
      return this.getSuccessFromSessionResponse(this.callChangePassword(changePassword));
   }

   public boolean changePassword(String currentPassword, String newPassword) throws RemoteException {
      return this.getSuccessFromSessionResponse(this.callChangePassword(currentPassword, newPassword));
   }

   public synchronized GetSelectValueResult callGetSelectValue(GetSelectValueFieldDescription getSelectValueFieldDescription, int pageIndex) throws RemoteException {
      return this.getPort().getSelectValue(getSelectValueFieldDescription, pageIndex);
   }

   public List getSelectValue(GetSelectValueFieldDescription getSelectValueFieldDescription, int pageIndex) throws RemoteException {
      GetSelectValueResult getSelectValueResult = this.callGetSelectValue(getSelectValueFieldDescription, pageIndex);
      if (getSelectValueResult.getStatus().isIsSuccess()) {
         BaseRefList baseRefList = getSelectValueResult.getBaseRefList();
         return baseRefList == null ? Collections.emptyList() : Arrays.asList(baseRefList.getBaseRef());
      } else {
         return null;
      }
   }

   public List getSelectValue(GetSelectValueFieldDescription getSelectValueFieldDescription) throws RemoteException {
      List allValues = new ArrayList();
      int totalPages = 1;
      int currentPage = 0;

      while(true) {
         ++currentPage;
         if (currentPage > totalPages) {
            return allValues;
         }

         GetSelectValueResult getSelectValueResult = this.callGetSelectValue(getSelectValueFieldDescription, currentPage);
         if (getSelectValueResult.getStatus() == null || !getSelectValueResult.getStatus().isIsSuccess()) {
            return null;
         }

         totalPages = getSelectValueResult.getTotalPages();
         BaseRefList baseRefList = getSelectValueResult.getBaseRefList();
         if (baseRefList != null) {
            allValues.addAll(Arrays.asList(baseRefList.getBaseRef()));
         }
      }
   }

   public synchronized GetBudgetExchangeRateResult callGetBudgetExchangeRate(BudgetExchangeRateFilter budgetExchangeRateFilter) throws RemoteException {
      return this.getPort().getBudgetExchangeRate(budgetExchangeRateFilter);
   }

   public List getBudgetExchangeRate(RecordRef period, RecordRef fromSubsidiary, RecordRef toSubsidiary) throws RemoteException {
      BudgetExchangeRateFilter budgetExchangeRateFilter = new BudgetExchangeRateFilter();
      budgetExchangeRateFilter.setPeriod(period);
      budgetExchangeRateFilter.setFromSubsidiary(fromSubsidiary);
      budgetExchangeRateFilter.setToSubsidiary(toSubsidiary);
      GetBudgetExchangeRateResult getBudgetExchangeRateResult = this.callGetBudgetExchangeRate(budgetExchangeRateFilter);
      if (getBudgetExchangeRateResult.getStatus() != null && getBudgetExchangeRateResult.getStatus().isIsSuccess()) {
         BudgetExchangeRateList budgetExchangeRateList = getBudgetExchangeRateResult.getBudgetExchangeRateList();
         return budgetExchangeRateList == null ? Collections.emptyList() : Arrays.asList(budgetExchangeRateList.getBudgetExchangeRate());
      } else {
         return null;
      }
   }

   public synchronized GetCurrencyRateResult callGetCurrencyRate(CurrencyRateFilter currencyRateFilter) throws RemoteException {
      return this.getPort().getCurrencyRate(currencyRateFilter);
   }

   public List getCurrencyRate(RecordRef baseCurrency, RecordRef fromCurrency, Calendar effectiveDate) throws RemoteException {
      CurrencyRateFilter currencyRateFilter = new CurrencyRateFilter();
      currencyRateFilter.setBaseCurrency(baseCurrency);
      currencyRateFilter.setFromCurrency(fromCurrency);
      currencyRateFilter.setEffectiveDate(effectiveDate);
      GetCurrencyRateResult currencyRateResult = this.callGetCurrencyRate(currencyRateFilter);
      if (currencyRateResult.getStatus() != null && currencyRateResult.getStatus().isIsSuccess()) {
         CurrencyRateList currencyRateList = currencyRateResult.getCurrencyRateList();
         return currencyRateList == null ? Collections.emptyList() : Arrays.asList(currencyRateList.getCurrencyRate());
      } else {
         return null;
      }
   }

   public synchronized GetCustomizationIdResult callGetCustomizationId(CustomizationType customizationType, boolean includeInactives) throws RemoteException {
      return this.getPort().getCustomizationId(customizationType, includeInactives);
   }

   public List getCustomizationId(GetCustomizationType customizationType, boolean includeInactives) throws RemoteException {
      GetCustomizationIdResult customizationIdResult = this.callGetCustomizationId(new CustomizationType(customizationType), includeInactives);
      if (customizationIdResult.getStatus() != null && customizationIdResult.getStatus().isIsSuccess()) {
         CustomizationRefList customizationRefList = customizationIdResult.getCustomizationRefList();
         return customizationRefList == null ? Collections.emptyList() : Arrays.asList(customizationRefList.getCustomizationRef());
      } else {
         return null;
      }
   }

   public synchronized GetItemAvailabilityResult callGetItemAvailability(ItemAvailabilityFilter itemAvailabilityFilter) throws RemoteException {
      return this.getPort().getItemAvailability(itemAvailabilityFilter);
   }

   public List getItemAvailability(Calendar lastQtyAvailableChange, RecordRef... itemsReferences) throws RemoteException {
      ItemAvailabilityFilter itemAvailabilityFilter = new ItemAvailabilityFilter();
      itemAvailabilityFilter.setItem(new RecordRefList(itemsReferences));
      itemAvailabilityFilter.setLastQtyAvailableChange(lastQtyAvailableChange);
      GetItemAvailabilityResult getItemAvailabilityResult = this.callGetItemAvailability(itemAvailabilityFilter);
      if (getItemAvailabilityResult.getStatus() != null && getItemAvailabilityResult.getStatus().isIsSuccess()) {
         ItemAvailabilityList itemAvailabilityList = getItemAvailabilityResult.getItemAvailabilityList();
         return itemAvailabilityList == null ? Collections.emptyList() : Arrays.asList(itemAvailabilityList.getItemAvailability());
      } else {
         return null;
      }
   }

   public synchronized GetPostingTransactionSummaryResult callGetPostingTransactionSummary(PostingTransactionSummaryField postingTransactionSummaryField, PostingTransactionSummaryFilter postingTransactionSummaryFilter, int pageIndex, @Nullable String operationId) throws RemoteException {
      return this.getPort().getPostingTransactionSummary(postingTransactionSummaryField, postingTransactionSummaryFilter, pageIndex, operationId);
   }

   public GetPostingTransactionSummaryResult getPostingTransactionSummary(PostingTransactionSummaryField postingTransactionSummaryField, PostingTransactionSummaryFilter postingTransactionSummaryFilter, int pageIndex) throws RemoteException {
      GetPostingTransactionSummaryResult postingTransactionSummaryResult = this.callGetPostingTransactionSummary(postingTransactionSummaryField, postingTransactionSummaryFilter, pageIndex, pageIndex == 1 ? null : this.lastGetPostingTransactionSummaryJobId);
      if (pageIndex == 1) {
         this.lastGetPostingTransactionSummaryJobId = this.getLastJobId();
      }

      return postingTransactionSummaryResult;
   }

   public synchronized GetSavedSearchResult callGetSavedSearch(GetSavedSearchRecord searchRecordType) throws RemoteException {
      return this.getPort().getSavedSearch(searchRecordType);
   }

   public List getSavedSearch(SearchRecordType searchRecordType) throws RemoteException {
      GetSavedSearchRecord getSavedSearchRecord = new GetSavedSearchRecord();
      getSavedSearchRecord.setSearchType(searchRecordType);
      GetSavedSearchResult getSavedSearchResult = this.callGetSavedSearch(getSavedSearchRecord);
      if (getSavedSearchResult.getStatus() != null && getSavedSearchResult.getStatus().isIsSuccess()) {
         RecordRefList recordRefList = getSavedSearchResult.getRecordRefList();
         return recordRefList != null && recordRefList.getRecordRef() != null ? Arrays.asList(recordRefList.getRecordRef()) : Collections.emptyList();
      } else {
         return null;
      }
   }

   public WriteResponse callUpdateInviteeStatus(String eventInternalId, CalendarEventAttendeeResponse response) throws RemoteException {
      UpdateInviteeStatusReference updateInviteeStatusReference = new UpdateInviteeStatusReference();
      updateInviteeStatusReference.setEventId(Utils.createRecordRef(eventInternalId));
      updateInviteeStatusReference.setResponseCode(response);
      synchronized(this) {
         return this.getPort().updateInviteeStatus(updateInviteeStatusReference);
      }
   }

   public RecordRef updateInviteeStatus(String eventInternalId, CalendarEventAttendeeResponse response) throws RemoteException {
      WriteResponse writeResponse = this.callUpdateInviteeStatus(eventInternalId, response);
      return writeResponse.getStatus() != null && writeResponse.getStatus().isIsSuccess() ? (RecordRef)writeResponse.getBaseRef() : null;
   }

   public synchronized WriteResponseList callUpdateInviteeStatusList(UpdateInviteeStatusReference... updateInviteeStatusReferences) throws RemoteException {
      return this.getPort().updateInviteeStatusList(updateInviteeStatusReferences);
   }

   public WriteResponseList callUpdateInviteeStatusList(List updateInviteeStatusReferences) throws RemoteException {
      return this.callUpdateInviteeStatusList((UpdateInviteeStatusReference[])updateInviteeStatusReferences.toArray(new UpdateInviteeStatusReference[updateInviteeStatusReferences.size()]));
   }

   public List updateInviteeStatusList(UpdateInviteeStatusReference... updateInviteeStatusReferences) throws RemoteException {
      WriteResponseList writeResponseList = this.callUpdateInviteeStatusList(updateInviteeStatusReferences);
      return this.getUpdatedEventsFromResponseList(writeResponseList);
   }

   public List updateInviteeStatusList(List updateInviteeStatusReferences) throws RemoteException {
      return this.updateInviteeStatusList((UpdateInviteeStatusReference[])updateInviteeStatusReferences.toArray(new UpdateInviteeStatusReference[updateInviteeStatusReferences.size()]));
   }

   public WriteResponseList callUpdateInviteeStatusList(Map<String,CalendarEventAttendeeResponse> statusUpdates) throws RemoteException {
      return this.callUpdateInviteeStatusList((UpdateInviteeStatusReference[])statusUpdates.entrySet().stream().map((entry) -> {
         UpdateInviteeStatusReference updateInviteeStatusReference = new UpdateInviteeStatusReference();
         updateInviteeStatusReference.setEventId(Utils.createRecordRef((String)entry.getKey()));
         updateInviteeStatusReference.setResponseCode((CalendarEventAttendeeResponse)entry.getValue());
         return updateInviteeStatusReference;
      }).toArray((x$0) -> new UpdateInviteeStatusReference[x$0]));
   }

   public List updateInviteeStatusList(Map statusUpdates) throws RemoteException {
      WriteResponseList writeResponseList = this.callUpdateInviteeStatusList(statusUpdates);
      return this.getUpdatedEventsFromResponseList(writeResponseList);
   }

   private List getUpdatedEventsFromResponseList(WriteResponseList writeResponseList) {
      return (List)Arrays.stream(writeResponseList.getWriteResponse()).map((writeResponse) -> {
         return writeResponse.getStatus().isIsSuccess() ? (RecordRef)writeResponse.getBaseRef() : null;
      }).collect(Collectors.toList());
   }

   public SOAPEnvelope sendSoapMessage(String soapAction, String soapMessage, Charset soapMessageEncoding) {
      try {
         MimeHeaders httpHeaders = new MimeHeaders();
         SOAPMessage message = (new MessageFactoryImpl()).createMessage(httpHeaders, new ByteArrayInputStream(soapMessage.getBytes(soapMessageEncoding)));
         Call call = (Call)this.getLocator().createCall();
         call.setTargetEndpointAddress(this.getEndpointUrl());
         call.setSOAPActionURI(soapAction);
         return call.invoke((Message)message);
      } catch (ServiceException | IOException | SOAPException var7) {
         throw new RuntimeException(var7);
      }
   }

   public SOAPEnvelope sendSoapMessage(String soapAction, String soapMessage) {
      return this.sendSoapMessage(soapAction, soapMessage, Charset.forName("UTF-8"));
   }

   public SOAPEnvelope sendSoapMessage(String soapMessage) {
      String soapAction = null;

      try {
         XPath xPath = XPathFactory.newInstance().newXPath();
         soapAction = ((Node)xPath.evaluate("/*[local-name()='Envelope']/*[local-name()='Body']/*[1]", new InputSource(new StringReader(soapMessage)), XPathConstants.NODE)).getLocalName();
         if (CommonUtils.isEmptyString(soapAction)) {
            throw new IllegalStateException("SOAPAction cannot be determined from provided soapMessage. Please check if Body element contains the correct child element.");
         }
      } catch (XPathExpressionException var4) {
         var4.printStackTrace();
      }

      return this.sendSoapMessage(soapAction, soapMessage);
   }
}
