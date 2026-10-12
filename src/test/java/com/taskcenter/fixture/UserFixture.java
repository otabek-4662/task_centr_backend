package com.taskcenter.fixture;

import com.taskcenter.model.User;

import java.util.UUID;

/**
 * RealWorld Test Data Builder naqshi bo'yicha User obyektlarini yaratish fixturasi.
 */
public class UserFixture {

    public static UserBuilder aUser() {
        String randomSuffix = UUID.randomUUID().toString().substring(0, 8);
        return new UserBuilder()
                .id(UUID.randomUUID().toString())
                .name("user_" + randomSuffix)
                .fullName("Test User " + randomSuffix)
                .email("user_" + randomSuffix + "@taskcenter.test")
                .password("$2a$10$dummyHashedPasswordForTestEnvironmentOnly123456")
                .role(User.Role.USER);
    }

    public static class UserBuilder {
        private String id;
        private String name;
        private String fullName;
        private String email;
        private String password;
        private User.Role role;

        public UserBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UserBuilder name(String name) {
            this.name = name;
            return this;
        }

        public UserBuilder fullName(String fullName) {
            this.fullName = fullName;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder role(User.Role role) {
            this.role = role;
            return this;
        }

        public User build() {
            return User.builder()
                    .id(id != null ? id : UUID.randomUUID().toString())
                    .name(name)
                    .fullName(fullName)
                    .email(email)
                    .password(password)
                    .role(role != null ? role : User.Role.USER)
                    .build();
        }
    }
}
