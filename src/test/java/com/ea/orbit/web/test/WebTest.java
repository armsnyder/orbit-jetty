/*
 Copyright (C) 2016 Electronic Arts Inc.  All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Electronic Arts, Inc. ("EA") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ea.orbit.web.test;

import com.ea.orbit.container.Container;
import com.ea.orbit.util.IOUtils;
import com.ea.orbit.util.NetUtils;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import javax.ws.rs.Path;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

/**
 * Created by joe@bioware.com on 2016-02-16.
 */
public class WebTest
{
    private static Container container;
    private static String serverPath = null;

    @ClientEndpoint
    public static class AClientEndpoint
    {
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();
        private Session session;

        @OnOpen
        public void onOpen(Session session)
        {
            this.session = session;
        }

        @OnMessage
        public void onMessage(String message)
        {
            messages.add(message);
        }
    }

    @Path("/servlet1")
    public static class Servlet1 extends HttpServlet
    {
        @Override
        protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException
        {
            resp.getOutputStream().print("hello");
        }
    }


    @BeforeClass
    public static void setupServer() throws URISyntaxException, IOException, DeploymentException, InterruptedException
    {
        container = new Container();
        container.start();
        serverPath = "localhost:9090";
    }

    @AfterClass
    public static void stopServer()
    {
        container.stop();
    }

    private static String getHttpPath()
    {
        return "http://" + serverPath;
    }

    private static String getWsPath()
    {
        return "ws://" + serverPath;
    }

    @Test
    public void rawTest()
    {
        assertEquals(200, ClientBuilder.newClient().target(getHttpPath())
                .path("test/helloRaw").request(MediaType.APPLICATION_JSON)
                .get().getStatus());
    }

    @Test
    public void taskTest()
    {
        assertEquals(200, ClientBuilder.newClient().target(getHttpPath())
                .path("test/helloTask").request(MediaType.APPLICATION_JSON)
                .get().getStatus());
    }

    @Test
    public void notFoundTest()
    {
        assertEquals(404, ClientBuilder.newClient().target(getHttpPath())
                .path("test/noResource").request(MediaType.APPLICATION_JSON)
                .get().getStatus());
    }

    @Test
    public void serverErrorRaw()
    {
        assertEquals(500, ClientBuilder.newClient().target(getHttpPath())
                .path("test/serverErrorRaw").request(MediaType.APPLICATION_JSON)
                .get().getStatus());
    }

    @Test
    public void serverErrorTask()
    {
        assertEquals(500, ClientBuilder.newClient().target(getHttpPath())
                .path("test/serverErrorTask").request(MediaType.APPLICATION_JSON)
                .get().getStatus());
    }

    @Test
    public void serverErrorNestedTask()
    {
        assertEquals(500, ClientBuilder.newClient().target(getHttpPath())
                .path("test/serverErrorNestedTask").request(MediaType.APPLICATION_JSON)
                .get().getStatus());
    }

    @Test
    public void forbiddenRaw()
    {
        assertEquals(403, ClientBuilder.newClient().target(getHttpPath())
                .path("test/forbiddenRaw").request(MediaType.APPLICATION_JSON)
                .get().getStatus());
    }

    @Test
    public void forbiddenTask()
    {
        assertEquals(403, ClientBuilder.newClient().target(getHttpPath())
                .path("test/forbiddenTask").request(MediaType.APPLICATION_JSON)
                .get().getStatus());
    }

    @Test
    public void listTask()
    {
        assertEquals(200, ClientBuilder.newClient().target(getHttpPath())
                .path("test/listTask").request(MediaType.APPLICATION_JSON)
                .get().getStatus());
    }

    @Test
    public void webSocketTest() throws URISyntaxException, IOException, DeploymentException, InterruptedException
    {
        final AClientEndpoint client = new AClientEndpoint();
        WebSocketContainer socketContainer = ContainerProvider.getWebSocketContainer();
        socketContainer.connectToServer(client, new URI(getWsPath() + "/echo"));

        // sends some messages to the server
        client.session.getBasicRemote().sendText("test1");
        client.session.getAsyncRemote().sendText("test2");

        // waits for the responses
        assertEquals("test1", client.messages.poll(10, TimeUnit.SECONDS));
        assertEquals("test2", client.messages.poll(10, TimeUnit.SECONDS));
        client.session.close();
    }

    @Test
    public void servletTest() throws IOException
    {
        // IDK anyone would use servlets now days, but there you go.

        Response servlet1 = ClientBuilder.newClient().target(getHttpPath())
                .path("servlet1").request(MediaType.TEXT_PLAIN)
                .get();
        assertEquals(200, servlet1.getStatus());
        assertEquals("hello", IOUtils.toString((InputStream) servlet1.getEntity()));
    }
}