package com.calvin.figure;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class CalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CalExceptionHandler.class);

    @ExceptionHandler({ MissingServletRequestPartException.class,
            HttpMessageConversionException.class })
    public ResponseEntity<Map<String, Object>> badRequestException(Exception e) {
        logger.debug(e.getMessage(), e);
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<Map<String, Object>> httpStatusCodeException(HttpStatusCodeException e) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getStatusText());
        return ResponseEntity.status(e.getStatusCode()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> accessDeniedException(AccessDeniedException e) {
        logger.debug(e.getMessage(), e);
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> authenticationException(AuthenticationException e) {
        logger.debug(e.getMessage(), e);
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> exception(Exception e) {
        logger.error(e.getMessage(), e);
        Map<String, Object> body = new HashMap<>();
        body.put("error", e.getMessage());
        return ResponseEntity.internalServerError().body(body);
    }

}
