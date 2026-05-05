package de.evia.travelmate.expense.adapters.web;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import de.evia.travelmate.webcommons.AbstractGlobalExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(final MessageSource messageSource) {
        super(messageSource);
        this.messageSource = messageSource;
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSize(final MaxUploadSizeExceededException ex,
                                      final HttpServletRequest request,
                                      final HttpServletResponse response,
                                      final Locale locale,
                                      final Model model) {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        final String message = messageSource.getMessage("error.fileTooLarge", null, locale);
        if (isHtmxRequest(request)) {
            triggerErrorToast(response, message);
            return "fragments/empty :: empty";
        }
        model.addAttribute("message", message);
        return "error/error";
    }
}
