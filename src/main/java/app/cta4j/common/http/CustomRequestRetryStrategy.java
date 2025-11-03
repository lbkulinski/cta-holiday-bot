package app.cta4j.common.http;

import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;

public class CustomRequestRetryStrategy extends DefaultHttpRequestRetryStrategy {
    @Override
    public boolean retryRequest(HttpRequest httpRequest, IOException e, int i, HttpContext httpContext) {
        return false;
    }

    @Override
    public boolean retryRequest(HttpResponse httpResponse, int i, HttpContext httpContext) {
        return false;
    }

    @Override
    public TimeValue getRetryInterval(HttpResponse httpResponse, int i, HttpContext httpContext) {
        return null;
    }
}
