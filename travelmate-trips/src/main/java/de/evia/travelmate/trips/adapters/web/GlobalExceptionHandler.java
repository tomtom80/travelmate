package de.evia.travelmate.trips.adapters.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.server.ResponseStatusException;

import de.evia.travelmate.common.domain.BusinessRuleViolationException;
import de.evia.travelmate.common.domain.DuplicateEntityException;
import de.evia.travelmate.common.domain.EntityNotFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_ERROR_MESSAGE = "Ein unerwarteter Fehler ist aufgetreten.";
    private final MessageSource messageSource;

    public GlobalExceptionHandler(final MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public String handleEntityNotFound(final EntityNotFoundException ex,
                                       final HttpServletRequest request,
                                       final HttpServletResponse response,
                                       final Model model) {
        LOG.info("Entity not found: {}", ex.getMessage());
        response.setStatus(HttpStatus.NOT_FOUND.value());
        if (isHtmxRequest(request)) {
            triggerErrorToast(response, ex.getMessage());
            return "fragments/empty :: empty";
        }
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(DuplicateEntityException.class)
    public String handleDuplicateEntity(final DuplicateEntityException ex,
                                        final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final Model model) {
        final String message = resolveMessage(ex.getMessage());
        LOG.info("Duplicate entity: {}", message);
        response.setStatus(HttpStatus.CONFLICT.value());
        if (isHtmxRequest(request)) {
            triggerErrorToast(response, message);
            return "fragments/empty :: empty";
        }
        model.addAttribute("message", message);
        return "error/error";
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public String handleBusinessRuleViolation(final BusinessRuleViolationException ex,
                                              final HttpServletRequest request,
                                              final HttpServletResponse response,
                                              final Model model) {
        final String message = resolveMessage(ex.getMessage());
        LOG.warn("Business rule violation: {}", message);
        response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
        if (isHtmxRequest(request)) {
            triggerErrorToast(response, message);
            return "fragments/empty :: empty";
        }
        model.addAttribute("message", message);
        return "error/error";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(final ResponseStatusException ex,
                                                 final HttpServletRequest request,
                                                 final HttpServletResponse response,
                                                 final Model model) {
        LOG.info("Response status exception: {} {}", ex.getStatusCode(), ex.getReason());
        response.setStatus(ex.getStatusCode().value());
        if (isHtmxRequest(request)) {
            triggerErrorToast(response, ex.getReason() != null ? ex.getReason() : GENERIC_ERROR_MESSAGE);
            return "fragments/empty :: empty";
        }
        model.addAttribute("message", ex.getReason() != null ? ex.getReason() : GENERIC_ERROR_MESSAGE);
        return "error/error";
    }

    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(final RuntimeException ex,
                                         final HttpServletRequest request,
                                         final HttpServletResponse response,
                                         final Model model) {
        LOG.error("Unexpected error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        if (isHtmxRequest(request)) {
            triggerErrorToast(response, GENERIC_ERROR_MESSAGE);
            return "fragments/empty :: empty";
        }
        model.addAttribute("message", GENERIC_ERROR_MESSAGE);
        return "error/error";
    }

    private void triggerErrorToast(final HttpServletResponse response, final String message) {
        response.setHeader("HX-Reswap", "none");
        response.setHeader("HX-Trigger",
            "{\"showToast\":{\"level\":\"error\",\"message\":\"" + escapeJson(message) + "\"}}");
    }

    private String resolveMessage(final String messageKey) {
        if (messageKey == null || messageSource == null) {
            return GENERIC_ERROR_MESSAGE;
        }
        try {
            return messageSource.getMessage(messageKey, null, LocaleContextHolder.getLocale());
        } catch (final NoSuchMessageException e) {
            return messageKey;
        }
    }

    private boolean isHtmxRequest(final HttpServletRequest request) {
        return "true".equals(request.getHeader("HX-Request"));
    }

    private String escapeJson(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
