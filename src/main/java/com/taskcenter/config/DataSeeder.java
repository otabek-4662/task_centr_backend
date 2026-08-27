package com.taskcenter.config;

import com.taskcenter.model.*;
import com.taskcenter.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               WorkspaceRepository workspaceRepository,
                               ColumnRepository columnRepository,
                               LabelRepository labelRepository,
                               TaskRepository taskRepository,
                               WorkspaceMemberRepository memberRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            String wsId = "6a45163133ff7819b28ef909";
            if (workspaceRepository.existsById(wsId)) {
                System.out.println("Seed data already exists, skip");
                return;
            }

            User elshod = User.builder()
                    .name("elshod")
                    .fullName("Elshod T")
                    .password(passwordEncoder.encode("password123"))
                    .role(User.Role.USER)
                    .build();
            User mirxon = User.builder()
                    .name("mirxon")
                    .fullName("Mirxonjon Ismanov")
                    .password(passwordEncoder.encode("password123"))
                    .role(User.Role.USER)
                    .build();
            User xusan = User.builder()
                    .name("xusan")
                    .fullName("Xusan Yusupov")
                    .password(passwordEncoder.encode("password123"))
                    .role(User.Role.USER)
                    .build();
            userRepository.save(elshod);
            userRepository.save(mirxon);
            userRepository.save(xusan);

            Workspace ws = Workspace.builder()
                    .id(wsId)
                    .title("Test Workspace")
                    .bgColor("#1a1b41")
                    .description("Test workspace description")
                    .ownerId(xusan.getId())
                    .build();
            workspaceRepository.save(ws);

            memberRepository.save(WorkspaceMember.builder().workspaceId(wsId).userId(elshod.getId()).role("MEMBER").build());
            memberRepository.save(WorkspaceMember.builder().workspaceId(wsId).userId(mirxon.getId()).role("MEMBER").build());
            memberRepository.save(WorkspaceMember.builder().workspaceId(wsId).userId(xusan.getId()).role("OWNER").build());

            BoardColumn todo = BoardColumn.builder().id("6a45163133ff7819b28ef90d").workspaceId(wsId).title("To Do").order(1).isDefault(true).build();
            BoardColumn inProgress = BoardColumn.builder().id("6a45163133ff7819b28ef90e").workspaceId(wsId).title("In Progress").order(2).isDefault(true).build();
            BoardColumn done = BoardColumn.builder().id("6a45163133ff7819b28ef90f").workspaceId(wsId).title("Done").order(3).isDefault(true).build();
            BoardColumn blocked = BoardColumn.builder().id("6a4b3f71d253c0ad3587bb9e").workspaceId(wsId).title("BLOCKED \uD83D\uDD12").order(4).isDefault(false).build();
            columnRepository.save(todo);
            columnRepository.save(inProgress);
            columnRepository.save(done);
            columnRepository.save(blocked);

            Label frontend = Label.builder().id("6a45f89833ff7819b28efb4a").workspaceId(wsId).name("frontend").color("BLUE").build();
            Label backend = Label.builder().id("6a45f8ea33ff7819b28efc42").workspaceId(wsId).name("backend").color("BLACK").build();
            Label design = Label.builder().id("6a45f8f733ff7819b28efc63").workspaceId(wsId).name("design").color("PINK").build();
            Label management = Label.builder().id("6a460871c1fec29daeb02c03").workspaceId(wsId).name("management").color("YELLOW").build();
            labelRepository.save(frontend);
            labelRepository.save(backend);
            labelRepository.save(design);
            labelRepository.save(management);

            Task wfm293 = Task.builder()
                    .id("6a4df77014e89613375ad6ff")
                    .publicId("WFM-293")
                    .workspaceId(wsId)
                    .columnId(todo.getId())
                    .title("Actual building")
                    .description("")
                    .order(2)
                    .build();
            Task wfm375 = Task.builder()
                    .id("6a672de214e89613376ee0ca")
                    .publicId("WFM-375")
                    .workspaceId(wsId)
                    .columnId(inProgress.getId())
                    .title("Sxema diagrammasini chizish")
                    .description("")
                    .order(1)
                    .build();
            Task wfm376 = Task.builder()
                    .id("6a673aa814e89613376ef8d7")
                    .publicId("WFM-376")
                    .workspaceId(wsId)
                    .columnId(todo.getId())
                    .title("trigger api")
                    .description("")
                    .order(3)
                    .build();
            wfm376.getLabels().add(backend);
            wfm376.getAssignees().add(elshod);

            Task wfm417 = Task.builder()
                    .id("6a869a2f14e89613377b032c")
                    .publicId("WFM-417")
                    .workspaceId(wsId)
                    .columnId(inProgress.getId())
                    .title("Manual assign refactor")
                    .description("1. Manual assignda end date kiritilmasa, oxirgacha o'sha branch set bo'lib qoladi. Start -required  , end - optional. \u2705\n2. Manual assign arxiv zalivka qilish - arxiv beraman - excelni bir marta zalivka qilish kerak. Cron kerakmas; \n3. Agar xodim uvolen bo'lsa (absentda), shtatda chiqmasligi kerak\n4. Manual assignni sort tashkillashtirish. fullname, start date, end date; \u2705\n5. Дата => период; front\u2705")
                    .order(3)
                    .build();
            wfm417.getLabels().add(backend);
            wfm417.getLabels().add(frontend);
            wfm417.getAssignees().add(mirxon);
            wfm417.getAssignees().add(xusan);

            taskRepository.save(wfm293);
            taskRepository.save(wfm375);
            taskRepository.save(wfm376);
            taskRepository.save(wfm417);

            System.out.println("Seed data inserted: workspace 6a451..., 4 columns, 4 labels, 4 tasks");
        };
    }
}
