package com.taskcenter.service;

import com.taskcenter.model.Task;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class BoardTasks {
    private BoardTasks() {}

    static Map<String, List<Task>> groupByColumn(List<Task> tasks) {
        Map<String, List<Task>> byColumn = tasks.stream().collect(Collectors.groupingBy(Task::getColumnId));
        Comparator<Task> byRank = Comparator.comparing(Task::getLexoRank, Comparator.nullsLast(Comparator.naturalOrder()));
        byColumn.values().forEach(column -> column.sort(byRank));
        return byColumn;
    }
}
