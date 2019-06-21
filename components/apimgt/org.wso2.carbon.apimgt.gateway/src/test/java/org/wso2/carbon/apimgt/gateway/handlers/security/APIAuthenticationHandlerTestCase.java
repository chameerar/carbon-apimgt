package org.wso2.carbon.apimgt.gateway.handlers.security;

/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import org.apache.axis2.client.Options;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.synapse.MessageContext;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.rest.RESTConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.wso2.carbon.apimgt.gateway.APIMgtGatewayConstants;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.impl.APIManagerConfigurationService;
import org.wso2.carbon.apimgt.impl.utils.APIUtil;
import org.wso2.carbon.caching.impl.Util;
import org.wso2.carbon.metrics.manager.MetricManager;
import org.wso2.carbon.metrics.manager.Timer;

import java.util.TreeMap;

/*
* Test class for APIAuthenticationhandler
* */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Util.class, MetricManager.class, Timer.Context.class, APIUtil.class})
public class APIAuthenticationHandlerTestCase {
    private Timer.Context context;
    private SynapseEnvironment synapseEnvironment;
    private MessageContext messageContext;
    private org.apache.axis2.context.MessageContext axis2MsgCntxt;



    @Before
    public void setup(){
        synapseEnvironment = Mockito.mock(SynapseEnvironment.class);
        messageContext = Mockito.mock(Axis2MessageContext.class);
        axis2MsgCntxt = Mockito.mock(org.apache.axis2.context.MessageContext.class);
        Mockito.when(axis2MsgCntxt.getProperty(APIMgtGatewayConstants.REQUEST_RECEIVED_TIME)).thenReturn("1506576365");
        Mockito.when(((Axis2MessageContext) messageContext).getAxis2MessageContext()).thenReturn(axis2MsgCntxt);

        PowerMockito.mockStatic(Timer.Context.class);
        context = Mockito.mock(Timer.Context.class);
    }

