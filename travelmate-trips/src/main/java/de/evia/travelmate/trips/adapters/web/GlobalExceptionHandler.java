package de.evia.travelmate.trips.adapters.web;

import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.ControllerAdvice;

import de.evia.travelmate.webcommons.AbstractGlobalExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {

    public GlobalExceptionHandler(final MessageSource messageSource) {
        super(messageSource);
    }
}
