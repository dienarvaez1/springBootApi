package org.example.cucumber;

import io.cucumber.datatable.DataTable;
import org.example.entities.User;
import org.example.repo.UserRepo;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/** Makes a mocked {@link UserRepo} behave like a repository holding a list of users. */
final class UserRepoStubs {

    private UserRepoStubs() {
    }

    /** Stubs the repository to read from {@code storedUsers}, which steps may keep changing. */
    static void backWith(UserRepo userRepo, List<User> storedUsers) {
        when(userRepo.findById(anyInt())).thenAnswer(inv -> storedUsers.stream()
                .filter(u -> u.getId().equals(inv.getArgument(0)))
                .findFirst());
        when(userRepo.findAll(any(Sort.class))).thenAnswer(inv -> List.copyOf(storedUsers));
        // Return a copy so captured arguments keep exactly what the controller passed in
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User input = inv.getArgument(0);
            int id = input.getId() != null ? input.getId() : storedUsers.size() + 1;
            return user(id, input.getFirstName(), input.getLastName());
        });
    }

    /** Reads a table with id, firstName and lastName columns. */
    static List<User> usersFrom(DataTable table) {
        return table.asMaps().stream()
                .map(row -> user(Integer.parseInt(row.get("id")), row.get("firstName"), row.get("lastName")))
                .toList();
    }

    static User user(int id, String firstName, String lastName) {
        User user = new User();
        user.setId(id);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }
}
