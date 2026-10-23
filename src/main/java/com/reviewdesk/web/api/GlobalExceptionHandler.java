package com.reviewdesk.web.api;

import com.reviewdesk.domain.StaleLessonPlanException;
import com.reviewdesk.domain.StaleReviewException;
import com.reviewdesk.service.AssignmentNotFoundException;
import com.reviewdesk.service.ReviewNotFoundException;
import com.reviewdesk.service.ReviewVersionNotFoundException;
import com.reviewdesk.service.SubmissionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将审核业务异常转换为稳定的 REST 错误响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StaleReviewException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError staleReview(StaleReviewException exception) {
        return new ApiError("STALE_REVISION", exception.getMessage(),
                exception.getCurrentRevision());
    }

    @ExceptionHandler(StaleLessonPlanException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiError staleLessonPlan(StaleLessonPlanException exception) {
        return new ApiError("STALE_REVISION", exception.getMessage(),
                exception.getCurrentRevision());
    }

    @ExceptionHandler({
            AssignmentNotFoundException.class,
            SubmissionNotFoundException.class,
            ReviewNotFoundException.class,
            ReviewVersionNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(RuntimeException exception) {
        return ApiError.of("NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidRequest(IllegalArgumentException exception) {
        return ApiError.of("INVALID_REQUEST", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError invalidState(IllegalStateException exception) {
        return ApiError.of("INVALID_STATE", exception.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError forbidden(SecurityException exception) {
        return ApiError.of("FORBIDDEN", exception.getMessage());
    }
}

