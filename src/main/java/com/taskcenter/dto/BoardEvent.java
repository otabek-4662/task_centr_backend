package com.taskcenter.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BoardEvent {

    public enum Type {
        TASK_CREATED,
        TASK_UPDATED,
        TASK_DELETED,
        COLUMN_CREATED,
        COLUMN_UPDATED,
        COLUMN_DELETED,
        LABEL_CREATED,
        LABEL_DELETED
    }

    private Type type;
    private Object data;

}