    /*
    * This method will test handleRequest method for it's happy path
    * */
    @Test
    public void testHandleRequest() {

        APIAuthenticationHandler apiAuthenticationHandler = createAPIAuthenticationHandler();
        apiAuthenticationHandler.init(synapseEnvironment);
        Assert.assertEquals(apiAuthenticationHandler.startMetricTimer(), null);

        Options options = Mockito.mock(Options.class);
        Mockito.when(options.getMessageId()).thenReturn("1");
        Mockito.when(axis2MsgCntxt.getOptions()).thenReturn(options);

        TreeMap transportHeaders = new TreeMap();
        transportHeaders.put(APIConstants.USER_AGENT, "");
        transportHeaders.put(APIMgtGatewayConstants.AUTHORIZATION, "gsu64r874tcin7ry8oe");
        messageContext.setProperty(RESTConstants.REST_FULL_REQUEST_PATH, "");
        messageContext.setProperty(APIMgtGatewayConstants.APPLICATION_NAME, "abc");
        messageContext.setProperty(APIMgtGatewayConstants.END_USER_NAME, "admin");
        Mockito.when(axis2MsgCntxt.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS)).thenReturn(transportHeaders);
        Assert.assertTrue(apiAuthenticationHandler.handleRequest(messageContext));
    }

    /*
    *  This method will test Response method
    * */
    @Test
    public void testHandleResponse() {
        MessageContext messageContext = Mockito.mock(MessageContext.class);
        APIAuthenticationHandler apiAuthenticationHandler = createAPIAuthenticationHandler();
        Assert.assertTrue(apiAuthenticationHandler.handleResponse(messageContext));
    }

    /*
    * This method will test handleRequest method when APISecurityException is thrown
    * */
    @Test
    public void testHandleRequestSecurityException() {
        SynapseEnvironment synapseEnvironment = Mockito.mock(SynapseEnvironment.class);
        MessageContext messageContext = Mockito.mock(Axis2MessageContext.class);
        org.apache.axis2.context.MessageContext axis2MsgCntxt = Mockito.mock(org.apache.axis2.context.MessageContext.class);
        Mockito.when(((Axis2MessageContext) messageContext).getAxis2MessageContext()).thenReturn(axis2MsgCntxt);
        APIAuthenticationHandler apiAuthenticationHandler = createAPIAuthenticationHandlerForExceptionTest();
        apiAuthenticationHandler.init(synapseEnvironment);

        Options options = Mockito.mock(Options.class);
        Mockito.when(options.getMessageId()).thenReturn("1");
        Mockito.when(axis2MsgCntxt.getOptions()).thenReturn(options);

        TreeMap transportHeaders = new TreeMap();
        transportHeaders.put(APIConstants.USER_AGENT, "");
        transportHeaders.put(APIMgtGatewayConstants.AUTHORIZATION, "gsu64r874tcin7ry8oe");
        messageContext.setProperty(RESTConstants.REST_FULL_REQUEST_PATH, "");
        Mockito.when(axis2MsgCntxt.getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS)).thenReturn(transportHeaders);
        axis2MsgCntxt.setProperty(APIMgtGatewayConstants.REQUEST_RECEIVED_TIME, null);
        Assert.assertFalse(apiAuthenticationHandler.handleRequest(messageContext));

        Mockito.when(messageContext.isDoingGET()).thenReturn(true);

        Assert.assertFalse(apiAuthenticationHandler.handleRequest(messageContext));

        Assert.assertTrue(apiAuthenticationHandler.isAnalyticsEnabled());

    }

    /*
    *  This method will test for setAPIParametersToMessageContext
    * */
    @Test
    public void testSetAPIParametersToMessageContext() {
        MessageContext messageContext = Mockito.mock(Axis2MessageContext.class);
        org.apache.axis2.context.MessageContext axis2MsgCntxt = Mockito.mock(org.apache.axis2.context.MessageContext.class);
        Mockito.when(((Axis2MessageContext) messageContext).getAxis2MessageContext()).thenReturn(axis2MsgCntxt);

        APIAuthenticationHandler apiAuthenticationHandler = new APIAuthenticationHandler() {
            @Override
            protected AuthenticationContext getAuthenticationContext(MessageContext messageContext) {
                return new AuthenticationContext();
            }
        };
        AuthenticationContext authContext = apiAuthenticationHandler.getAuthenticationContext(messageContext);

        authContext.setConsumerKey("jhgfhkyuynoiluilj");
        authContext.setUsername("ishara");
        authContext.setApplicationName("abc");
        authContext.setApplicationId("123");

        Mockito.when(messageContext.getProperty(RESTConstants.REST_API_CONTEXT)).thenReturn("");
        Mockito.when(messageContext.getProperty(RESTConstants.SYNAPSE_REST_API)).thenReturn("admin-AT-wso2.com--PizzaShackAPI");
        Mockito.when(messageContext.getProperty(APIMgtGatewayConstants.API_PUBLISHER)).thenReturn(null);

        Mockito.when(messageContext.getProperty(RESTConstants.REST_FULL_REQUEST_PATH)).thenReturn("");
        apiAuthenticationHandler.setAPIParametersToMessageContext(messageContext);

        Assert.assertEquals("setAPIParametersToMessageContextTest", 0, (axis2MsgCntxt).getProperties().size());

    }

    /*
    * This method will test for destroy()
    * */
    @Test
    public void testDestroy() {
        APIAuthenticationHandler apiAuthenticationHandler = new APIAuthenticationHandler();
        apiAuthenticationHandler.destroy();
        SynapseEnvironment synapseEnvironment = Mockito.mock(SynapseEnvironment.class);
        SynapseConfiguration synapseConfiguration = Mockito.mock(SynapseConfiguration.class);
        AxisConfiguration axisConfiguration = Mockito.mock(AxisConfiguration.class);
        Mockito.when(synapseEnvironment.getSynapseConfiguration()).thenReturn(synapseConfiguration);
        Mockito.when(synapseConfiguration.getAxisConfiguration()).thenReturn(axisConfiguration);
        PowerMockito.mockStatic(Util.class);
        PowerMockito.when(Util.getTenantDomain()).thenReturn("carbon.super");
        apiAuthenticationHandler.init(synapseEnvironment);
        apiAuthenticationHandler.destroy();
    }

    /*
    * This method will create an instance of APIAuthenticationHandler
    * */
    private APIAuthenticationHandler createAPIAuthenticationHandler() {
        return new APIAuthenticationHandler() {
            
            @Override
            protected APIManagerConfigurationService getApiManagerConfigurationService() {
                return Mockito.mock(APIManagerConfigurationService.class);
            }

            @Override
            protected boolean isAnalyticsEnabled() {
                return true;
            }

            @Override
            protected void initializeAuthenticators() {}

            @Override
            protected boolean isAuthenticate(MessageContext messageContext) throws APISecurityException {
                return true;
            }

            @Override
            protected void setAPIParametersToMessageContext(MessageContext messageContext) {

            }

            @Override
            protected void stopMetricTimer(Timer.Context context) {

            }

            @Override
            protected Timer.Context startMetricTimer() {
                return null;
            }
        };
    }

    /*
    * This method will create an instance of APIAuthenticationHandler so as to give APISecurityException
    * */
    private APIAuthenticationHandler createAPIAuthenticationHandlerForExceptionTest() {
        return new APIAuthenticationHandler() {

            @Override
            protected APIManagerConfigurationService getApiManagerConfigurationService() {
                return Mockito.mock(APIManagerConfigurationService.class);
            }

            @Override
            protected boolean isAnalyticsEnabled() {
                return true;
            }

            @Override
            protected void initializeAuthenticators() {}

            @Override
            protected boolean isAuthenticate(MessageContext messageContext) throws APISecurityException {
                throw new APISecurityException(1000, "test");
            }

            @Override
            protected void setAPIParametersToMessageContext(MessageContext messageContext) {

            }

            @Override
            protected void stopMetricTimer(Timer.Context context) {

            }

            @Override
            protected Timer.Context startMetricTimer() {
                return null;
            }

            @Override
            protected void setSOAPFault(MessageContext messageContext, APISecurityException e) {

            }

            @Override
            protected void sendFault(MessageContext messageContext, int status) {

            }

            @Override
            protected void setFaultPayload(MessageContext messageContext, APISecurityException e) {
                getFaultPayload(e);
            }
        };
    }

    @Test
    public void testStartMetricTimer(){
      APIAuthenticationHandler apiAuthenticationHandler = new APIAuthenticationHandler();
        PowerMockito.mockStatic(MetricManager.class);
        Timer timer = Mockito.mock(Timer.class);
        Mockito.when(timer.start()).thenReturn(context);
        PowerMockito.when(MetricManager.name(APIConstants.METRICS_PREFIX, "APIAuthenticationHandler"))
                .thenReturn("org.wso2.amAPIAuthenticationHandler");
        PowerMockito.when(MetricManager.timer(org.wso2.carbon.metrics.manager.Level.INFO, "org.wso2.amAPIAuthenticationHandler"))
                .thenReturn(timer);
        Mockito.verify(apiAuthenticationHandler.startMetricTimer());
    }

    @Test
    public void testStopMetricTimer(){
      APIAuthenticationHandler apiAuthenticationHandler = new APIAuthenticationHandler();
        Mockito.when(context.stop()).thenReturn(1000L);
        apiAuthenticationHandler.stopMetricTimer(context);
        Assert.assertTrue(true);
    }

}

