package org.example.controller;

import org.example.config.LoggingUtil;
import org.example.entities.User;
import org.example.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@RestController
@RequestMapping("/api")
public class UserController {
    Logger logger = LoggerFactory.getLogger(LoggingUtil.class);

    @Autowired
    private UserRepo userRepo;
    private final Map<Integer, User> users = new HashMap<>();

    @GetMapping("/getUsers")
    public List<User> findAllUser() {
        return userRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @GetMapping("/getUser/{id}")
    public Optional<User> findAUser(@PathVariable Integer id)
    {
        return userRepo.findById(id);
    }

    @PostMapping("/addUser")
    public ResponseEntity<User> insertUser(@RequestBody User user) {
        User insertUser = userRepo.save(user);
        return ResponseEntity.ok(insertUser);
    }

    @PutMapping("/updateUser/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User user) {
        User updateUser = userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not exist with id: " + id));

        updateUser.setFirstName(user.firstName());
        updateUser.setLastName(user.lastName());
        updateUser.setUpdatedAt(user.updated_at());
        users.put(id, updateUser);
        userRepo.save(updateUser);
        return ResponseEntity.ok(updateUser);
    }

    @DeleteMapping("/deleteUser/{id}")
    public void deleteUser(@PathVariable Integer id) {
            User deleteUser = userRepo.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User not exist with id: " + id));
            userRepo.delete(deleteUser);

    }
}
