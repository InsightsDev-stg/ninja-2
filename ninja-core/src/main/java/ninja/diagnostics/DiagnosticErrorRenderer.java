package ninja.diagnostics;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.List;
import ninja.Context;
import ninja.Result;
import ninja.exceptions.InternalServerErrorException;
import ninja.utils.ResponseStreams;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for rendering <code>DiagnosticError</code> instances as
 * a Result.
 * 
 * @author Joe Lauer (https://twitter.com/jjlauer)
 */
public class DiagnosticErrorRenderer {
    private static final Logger logger = LoggerFactory.getLogger(DiagnosticErrorRenderer.class);
    
    private final StringBuilder s;
    
    private DiagnosticErrorRenderer() {
        s = new StringBuilder();
    }
    
    public String render() {
        return s.toString();
    }
    
    static public void tryToRenderDiagnosticError(Context context, Result result, DiagnosticError diagnosticError, boolean throwInternalServerExceptionOnError) {
        DiagnosticErrorRenderer errorRenderer = build(context, result, diagnosticError);
        errorRenderer.tryToRenderResult(
                context,
                result,
                throwInternalServerExceptionOnError);
    }
    
    public void tryToRenderResult(Context context, Result result, boolean throwInternalServerExceptionOnError) {
        try {
            String out = render();

            ResponseStreams responseStreams = context.finalizeHeaders(result);
            try (Writer w = responseStreams.getWriter()) {
                w.write(out);
                w.flush();
                w.close();
            }
        } catch (IOException e) {
            // fallback to ninja system-wide error handler?
            if (throwInternalServerExceptionOnError) {
                throw new InternalServerErrorException(e);
            } else {
                logger.error("Unable to render diagnostic error", e);
            }
        }
    }
    
    static public DiagnosticErrorRenderer build(Context context,
                                                Result result,
                                                DiagnosticError diagnosticError) {
        return new DiagnosticErrorRenderer()
            .appendHeader(
                    context,
                    result,
                    diagnosticError.getTitle())
            .appendSourceSnippet(
                    diagnosticError.getSourceLocation(),
                    diagnosticError.getSourceLines(),
                    diagnosticError.getLineNumberOfSourceLines(),
                    diagnosticError.getLineNumberOfError())
            .appendThrowable(
                    diagnosticError.getThrowable()) 
            .appendFooter();
    }
    
    private DiagnosticErrorRenderer appendHeader(Context context,
                                        Result result,
                                        String title) {
           s.append("<!DOCTYPE html>\n")
            .append("<!-- Ninja Diagnostic Error -->")
            .append("<html>\n")
            .append("  <head>\n")
            .append("    <title>").append(title).append("</title>\n")
            .append("    ").append(defaultStyle())
            .append("  </head>\n")
            .append("  <body>\n")
            .append("    <h1>")
            .append("      <img id=\"logo\" src=\"data:image/png;base64,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\"/>")
            .append("      ")
            .append(title)
            .append("      <img id=\"mode\" style=\"float:right; height:40px;\" title=\"Dev Mode!\" src=\"data:image/png;base64,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\"/>")
            .append("    </h1>\n")
            .append("    <p id=\"detail\">\n");
                   
            if (result.getStatusCode() != 200) {
                s.append ("Status code ").append(result.getStatusCode());
            }
                   
            s.append(" for request '").append(context.getMethod()).append(" ").append( context.getRequestPath()).append("'\n")
            .append("    </p>\n");
        return this;
    }
    
    private DiagnosticErrorRenderer appendFooter() {
           s.append("  </body>\n")
            .append("</html>\n");
        return this;
    }
    
    private DiagnosticErrorRenderer appendSourceSnippet(URI sourceLocation,
                                                        List<String> sourceLines,
                                                        int lineNumberOfSourceLines,
                                                        int lineNumberOfError) {
        if (sourceLocation != null) {
            s.append("    <h2>").append(sourceLocation).append("</h2>\n");
        }

        if (sourceLines != null) {
            s.append("    <div>\n");
            for (int i = 0; i < sourceLines.size(); i++) {
                s.append("<pre>");
                
                int lineNumber = lineNumberOfSourceLines + i;
                
                // line of error?
                String cssClass = (lineNumber == lineNumberOfError ? "line error" : "line info");

                s.append("<span class=\"").append(cssClass).append("\">").append(lineNumber).append("</span>");
                s.append("<span class=\"")
                        .append("route")
                        .append("\">")
                        .append(StringEscapeUtils.escapeHtml4(sourceLines.get(i)))
                        .append("</span>");
                s.append("</pre>");
            }
            s.append("    </div>\n");
        }

        return this;
    }
    
    private DiagnosticErrorRenderer appendThrowable(Throwable throwable) {
        if (throwable != null) {
            s.append("    <div>\n")
            .append("      <pre><span class=\"stacktrace\">\n")
            .append(throwableStackTraceToString(throwable))
            .append("      </span></pre>\n")
            .append("    </div>");
        }
        return this;
    }
    
    private String throwableStackTraceToString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
        }
        return sw.toString();
    }
    
    private String defaultStyle() {
        return
"        <style>\n" +
"            html, body {\n" +
"                margin: 0;\n" +
"                padding: 0;\n" +
"                font-family: Helvetica, 'Nimbus Sans L', 'Liberation Sans', Arial, sans-serif;\n" +
"                background: #ECECEC;\n" +
"            }\n" +
"            h1 {\n" +
"                margin: 0;\n" +
"                background: #68006c;\n" +
"                color: #fff;\n" +
"                font-size: 28px;\n" +
"                padding: 12px 12px;\n" +
"            }\n" +
"            h1 img#logo {\n" +
"                height: 49px;" +
"                vertical-align: text-top;\n" +
"            }\n" +
"            p#detail {\n" +
"                margin: 0;\n" +
"                padding: 15px 45px;\n" +
"                background: #F7C2F9;\n" +
"                color: #333;\n" +
"                font-size: 14px;\n" +
"            }\n" +
"            h2 {\n" +
"                margin: 0;\n" +
"                padding: 5px 45px;\n" +
"                font-size: 12px;\n" +
"                background: #333;\n" +
"                color: #fff;\n" +
"            }\n" +
"            pre {\n" +
"                margin: 0;\n" +
"                padding: 0;\n" +
"                font-family: Monaco, 'Lucida Console', monospace;\n" +
"                background: #ECECEC;\n" +
"                margin: 0;\n" +
"                border-bottom: 1px solid #DDD;\n" +
"                position: relative;\n" +
"                font-size: 12px;\n" +
"            }\n" +
"            pre span.line {\n" +
"                text-align: right;\n" +
"                display: inline-block;\n" +
"                padding: 5px 5px;\n" +
"                width: 30px;\n" +
"                background: #D6D6D6;\n" +
"                color: #8B8B8B;\n" +
"                font-weight: bold;\n" +
"            }\n" +
"            pre span.line.error {\n" +
"                background: #C21600;\n" +
"                color: #fff;\n" +
"            }\n" +
"            pre span.stacktrace {\n" +
"                padding: 10px 10px;\n" +
"            }\n" +
"            pre span.route {\n" +
"                padding: 5px 5px;\n" +
"                position: absolute;\n" +
"                right: 0;\n" +
"                left: 40px;\n" +
"            }\n" +
"            pre span.route span.verb {\n" +
"                display: inline-block;\n" +
"                width: 5%;\n" +
"                min-width: 50px;\n" +
"                overflow: hidden;\n" +
"                margin-right: 10px;\n" +
"            }\n" +
"            pre span.route span.path {\n" +
"                display: inline-block;\n" +
"                width: 30%;\n" +
"                min-width: 200px;\n" +
"                overflow: hidden;\n" +
"                margin-right: 10px;\n" +
"            }\n" +
"            pre span.route span.call {\n" +
"                display: inline-block;\n" +
"                width: 50%;\n" +
"                overflow: hidden;\n" +
"                margin-right: 10px;\n" +
"            }\n" +
"            pre:first-child span.route {\n" +
"                border-top: 4px solid #CDCDCD;\n" +
"            }\n" +
"            pre:first-child span.line {\n" +
"                border-top: 4px solid #B6B6B6;\n" +
"            }\n" +
"            pre.error span.line {\n" +
"                background: #A31012;\n" +
"                color: #fff;\n" +
"                text-shadow: 1px 1px 1px rgba(0,0,0,.3);\n" +
"            }\n" +
"        </style>";
    }
    
}
