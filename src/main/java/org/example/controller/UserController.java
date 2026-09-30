package org.example.controller;

import jakarta.validation.Valid;
import org.example.entities.User;
import org.example.repo.UserRepo;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepo userRepo;

    public UserController(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/getUsers")
    public List<User> findAllUser() {
        return userRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @GetMapping("/getUser/{id}")
    public User findAUser(@PathVariable Integer id) {
        return findUserOrThrow(id);
    }

    @PostMapping("/addUser")
    public ResponseEntity<User> insertUser(@Valid @RequestBody UserRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        return ResponseEntity.status(HttpStatus.CREATED).body(userRepo.save(user));
    }

    @PutMapping("/updateUser/{id}")
    public User updateUser(@PathVariable Integer id, @Valid @RequestBody UserRequest request) {
        User user = findUserOrThrow(id);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        return userRepo.save(user);
    }

    @DeleteMapping("/deleteUser/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userRepo.delete(findUserOrThrow(id));
        return ResponseEntity.noContent().build();
    }

    private User findUserOrThrow(Integer id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not exist with id: " + id));
    }
}
