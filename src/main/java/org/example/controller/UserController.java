package org.example.controller;

import org.example.config.LoggingUtil;
import org.example.entities.User;
import org.example.repo.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {
    Logger logger = LoggerFactory.getLogger(LoggingUtil.class);

    @Autowired
    private UserRepo userRepo;
    private final Map<Integer, User> users = new HashMap<>();

    @GetMapping("/getUsers")
    public List<User> findAllUser() {
        return userRepo.findAll();
    }

    @PostMapping("/saveUser")
    public void insertUser(@RequestBody User user) {
        userRepo.save(user);
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
}
