package uk.dsxt.voting.common.utils.web;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class RequestsRouter extends ProxyHandler {

    private final Map<Pattern, Handler> pathToHandler;

    public RequestsRouter(Handler defaultHandler, Map<Pattern, Handler> pathToHandler) {
        super(defaultHandler);
        this.pathToHandler = pathToHandler;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        for (Map.Entry<Pattern, Handler> patternToHandler : pathToHandler.entrySet()) {
            if (patternToHandler.getKey().matcher(target).matches()) {
                patternToHandler.getValue().handle(target, baseRequest, request, response);
                return;
            }
        }
        super.handle(target, baseRequest, request, response);
    }
}
