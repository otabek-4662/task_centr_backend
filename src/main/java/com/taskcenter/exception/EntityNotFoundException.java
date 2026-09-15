package com.taskcenter.exception;

/**
 * Resurs (workspace, task, column va h.k.) topilmaganda otiladi.
 * GlobalExceptionHandler bu exceptionni 404 NOT FOUND ga aylantiradi.
 *
 * Misol:
 *   throw new EntityNotFoundException("Task", taskId);
 *   // => "Task topilmadi: abc-123"
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }

    public EntityNotFoundException(String entityName, String id) {
        super(entityName + " topilmadi: " + id);
    }
}
