package com.taskcenter.config;

import com.taskcenter.model.*;
import com.taskcenter.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(UserRepository userRepository,
                               WorkspaceRepository workspaceRepository,
                               ColumnRepository columnRepository,
                               LabelRepository labelRepository,
                               TaskRepository taskRepository,
                               WorkspaceMemberRepository memberRepository,
                               PasswordEncoder passwordEncoder,
                               DataSource dataSource) {
        return args -> {
            try (java.sql.Connection conn = dataSource.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {

                var rs = stmt.executeQuery(
                    "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'workspaces' AND column_name = 'title')"
                );
                rs.next();
                boolean hasTitle = rs.getBoolean(1);

                if (!hasTitle) {
                    System.out.println("SchemaFix: title column MISSING — rebuilding all tables...");

                    String[] drops = {
                        "DROP TABLE IF EXISTS task_assignees CASCADE",
                        "DROP TABLE IF EXISTS task_labels CASCADE",
                        "DROP TABLE IF EXISTS tasks CASCADE",
                        "DROP TABLE IF EXISTS board_columns CASCADE",
                        "DROP TABLE IF EXISTS workspace_members CASCADE",
                        "DROP TABLE IF EXISTS labels CASCADE",
                        "DROP TABLE IF EXISTS workspaces CASCADE",
                        "DROP TABLE IF EXISTS users CASCADE"
                    };
                    for (String sql : drops) stmt.execute(sql);

                    String[] creates = {
                        "CREATE TABLE users (id VARCHAR(255) PRIMARY KEY, name VARCHAR(255) NOT NULL UNIQUE, full_name VARCHAR(255), email VARCHAR(255) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL, role VARCHAR(255) NOT NULL DEFAULT 'USER')",
                        "CREATE TABLE workspaces (id VARCHAR(255) PRIMARY KEY, title VARCHAR(255) NOT NULL, bg_color VARCHAR(255), description TEXT, owner_id VARCHAR(255) NOT NULL)",
                        "CREATE TABLE board_columns (id VARCHAR(255) PRIMARY KEY, workspace_id VARCHAR(255) NOT NULL, title VARCHAR(255) NOT NULL, column_order INTEGER NOT NULL DEFAULT 0, is_default BOOLEAN NOT NULL DEFAULT FALSE)",
                        "CREATE TABLE labels (id VARCHAR(255) PRIMARY KEY, workspace_id VARCHAR(255) NOT NULL, name VARCHAR(255) NOT NULL, color VARCHAR(255) NOT NULL)",
                        "CREATE TABLE tasks (id VARCHAR(255) PRIMARY KEY, public_id VARCHAR(255) NOT NULL, workspace_id VARCHAR(255) NOT NULL, column_id VARCHAR(255) NOT NULL, title VARCHAR(255) NOT NULL, description TEXT, task_order INTEGER NOT NULL DEFAULT 0)",
                        "CREATE TABLE task_labels (task_id VARCHAR(255) NOT NULL, label_id VARCHAR(255) NOT NULL, PRIMARY KEY (task_id, label_id))",
                        "CREATE TABLE task_assignees (task_id VARCHAR(255) NOT NULL, user_id VARCHAR(255) NOT NULL, PRIMARY KEY (task_id, user_id))",
                        "CREATE TABLE workspace_members (workspace_id VARCHAR(255) NOT NULL, user_id VARCHAR(255) NOT NULL, role VARCHAR(255) NOT NULL, PRIMARY KEY (workspace_id, user_id))"
                    };
                    for (String sql : creates) stmt.execute(sql);

                    System.out.println("SchemaFix: ALL tables recreated successfully");
                } else {
                    System.out.println("SchemaFix: schema OK");
                }

                stmt.execute("DELETE FROM task_assignees WHERE task_id IN (SELECT id FROM tasks WHERE workspace_id IN (SELECT id FROM workspaces WHERE title IS NULL))");
                stmt.execute("DELETE FROM task_labels WHERE task_id IN (SELECT id FROM tasks WHERE workspace_id IN (SELECT id FROM workspaces WHERE title IS NULL))");
                stmt.execute("DELETE FROM tasks WHERE workspace_id IN (SELECT id FROM workspaces WHERE title IS NULL)");
                stmt.execute("DELETE FROM board_columns WHERE workspace_id IN (SELECT id FROM workspaces WHERE title IS NULL)");
                stmt.execute("DELETE FROM workspace_members WHERE workspace_id IN (SELECT id FROM workspaces WHERE title IS NULL)");
                stmt.execute("DELETE FROM labels WHERE workspace_id IN (SELECT id FROM workspaces WHERE title IS NULL)");
                stmt.execute("DELETE FROM workspaces WHERE title IS NULL");
                System.out.println("Cleanup: done");

            } catch (Exception e) {
                System.out.println("SchemaFix/Cleanup error: " + e.getMessage());
            }

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
