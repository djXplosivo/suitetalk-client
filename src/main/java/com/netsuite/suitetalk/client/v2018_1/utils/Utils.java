package com.netsuite.suitetalk.client.v2018_1.utils;

import com.netsuite.suitetalk.proxy.v2018_1.platform.core.*;
import com.netsuite.suitetalk.proxy.v2018_1.platform.core.types.RecordType;
import com.netsuite.suitetalk.proxy.v2018_1.platform.messages.ReadResponseList;
import com.netsuite.suitetalk.proxy.v2018_1.platform.messages.WriteResponseList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class Utils {
   private Utils() {
      super();
   }

   public static Passport convertPassport(com.netsuite.suitetalk.client.common.authentication.Passport passport) {
      Passport endpointPassport = new Passport();
      endpointPassport.setEmail(passport.getEmail());
      endpointPassport.setPassword(passport.getPassword());
      endpointPassport.setAccount(passport.getAccount());
      endpointPassport.setRole(createRecordRef(passport.getRole()));
      return endpointPassport;
   }

   public static SsoPassport convertSsoPassport(com.netsuite.suitetalk.client.common.authentication.SsoPassport ssoPassport) {
      SsoPassport endpointSsoPassport = new SsoPassport();
      endpointSsoPassport.setAuthenticationToken(ssoPassport.getAuthenticationToken());
      endpointSsoPassport.setPartnerId(ssoPassport.getPartnerId());
      endpointSsoPassport.setPartnerAccount(ssoPassport.getPartnerAccount());
      endpointSsoPassport.setPartnerUserId(ssoPassport.getPartnerUserId());
      return endpointSsoPassport;
   }

   public static TokenPassport convertTokenPassport(com.netsuite.suitetalk.client.common.authentication.TokenPassport tokenPassport) {
      TokenPassport endpointTokenPassport = new TokenPassport();
      endpointTokenPassport.setAccount(tokenPassport.getAccount());
      endpointTokenPassport.setConsumerKey(tokenPassport.getConsumerKey());
      endpointTokenPassport.setToken(tokenPassport.getToken());
      endpointTokenPassport.setNonce(tokenPassport.getNonce());
      endpointTokenPassport.setTimestamp(tokenPassport.getTimestamp());
      TokenPassportSignature signature = new TokenPassportSignature();
      signature.setAlgorithm(tokenPassport.getSignatureAlgorithm().getNetSuiteFormat());
      signature.set_value(tokenPassport.getSignature());
      endpointTokenPassport.setSignature(signature);
      return endpointTokenPassport;
   }

   public static RecordRef createRecordRef(String internalId) {
      RecordRef recordRef = new RecordRef();
      recordRef.setInternalId(internalId);
      return recordRef;
   }

   public static RecordRef createRecordRef(String internalId, RecordType recordType) {
      RecordRef recordRef = createRecordRef(internalId);
      recordRef.setType(recordType);
      return recordRef;
   }

   public static RecordRef createRecordRefWithExternalId(String externalId, RecordType recordType) {
      RecordRef recordRef = new RecordRef();
      recordRef.setExternalId(externalId);
      recordRef.setType(recordType);
      return recordRef;
   }

   public static String getInternalId(BaseRef baseRef) {
      try {
         Method getInternalIdMethod = baseRef.getClass().getMethod("getInternalId");
         return (String)getInternalIdMethod.invoke(baseRef);
      } catch (NoSuchMethodException var2) {
         throw new UnsupportedOperationException("Cannot call getInternalId() on " + baseRef.getClass().getName());
      } catch (IllegalAccessException | InvocationTargetException var3) {
         throw new IllegalStateException(var3);
      }
   }

   public static List getInternalIds(WriteResponseList writeResponseList) {
      return (List)Arrays.stream(writeResponseList.getWriteResponse()).map((writeResponse) -> {
         return writeResponse != null && writeResponse.getStatus().isIsSuccess() ? getInternalId(writeResponse.getBaseRef()) : null;
      }).collect(Collectors.toList());
   }

   public static List getSuccess(WriteResponseList writeResponseList) {
      return (List)Arrays.stream(writeResponseList.getWriteResponse()).map((writeResponse) -> {
         return writeResponse != null && writeResponse.getStatus().isIsSuccess();
      }).collect(Collectors.toList());
   }

   public static List getRecords(ReadResponseList readResponseList) {
      return (List)Arrays.stream(readResponseList.getReadResponse()).map((readResponse) -> {
         return readResponse == null ? null : readResponse.getRecord();
      }).collect(Collectors.toList());
   }

   public static List getSearchResults(SearchResult searchResult) {
      if (searchResult != null && searchResult.getStatus() != null && searchResult.getStatus().isIsSuccess()) {
         if (searchResult.getTotalRecords() == 0) {
            return Collections.emptyList();
         } else {
            RecordList recordList = searchResult.getRecordList();
            SearchRowList searchRowList = searchResult.getSearchRowList();
            return recordList == null && searchRowList == null ? null : Arrays.asList((Object[])(searchRowList != null ? searchRowList.getSearchRow() : recordList.getRecord()));
         }
      } else {
         return null;
      }
   }
}
