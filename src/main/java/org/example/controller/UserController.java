package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Users")
@ApiResponse(responseCode = "401", description = "Missing or invalid credentials",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class UserController {

    private final UserRepo userRepo;

    public UserController(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping("/getUsers")
    @Operation(summary = "List all users, newest first")
    @ApiResponse(responseCode = "200", description = "All users",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = User.class))))
    public List<User> findAllUser() {
        return userRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @GetMapping("/getUser/{id}")
    @Operation(summary = "Get a user by id")
    @ApiResponse(responseCode = "200", description = "The user",
            content = @Content(schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "400", description = "Id is not a number",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No user with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public User findAUser(@Parameter(description = "User id") @PathVariable Integer id) {
        return findUserOrThrow(id);
    }

    @PostMapping("/addUser")
    @Operation(summary = "Create a user")
    @ApiResponse(responseCode = "201", description = "User created",
            content = @Content(schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "400", description = "Blank or missing name, or malformed JSON",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Request body is not application/json",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<User> insertUser(@Valid @RequestBody UserRequest request) {
        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        return ResponseEntity.status(HttpStatus.CREATED).body(userRepo.save(user));
    }

    @PutMapping("/updateUser/{id}")
    @Operation(summary = "Update a user's names")
    @ApiResponse(responseCode = "200", description = "The updated user",
            content = @Content(schema = @Schema(implementation = User.class)))
    @ApiResponse(responseCode = "400", description = "Blank or missing name, or malformed JSON, or id is not a number",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Request body is not application/json",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No user with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public User updateUser(@Parameter(description = "User id") @PathVariable Integer id,
                           @Valid @RequestBody UserRequest request) {
        User user = findUserOrThrow(id);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        return userRepo.save(user);
    }

    @DeleteMapping("/deleteUser/{id}")
    @Operation(summary = "Delete a user")
    @ApiResponse(responseCode = "204", description = "User deleted", content = @Content)
    @ApiResponse(responseCode = "400", description = "Id is not a number",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "No user with this id",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteUser(@Parameter(description = "User id") @PathVariable Integer id) {
        userRepo.delete(findUserOrThrow(id));
        return ResponseEntity.noContent().build();
    }

    private User findUserOrThrow(Integer id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not exist with id: " + id));
    }
}
