package uk.dsxt.voting.common.utils.web;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Pattern;

public class RequestsRouter extends ProxyHandler {

    private final Map<Pattern, Handler> pathToHandler;
    private final String defaultRootPath;

    public RequestsRouter(Handler defaultHandler, Map<Pattern, Handler> pathToHandler, String defaultRootPath) {
        super(defaultHandler);
        this.pathToHandler = pathToHandler;
        this.defaultRootPath = defaultRootPath;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        for (Map.Entry<Pattern, Handler> patternToHandler : pathToHandler.entrySet()) {
            if (patternToHandler.getKey().matcher(target).matches()) {
                patternToHandler.getValue().handle(target, baseRequest, request, response);
                return;
            }
        }
        File file = new File(Paths.get(defaultRootPath, target).toString());
        if (file.exists())
            super.handle(target, baseRequest, request, response);
        else
            super.handle("/", baseRequest, request, response);
    }
}
