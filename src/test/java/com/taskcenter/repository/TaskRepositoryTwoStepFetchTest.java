package com.taskcenter.repository;

import com.taskcenter.dto.TaskFilterRequest;
import com.taskcenter.model.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
public class TaskRepositoryTwoStepFetchTest {

    @Autowired
    private TaskRepository taskRepository;

    @Test
    public void testTwoStepFetchPreservesOrderAndFilters() {
        // Prepare data
        Task task1 = Task.builder().publicId("T-1").workspaceId("ws-1").columnId("col-1").title("A").lexoRank("1").dueDate(LocalDate.of(2026, 1, 1)).build();
        Task task2 = Task.builder().publicId("T-2").workspaceId("ws-1").columnId("col-1").title("B").lexoRank("2").dueDate(LocalDate.of(2026, 1, 2)).build();
        Task task3 = Task.builder().publicId("T-3").workspaceId("ws-1").columnId("col-1").title("C").lexoRank("3").dueDate(LocalDate.of(2026, 1, 3)).build();
        Task task4 = Task.builder().publicId("T-4").workspaceId("ws-2").columnId("col-1").title("D").lexoRank("4").dueDate(LocalDate.of(2026, 1, 1)).build(); // different workspace
        
        taskRepository.save(task1);
        taskRepository.save(task2);
        taskRepository.save(task3);
        taskRepository.save(task4);

        // Step 1: Find IDs ordered by title descending
        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setColumnId("col-1");
        
        PageRequest pageReq = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "title"));
        
        Page<String> idPage = taskRepository.findIdsByWorkspaceIdFiltered("ws-1", filter, pageReq);
        
        assertThat(idPage.getTotalElements()).isEqualTo(3);
        List<String> ids = idPage.getContent();
        // Since it is ordered by title DESC, it should be C, B, A (task3, task2, task1)
        assertThat(ids).containsExactly(task3.getId(), task2.getId(), task1.getId());

        // Step 2: Fetch by IN
        List<Task> fetchedTasks = taskRepository.findByIdIn(ids);
        
        // Ensure that fetched tasks count is same
        assertThat(fetchedTasks).hasSize(3);
        
        // Demonstrate how service preserves order
        Map<String, Task> taskMap = fetchedTasks.stream().collect(Collectors.toMap(Task::getId, t -> t));
        List<Task> orderedTasks = ids.stream().map(taskMap::get).collect(Collectors.toList());
        
        assertThat(orderedTasks.get(0).getTitle()).isEqualTo("C");
        assertThat(orderedTasks.get(1).getTitle()).isEqualTo("B");
        assertThat(orderedTasks.get(2).getTitle()).isEqualTo("A");
    }
}
