package org.dromara.jpom.ssh;

import com.github.dockerjava.transport.DockerHttpClient;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.ManagedHttpClientConnectionFactory;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.impl.DefaultContentLengthStrategy;
import org.apache.hc.core5.http.impl.io.EmptyInputStream;
import org.apache.hc.core5.http.io.entity.InputStreamEntity;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.net.URIAuthority;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class JschDockerHttpClient implements DockerHttpClient {


    private final Session session;
    private final CloseableHttpClient httpClient;
    private final HttpHost host;

    public JschDockerHttpClient(URI dockerHostUri, Supplier<Session> sessionSupplier) {
        this.session = sessionSupplier.get();
        Registry<ConnectionSocketFactory> socketFactoryRegistry = createConnectionSocketFactoryRegistry(session);
        //
        if ("ssh".equals(dockerHostUri.getScheme())) {
            host = new HttpHost(dockerHostUri.getScheme(), dockerHostUri.getHost(), 2375);
        } else {
            host = HttpHost.create(dockerHostUri);
        }
        httpClient = HttpClients.custom()
            .setRequestExecutor(new HijackingHttpRequestExecutor(null))
            .setConnectionManager(new PoolingHttpClientConnectionManager(
                socketFactoryRegistry,
                new ManagedHttpClientConnectionFactory(
                    null,
                    null,
                    null,
                    null,
                    message -> {
                        Header transferEncodingHeader = message.getFirstHeader(HttpHeaders.TRANSFER_ENCODING);
                        if (transferEncodingHeader != null) {
                            if ("identity".equalsIgnoreCase(transferEncodingHeader.getValue())) {
                                return ContentLengthStrategy.UNDEFINED;
                            }
                        }
                        return DefaultContentLengthStrategy.INSTANCE.determineLength(message);
                    },
                    null
                )
            ))
            .build();
    }


    @Override
    public Response execute(Request request) {

        HttpContext context = new BasicHttpContext();
        HttpUriRequestBase httpUriRequest = new HttpUriRequestBase(request.method(), URI.create(request.path()));
        httpUriRequest.setScheme(host.getSchemeName());
        httpUriRequest.setAuthority(new URIAuthority(host.getHostName(), host.getPort()));

        request.headers().forEach(httpUriRequest::addHeader);

        InputStream body = request.body();
        if (body != null) {
            httpUriRequest.setEntity(new InputStreamEntity(body, null));
        }

        if (request.hijackedInput() != null) {
            context.setAttribute(HijackingHttpRequestExecutor.HIJACKED_INPUT_ATTRIBUTE, request.hijackedInput());
            httpUriRequest.setHeader("Upgrade", "tcp");
            httpUriRequest.setHeader("Connection", "Upgrade");
        }

        try {
            CloseableHttpResponse response = httpClient.execute(host, httpUriRequest, context);

            return new ApacheResponse(httpUriRequest, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            session.disconnect();
        } catch (Exception e) {
            log.debug("session error", e);
        }
        httpClient.close();
    }

    @Slf4j
    static class ApacheResponse implements Response {


        private final HttpUriRequestBase request;

        private final CloseableHttpResponse response;

        ApacheResponse(HttpUriRequestBase httpUriRequest, CloseableHttpResponse response) {
            this.request = httpUriRequest;
            this.response = response;
        }

        @Override
        public int getStatusCode() {
            return response.getCode();
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return Stream.of(response.getHeaders()).collect(Collectors.groupingBy(
                NameValuePair::getName,
                Collectors.mapping(NameValuePair::getValue, Collectors.toList())
            ));
        }

        @Override
        public String getHeader(String name) {
            Header firstHeader = response.getFirstHeader(name);
            return firstHeader != null ? firstHeader.getValue() : null;
        }

        @Override
        public InputStream getBody() {
            try {
                return response.getEntity() != null
                    ? response.getEntity().getContent()
                    : EmptyInputStream.INSTANCE;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            try {
                request.abort();
            } catch (Exception e) {
                log.debug("Failed to abort the request", e);
            }

            try {
                response.close();
            } catch (ConnectionClosedException e) {
                log.trace("Failed to close the response", e);
            } catch (Exception e) {
                log.debug("Failed to close the response", e);
            }
        }
    }

    private Registry<ConnectionSocketFactory> createConnectionSocketFactoryRegistry(Session session) {
        RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder = RegistryBuilder.create();
        return socketFactoryRegistryBuilder
            .register("ssh", new PlainConnectionSocketFactory() {
                @Override
                public Socket createSocket(HttpContext context) throws IOException {
                    return new JschSocket(session);
                }
            })
            .build();
    }
}
