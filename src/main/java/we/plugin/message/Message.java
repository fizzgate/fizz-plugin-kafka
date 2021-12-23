package we.plugin.message;

import org.springframework.http.HttpCookie;
import org.springframework.util.MultiValueMap;
import we.util.JacksonUtils;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * kafka message domain object
 */
public class Message {
    private Request request;
    private Response response;

    public Message() {
        this.request = new Request();
        this.response = new Response();
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public static class Request {
        private String Id;
        private Map<String, String> queryParams;
        private Map<String, HttpCookie> cookies;
        private InetSocketAddress inetSocketAddress;
        private InetSocketAddress remoteAddress;
        private Map<String, String> headers;
        private String path;
        private String body;
        private String method;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getId() {
            return Id;
        }

        public void setId(String id) {
            Id = id;
        }

        public Map<String, String> getQueryParams() {
            return queryParams;
        }

        public void setQueryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
        }

        public Map<String, HttpCookie> getCookies() {
            return cookies;
        }

        public void setCookies(Map<String, HttpCookie> cookies) {
            this.cookies = cookies;
        }

        public InetSocketAddress getInetSocketAddress() {
            return inetSocketAddress;
        }

        public void setInetSocketAddress(InetSocketAddress inetSocketAddress) {
            this.inetSocketAddress = inetSocketAddress;
        }

        public InetSocketAddress getRemoteAddress() {
            return remoteAddress;
        }

        public void setRemoteAddress(InetSocketAddress remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        public Map<String, String> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, String> headers) {
            this.headers = headers;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

    public static class Response {
        private String body;

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }
    }

  public String toJsonString(){
        return JacksonUtils.writeValueAsString(this);
  }

}
