package com.example.gymtrackerserver;

import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

// TODO: remove cross origin annotation
@CrossOrigin(origins = "http://127.0.0.1:5173")
@RestController
public class WorkoutController {

    // jdbc:postgresql://<database_host>:<port>/<database_name>
    private final String url = "jdbc:postgresql://gym-tracker.ctvjp0tbn6qi.eu-west-2.rds.amazonaws.com:4323/";
    private final String user = "postgres";
    private final String password = "#V%C9Cc&P7jh59zB";

    final int userID = 15;

    @PostMapping("/workout")
    Workout newWorkout(@RequestBody Workout newWorkout) {
        String workoutSQL = "INSERT INTO workout(user_id, duration, timestamp) VALUES(?, ?, ?)";
        String setSQL = "INSERT INTO set(workout_id, exercise_id, type, weight, reps) VALUES(?, ?, ?, ?, ?)";
        int workoutID;

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);

            PreparedStatement pstmt = conn.prepareStatement(workoutSQL, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, userID);
            pstmt.setInt(2, newWorkout.duration());
            pstmt.setTimestamp(3, newWorkout.date());

            pstmt.execute();
            ResultSet rs = pstmt.getGeneratedKeys();
            rs.next();
            workoutID = (int) rs.getLong(1);

            for (Exercise exercise : newWorkout.exercises()) {
                for (Set set : exercise.sets()) {
                    System.out.println(set);
                    pstmt = conn.prepareStatement(setSQL);
                    pstmt.setInt(1, workoutID);
                    pstmt.setInt(2, exercise.id());
                    pstmt.setInt(3, set.type());
                    pstmt.setFloat(4, set.weight());
                    pstmt.setInt(5, set.reps());
                    pstmt.execute();
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return newWorkout;
    }

    @GetMapping("/workout")
    Workout[] workouts() {
        List<Workout> workouts = new ArrayList<>();

        String workoutSQL = "SELECT * FROM workout WHERE user_id = ?";
        String setSQL = "SELECT workout_id, exercise_id, type, weight, reps"
                + " FROM set INNER JOIN workout ON workout.id = set.workout_id"
                + " WHERE user_id = ?";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);

            PreparedStatement workoutPstmt = conn.prepareStatement(workoutSQL);
            workoutPstmt.setInt(1, userID);
            ResultSet workoutRS = workoutPstmt.executeQuery();

            PreparedStatement setPstmt = conn.prepareStatement(setSQL, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
            setPstmt.setInt(1, userID);
            ResultSet setRS = setPstmt.executeQuery();

            while (workoutRS.next()) {
                int workoutID = workoutRS.getInt("id");

                // Linked hash map used as it keeps the elements in the order they were inserted
                Map<Integer, List<Set>> exercisesAndSets = new LinkedHashMap<>();
                while (setRS.next()) {
                    if (setRS.getInt("workout_id") == workoutID) {

                        int exerciseID = setRS.getInt("exercise_id");

                        int type = setRS.getInt("type");
                        float weight = setRS.getFloat("weight");
                        int reps = setRS.getInt("reps");

                        if (!exercisesAndSets.containsKey(exerciseID)) {
                            exercisesAndSets.put(exerciseID, new ArrayList<>());
                        }
                        exercisesAndSets.get(exerciseID).add(new Set(type, weight, reps));
                    }
                }

                // Reset result set of exercise sets back to beginning for next loop
                setRS.beforeFirst();

                List<Exercise> exercisesList = new ArrayList<>();
                for (Integer exerciseID : exercisesAndSets.keySet()) {

                    List<Set> setsList = exercisesAndSets.get(exerciseID);
                    Set[] sets = setsList.toArray(new Set[0]);

                    exercisesList.add(new Exercise(exerciseID, sets));
                }
                Exercise[] exercises = exercisesList.toArray(new Exercise[0]);

                workouts.add(
                        new Workout(
                                workoutRS.getTimestamp("timestamp"),
                                workoutRS.getInt("duration"),
                                exercises
                        )
                );
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return workouts.toArray(new Workout[0]);
    }

    @PostMapping("user")
    Connection newUser(@RequestBody User newUser) {
        String SQL = "INSERT INTO \"user\" (email, name, age, password) VALUES (?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(url, user, password);

            PreparedStatement pstmt = conn.prepareStatement(SQL);

            pstmt.setString(1, newUser.email());
            pstmt.setString(2, newUser.name());
            pstmt.setInt(3, newUser.age());
            pstmt.setString(4, newUser.password());

            pstmt.execute();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }
}
