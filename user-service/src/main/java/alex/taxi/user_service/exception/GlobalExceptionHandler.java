package alex.taxi.user_service.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import alex.taxi.user_service.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .status(404)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .status(400)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(message)
                        .status(400)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String message = "Invalid request body";
        if (ex.getCause() instanceof InvalidFormatException ife) {
            message = "Invalid value for field '" + ife.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .filter(f -> f != null)
                    .collect(Collectors.joining(".")) + "'";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .message(message)
                        .status(400)
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .message("Internal server error")
                        .status(500)
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
